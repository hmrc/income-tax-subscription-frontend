/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.individual.business

import agent.audit.mocks.MockAuditingService
import common.Constants
import controllers.ControllerBaseSpec
import models.audits.SaveAndComebackAuditing
import models.audits.SaveAndComebackAuditing.SaveAndComeBackAuditModel
import models.common.business._
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel, TimestampModel}
import models.{Cash, DateModel, Next}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.Configuration
import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import utilities.TestModels.testCacheMap
import utilities.individual.TestConstants.testCredId
import utilities.{CacheExpiryDateProvider, CurrentDateProvider}
import views.html.individual.incometax.business.ProgressSaved

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class ProgressSavedControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockIncomeTaxSubscriptionConnector {
  override val controllerName: String = "ProgressSavedController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()
  implicit lazy val config: Configuration = app.injector.instanceOf[Configuration]

  override def mockNinoAndUtrRetrieval(): Unit = {
    val ninoEnrolment = Enrolment(
      Constants.ninoEnrolmentName,
      Seq(EnrolmentIdentifier(Constants.ninoEnrolmentIdentifierKey, "GA714758A")),
      "Activated"
    )

    val utrEnrolment = Enrolment(
      Constants.utrEnrolmentName,
      Seq(EnrolmentIdentifier(Constants.utrEnrolmentIdentifierKey, "4f7d0ced-89ed-4488-859d-a51ec979a9a6")),
      "Activated"
    )
    mockRetrievalSuccess(
      new ~(
        new ~(new ~(new ~(Enrolments(Set(ninoEnrolment, utrEnrolment)), Some(AffinityGroup.Individual)), Some(User)), testConfidenceLevel),
        Some(Credentials(testCredId, ""))
      )
    )
  }

  private val testTimestamp = TimestampModel(
    LocalDateTime.of(1970, 1, 1, 1, 0, 0, 0)
  )

  private val currentYear = 2023
  private val selectedTaxYear = Some(AccountingYearModel(Next))
  private val selfEmployments = Seq(
    SelfEmploymentData(
      id = "id",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name")),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel("123", Address(Seq("line 1"), Some("ZZ1 1ZZ"))))
    )
  )
  private val selfEmploymentAccountingMethod = Some(AccountingMethodModel(Cash))
  private val property = Some(PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("1", "1", "1980")),
    confirmed = true
  ))
  private val overseasProperty = Some(OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("1", "1", "1980")),
    confirmed = true
  ))

  "signInUrl" should {
    "return the sign in url" in withController { (controller, _) =>
      controller.signInUrl mustBe "/bas-gateway/sign-in"
    }
  }

  "Show" should {
    "return status OK and render the correct expiry date" when {
      "the location parameter is not provided" in withController { (controller, mockedView) =>
        mockFetchLastUpdatedTimestamp(Some(testTimestamp))

        val result: Future[Result] = await(controller.show()(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)


        verify(mockedView).apply(meq("Monday, 20 October 2021"), any())(any(), any(), any())
      }

      "the location parameter is provided" in withController { (controller, mockedView) =>
        mockNinoAndUtrRetrieval()
        mockFetchLastUpdatedTimestamp(Some(testTimestamp))
        val testBusinessCacheMap = testCacheMap(
          selectedTaxYear = selectedTaxYear
        )
        mockFetchAllFromSubscriptionDetails(Some(testBusinessCacheMap))
        mockGetSelfEmploymentsSeq[SelfEmploymentData](BusinessesKey)(selfEmployments)
        mockGetSelfEmployments[AccountingMethodModel](BusinessAccountingMethod)(selfEmploymentAccountingMethod)
        mockFetchProperty(property)
        mockFetchOverseasProperty(overseasProperty)

        val result: Future[Result] = await(controller.show(location = Some("test-location"))(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)


        verify(mockedView).apply(meq("Monday, 20 October 2021"), any())(any(), any(), any())

        verifyAudit(SaveAndComeBackAuditModel(
          userType = SaveAndComebackAuditing.individualUserType,
          utr = "4f7d0ced-89ed-4488-859d-a51ec979a9a6",
          nino = "GA714758A",
          saveAndRetrieveLocation = "test-location",
          currentTaxYear = currentYear,
          selectedTaxYear = selectedTaxYear,
          selfEmployments = selfEmployments,
          maybeSelfEmploymentAccountingMethod = selfEmploymentAccountingMethod,
          maybePropertyModel = property,
          maybeOverseasPropertyModel = overseasProperty
        ))
      }
    }

    "throw an exception if the last updated timestamp cannot be retrieve" in withController { (controller, _) =>
      mockFetchLastUpdatedTimestamp(None)

      val result: Future[Result] = await(controller.show()(subscriptionRequest))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
    }
  }

  private def withController(testCode: (ProgressSavedController, ProgressSaved) => Any): Unit = {
    val progressSavedView = mock[ProgressSaved]

    when(progressSavedView(meq("Monday, 20 October 2021"), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val cacheExpiryDateProvider = mock[CacheExpiryDateProvider]
    val currentDateProvider = mock[CurrentDateProvider]

    when(cacheExpiryDateProvider.expiryDateOf(any())(any()))
      .thenReturn("Monday, 20 October 2021")

    when(currentDateProvider.getCurrentDate)
      .thenReturn(LocalDate.of(2022, 5, 6))

    val controller = new ProgressSavedController(
      progressSavedView,
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockIncomeTaxSubscriptionConnector,
      currentDateProvider,
      cacheExpiryDateProvider
    )

    testCode(controller, progressSavedView)
  }
}
