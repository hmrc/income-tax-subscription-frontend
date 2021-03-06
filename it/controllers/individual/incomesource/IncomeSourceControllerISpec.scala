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

package controllers.individual.incomesource


import config.featureswitch.FeatureSwitch.{ForeignProperty, ReleaseFour}
import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.AuthStub
import models.common.IncomeSourceModel
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys

class IncomeSourceControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    disable(ForeignProperty)
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/details/income-receive" when {

    "the Subscription Details Connector returns all data" should {
      "show the income source page with the options selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()

        When("GET /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.incomeSource()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the income source page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("income_source.title") + serviceNameGovUk),
          checkboxSet(id = "IncomeSource", selectedCheckbox = Some(messages("income_source.selfEmployed"))),
          checkboxSet(id = "IncomeSource-2", selectedCheckbox = Some(messages("income_source.rentUkProperty")))
        )
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the rent uk property page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()

        When("GET /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.incomeSource()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the rent uk property page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("income_source.title") + serviceNameGovUk),
          checkboxSet(id = "IncomeSource", selectedCheckbox = None),
          checkboxSet(id = "IncomeSource-2", selectedCheckbox = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/details/income-receive" when {
    "foreign property is disabled" when {
      "the user selected foreign property only" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
    "not in edit mode and release four is disabled" when {
      "the user is self-employed, doesn't have uk and foreign property " in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
      "the user is self-employed and has uk property but doesn't have foreign property" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
      "the user is self-employed and has uk and foreign property " in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
      "the user is self-employed and has foreign property but doesn't have uk property " in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
      "the user rents a uk property, is not self-employed and doesn't have foreign property" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of property commencement date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingMethodPropertyURI)
        )
      }
      "the user rents a uk property and has foreign property but is not self-employed" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)


        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of property commencement date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingMethodPropertyURI)
        )
      }
      "the user rents a foreign property, is not self-employed and doesn't have uk property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of overseas property commencement date")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(overseasPropertyStartDateURI)
        )
      }
    }

    "it is in edit mode" should {
      "the user selects self-employment and self-employment journey has not been completed before" when {
        "release four is disabled" should {
          s"redirect to ${controllers.individual.business.routes.BusinessNameController.show().url}" in {
            val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)

            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                propertyAccountingMethod = Some(testAccountingMethodProperty)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", NO_CONTENT)

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of the initialisation of the self employment journey")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(controllers.individual.business.routes.BusinessNameController.show().url)
            )
          }
        }

        "ReleaseFour is enabled" should {
          s"redirect to ${appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl}" in {
            enable(ReleaseFour)
            val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)

            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                selectedTaxYear = Some(testAccountingYearNext),
                propertyAccountingMethod = Some(testAccountingMethodProperty)
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
              redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl)
            )
          }
        }

      }

      "the user selected UK property and UK property journey has not been completed before" when {
        "when ReleaseFour is enabled" should {
          s" redirect to ${controllers.individual.business.routes.PropertyStartDateController.show()}" in {
            enable(ReleaseFour)
            val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
                overseasPropertyStartDate = Some(testOverseasPropertyStartDate),
                overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", 200, Json.toJson(Some(testSummaryDataSelfEmploymentData)))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", 200, Json.toJson(Some(testAccountingMethod)))

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of property commencement date")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(propertyStartDateURI)
            )
          }
        }


        "when ReleaseFour is disabled" should {
          s" redirect to ${controllers.individual.business.routes.PropertyAccountingMethodController.show().url}" in {
            val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
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
              redirectURI(accountingMethodPropertyURI)
            )
          }
        }
      }

      "the user selected overseas property and overseas property journey has not been completed before" when {
        "when ReleaseFour is enabled" should {
          s" redirect to ${controllers.individual.business.routes.OverseasPropertyStartDateController.show().url}" in {
            enable(ReleaseFour)
            val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
                propertyStartDate = Some(testPropertyStartDate),
                propertyAccountingMethod = Some(testAccountingMethodProperty)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel]("subscriptionId", userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("Businesses", 200, Json.toJson(Some(testSummaryDataSelfEmploymentData)))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("BusinessAccountingMethod", 200, Json.toJson(Some(testAccountingMethod)))

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of overseas property commencement date")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(overseasPropertyStartDateURI)
            )
          }
        }
      }


      "the user select self-employment and self-employment journey has completed before and ReleaseFour is enabled" should {
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(testAccountingYearCurrent),
              propertyStartDate = Some(testPropertyStartDate),
              propertyAccountingMethod = Some(testAccountingMethodProperty)
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
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)),
              propertyStartDate = Some(testPropertyStartDate),
              propertyAccountingMethod = Some(testAccountingMethodProperty)
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
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)),
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
        s"return an SEE OTHER (303)" + s"${controllers.individual.subscription.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
              propertyStartDate = Some(testPropertyStartDate),
              propertyAccountingMethod = Some(testAccountingMethodProperty),
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
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
              propertyStartDate = Some(testPropertyStartDate),
              propertyAccountingMethod = Some(testAccountingMethodProperty)
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
        s"return an SEE OTHER (303)" + s"${controllers.individual.subscription.routes.CheckYourAnswersController.submit()}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
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
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)),
              propertyStartDate = Some(testPropertyStartDate),
              propertyAccountingMethod = Some(testAccountingMethodProperty),
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
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show()}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
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

}
