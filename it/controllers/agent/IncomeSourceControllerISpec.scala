/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.agent

import config.featureswitch.FeatureSwitch.{ForeignProperty, ReleaseFour}
import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.agent.IntegrationTestModels.{subscriptionData, testAccountingMethod, testAccountingMethodForeignProperty,
  testAccountingMethodProperty, testAccountingYearCurrent, testAccountingYearNext, testBusinessName, testOverseasPropertyStartDate, testPropertyStartDate, testSummaryDataSelfEmploymentData}
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.servicemocks.AuthStub
import models.common.IncomeSourceModel
import org.jsoup.Jsoup
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys

class IncomeSourceControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    disable(ForeignProperty)
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/client/income" when {

    "FS ForeignProperty is enabled" when {

      "the Subscription Details Connector returns all data" should {
        "show the income source page with business, UK property and foreign property options selected" in {
          enable(ForeignProperty)
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()

          When("GET /income is called")
          val res = IncomeTaxSubscriptionFrontend.income()
          val serviceNameGovUK = " - Report your income and expenses quarterly - GOV.UK"
          Then("Should return a OK with the income source page")
          res should have(
            httpStatus(OK),
            pageTitle(messages("agent.income_source.heading") + serviceNameGovUK),
            checkboxSet(id = "Business", selectedCheckbox = Some(messages("agent.income_source.selfEmployed"))),
            checkboxSet(id = "UKProperty", selectedCheckbox = Some(messages("agent.income_source.rentUkProperty"))),
            checkboxSet(id = "ForeignProperty", selectedCheckbox = Some(messages("agent.income_source.foreignProperty")))
          )
        }
      }

      "the Subscription Details Connector returns no data" should {
        "show the income source page without an option selected" in {
          enable(ForeignProperty)
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()

          When("GET /income is called")
          val res = IncomeTaxSubscriptionFrontend.income()
          val serviceNameGovUK = " - Report your income and expenses quarterly - GOV.UK"
          Then("Should return a OK with the income source page")
          res should have(
            httpStatus(OK),
            pageTitle(messages("agent.income_source.heading") + serviceNameGovUK),
            checkboxSet(id = "Business", selectedCheckbox = None),
            checkboxSet(id = "UKProperty", selectedCheckbox = None),
            checkboxSet(id = "ForeignProperty", selectedCheckbox = None)
          )
        }
      }
    }

    "FS ForeignProperty is disabled" when {
      "the Subscription Details Connector returns all data" should {
        "only show business and UK property options selected but does not display foreign property option" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()

          When("GET /income is called")
          val res = IncomeTaxSubscriptionFrontend.income()
          val serviceNameGovUK = " - Report your income and expenses quarterly - GOV.UK"
          Then("Should return a OK with the income source page")
          res should have(
            httpStatus(OK),
            pageTitle(messages("agent.income_source.heading") + serviceNameGovUK),
            checkboxSet(id = "Business", selectedCheckbox = Some(messages("agent.income_source.selfEmployed"))),
            checkboxSet(id = "UKProperty", selectedCheckbox = Some(messages("agent.income_source.rentUkProperty")))
          )

          val checkboxes = Jsoup.parse(res.body).select(".multiple-choice")
          checkboxes.size() shouldBe 2

          val checkboxTextForeignProperty = Jsoup.parse(res.body).select(s"label[for=ForeignProperty]").text()
          checkboxTextForeignProperty shouldBe empty

        }
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/income" when {

    "it is in edit mode" should {

      "the user selects self-employment and self-employment journey has not been completed before" when {
        "FS Release Four is disabled and selected tax year page has not been completed before" should {
          s"redirect to ${controllers.agent.business.routes.BusinessNameController.show().url}" in {
            val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)

            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(true, false, false)),
                accountingMethodProperty = Some(testAccountingMethodProperty)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", NO_CONTENT)

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of Business Name page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(businessNameURI)
            )
          }
        }


        "Selected tax year page has been completed before and ReleaseFour is enabled" should {
          s"redirect to ${appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl}" in {
            enable(ReleaseFour)
            val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)

            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(true, false, false)),
                selectedTaxYear = Some(testAccountingYearNext),
                accountingMethodProperty = Some(testAccountingMethodProperty)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", NO_CONTENT)

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of self-employment frontend initialise")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
            )
          }
        }


        "Selected tax year page has been completed before and FS ReleaseFour is disabled " +
          "and the user has no uk property and has an overseas property income" should {
          s" redirect to ${controllers.agent.business.routes.BusinessNameController.show().url}" in {
            val userInput: IncomeSourceModel = IncomeSourceModel(true, false, true)

            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(true, false, true)),
                selectedTaxYear = Some(testAccountingYearNext),
                accountingMethodProperty = Some(testAccountingMethodProperty)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", NO_CONTENT)

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of business name")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(businessNameURI)
            )
          }
        }


        "Selected tax year page has been completed before and FS ReleaseFour is disabled" +
          "and the user has a uk property and has no overseas property income" should {
          s" redirect to ${controllers.agent.business.routes.BusinessNameController.show().url}" in {
            val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(true, true, false)),
                selectedTaxYear = Some(testAccountingYearNext),
                accountingMethodProperty = Some(testAccountingMethodProperty)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", NO_CONTENT)

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of business name")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(businessNameURI)
            )
          }
        }

        "FS ReleaseFour both are enabled and selected tax year page has been completed before " +
          "and the user has no uk property and no overseas property income" should {
          s" redirect to ${appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl}" in {
            enable(ReleaseFour)
            val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(true, false, false)),
                selectedTaxYear = Some(testAccountingYearNext),
                propertyStartDate = Some(testPropertyStartDate),
                accountingMethodProperty = Some(testAccountingMethodProperty)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", NO_CONTENT)

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of self-employment Frontend Initialise")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
            )
          }
        }

        "FS ReleaseFour is enabled and selected tax year page has not been completed before " +
          "and the user has no uk property and no overseas property income" should {
          s" redirect to ${appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl}" in {
            enable(ReleaseFour)
            val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(true, false, false)),
                propertyStartDate = Some(testPropertyStartDate),
                accountingMethodProperty = Some(testAccountingMethodProperty)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", NO_CONTENT)

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of self-employment Frontend Initialise")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
            )
          }
        }

      }

      "the user selected UK property and UK property journey has not been completed before" when {
        "when ReleaseFour is enabled" should {
          s" redirect to ${controllers.agent.business.routes.PropertyStartDateController.show()}" in {
            enable(ReleaseFour)
            val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(false, true, false)),
                overseasPropertyStartDate = Some(testOverseasPropertyStartDate),
                overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", 200, Json.toJson(Some(testSummaryDataSelfEmploymentData)))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", 200, Json.toJson(Some(testAccountingMethod)))

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of property start date")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(propertyStartDateURI)
            )
          }
        }


        "when ReleaseFour is disabled" should {
          s" redirect to ${controllers.agent.business.routes.PropertyAccountingMethodController.show().url}" in {
            val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(false, true, false)),
                overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", 200, Json.toJson(Some(testSummaryDataSelfEmploymentData)))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", 200, Json.toJson(Some(testAccountingMethod)))

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of property accounting method")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(propertyAccountingMethodURI)
            )
          }
        }
      }

      "the user selected overseas property and overseas property journey has not been completed before" when {
        "when ReleaseFour is enabled" should {
          s" redirect to ${controllers.agent.business.routes.OverseasPropertyStartDateController.show().url}" in {
            enable(ReleaseFour)
            val userInput: IncomeSourceModel = IncomeSourceModel(false, false, true)
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(false, false, true)),
                propertyStartDate = Some(testPropertyStartDate),
                accountingMethodProperty = Some(testAccountingMethodProperty)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", 200, Json.toJson(Some(testSummaryDataSelfEmploymentData)))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", 200, Json.toJson(Some(testAccountingMethod)))

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of overseas property start date")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(overseasPropertyStartDateURI)
            )
          }
        }
      }


      "the user select self-employment and self-employment journey has completed before and ReleaseFour is enabled" should {
        s" redirect to ${controllers.agent.routes.CheckYourAnswersController.show().url}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(true, false, false)),
              selectedTaxYear = Some(testAccountingYearCurrent),
              propertyStartDate = Some(testPropertyStartDate),
              accountingMethodProperty = Some(testAccountingMethodProperty)
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", 200, Json.toJson(Some(testSummaryDataSelfEmploymentData)))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", 200, Json.toJson(Some(testAccountingMethod)))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }


      "the user select self-employment and UK property and both journeys have been completed before and ReleaseFour is enabled" should {
        s" redirect to ${controllers.agent.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(true, true, false)),
              propertyStartDate = Some(testPropertyStartDate),
              accountingMethodProperty = Some(testAccountingMethodProperty)
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", 200, Json.toJson(Some(testSummaryDataSelfEmploymentData)))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", 200, Json.toJson(Some(testAccountingMethod)))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select self-employment and overseas property and both journeys have been completed before and ReleaseFour is enabled" should {
        s" redirect to ${controllers.agent.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(true, false, true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(true, false, true)),
              overseasPropertyStartDate = Some(testOverseasPropertyStartDate),
              overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", 200, Json.toJson(Some(testSummaryDataSelfEmploymentData)))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", 200, Json.toJson(Some(testAccountingMethod)))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select self-employment, UK property and overseas property and all three journeys have been completed before " +
        "and ReleaseFour is enabled" should {
        s"return an SEE OTHER (303)" + s"${controllers.agent.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(true, true, true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(true, true, true)),
              propertyStartDate = Some(testPropertyStartDate),
              accountingMethodProperty = Some(testAccountingMethodProperty),
              overseasPropertyStartDate = Some(testOverseasPropertyStartDate),
              overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", 200, Json.toJson(Some(testSummaryDataSelfEmploymentData)))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", 200, Json.toJson(Some(testAccountingMethod)))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }


      "the user select UK property and UK property journeys has been completed before and ReleaseFour is enabled" should {
        s" redirect to ${controllers.agent.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(false, true, false)),
              propertyStartDate = Some(testPropertyStartDate),
              accountingMethodProperty = Some(testAccountingMethodProperty)
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", NO_CONTENT)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select overseas property and overseas property journeys has been completed before and ReleaseFour is enabled" should {
        s"return an SEE OTHER (303)" + s"${controllers.agent.routes.CheckYourAnswersController.submit()}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(false, false, true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(false, false, true)),
              overseasPropertyStartDate = Some(testOverseasPropertyStartDate),
              overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", NO_CONTENT)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select UK property and overseas property and both journeys have been completed before and ReleaseFour is enabled" should {
        s" redirect to ${controllers.agent.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(false, true, true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(false, true, true)),
              propertyStartDate = Some(testPropertyStartDate),
              accountingMethodProperty = Some(testAccountingMethodProperty),
              overseasPropertyStartDate = Some(testOverseasPropertyStartDate),
              overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", NO_CONTENT)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user selects self-employment and no UK property or overseas property and self-employment journey has been completed before and FS Release four " +
        "is disabled" should {
        s" redirect to ${controllers.agent.routes.CheckYourAnswersController.show()}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(true, false, false)),
              selectedTaxYear = Some(testAccountingYearNext),
              businessName = Some(testBusinessName),
              accountingMethod = Some(testAccountingMethod)
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", 200, Json.toJson(Some(testSummaryDataSelfEmploymentData)))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", 200, Json.toJson(Some(testAccountingMethod)))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }
    }
  }


  "FS ForeignProperty & ReleaseFour are disabled" when {
    "not in edit mode" when {

      "the user is self-employed only" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails("testId", userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of Business Name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "the user is self-employed and has a UK property " in {
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "the user has only a UK property " in {
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of property accounting method page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyAccountingMethodURI)
        )
      }
    }
  }


  "FS ForeignProperty is enabled but ReleaseFour is disabled" when {
    "not in edit mode" when {
      "the user is self-employed only" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)


        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of Business Name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "the user is self-employed and has a UK property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "the user has a UK property only" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of UK property accounting method page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyAccountingMethodURI)
        )
      }

      "the user has a foreign property only" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, false, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $INTERNAL_SERVER_ERROR with an internal server error page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(overseasPropertyStartDateURI)
        )
      }

      "the user has a UK property and has a overseas property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER  with a redirect location of UK property accounting method page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyAccountingMethodURI)
        )
      }

      "the user is self-employed and has a UK property and a overseas property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER  with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "the user is self-employed and has a foreign property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER  with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

    }
  }

  "FS ReleaseFour is enabled but ForeignProperty is disabled" when {
    "not in edit mode" when {

      "the user is self-employed only" in {
        enable(ReleaseFour)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)


        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of Business Name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameSEURI)
        )
      }

      "the user is self-employed and has a UK property" in {
        enable(ReleaseFour)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of self-employment client initialise page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
        )
      }

      "the user has a UK property only" in {
        enable(ReleaseFour)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of UK property start date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyStartDateURI)
        )
      }
    }
  }


  "Both FS ReleaseFour and ForeignProperty are enabled" when {
    "not in edit mode" when {
      "the user is self-employed only" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of Business Name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameSEURI)
        )
      }

      "the user is self-employed and has a UK property" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of self-employment client initialise page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
        )
      }

      "the user is self-employed and has a foreign property" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of self-employment client initialise page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
        )
      }

      "the user is self-employed and has a UK property and a foreign property " in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of self-employment client initialise page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
        )
      }

      "the user has a UK property and a foreign property " in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of UK property start date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyStartDateURI)
        )
      }

      "the user has a UK property only" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of UK property start date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyStartDateURI)
        )
      }

      "the user has a foreign property only" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, false, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $INTERNAL_SERVER_ERROR with an internal server error")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(overseasPropertyStartDateURI)
        )
      }
    }
  }

}
