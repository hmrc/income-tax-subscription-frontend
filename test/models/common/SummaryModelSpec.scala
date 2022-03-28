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

package models.common

import models._
import models.common.business._
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import utilities.AccountingPeriodUtil._
import utilities.individual.TestConstants.testNino

class SummaryModelSpec extends AnyWordSpecLike with Matchers with OptionValues {

  val date: DateModel = DateModel("1", "2", "1980")
  val completeSeModel: SelfEmploymentData = SelfEmploymentData(
    id = "",
    businessStartDate = Some(BusinessStartDate(date)),
    businessName = Some(BusinessNameModel("Fake Name")),
    businessTradeName = Some(BusinessTradeNameModel("Trade")),
    businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line1"), "Postcode")))
  )

  "Individual Summary" should {

    "convert correctly to a BusinessSubscriptionDetailsModel" when {

      "provided with a valid set of data for uk property" in {
        val summary = IndividualSummary(
          incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
          selectedTaxYear = Some(AccountingYearModel(Next)),
          propertyStartDate = Some(PropertyStartDateModel(date)),
          accountingMethodProperty = Some(AccountingMethodPropertyModel(Cash))
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          nino = testNino,
          incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false),
          accountingPeriod = getNextTaxYear,
          propertyStartDate = Some(PropertyStartDateModel(date)),
          propertyAccountingMethod = Some(AccountingMethodPropertyModel(Cash))
        )

        summary.toBusinessSubscriptionDetailsModel(testNino) shouldBe expectedModel
      }

      "provided with a valid set of data for SE" in {
        val summary = IndividualSummary(
          incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
          selfEmployments = Some(Seq(completeSeModel)),
          accountingMethod = Some(AccountingMethodModel(Cash)),
          selectedTaxYear = Some(AccountingYearModel(Next))
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          nino = testNino,
          incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
          accountingPeriod = getNextTaxYear,
          selfEmploymentsData = Some(Seq(completeSeModel)),
          accountingMethod = Some(Cash)
        )

        summary.toBusinessSubscriptionDetailsModel(testNino) shouldBe expectedModel
      }

      "provided with a valid set of data for SE filtering out incomplete sets" in {
        val summary = IndividualSummary(
          incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
          selfEmployments = Some(Seq(completeSeModel, SelfEmploymentData("badData"))),
          accountingMethod = Some(AccountingMethodModel(Cash)),
          selectedTaxYear = Some(AccountingYearModel(Next))
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          nino = testNino,
          incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
          accountingPeriod = getNextTaxYear,
          selfEmploymentsData = Some(Seq(completeSeModel)),
          accountingMethod = Some(Cash)
        )

        summary.toBusinessSubscriptionDetailsModel(testNino) shouldBe expectedModel
      }

      "provided with a valid set of data for uk property when selecting current tax year" in {
        val summary = IndividualSummary(
          incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
          selectedTaxYear = Some(AccountingYearModel(Current)),
          propertyStartDate = Some(PropertyStartDateModel(date)),
          accountingMethodProperty = Some(AccountingMethodPropertyModel(Cash))
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          nino = testNino,
          incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false),
          accountingPeriod = getCurrentTaxYear,
          propertyStartDate = Some(PropertyStartDateModel(date)),
          propertyAccountingMethod = Some(AccountingMethodPropertyModel(Cash))
        )

        summary.toBusinessSubscriptionDetailsModel(testNino) shouldBe expectedModel
      }

      "provided with a valid set of data for foreign property" in {
        val summary = IndividualSummary(
          incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
          selectedTaxYear = Some(AccountingYearModel(Next)),
          overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(date)),
          overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          nino = testNino,
          incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true),
          accountingPeriod = getNextTaxYear,
          overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(date)),
          overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
        )

        summary.toBusinessSubscriptionDetailsModel(testNino) shouldBe expectedModel
      }

      "provided with a valid set of all data" in {
        val summary = IndividualSummary(
          incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
          selfEmployments = Some(Seq(completeSeModel)),
          accountingMethod = Some(AccountingMethodModel(Cash)),
          selectedTaxYear = Some(AccountingYearModel(Current)),
          propertyStartDate = Some(PropertyStartDateModel(date)),
          accountingMethodProperty = Some(AccountingMethodPropertyModel(Cash)),
          overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(date)),
          overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          nino = testNino,
          incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
          accountingPeriod = getCurrentTaxYear,
          selfEmploymentsData = Some(Seq(completeSeModel)),
          accountingMethod = Some(Cash),
          propertyStartDate = Some(PropertyStartDateModel(date)),
          propertyAccountingMethod = Some(AccountingMethodPropertyModel(Cash)),
          overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(date)),
          overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
        )

        summary.toBusinessSubscriptionDetailsModel(testNino) shouldBe expectedModel
      }

      "provided with valid data including data not selected as an income source" in {
        val summary = IndividualSummary(
          incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = false)),
          selfEmployments = Some(Seq(completeSeModel)),
          accountingMethod = Some(AccountingMethodModel(Cash)),
          propertyStartDate = Some(PropertyStartDateModel(date)),
          accountingMethodProperty = Some(AccountingMethodPropertyModel(Cash)),
          overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(date)),
          overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash)),
          selectedTaxYear = Some(AccountingYearModel(Current))
        )

        val expectedModel = BusinessSubscriptionDetailsModel(
          nino = testNino,
          incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = false),
          accountingPeriod = getCurrentTaxYear
        )

        summary.toBusinessSubscriptionDetailsModel(testNino) shouldBe expectedModel
      }
    }

    "should throw the correct exception" when {

      "incomplete se data is submitted when required" in {
        val summary = IndividualSummary(
          incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
          selectedTaxYear = Some(AccountingYearModel(Next))
        )

        the[Exception] thrownBy summary.toBusinessSubscriptionDetailsModel(testNino) should have message "Missing data items for valid self employments submission"
      }

      "incomplete property data is submitted when required" in {
        val summary = IndividualSummary(
          incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)))

        the[Exception] thrownBy summary.toBusinessSubscriptionDetailsModel(testNino) should have message "Missing data items for valid property submission"
      }

      "incomplete foreign property data is submitted when required" in {
        val summary = IndividualSummary(
          incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)))

        the[Exception] thrownBy summary.toBusinessSubscriptionDetailsModel(testNino) should have message "Missing data items for valid foreign property submission"
      }

      "accounting year selection is missing when se employments are the only income" in {
        val summary = IndividualSummary(
          incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
          selfEmployments = Some(Seq(completeSeModel)),
          accountingMethod = Some(AccountingMethodModel(Cash))
        )

        the[Exception] thrownBy summary.toBusinessSubscriptionDetailsModel(testNino) should have message "Accounting period not defined for BusinessSubscriptionDetailsModel"
      }

      "income source model is missing on any submission" in {
        val summary = IndividualSummary(
          selfEmployments = Some(Seq(completeSeModel)),
          accountingMethod = Some(AccountingMethodModel(Cash)),
          selectedTaxYear = Some(AccountingYearModel(Next))
        )

        the[Exception] thrownBy summary.toBusinessSubscriptionDetailsModel(testNino) should have message "IncomeSource model not defined for BusinessSubscriptionDetailsModel"
      }
    }
  }
}
