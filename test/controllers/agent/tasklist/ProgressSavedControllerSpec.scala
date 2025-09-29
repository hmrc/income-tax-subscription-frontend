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

import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import models.audits.SaveAndComebackAuditing.SaveAndComeBackAuditModel
import models.common.business._
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel, TimestampModel}
import models.{Current, DateModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, status}
import play.api.{Configuration, Environment}
import services.mocks._
import uk.gov.hmrc.http.InternalServerException
import utilities.{AccountingPeriodUtil, CacheExpiryDateProvider, MockCurrentDateProvider}
import views.agent.tasklist.mocks.MockProgressSaved

import java.time.{LocalDate, LocalDateTime, LocalTime}
import scala.concurrent.Future

class ProgressSavedControllerSpec extends ControllerSpec
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockAuditingService
  with MockCurrentDateProvider
  with MockSubscriptionDetailsService
  with MockProgressSaved {

  "show" when {
    "no last updated timestamp could be found" should {
      "throw an exception" in new Setup {
        mockFetchLastUpdatedTimestamp(None)

        intercept[InternalServerException](await(controller.show()(request)))
          .message mustBe "[ProgressSavedController][show] - The last updated timestamp cannot be retrieved"
      }
    }
    "a last updated timestamp was returned" when {
      "no location was provided" should {
        "display the page" in new Setup {
          mockFetchLastUpdatedTimestamp(Some(TimestampModel(dateTime)))
          mockProgressSaved(fakeExpiryDate, clientDetails)

          val result: Future[Result] = controller.show()(request)

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
      }
      "location was provided" should {
        "audit the details of the users journey and display the page" when {
          "the user has full journey details" in new Setup {
            mockFetchLastUpdatedTimestamp(Some(TimestampModel(dateTime)))
            mockProgressSaved(fakeExpiryDate, clientDetails)
            mockCurrentDate(LocalDate.now())
            mockFetchAllSelfEmployments(selfEmployments)
            mockFetchProperty(Some(ukProperty))
            mockFetchOverseasProperty(Some(foreignProperty))
            mockFetchSelectedTaxYear(Some(accountingYear))

            val result: Future[Result] = controller.show(location = Some("test-location"))(request)

            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)

            verifyAudit(SaveAndComeBackAuditModel(
              userType = "agent",
              utr = utr,
              nino = nino,
              maybeAgentReferenceNumber = Some(testARN),
              saveAndRetrieveLocation = "test-location",
              currentTaxYear = AccountingPeriodUtil.getTaxEndYear(LocalDate.now),
              selectedTaxYear = Some(accountingYear),
              selfEmployments = selfEmployments,
              maybePropertyModel = Some(ukProperty),
              maybeOverseasPropertyModel = Some(foreignProperty)
            ))
          }
          "the user has minimal data stored" in new Setup {
            mockFetchLastUpdatedTimestamp(Some(TimestampModel(dateTime)))
            mockProgressSaved(fakeExpiryDate, clientDetails)
            mockCurrentDate(LocalDate.now())
            mockFetchAllSelfEmployments(Seq.empty)
            mockFetchProperty(None)
            mockFetchOverseasProperty(None)
            mockFetchSelectedTaxYear(None)

            val result: Future[Result] = controller.show(location = Some("test-location"))(request)

            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)

            verifyAudit(SaveAndComeBackAuditModel(
              userType = "agent",
              utr = utr,
              nino = nino,
              maybeAgentReferenceNumber = Some(testARN),
              saveAndRetrieveLocation = "test-location",
              currentTaxYear = AccountingPeriodUtil.getTaxEndYear(LocalDate.now),
              selectedTaxYear = None,
              selfEmployments = Seq.empty,
              maybePropertyModel = None,
              maybeOverseasPropertyModel = None
            ))
          }
        }
      }
    }
  }

  trait Setup {
    val mockCacheExpiryDateProvider: CacheExpiryDateProvider = mock[CacheExpiryDateProvider]
    val mockConfiguration: Configuration = mock[Configuration]
    val mockEnvironment: Environment = mock[Environment]

    val fakeExpiryDate: String = "1st January 2020"

    when(mockCacheExpiryDateProvider.expiryDateOf(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(fakeExpiryDate)

    val controller: ProgressSavedController = new ProgressSavedController(
      identify = fakeIdentifierAction,
      journeyRefiner = fakeConfirmedClientJourneyRefiner,
      auditingService = mockAuditingService,
      cacheExpiryDateProvider = mockCacheExpiryDateProvider,
      currentDateProvider = mockCurrentDateProvider,
      subscriptionDetailsService = mockSubscriptionDetailsService,
      view = mockView
    )(mockConfiguration, mockEnvironment) {
      override def ggLoginUrl: String = "/"
    }
  }

  lazy val dateTime: LocalDateTime = LocalDateTime.of(LocalDate.now, LocalTime.of(0, 0))

  lazy val selfEmployments: Seq[SelfEmploymentData] = Seq(
    selfEmployment
  )

  lazy val selfEmployment: SelfEmploymentData = SelfEmploymentData(
    id = "test-id",
    startDateBeforeLimit = Some(false),
    businessStartDate = Some(BusinessStartDate(DateModel.dateConvert(LocalDate.now))),
    businessName = Some(BusinessNameModel("test-name")),
    businessTradeName = Some(BusinessTradeNameModel("test-trade")),
    businessAddress = Some(BusinessAddressModel(Address(
      lines = Seq("1 long road"),
      postcode = Some("ZZ1 1ZZ")
    )))
  )

  lazy val ukProperty: PropertyModel = PropertyModel(
    startDateBeforeLimit = Some(false),
    startDate = Some(DateModel.dateConvert(LocalDate.now))
  )

  lazy val foreignProperty: OverseasPropertyModel = OverseasPropertyModel(
    startDateBeforeLimit = Some(false),
    startDate = Some(DateModel.dateConvert(LocalDate.now))
  )

  lazy val accountingYear: AccountingYearModel = AccountingYearModel(Current)

}
