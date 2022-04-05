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

package utilities

import models.common.subscription.CreateIncomeSourcesModel
import models.common.{AccountingYearModel, IncomeSourceModel}
import models.{AgentSummary, Current, IndividualSummary, Next}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers._
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataUtil._
import utilities.TestModels._
import utilities.agent.TestModels.testSelfEmployments
import utilities.individual.TestConstants._

class SubscriptionDataUtilSpec extends UnitTestTrait
  
  with BeforeAndAfterEach {

  "SubscriptionDataUtil" should {

    "In the respective get calls, return None if they are not in the cachemap" in {
      emptyCacheMap.getIncomeSource shouldBe None
      emptyCacheMap.getIncomeSource shouldBe None
      emptyCacheMap.getBusinessName shouldBe None
      emptyCacheMap.getSelectedTaxYear shouldBe None
      emptyCacheMap.getAccountingMethod shouldBe None
    }

    "In the respective get calls, return the models if they are in the cachemap" in {
      testCacheMap.getIncomeSource shouldBe Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false))
      testCacheMap.getBusinessName shouldBe Some(testBusinessName)
      testCacheMap.getAccountingMethod shouldBe Some(testAccountingMethod)
      testCacheMap.getSelectedTaxYear shouldBe Some(testSelectedTaxYearNext)
    }

    "The createIncomeSources call" when {
      "income source is just sole trader business" when {
        "the data returns both completed self employments data and self employments accounting Method" should {
          "successfully populate the CreateIncomeSourcesModel with self employment income" in {

            def result: CreateIncomeSourcesModel = testCacheMap(Some(testIncomeSourceBusiness), Some(testBusinessName), Some(testSelectedTaxYearCurrent),
              Some(testAccountingMethod)).createIncomeSources(testNino, Some(testSelfEmploymentData), Some(testAccountingMethod))

            result shouldBe
              CreateIncomeSourcesModel(
                testNino,
                Some(testSoleTraderBusinesses)
              )
          }
        }

        "incomplete selected tax year returned" should {
          "throw InternalServerException" in {

            def result: CreateIncomeSourcesModel = testCacheMap(Some(testIncomeSourceBusiness), businessName = None, Some(testSelectedTaxYearCurrent),
              Some(testAccountingMethod)).createIncomeSources(testNino, Some(testUncompletedSelfEmploymentData), Some(testAccountingMethod))

            intercept[InternalServerException](result).message mustBe
              "[SubscriptionDataUtil][createIncomeSource] - not all self employment businesses are complete"

          }
        }

        "incomplete self employment data and self employment accounting method are returned" should {
          "throw InternalServerException" in {

            def result: CreateIncomeSourcesModel = testCacheMap(Some(testIncomeSourceBusiness), businessName = None, Some(AccountingYearModel(Current)),
              Some(testAccountingMethod)).createIncomeSources(testNino, Some(testUncompletedSelfEmploymentData), Some(testAccountingMethod))

            intercept[InternalServerException](result).message mustBe
              "[SubscriptionDataUtil][createIncomeSources] - Could not create the create income sources model as the user has not confirmed their selected tax year"

          }
        }

        "some self employments data are returned but not self employments accounting method data" should {
          "throw InternalServerException" in {

            def result: CreateIncomeSourcesModel = testCacheMap(Some(testIncomeSourceBusiness), Some(testBusinessName), Some(testSelectedTaxYearCurrent),
              accountingMethod = None).createIncomeSources(testNino, Some(testSelfEmploymentData))

            intercept[InternalServerException](result).message mustBe
              "[SubscriptionDataUtil][createIncomeSource] - self employment businesses found without any accounting method"

          }

        }

        "some self employments accounting method data are returned but not self employments data" should {
          "throw InternalServerException" in {

            def result: CreateIncomeSourcesModel = testCacheMap(Some(testIncomeSourceBusiness), businessName = None, Some(testSelectedTaxYearCurrent),
              Some(testAccountingMethod)).createIncomeSources(testNino, selfEmployments = None, Some(testAccountingMethod))

            intercept[InternalServerException](result).message mustBe
              "[SubscriptionDataUtil][createIncomeSource] - self employment accounting method found without any self employments"

          }
        }

        "both self employments data and self employments accounting method data are not returned" should {
          "throw IllegalArgumentException due to no any income source has been added into the model" in {

            def result: CreateIncomeSourcesModel = testCacheMap(Some(testIncomeSourceBusiness), businessName = None, Some(testSelectedTaxYearCurrent),
              accountingMethod = None).createIncomeSources(testNino)

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

          def result: CreateIncomeSourcesModel = testCacheMap(
            incomeSource = Some(testIncomeSourceProperty),
            selectedTaxYear = Some(testSelectedTaxYearCurrent)
          ).createIncomeSources(testNino, property = Some(testFullPropertyModel))

          result shouldBe
            CreateIncomeSourcesModel(
              testNino,
              selfEmployments = None,
              Some(testUkProperty),
              None
            )
        }

      }

      "the data returns uk property start date but not uk property accounting Method" should {
        "throw InternalServerException" in {

          def result: CreateIncomeSourcesModel = testCacheMap(
            incomeSource = Some(testIncomeSourceProperty),
            selectedTaxYear = Some(testSelectedTaxYearCurrent)
          ).createIncomeSources(testNino, property = Some(testFullPropertyModel.copy(accountingMethod = None)))

          intercept[InternalServerException](result).message mustBe
            "[SubscriptionDataUtil][createIncomeSource] - uk property accounting method missing"
        }

      }

      "the data returns uk property accounting Method but not uk property start date" should {
        "throw InternalServerException" in {

          def result: CreateIncomeSourcesModel = testCacheMap(
            incomeSource = Some(testIncomeSourceProperty),
            selectedTaxYear = Some(testSelectedTaxYearCurrent)
          ).createIncomeSources(testNino, property = Some(testFullPropertyModel.copy(startDate = None)))

          intercept[InternalServerException](result).message mustBe
            "[SubscriptionDataUtil][createIncomeSource] - uk property start date missing"
        }

      }

      "both uk property start date and uk property accounting Method data are not returned" should {
        "throw IllegalArgumentException due to no any income source has been added into the model" in {

          def result: CreateIncomeSourcesModel = testCacheMap(
            incomeSource = Some(testIncomeSourceProperty),
            selectedTaxYear = Some(testSelectedTaxYearCurrent)
          ).createIncomeSources(testNino, property = Some(testFullPropertyModel.copy(accountingMethod = None, startDate = None)))

          intercept[IllegalArgumentException] {
            result
          }.getMessage shouldBe "requirement failed: at least one income source is required"
        }
      }

    }

    "income source is just overseas property business" when {
      "the data returns both overseas property start date and overseas property accounting Method" should {
        "successfully populate the CreateIncomeSourcesModel with overseas property income" in {

          def result: CreateIncomeSourcesModel = testCacheMap(
            incomeSource = Some(testIncomeSourceOverseasProperty),
            selectedTaxYear = Some(testSelectedTaxYearCurrent),
          ).createIncomeSources(testNino, overseasProperty = Some(testFullOverseasPropertyModel))

          result shouldBe
            CreateIncomeSourcesModel(
              testNino,
              selfEmployments = None,
              ukProperty = None,
              Some(testOverseasProperty)

            )
        }
      }

      "the data returns overseas property start date but not overseas property accounting Method" should {
        "throw InternalServerException" in {

          def result: CreateIncomeSourcesModel = testCacheMap(
            incomeSource = Some(testIncomeSourceOverseasProperty),
            selectedTaxYear = Some(testSelectedTaxYearCurrent),
          ).createIncomeSources(testNino, overseasProperty = Some(testFullOverseasPropertyModel.copy(accountingMethod = None)))


          intercept[InternalServerException](result).message mustBe
            "[SubscriptionDataUtil][createIncomeSource] - oversea property accounting method missing"
        }

      }

      "the data returns overseas property accounting Method but not overseas property start date" should {
        "throw InternalServerException" in {

          def result: CreateIncomeSourcesModel = testCacheMap(
            incomeSource = Some(testIncomeSourceOverseasProperty),
            selectedTaxYear = Some(testSelectedTaxYearCurrent)
          ).createIncomeSources(testNino, overseasProperty = Some(testFullOverseasPropertyModel.copy(startDate = None)))

          intercept[InternalServerException](result).message mustBe
            "[SubscriptionDataUtil][createIncomeSource] - oversea property start date missing"
        }

      }

      "both overseas property start date and overseas property accounting Method data are not returned" should {
        "throw IllegalArgumentException due to no any income source has been added into the model" in {

          def result: CreateIncomeSourcesModel = testCacheMap(
            incomeSource = Some(testIncomeSourceProperty),
            selectedTaxYear = Some(testSelectedTaxYearCurrent)
          ).createIncomeSources(testNino, overseasProperty = Some(testFullOverseasPropertyModel.copy(accountingMethod = None, startDate = None)))

          intercept[IllegalArgumentException] {
            result
          }.getMessage shouldBe "requirement failed: at least one income source is required"
        }

      }

    }

    "sign up all three types of income source" when {
      "all the requirements data have been answered and returned" should {
        "successfully populate the CreateIncomeSourcesModel with all three types of income" in {

          def result: CreateIncomeSourcesModel = testCacheMap(
            Some(testIncomeSourceBusiness),
            Some(testBusinessName),
            Some(testSelectedTaxYearCurrent),
            Some(testAccountingMethod)
          ).createIncomeSources(testNino, Some(testSelfEmploymentData), Some(testAccountingMethod), Some(testFullPropertyModel), Some(testFullOverseasPropertyModel))

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

  "The getSummary should populate the Summary model correctly" when {
    "income source is just uk property" in {
      testCacheMapCustom(incomeSource = Some(testIncomeSourceProperty)).getSummary(property = Some(testFullPropertyModel.copy(startDate = None))) shouldBe
        IndividualSummary(
          incomeSource = Some(testIncomeSourceProperty),
          accountingMethodProperty = Some(testAccountingMethodProperty),
          selectedTaxYear = Some(testSelectedTaxYearNext)
        )
    }
    
    "income source is just business" in {
      testCacheMapCustom(incomeSource = Some(testIncomeSourceBusiness)).getSummary() shouldBe
        IndividualSummary(
          incomeSource = Some(testIncomeSourceBusiness),
          businessName = Some(testBusinessName),
          selectedTaxYear = Some(testSelectedTaxYearNext),
          accountingMethod = Some(testAccountingMethod)
        )
    }

    "income source is only foreign property" in {
      testCacheMapCustom(
        incomeSource = Some(testIncomeSourceOverseasProperty)
      ).getSummary(overseasProperty = Some(testFullOverseasPropertyModel.copy(startDate = None))) shouldBe
        IndividualSummary(
          incomeSource = Some(testIncomeSourceOverseasProperty),
          selectedTaxYear = Some(testSelectedTaxYearNext),
          overseasAccountingMethodProperty = Some(testOverseasAccountingMethodProperty)
        )
    }

    "income source is all property and business" in {
      testCacheMapCustom(incomeSource = Some(testIncomeSourceAll)).getSummary(
        selfEmployments = Some(testSelfEmploymentData),
        selfEmploymentsAccountingMethod = Some(testAccountingMethod),
        property = Some(testFullPropertyModel),
        overseasProperty = Some(testFullOverseasPropertyModel)
      ) shouldBe
        IndividualSummary(
          incomeSource = Some(testIncomeSourceAll),
          businessName = Some(testBusinessName),
          accountingMethod = Some(testAccountingMethod),
          accountingMethodProperty = Some(testAccountingMethodProperty),
          propertyStartDate = Some(testPropertyStartDateModel),
          overseasAccountingMethodProperty = Some(testOverseasAccountingMethodProperty),
          overseasPropertyStartDate = Some(testOverseasPropertyStartDateModel),
          selectedTaxYear = Some(testSelectedTaxYearNext),
          selfEmployments = Some(testSelfEmploymentData)
        )
    }
    "income source is neither property or business" in {
      emptyCacheMap.getSummary() shouldBe IndividualSummary()
    }
  }

  "The getAgentSummary should populate the Summary model correctly" when {
    "the income type is property" in {
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceProperty)
      ).getAgentSummary(
        selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = Some(testAccountingMethodAccrual),
        property = Some(testFullPropertyModel)
      ) shouldBe AgentSummary(
        incomeSource = Some(testAgentIncomeSourceProperty),
        selectedTaxYear = Some(AccountingYearModel(Next)),
        businessName = None,
        accountingMethod = None,
        accountingMethodProperty = Some(testAccountingMethodProperty),
        propertyStartDate = Some(testPropertyStartDateModel)
      )
    }

    "the income type is business" in {
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceBusiness)
      ).getAgentSummary(selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = Some(testAccountingMethodAccrual)
      ) shouldBe AgentSummary(
        incomeSource = Some(testAgentIncomeSourceBusiness),
        selectedTaxYear = Some(testSelectedTaxYearNext),
        businessName = Some(testBusinessName),
        accountingMethod = Some(testAccountingMethodAccrual),
        accountingMethodProperty = None,
        selfEmployments = testSelfEmployments
      )
    }

    "the income type is foreign property" in {
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceForeignProperty),
      ).getAgentSummary(selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = Some(testAccountingMethodAccrual),
        overseasProperty = Some(testFullOverseasPropertyModel.copy(startDate = None)),
      ) shouldBe AgentSummary(
        incomeSource = Some(testAgentIncomeSourceForeignProperty),
        selectedTaxYear = Some(testSelectedTaxYearNext),
        businessName = None,
        accountingMethod = None,
        accountingMethodProperty = None,
        overseasAccountingMethodProperty = Some(testOverseasAccountingMethodProperty)
      )
    }

    "the income type is both business and property" in {
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceBusinessProperty)
      ).getAgentSummary(selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = Some(testAccountingMethodAccrual),
        property = Some(testFullPropertyModel),
      ) shouldBe
        AgentSummary(
          incomeSource = Some(testAgentIncomeSourceBusinessProperty),
          selectedTaxYear = Some(testSelectedTaxYearNext),
          businessName = Some(testBusinessName),
          accountingMethod = Some(testAccountingMethodAccrual),
          accountingMethodProperty = Some(testAccountingMethodProperty),
          propertyStartDate = Some(testPropertyStartDateModel),
          selfEmployments = testSelfEmployments
        )
    }

    "the income type is both business and overseas property" in {
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceBusinessOverseasProperty)
      ).getAgentSummary(selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = Some(testAccountingMethodAccrual),
        overseasProperty = Some(testFullOverseasPropertyModel.copy(startDate = None)),
      ) shouldBe
        AgentSummary(
          incomeSource = Some(testAgentIncomeSourceBusinessOverseasProperty),
          businessName = Some(testBusinessName),
          selectedTaxYear = Some(testSelectedTaxYearNext),
          accountingMethod = Some(testAccountingMethodAccrual),
          overseasAccountingMethodProperty = Some(testOverseasAccountingMethodProperty),
          selfEmployments = testSelfEmployments
        )
    }

    "the income type is both UK property and overseas property" in {
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceUkPropertyOverseasProperty)
      ).getAgentSummary(selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = Some(testAccountingMethodAccrual),
        property = Some(testFullPropertyModel),
        overseasProperty = Some(testFullOverseasPropertyModel),
      ) shouldBe AgentSummary(
        incomeSource = Some(testAgentIncomeSourceUkPropertyOverseasProperty),
        selectedTaxYear = Some(testSelectedTaxYearNext),
        businessName = None,
        accountingMethod = None,
        accountingMethodProperty = Some(testAccountingMethodProperty),
        propertyStartDate = Some(testPropertyStartDateModel),
        overseasAccountingMethodProperty = Some(testOverseasAccountingMethodProperty),
        overseasPropertyStartDate = Some(testOverseasPropertyStartDateModel)
      )
    }

    "the income type is not set" in {
      emptyCacheMap.getAgentSummary() shouldBe AgentSummary()
    }
  }
}
