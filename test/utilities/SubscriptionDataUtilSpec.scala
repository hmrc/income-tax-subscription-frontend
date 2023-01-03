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

import models.Current
import models.common.AccountingYearModel
import models.common.subscription.CreateIncomeSourcesModel
import models.common.subscription.CreateIncomeSourcesModel.createIncomeSources
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers._
import uk.gov.hmrc.http.InternalServerException
import utilities.TestModels._
import utilities.individual.TestConstants._

class SubscriptionDataUtilSpec extends UnitTestTrait

  with BeforeAndAfterEach {

  "SubscriptionDataUtil" should {

    "The createIncomeSources call" when {
      "income source is just sole trader business" when {
        "the data returns both completed self employments data and self employments accounting Method" should {
          "successfully populate the CreateIncomeSourcesModel with self employment income" in {

            def result: CreateIncomeSourcesModel = createIncomeSources(testNino, testSelfEmploymentData, Some(testAccountingMethod), accountingYear = Some(testSelectedTaxYearCurrent))

            result shouldBe
              CreateIncomeSourcesModel(
                testNino,
                Some(testSoleTraderBusinesses)
              )
          }
        }

        "incomplete selected tax year returned" should {
          "throw InternalServerException" in {

            def result: CreateIncomeSourcesModel = createIncomeSources(testNino, testUncompletedSelfEmploymentData, Some(testAccountingMethod), accountingYear = Some(testSelectedTaxYearCurrent))

            intercept[InternalServerException](result).message mustBe
              "[SubscriptionDataUtil][createIncomeSource] - not all self employment businesses are complete"

          }
        }

        "incomplete self employment data and self employment accounting method are returned" should {
          "throw InternalServerException" in {

            def result: CreateIncomeSourcesModel = createIncomeSources(testNino, testUncompletedSelfEmploymentData, Some(testAccountingMethod), accountingYear = Some(AccountingYearModel(Current)))

            intercept[InternalServerException](result).message mustBe
              "[SubscriptionDataUtil][createIncomeSources] - Could not create the create income sources model as the user has not confirmed their selected tax year"

          }
        }

        "some self employments data are returned but not self employments accounting method data" should {
          "throw InternalServerException" in {

            def result: CreateIncomeSourcesModel = createIncomeSources(testNino, testSelfEmploymentData, accountingYear = Some(testSelectedTaxYearCurrent))

            intercept[InternalServerException](result).message mustBe
              "[SubscriptionDataUtil][createIncomeSource] - self employment businesses found without any accounting method"

          }

        }

        "some self employments accounting method data are returned but not self employments data" should {
          "throw InternalServerException" in {

            def result: CreateIncomeSourcesModel = createIncomeSources(testNino, selfEmployments = Seq.empty, Some(testAccountingMethod), accountingYear = Some(testSelectedTaxYearCurrent))

            intercept[InternalServerException](result).message mustBe
              "[SubscriptionDataUtil][createIncomeSource] - self employment accounting method found without any self employments"

          }
        }

        "both self employments data and self employments accounting method data are not returned" should {
          "throw IllegalArgumentException due to no any income source has been added into the model" in {

            def result: CreateIncomeSourcesModel = createIncomeSources(testNino, accountingYear = Some(testSelectedTaxYearCurrent))

            intercept[IllegalArgumentException] {
              result
            }.getMessage shouldBe "requirement failed: at least one income source is required"
          }
        }
      }
    }

    "income source is just uk property business" when {
      "the data returns both uk property start date and uk property accounting Method" should {
        "successfully populate the CreateIncomeSourcesModel with uk property income" in {

          def result: CreateIncomeSourcesModel = createIncomeSources(testNino, property = Some(testFullPropertyModel), accountingYear = Some(testSelectedTaxYearCurrent))

          result shouldBe
            CreateIncomeSourcesModel(
              testNino,
              soleTraderBusinesses = None,
              Some(testUkProperty),
              None
            )
        }

      }

      "the data returns uk property start date but not uk property accounting Method" should {
        "throw InternalServerException" in {

          def result: CreateIncomeSourcesModel = createIncomeSources(testNino, property = Some(testFullPropertyModel.copy(accountingMethod = None)), accountingYear = Some(testSelectedTaxYearCurrent))

          intercept[InternalServerException](result).message mustBe
            "[SubscriptionDataUtil][createIncomeSource] - uk property accounting method missing"
        }

      }

      "the data returns uk property accounting Method but not uk property start date" should {
        "throw InternalServerException" in {

          def result: CreateIncomeSourcesModel = createIncomeSources(testNino, property = Some(testFullPropertyModel.copy(startDate = None)), accountingYear = Some(testSelectedTaxYearCurrent))

          intercept[InternalServerException](result).message mustBe
            "[SubscriptionDataUtil][createIncomeSource] - uk property start date missing"
        }

      }

      "both uk property start date and uk property accounting Method data are not returned" should {
        "throw IllegalArgumentException due to no any income source has been added into the model" in {

          def result: CreateIncomeSourcesModel = createIncomeSources(testNino, property = Some(testFullPropertyModel.copy(accountingMethod = None, startDate = None)), accountingYear = Some(testSelectedTaxYearCurrent))

          intercept[IllegalArgumentException] {
            result
          }.getMessage shouldBe "requirement failed: at least one income source is required"
        }
      }

    }

    "income source is just overseas property business" when {
      "the data returns both overseas property start date and overseas property accounting Method" should {
        "successfully populate the CreateIncomeSourcesModel with overseas property income" in {

          def result: CreateIncomeSourcesModel = createIncomeSources(testNino, overseasProperty = Some(testFullOverseasPropertyModel), accountingYear = Some(testSelectedTaxYearCurrent))

          result shouldBe
            CreateIncomeSourcesModel(
              testNino,
              soleTraderBusinesses = None,
              ukProperty = None,
              Some(testOverseasProperty)

            )
        }
      }

      "the data returns overseas property start date but not overseas property accounting Method" should {
        "throw InternalServerException" in {

          def result: CreateIncomeSourcesModel = createIncomeSources(testNino, overseasProperty = Some(testFullOverseasPropertyModel.copy(accountingMethod = None)), accountingYear = Some(testSelectedTaxYearCurrent))


          intercept[InternalServerException](result).message mustBe
            "[SubscriptionDataUtil][createIncomeSource] - oversea property accounting method missing"
        }

      }

      "the data returns overseas property accounting Method but not overseas property start date" should {
        "throw InternalServerException" in {

          def result: CreateIncomeSourcesModel = createIncomeSources(testNino, overseasProperty = Some(testFullOverseasPropertyModel.copy(startDate = None)), accountingYear = Some(testSelectedTaxYearCurrent))

          intercept[InternalServerException](result).message mustBe
            "[SubscriptionDataUtil][createIncomeSource] - oversea property start date missing"
        }

      }

      "both overseas property start date and overseas property accounting Method data are not returned" should {
        "throw IllegalArgumentException due to no any income source has been added into the model" in {

          def result: CreateIncomeSourcesModel = createIncomeSources(testNino, overseasProperty = Some(testFullOverseasPropertyModel.copy(accountingMethod = None, startDate = None)), accountingYear = Some(testSelectedTaxYearCurrent))

          intercept[IllegalArgumentException] {
            result
          }.getMessage shouldBe "requirement failed: at least one income source is required"
        }

      }

    }

    "sign up all three types of income source" when {
      "all the requirements data have been answered and returned" should {
        "successfully populate the CreateIncomeSourcesModel with all three types of income" in {

          def result: CreateIncomeSourcesModel = createIncomeSources(testNino, testSelfEmploymentData, Some(testAccountingMethod), Some(testFullPropertyModel), Some(testFullOverseasPropertyModel), accountingYear = Some(testSelectedTaxYearCurrent))

          result shouldBe
            CreateIncomeSourcesModel(
              testNino,
              Some(testSoleTraderBusinesses),
              Some(testUkProperty),
              Some(testOverseasProperty)
            )
        }

      }
    }
  }
}
