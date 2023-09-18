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

import models.common.business._
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel}
import models.{Cash, Current, DateModel}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.GetCompleteDetailsService._
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class GetCompleteDetailsServiceSpec extends PlaySpec with Matchers with MockSubscriptionDetailsService {

  trait Setup {
    val service: GetCompleteDetailsService = new GetCompleteDetailsService(
      subscriptionDetailsService = MockSubscriptionDetailsService
    )
  }

  val selfEmployment: SelfEmploymentData = SelfEmploymentData(
    id = "test-id",
    businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
    businessName = Some(BusinessNameModel("ABC Limited").encrypt(crypto.QueryParameterCrypto)),
    businessTradeName = Some(BusinessTradeNameModel("Plumbing")),
    businessAddress = Some(BusinessAddressModel(address = Address(
      lines = Seq("1 Long Road", "Lonely city"),
      postcode = Some("ZZ11ZZ")
    )).encrypt(crypto.QueryParameterCrypto)),
    confirmed = true
  )

  val selfEmploymentsAccountingMethod: AccountingMethodModel = AccountingMethodModel(Cash)

  val ukProperty: PropertyModel = PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("2", "1", "1980"))
  )

  val foreignProperty: OverseasPropertyModel = OverseasPropertyModel(
    accountingMethod = Some(Cash),
    count = None,
    startDate = Some(DateModel("3", "1", "1980"))
  )

  val accountingYear: AccountingYearModel = AccountingYearModel(Current)

  val completeDetails: CompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = Some(SoleTraderBusinesses(
        accountingMethod = Cash,
        businesses = Seq(SoleTraderBusiness(
          id = "test-id",
          name = "ABC Limited",
          trade = "Plumbing",
          startDate = LocalDate.of(1980, 1, 1),
          address = Address(
            lines = Seq("1 Long Road", "Lonely city"),
            postcode = Some("ZZ11ZZ")
          )
        ))
      )),
      ukProperty = Some(UKProperty(
        startDate = LocalDate.of(1980, 1, 2),
        accountingMethod = Cash
      )),
      foreignProperty = Some(ForeignProperty(
        startDate = LocalDate.of(1980, 1, 3),
        accountingMethod = Cash
      ))
    ),
    taxYear = Current,
    hasSoftware = true
  )

  "getCompleteSignUpDetails" must {
    "return a complete details model" when {
      "all fetches were successful and are full data sets" in new Setup {
        mockFetchAllSelfEmployments(Seq(selfEmployment))
        mockFetchSelfEmploymentAccountingMethod(Some(selfEmploymentsAccountingMethod))
        mockFetchProperty(Some(ukProperty))
        mockFetchOverseasProperty(Some(foreignProperty))
        mockFetchSelectedTaxYear(Some(accountingYear))

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference")(FakeRequest(), HeaderCarrier())
        }

        await(result) mustBe Right(completeDetails)
      }
    }

    "return a GetCompleteDetailsFailure" when {
      "there is no selected tax year" in new Setup {
        mockFetchAllSelfEmployments(Seq(selfEmployment))
        mockFetchSelfEmploymentAccountingMethod(Some(selfEmploymentsAccountingMethod))
        mockFetchProperty(Some(ukProperty))
        mockFetchOverseasProperty(Some(foreignProperty))
        mockFetchSelectedTaxYear(None)

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference")(FakeRequest(), HeaderCarrier())
        }

        await(result) mustBe Left(GetCompleteDetailsFailure)
      }
      "there is a sole trader business" which {
        "does not have a name" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment.copy(businessName = None)))
          mockFetchSelfEmploymentAccountingMethod(Some(selfEmploymentsAccountingMethod))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(FakeRequest(), HeaderCarrier())
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "does not have a trade" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment.copy(businessTradeName = None)))
          mockFetchSelfEmploymentAccountingMethod(Some(selfEmploymentsAccountingMethod))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(FakeRequest(), HeaderCarrier())
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "does not have a start date" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment.copy(businessStartDate = None)))
          mockFetchSelfEmploymentAccountingMethod(Some(selfEmploymentsAccountingMethod))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(FakeRequest(), HeaderCarrier())
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "does not have an address" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment.copy(businessAddress = None)))
          mockFetchSelfEmploymentAccountingMethod(Some(selfEmploymentsAccountingMethod))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(FakeRequest(), HeaderCarrier())
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
      }
      "there is a uk property business" which {
        "does not have a start date" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment))
          mockFetchSelfEmploymentAccountingMethod(Some(selfEmploymentsAccountingMethod))
          mockFetchProperty(Some(ukProperty.copy(startDate = None)))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(FakeRequest(), HeaderCarrier())
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "does not have an accounting method" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment))
          mockFetchSelfEmploymentAccountingMethod(Some(selfEmploymentsAccountingMethod))
          mockFetchProperty(Some(ukProperty.copy(accountingMethod = None)))
          mockFetchOverseasProperty(Some(foreignProperty))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(FakeRequest(), HeaderCarrier())
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
      }
      "there is a foreign property business" which {
        "does not have a start date" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment))
          mockFetchSelfEmploymentAccountingMethod(Some(selfEmploymentsAccountingMethod))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty.copy(startDate = None)))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(FakeRequest(), HeaderCarrier())
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "does not have an accounting method" in new Setup {
          mockFetchAllSelfEmployments(Seq(selfEmployment))
          mockFetchSelfEmploymentAccountingMethod(Some(selfEmploymentsAccountingMethod))
          mockFetchProperty(Some(ukProperty))
          mockFetchOverseasProperty(Some(foreignProperty.copy(accountingMethod = None)))
          mockFetchSelectedTaxYear(Some(accountingYear))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference")(FakeRequest(), HeaderCarrier())
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
      }
    }
  }

}
