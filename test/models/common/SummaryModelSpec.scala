/*
 * Copyright 2020 HM Revenue & Customs
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

package models.common

import agent.assets.MessageLookup.PropertyAccountingMethod
import models._
import models.individual.business._
import models.individual.incomesource.IncomeSourceModel
import uk.gov.hmrc.play.test.UnitSpec
import utilities.AccountingPeriodUtil._

class SummaryModelSpec extends UnitSpec {

  val date = DateModel("1", "2", "1980")
  val completeSeModel = SelfEmploymentData(
    id = "",
    businessStartDate = Some(BusinessStartDate(date)),
    businessName = Some(BusinessNameModel("Fake Name")),
    businessTradeName = Some(BusinessTradeNameModel("Trade")),
    businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line1"), "Postcode")))
  )

  "Individual Summary" should {

    "convert correctly to a BusinessSubscriptionDetailsModel" when {

      "provided with a valid set of data for SE" in {
        val summary = IndividualSummary(
          incomeSourceIndiv = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
          selfEmployments = Some(Seq(completeSeModel)),
          accountingMethod = Some(AccountingMethodModel(Cash)),
          accountingPeriodDate = Some(getNextTaxYear)
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
          accountingPeriod = getNextTaxYear,
          selfEmploymentsData = Some(Seq(completeSeModel)),
          accountingMethod = Some(Cash)
        )

        summary.toBusinessSubscriptionDetailsModel shouldBe expectedModel
      }

      "provided with a valid set of data for SE filtering out incomplete sets" in {
        val summary = IndividualSummary(
          incomeSourceIndiv = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
          selfEmployments = Some(Seq(completeSeModel, SelfEmploymentData("badData"))),
          accountingMethod = Some(AccountingMethodModel(Cash)),
          accountingPeriodDate = Some(getNextTaxYear)
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
          accountingPeriod = getNextTaxYear,
          selfEmploymentsData = Some(Seq(completeSeModel)),
          accountingMethod = Some(Cash)
        )

        summary.toBusinessSubscriptionDetailsModel shouldBe expectedModel
      }

      "provided with a valid set of data for uk property" in {
        val summary = IndividualSummary(
          incomeSourceIndiv = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
          propertyCommencementDate = Some(PropertyCommencementDateModel(date)),
          accountingMethodProperty = Some(AccountingMethodPropertyModel(Cash))
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false),
          accountingPeriod = getCurrentTaxYear,
          propertyCommencementDate = Some(PropertyCommencementDateModel(date)),
          propertyAccountingMethod = Some(AccountingMethodPropertyModel(Cash))
        )

        summary.toBusinessSubscriptionDetailsModel shouldBe expectedModel
      }

      "provided with a valid set of data for foreign property" in {
        val summary = IndividualSummary(
          incomeSourceIndiv = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
          overseasPropertyCommencementDateModel = Some(OverseasPropertyCommencementDateModel(date)),
          overseasAccountingMethodPropertyModel = Some(OverseasAccountingMethodPropertyModel(Cash))
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true),
          accountingPeriod = getCurrentTaxYear,
          overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(date)),
          overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
        )

        summary.toBusinessSubscriptionDetailsModel shouldBe expectedModel
      }

      "provided with a valid set of all data" in {
        val summary = IndividualSummary(
          incomeSourceIndiv = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
          selfEmployments = Some(Seq(completeSeModel)),
          accountingMethod = Some(AccountingMethodModel(Cash)),
          propertyCommencementDate = Some(PropertyCommencementDateModel(date)),
          accountingMethodProperty = Some(AccountingMethodPropertyModel(Cash)),
          overseasPropertyCommencementDateModel = Some(OverseasPropertyCommencementDateModel(date)),
          overseasAccountingMethodPropertyModel = Some(OverseasAccountingMethodPropertyModel(Cash))
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
          accountingPeriod = getCurrentTaxYear,
          selfEmploymentsData = Some(Seq(completeSeModel)),
          accountingMethod = Some(Cash),
          propertyCommencementDate = Some(PropertyCommencementDateModel(date)),
          propertyAccountingMethod = Some(AccountingMethodPropertyModel(Cash)),
          overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(date)),
          overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
        )

        summary.toBusinessSubscriptionDetailsModel shouldBe expectedModel
      }

      "provided with valid data including data not selected as an income source" in {
        val summary = IndividualSummary(
          incomeSourceIndiv = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = false)),
          selfEmployments = Some(Seq(completeSeModel)),
          accountingMethod = Some(AccountingMethodModel(Cash)),
          propertyCommencementDate = Some(PropertyCommencementDateModel(date)),
          accountingMethodProperty = Some(AccountingMethodPropertyModel(Cash)),
          overseasPropertyCommencementDateModel = Some(OverseasPropertyCommencementDateModel(date)),
          overseasAccountingMethodPropertyModel = Some(OverseasAccountingMethodPropertyModel(Cash)),
          accountingPeriodDate = Some(getCurrentTaxYear)
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = false),
          accountingPeriod = getCurrentTaxYear
        )

        summary.toBusinessSubscriptionDetailsModel shouldBe expectedModel
      }
    }

    "should throw the correct exception" when {

      "incomplete se data is submitted when required" in {
        val summary = IndividualSummary(
          incomeSourceIndiv = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
          accountingPeriodDate = Some(getNextTaxYear)
        )

        the[Exception] thrownBy summary.toBusinessSubscriptionDetailsModel should have message "Missing data items for valid self employments submission"
      }

      "incomplete property data is submitted when required" in {
        val summary = IndividualSummary(
          incomeSourceIndiv = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)))

        the[Exception] thrownBy summary.toBusinessSubscriptionDetailsModel should have message "Missing data items for valid property submission"
      }

      "incomplete foreign property data is submitted when required" in {
        val summary = IndividualSummary(
          incomeSourceIndiv = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)))

        the[Exception] thrownBy summary.toBusinessSubscriptionDetailsModel should have message "Missing data items for valid foreign property submission"
      }

      "accounting year selection is missing when se employments are the only income" in {
        val summary = IndividualSummary(
          incomeSourceIndiv = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
          selfEmployments = Some(Seq(completeSeModel)),
          accountingMethod = Some(AccountingMethodModel(Cash))
        )

        the[Exception] thrownBy summary.toBusinessSubscriptionDetailsModel should have message "Accounting period not defined for BusinessSubscriptionDetailsModel"
      }

      "income source model is missing on any submission" in {
        val summary = IndividualSummary(
          selfEmployments = Some(Seq(completeSeModel)),
          accountingMethod = Some(AccountingMethodModel(Cash)),
          accountingPeriodDate = Some(getNextTaxYear)
        )

        the[Exception] thrownBy summary.toBusinessSubscriptionDetailsModel should have message "IncomeSource model not defined for BusinessSubscriptionDetailsModel"
      }
    }
  }
}
