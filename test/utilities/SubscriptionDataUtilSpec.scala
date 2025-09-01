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

package utilities

import models.common.AccountingYearModel
import models.common.business._
import models.common.subscription.CreateIncomeSourcesModel.createIncomeSources
import models.common.subscription.{CreateIncomeSourcesModel, OverseasProperty, SoleTraderBusinesses, UkProperty}
import models.{Cash, Current, DateModel}
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.play.PlaySpec
import services.GetCompleteDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.AccountingPeriodUtil.getCurrentTaxYear
import utilities.TestModels._
import utilities.individual.TestConstants._

import java.time.LocalDate

class SubscriptionDataUtilSpec extends PlaySpec {

  "createIncomeSources(nino, completeDetails)" must {
    "produce a create income sources model" which {
      "have a start date limit" in {
        createIncomeSources(testNino, completeDetails(false)) mustBe CreateIncomeSourcesModel(
          nino = testNino,
          soleTraderBusinesses = Some(SoleTraderBusinesses(
            accountingPeriod = getCurrentTaxYear,
            accountingMethod = Some(Cash),
            businesses = Seq(
              SelfEmploymentData(
                id = "test-id",
                startDateBeforeLimit = Some(true),
                businessStartDate = Some(BusinessStartDate(DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit))),
                businessName = Some(BusinessNameModel("test name")),
                businessTradeName = Some(BusinessTradeNameModel("test trade")),
                businessAddress = Some(BusinessAddressModel(address)),
                confirmed = true
              )
            )
          )),
          ukProperty = Some(UkProperty(
            startDateBeforeLimit = Some(true),
            accountingPeriod = getCurrentTaxYear,
            tradingStartDate = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit),
            accountingMethod = Some(Cash)
          )),
          overseasProperty = Some(OverseasProperty(
            startDateBeforeLimit = Some(true),
            accountingPeriod = getCurrentTaxYear,
            tradingStartDate = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit),
            accountingMethod = Some(Cash)
          ))
        )
      }
      "do not have a start date limit" in {
        createIncomeSources(testNino, completeDetails(true)) mustBe CreateIncomeSourcesModel(
          nino = testNino,
          soleTraderBusinesses = Some(SoleTraderBusinesses(
            accountingPeriod = getCurrentTaxYear,
            accountingMethod = Some(Cash),
            businesses = Seq(
              SelfEmploymentData(
                id = "test-id",
                startDateBeforeLimit = Some(false),
                businessStartDate = Some(BusinessStartDate(DateModel.dateConvert(LocalDate.now))),
                businessName = Some(BusinessNameModel("test name")),
                businessTradeName = Some(BusinessTradeNameModel("test trade")),
                businessAddress = Some(BusinessAddressModel(address)),
                confirmed = true
              )
            )
          )),
          ukProperty = Some(UkProperty(
            startDateBeforeLimit = Some(false),
            accountingPeriod = getCurrentTaxYear,
            tradingStartDate = DateModel.dateConvert(LocalDate.now),
            accountingMethod = Some(Cash)
          )),
          overseasProperty = Some(OverseasProperty(
            startDateBeforeLimit = Some(false),
            accountingPeriod = getCurrentTaxYear,
            tradingStartDate = DateModel.dateConvert(LocalDate.now),
            accountingMethod = Some(Cash)
          ))
        )
      }
    }

    "throw an exception" when {
      "no income source was provided" in {
        intercept[IllegalArgumentException](createIncomeSources(testNino, GetCompleteDetailsService.CompleteDetails(GetCompleteDetailsService.IncomeSources(None, None, None), AccountingYearModel(Current))))
          .getMessage mustBe "requirement failed: at least one income source is required"
      }
    }
  }

  lazy val address: Address = Address(
    lines = Seq("1 long road"),
    postcode = Some("ZZ1 1ZZ")
  )

  def completeDetails(hasStartDate: Boolean): GetCompleteDetailsService.CompleteDetails = GetCompleteDetailsService.CompleteDetails(
    incomeSources = GetCompleteDetailsService.IncomeSources(
      soleTraderBusinesses = Some(GetCompleteDetailsService.SoleTraderBusinesses(
        accountingMethod = Some(Cash),
        businesses = Seq(GetCompleteDetailsService.SoleTraderBusiness(
          id = "test-id",
          name = "test name",
          trade = "test trade",
          startDate = if (hasStartDate) Some(LocalDate.now) else None,
          address = address
        ))
      )),
      ukProperty = Some(
        GetCompleteDetailsService.UKProperty(
          startDate = if (hasStartDate) Some(LocalDate.now) else None,
          accountingMethod = Some(Cash)
        )
      ),
      foreignProperty = Some(
        GetCompleteDetailsService.ForeignProperty(
          startDate = if (hasStartDate) Some(LocalDate.now) else None,
          accountingMethod = Some(Cash)
        )
      )
    ),
    taxYear = AccountingYearModel(Current)
  )
}
