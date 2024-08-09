/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.agent.tasklist

import auth.agent.AgentSignUp
import common.Constants.ITSASessionKeys
import controllers.agent.AgentControllerBaseSpec
import models.audits.SaveAndComebackAuditing
import models.audits.SaveAndComebackAuditing.SaveAndComeBackAuditModel
import models.common.business._
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel, TimestampModel}
import models.status.MandationStatus.Voluntary
import models.{Cash, DateModel, EligibilityStatus, Next}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.Configuration
import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, status}
import play.twirl.api.HtmlFormat
import services.mocks._
import utilities.UserMatchingSessionUtil.{firstName, lastName}
import utilities.agent.TestConstants.{testARN, testNino, testUtr}
import utilities.{CacheExpiryDateProvider, CurrentDateProvider}
import views.html.agent.tasklist.ProgressSaved

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class ProgressSavedControllerSpec extends AgentControllerBaseSpec
  with MockAuditingService
  with MockReferenceRetrieval
  with MockClientDetailsRetrieval
  with MockUTRService
  with MockSubscriptionDetailsService {

  override val controllerName: String = "ProgressSavedController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()
  implicit lazy val config: Configuration = app.injector.instanceOf[Configuration]

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
      businessAddress = Some(BusinessAddressModel(Address(Seq("line 1"), Some("ZZ1 1ZZ"))))
    )
  )

  private val decryptedSelfEmployments = Seq(
    SelfEmploymentData(
      id = "id",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name")),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel(Address(Seq("line 1"), Some("ZZ1 1ZZ"))))
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

  "show" should {
    "return OK with progress saved page" when {
      "the location parameter is not provided" in withController { (controller, mockedView) =>
        mockFetchLastUpdatedTimestamp(Some(testTimestamp))

        val result: Future[Result] = await(controller.show()(subscriptionRequestWithName))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)

        verify(mockedView).apply(meq("Monday, 20 October 2021"), meq("/bas-gateway/sign-in"), any())(any(), any())
      }

      "the saveAndRetrieveLocation parameter is provided" in withController { (controller, mockedView) =>
        mockFetchLastUpdatedTimestamp(Some(testTimestamp))
        mockFetchLastUpdatedTimestamp(Some(testTimestamp))
        mockFetchAllSelfEmployments(selfEmployments, selfEmploymentAccountingMethod.map(_.accountingMethod))
        mockFetchProperty(property)
        mockFetchOverseasProperty(overseasProperty)
        mockFetchSelectedTaxYear(selectedTaxYear)
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockGetUTR(testUtr)

        val testRequest = FakeRequest().withSession(
          ITSASessionKeys.JourneyStateKey -> AgentSignUp.name,
          ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true",
          firstName -> "FirstName",
          lastName -> "LastName"
        ).withMethod("POST")

        val result: Future[Result] = await(controller.show(location = Some("test-location"))(testRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)

        verify(mockedView).apply(meq("Monday, 20 October 2021"), meq("/bas-gateway/sign-in"), any())(any(), any())

        verifyAudit(SaveAndComeBackAuditModel(
          userType = SaveAndComebackAuditing.agentUserType,
          utr = testUtr,
          nino = testNino,
          maybeAgentReferenceNumber = Some(testARN),
          saveAndRetrieveLocation = "test-location",
          currentTaxYear = currentYear,
          selectedTaxYear = selectedTaxYear,
          selfEmployments = decryptedSelfEmployments,
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

    when(progressSavedView(meq("Monday, 20 October 2021"), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)

    val cacheExpiryDateProvider = mock[CacheExpiryDateProvider]
    val currentDateProvider = mock[CurrentDateProvider]

    when(cacheExpiryDateProvider.expiryDateOf(any())(any()))
      .thenReturn("Monday, 20 October 2021")

    when(currentDateProvider.getCurrentDate)
      .thenReturn(LocalDate.of(2022, 5, 6))

    val controller = new ProgressSavedController(
      progressSavedView,
      currentDateProvider,
      MockSubscriptionDetailsService,
      mockReferenceRetrieval,
      mockUTRService,
      mockClientDetailsRetrieval,
      cacheExpiryDateProvider
    )(
      mockAuditingService,
      mockAuthService,
      appConfig,
      config,
      env
    )

    testCode(controller, progressSavedView)
  }
}
