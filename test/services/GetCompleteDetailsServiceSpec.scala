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

package services

import config.{AppConfig, MockConfig}
import models.common.business._
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel}
import models.status.MandationStatus.Voluntary
import models.{Current, DateModel, EligibilityStatus}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.GetCompleteDetailsService._
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.AccountingPeriodUtil

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetCompleteDetailsServiceSpec extends PlaySpec with Matchers with MockSubscriptionDetailsService {

  val appConfig: AppConfig = MockConfig

  trait Setup {
    val service: GetCompleteDetailsService = new GetCompleteDetailsService(
      subscriptionDetailsService = mockSubscriptionDetailsService,
      appConfig = MockConfig
    )
  }

  val hc: HeaderCarrier = HeaderCarrier()

  val startDateLimit: LocalDate = AccountingPeriodUtil.getStartDateLimit

  val selfEmployment: SelfEmploymentData = SelfEmploymentData(
    id = "test-id",
    startDateBeforeLimit = Some(false),
    businessStartDate = Some(BusinessStartDate(DateModel.dateConvert(startDateLimit))),
    businessName = Some(BusinessNameModel("ABC Limited")),
    businessTradeName = Some(BusinessTradeNameModel("Plumbing")),
    businessAddress = Some(BusinessAddressModel(address = Address(
      lines = Seq("1 Long Road", "Lonely city"),
      postcode = Some("ZZ11ZZ")
    ))),
    confirmed = true
  )

  val ukProperty: PropertyModel = PropertyModel(
    startDate = Some(DateModel.dateConvert(startDateLimit.plusDays(1))),
    confirmed = true
  )

  val foreignProperty: OverseasPropertyModel = OverseasPropertyModel(
    startDate = Some(DateModel.dateConvert(startDateLimit.plusDays(2))),
    confirmed = true
  )

  val accountingYear: AccountingYearModel = AccountingYearModel(Current)

  val completeDetails: CompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = Some(SoleTraderBusinesses(
        businesses = Seq(SoleTraderBusiness(
          id = "test-id",
          name = "ABC Limited",
          trade = "Plumbing",
          startDate = Some(startDateLimit),
          address = Address(
            lines = Seq("1 Long Road", "Lonely city"),
            postcode = Some("ZZ11ZZ")
          )
        ))
      )),
      ukProperty = Some(UKProperty(
        startDate = Some(startDateLimit.plusDays(1))
      )),
      foreignProperty = Some(ForeignProperty(
        startDate = Some(startDateLimit.plusDays(2))
      ))
    ),
    taxYear = AccountingYearModel(Current)
  )

  val completeDetailsNoDates: CompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = Some(SoleTraderBusinesses(
        businesses = Seq(SoleTraderBusiness(
          id = "test-id",
          name = "ABC Limited",
          trade = "Plumbing",
          startDate = None,
          address = Address(
            lines = Seq("1 Long Road", "Lonely city"),
            postcode = Some("ZZ11ZZ")
          )
        ))
      )),
      ukProperty = Some(UKProperty(
        startDate = None
      )),
      foreignProperty = Some(ForeignProperty(
        startDate = None
      ))
    ),
    taxYear = AccountingYearModel(Current)
  )

  "getCompleteSignUpDetails" must {
    "return a complete details model" when {
      "all fetches were successful and are full + confirmed data sets" in new Setup {
        mockFetchAllSelfEmployments(Seq(selfEmployment))
        mockFetchProperty(Some(ukProperty))
        mockFetchOverseasProperty(Some(foreignProperty))
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockFetchSelectedTaxYear(Some(accountingYear))

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference")(hc)
        }

        await(result) mustBe Right(completeDetails)
      }
      "all fetches were successful and all have selected their start dates are before the limit" in new Setup {
        mockFetchAllSelfEmployments(Seq(selfEmployment.copy(startDateBeforeLimit = Some(true))))
        mockFetchProperty(Some(ukProperty.copy(startDateBeforeLimit = Some(true))))
        mockFetchOverseasProperty(Some(foreignProperty.copy(startDateBeforeLimit = Some(true))))
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockFetchSelectedTaxYear(Some(accountingYear))

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference")(hc)
        }

        await(result) mustBe Right(completeDetailsNoDates)
      }
      "all fetches were successful, all have selected their start dates are after the limit, but their stored date is older than the limit" in new Setup {
        mockFetchAllSelfEmployments(Seq(
          selfEmployment.copy(
            startDateBeforeLimit = Some(false),
            businessStartDate = Some(BusinessStartDate(DateModel.dateConvert(startDateLimit.minusDays(1))))
          ))
        )
        mockFetchProperty(Some(ukProperty.copy(
          startDateBeforeLimit = Some(false),
          startDate = Some(DateModel.dateConvert(startDateLimit.minusDays(1)))
        )))
        mockFetchOverseasProperty(Some(foreignProperty.copy(
          startDateBeforeLimit = Some(false),
          startDate = Some(DateModel.dateConvert(startDateLimit.minusDays(1)))
        )))
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockFetchSelectedTaxYear(Some(accountingYear))

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference")(hc)
        }

        await(result) mustBe Right(completeDetailsNoDates)
      }
    }

    "return a GetCompleteDetailsFailure" when {
      "all income sources are complete" when {
        "self employment hasn't been confirmed" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment.copy(confirmed = false)))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockGetMandationService(Voluntary, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "uk property hasn't been confirmed" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment))
          mockFetchProperty(Some(ukProperty.copy(confirmed = false)))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockGetMandationService(Voluntary, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "foreign property hasn't been confirmed" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty.copy(confirmed = false)))
          mockGetMandationService(Voluntary, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
      }

      "there is no selected tax year" in new Setup {
        mockFetchAllSelfEmployments(Seq(selfEmployment))
        mockFetchProperty(Some(ukProperty))
        mockFetchOverseasProperty(Some(foreignProperty))
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockFetchSelectedTaxYear(None)

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference")(hc)
        }

        await(result) mustBe Left(GetCompleteDetailsFailure)
      }
      "there is a sole trader business" which {
        "does not have a name" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment.copy(businessName = None)))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockGetMandationService(Voluntary, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "does not have a trade" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment.copy(businessTradeName = None)))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockGetMandationService(Voluntary, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "does not have a start date" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment.copy(businessStartDate = None)))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockGetMandationService(Voluntary, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "does not have an address" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment.copy(businessAddress = None)))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockGetMandationService(Voluntary, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
      }
      "there is a uk property business" which {
        "does not have a start date" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment))
          mockFetchProperty(Some(ukProperty.copy(startDate = None)))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockGetMandationService(Voluntary, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
      }
      "there is a foreign property business" which {
        "does not have a start date" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty.copy(startDate = None)))
          mockGetMandationService(Voluntary, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
      }
    }
  }

}
