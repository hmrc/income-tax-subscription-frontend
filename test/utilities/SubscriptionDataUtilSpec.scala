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

import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import models.common.subscription.CreateIncomeSourcesModel
import models.common.{AccountingYearModel, IncomeSourceModel}
import models.{AgentSummary, Current, IndividualSummary, Next}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Matchers._
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataUtil._
import utilities.TestModels._
import utilities.agent.TestModels.testSelfEmployments
import utilities.individual.TestConstants._

class SubscriptionDataUtilSpec extends UnitTestTrait
  with FeatureSwitching
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

            def result: CreateIncomeSourcesModel = testCacheMap(testIncomeSourceBusiness, testBusinessName, testSelectedTaxYearCurrent,
              testAccountingMethod).createIncomeSources(testNino, testSelfEmploymentData, testAccountingMethod)

            result shouldBe
              CreateIncomeSourcesModel(
                testNino,
                testSoleTraderBusinesses
              )
          }
        }

        "incomplete selected tax year returned" should {
          "throw InternalServerException" in {

            def result: CreateIncomeSourcesModel = testCacheMap(testIncomeSourceBusiness, businessName = None, testSelectedTaxYearCurrent,
              testAccountingMethod).createIncomeSources(testNino, testUncompletedSelfEmploymentData, testAccountingMethod)

            intercept[InternalServerException](result).message mustBe
              "[SubscriptionDataUtil][createIncomeSource] - not all self employment businesses are complete"

          }
        }

        "incomplete self employment data and self employment accounting method are returned" should {
          "throw InternalServerException" in {

            def result: CreateIncomeSourcesModel = testCacheMap(testIncomeSourceBusiness, businessName = None, AccountingYearModel(Current),
              testAccountingMethod).createIncomeSources(testNino, testUncompletedSelfEmploymentData, testAccountingMethod)

            intercept[InternalServerException](result).message mustBe
              "[SubscriptionDataUtil][createIncomeSources] - Could not create the create income sources model as the user has not confirmed their selected tax year"

          }
        }

        "some self employments data are returned but not self employments accounting method data" should {
          "throw InternalServerException" in {

            def result: CreateIncomeSourcesModel = testCacheMap(testIncomeSourceBusiness, testBusinessName, testSelectedTaxYearCurrent,
              accountingMethod = None).createIncomeSources(testNino, testSelfEmploymentData)

            intercept[InternalServerException](result).message mustBe
              "[SubscriptionDataUtil][createIncomeSource] - self employment businesses found without any accounting method"

          }

        }

        "some self employments accounting method data are returned but not self employments data" should {
          "throw InternalServerException" in {

            def result: CreateIncomeSourcesModel = testCacheMap(testIncomeSourceBusiness, businessName = None, testSelectedTaxYearCurrent,
              testAccountingMethod).createIncomeSources(testNino, selfEmployments = None, testAccountingMethod)

            intercept[InternalServerException](result).message mustBe
              "[SubscriptionDataUtil][createIncomeSource] - self employment accounting method found without any self employments"

          }
        }

        "both self employments data and self employments accounting method data are not returned" should {
          "throw IllegalArgumentException due to no any income source has been added into the model" in {

            def result: CreateIncomeSourcesModel = testCacheMap(testIncomeSourceBusiness, businessName = None, testSelectedTaxYearCurrent,
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
            incomeSource = testIncomeSourceProperty,
            selectedTaxYear = testSelectedTaxYearCurrent
          ).createIncomeSources(testNino, property = testFullPropertyModel)

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
            incomeSource = testIncomeSourceProperty,
            selectedTaxYear = testSelectedTaxYearCurrent
          ).createIncomeSources(testNino, property = testFullPropertyModel.copy(accountingMethod = None))

          intercept[InternalServerException](result).message mustBe
            "[SubscriptionDataUtil][createIncomeSource] - uk property accounting method missing"
        }

      }

      "the data returns uk property accounting Method but not uk property start date" should {
        "throw InternalServerException" in {

          def result: CreateIncomeSourcesModel = testCacheMap(
            incomeSource = testIncomeSourceProperty,
            selectedTaxYear = testSelectedTaxYearCurrent
          ).createIncomeSources(testNino, property = testFullPropertyModel.copy(startDate = None))

          intercept[InternalServerException](result).message mustBe
            "[SubscriptionDataUtil][createIncomeSource] - uk property start date missing"
        }

      }

      "both uk property start date and uk property accounting Method data are not returned" should {
        "throw IllegalArgumentException due to no any income source has been added into the model" in {

          def result: CreateIncomeSourcesModel = testCacheMap(
            incomeSource = testIncomeSourceProperty,
            selectedTaxYear = testSelectedTaxYearCurrent
          ).createIncomeSources(testNino, property = testFullPropertyModel.copy(accountingMethod = None, startDate = None))

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
            incomeSource = testIncomeSourceOverseasProperty,
            selectedTaxYear = testSelectedTaxYearCurrent,
          ).createIncomeSources(testNino, overseasProperty = testFullOverseasPropertyModel)

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
            incomeSource = testIncomeSourceOverseasProperty,
            selectedTaxYear = testSelectedTaxYearCurrent,
          ).createIncomeSources(testNino, overseasProperty = testFullOverseasPropertyModel.copy(accountingMethod = None))


          intercept[InternalServerException](result).message mustBe
            "[SubscriptionDataUtil][createIncomeSource] - oversea property accounting method missing"
        }

      }

      "the data returns overseas property accounting Method but not overseas property start date" should {
        "throw InternalServerException" in {

          def result: CreateIncomeSourcesModel = testCacheMap(
            incomeSource = testIncomeSourceOverseasProperty,
            selectedTaxYear = testSelectedTaxYearCurrent
          ).createIncomeSources(testNino, overseasProperty = testFullOverseasPropertyModel.copy(startDate = None))

          intercept[InternalServerException](result).message mustBe
            "[SubscriptionDataUtil][createIncomeSource] - oversea property start date missing"
        }

      }

      "both overseas property start date and overseas property accounting Method data are not returned" should {
        "throw IllegalArgumentException due to no any income source has been added into the model" in {

          def result: CreateIncomeSourcesModel = testCacheMap(
            incomeSource = testIncomeSourceProperty,
            selectedTaxYear = testSelectedTaxYearCurrent
          ).createIncomeSources(testNino, overseasProperty = testFullOverseasPropertyModel.copy(accountingMethod = None, startDate = None))

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
            testIncomeSourceBusiness,
            testBusinessName,
            testSelectedTaxYearCurrent,
            testAccountingMethod
          ).createIncomeSources(testNino, testSelfEmploymentData, testAccountingMethod, testFullPropertyModel, testFullOverseasPropertyModel)

          result shouldBe
            CreateIncomeSourcesModel(
              testNino,
              testSoleTraderBusinesses,
              Some(testUkProperty),
              Some(testOverseasProperty)
            )
        }

      }
    }
  }

  "The getSummary should populate the Summary model correctly" when {
    "income source is just uk property" in {
      testCacheMapCustom(incomeSource = testIncomeSourceProperty).getSummary(property = testFullPropertyModel.copy(startDate = None)) shouldBe
        IndividualSummary(
          incomeSource = testIncomeSourceProperty,
          accountingMethodProperty = testAccountingMethodProperty,
          selectedTaxYear = None
        )
    }
    "income source is just business" in {
      testCacheMapCustom(incomeSource = testIncomeSourceBusiness).getSummary() shouldBe
        IndividualSummary(
          incomeSource = testIncomeSourceBusiness,
          businessName = testBusinessName,
          selectedTaxYear = testSelectedTaxYearNext,
          accountingMethod = testAccountingMethod
        )
    }
    "income source is only foreign property" in {
      testCacheMapCustom(
        incomeSource = testIncomeSourceOverseasProperty
      ).getSummary(overseasProperty = testFullOverseasPropertyModel.copy(startDate = None)) shouldBe
        IndividualSummary(
          incomeSource = testIncomeSourceOverseasProperty,
          selectedTaxYear = None,
          overseasAccountingMethodProperty = testOverseasAccountingMethodProperty
        )
    }
    "income source is all property and business and the feature switches are disabled" in {
      testCacheMapCustom(
        incomeSource = testIncomeSourceAll
      ).getSummary(
        property = testFullPropertyModel.copy(startDate = None),
        overseasProperty = testFullOverseasPropertyModel.copy(startDate = None)
      ) shouldBe
        IndividualSummary(
          incomeSource = testIncomeSourceAll,
          businessName = testBusinessName,
          selectedTaxYear = None,
          accountingMethod = testAccountingMethod,
          accountingMethodProperty = testAccountingMethodProperty,
          overseasAccountingMethodProperty = testOverseasAccountingMethodProperty
        )
    }

    "income source is all property and business and the feature switches are enabled" in {
      testCacheMapCustom(incomeSource = testIncomeSourceAll).getSummary(
        selfEmployments = Some(testSelfEmploymentData),
        selfEmploymentsAccountingMethod = Some(testAccountingMethod),
        property = Some(testFullPropertyModel),
        overseasProperty = Some(testFullOverseasPropertyModel),
        isReleaseFourEnabled = true
      ) shouldBe
        IndividualSummary(
          incomeSource = testIncomeSourceAll,
          businessName = testBusinessName,
          accountingMethod = testAccountingMethod,
          accountingMethodProperty = testAccountingMethodProperty,
          propertyStartDate = testPropertyStartDateModel,
          overseasAccountingMethodProperty = testOverseasAccountingMethodProperty,
          overseasPropertyStartDate = testOverseasPropertyStartDateModel,
          selectedTaxYear = testSelectedTaxYearNext,
          selfEmployments = testSelfEmploymentData
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
      ).getAgentSummary(property = testFullPropertyModel.copy(startDate = None)) shouldBe AgentSummary(
        incomeSource = Some(testAgentIncomeSourceProperty),
        businessName = None,
        accountingMethod = None,
        accountingMethodProperty = Some(testAccountingMethodProperty)
      )
    }

    "the income type is business" in {
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceBusiness)
      ).getAgentSummary() shouldBe AgentSummary(
        incomeSource = Some(testAgentIncomeSourceBusiness),
        selectedTaxYear = Some(testSelectedTaxYearNext),
        businessName = Some(testBusinessName),
        accountingMethod = Some(testAccountingMethod),
        accountingMethodProperty = None
      )
    }

    "the income type is both" in {
      disable(ReleaseFour)
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceBusinessProperty)
      ).getAgentSummary(property = testFullPropertyModel.copy(startDate = None)) shouldBe
        AgentSummary(
          incomeSource = testAgentIncomeSourceBusinessProperty,
          businessName = testBusinessName,
          accountingMethod = testAccountingMethod,
          accountingMethodProperty = testAccountingMethodProperty
        )
    }

    "the income type is not set" in {
      emptyCacheMap.getAgentSummary() shouldBe AgentSummary()
    }
  }

  "The getAgentSummary should populate the Summary model correctly with release four enabled" when {
    "the income type is property" in {
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceProperty)
      ).getAgentSummary(
        selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = testAccountingMethodAccrual,
        property = testFullPropertyModel,
        isReleaseFourEnabled = true
      ) shouldBe AgentSummary(
        incomeSource = Some(testAgentIncomeSourceProperty),
        selectedTaxYear = Some(AccountingYearModel(Next)),
        businessName = None,
        accountingMethod = None,
        accountingMethodProperty = Some(testAccountingMethodProperty),
        propertyStartDate = Some(testPropertyStartDateModel)
      )
    }

    "the income type is property with the property tax year feature switch enabled" in {
      enable(ReleaseFour)
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceProperty)
      ).getAgentSummary(
        selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = testAccountingMethodAccrual,
        property = testFullPropertyModel,
        isReleaseFourEnabled = true
      ) shouldBe AgentSummary(
        incomeSource = Some(testAgentIncomeSourceProperty),
        selectedTaxYear = testSelectedTaxYearNext,
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
        selfEmploymentsAccountingMethod = testAccountingMethodAccrual,
        isReleaseFourEnabled = true) shouldBe AgentSummary(
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
        incomeSource = Some(testAgentIncomeSourceForeignProperty)
      ).getAgentSummary(selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = testAccountingMethodAccrual,
        isReleaseFourEnabled = true) shouldBe AgentSummary(
        incomeSource = Some(testAgentIncomeSourceForeignProperty),
        selectedTaxYear = Some(testSelectedTaxYearNext),
        businessName = None,
        accountingMethod = None,
        accountingMethodProperty = None,
        overseasAccountingMethodProperty = None
      )
    }

    "the income type is foreign property with the property tax year feature switch enabled" in {
      enable(ReleaseFour)
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceForeignProperty),
      ).getAgentSummary(selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = testAccountingMethodAccrual,
        overseasProperty = Some(testFullOverseasPropertyModel.copy(startDate = None)),
        isReleaseFourEnabled = true) shouldBe AgentSummary(
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
        selfEmploymentsAccountingMethod = testAccountingMethodAccrual,
        property = testFullPropertyModel,
        isReleaseFourEnabled = true) shouldBe
        AgentSummary(
          incomeSource = testAgentIncomeSourceBusinessProperty,
          selectedTaxYear = Some(testSelectedTaxYearNext),
          businessName = testBusinessName,
          accountingMethod = testAccountingMethodAccrual,
          accountingMethodProperty = testAccountingMethodProperty,
          propertyStartDate = Some(testPropertyStartDateModel),
          selfEmployments = testSelfEmployments
        )
    }

    "the income type is both business and overseas property" in {
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceBusinessOverseasProperty)
      ).getAgentSummary(selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = testAccountingMethodAccrual,
        overseasProperty = Some(testFullOverseasPropertyModel.copy(startDate = None)),
        isReleaseFourEnabled = true) shouldBe
        AgentSummary(
          incomeSource = testAgentIncomeSourceBusinessOverseasProperty,
          businessName = testBusinessName,
          selectedTaxYear = Some(testSelectedTaxYearNext),
          accountingMethod = testAccountingMethodAccrual,
          overseasAccountingMethodProperty = Some(testOverseasAccountingMethodProperty),
          selfEmployments = testSelfEmployments
        )
    }

    "the income type is both UK property and overseas property" in {
      disable(ReleaseFour)
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceUkPropertyOverseasProperty)
      ).getAgentSummary(selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = testAccountingMethodAccrual,
        property = testFullPropertyModel,
        overseasProperty = testFullOverseasPropertyModel,
        isReleaseFourEnabled = true) shouldBe AgentSummary(
        incomeSource = Some(testAgentIncomeSourceUkPropertyOverseasProperty),
        selectedTaxYear = Some(AccountingYearModel(Next)),
        businessName = None,
        accountingMethod = None,
        accountingMethodProperty = Some(testAccountingMethodProperty),
        propertyStartDate = Some(testPropertyStartDateModel),
        overseasAccountingMethodProperty = Some(testOverseasAccountingMethodProperty),
        overseasPropertyStartDate = Some(testOverseasPropertyStartDateModel)
      )
    }

    "the income type is both UK property and overseas property with the property feature switch enabled" in {
      enable(ReleaseFour)
      testCacheMapCustom(
        incomeSource = Some(testAgentIncomeSourceUkPropertyOverseasProperty)
      ).getAgentSummary(selfEmployments = testSelfEmployments,
        selfEmploymentsAccountingMethod = testAccountingMethodAccrual,
        property = testFullPropertyModel,
        overseasProperty = Some(testFullOverseasPropertyModel),
        isReleaseFourEnabled = true) shouldBe AgentSummary(
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
