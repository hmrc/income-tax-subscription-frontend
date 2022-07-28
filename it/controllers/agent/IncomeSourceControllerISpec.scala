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

import config.featureswitch.FeatureSwitch.ForeignProperty
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import connectors.stubs.IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails
import helpers.IntegrationTestModels.{testFullOverseasPropertyModel, testFullPropertyModel}
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.IntegrationTestModels._
import helpers.agent.servicemocks.AuthStub
import models.common.business.{AccountingMethodModel, BusinessNameModel, SelfEmploymentData}
import models.common.{IncomeSourceModel, OverseasPropertyModel, PropertyModel}
import org.jsoup.Jsoup
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys._

class IncomeSourceControllerISpec extends ComponentSpecBase  {

  override def beforeEach(): Unit = {
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
          val serviceNameGovUK = " - Use software to report your client’s Income Tax - GOV.UK"
          Then("Should return a OK with the income source page")
          res must have(
            httpStatus(OK),
            pageTitle(messages("agent.income_source.heading") + serviceNameGovUK),
            checkboxSet(id = "IncomeSource", selectedCheckbox = Some(messages("agent.income_source.selfEmployed"))),
            checkboxSet(id = "IncomeSource-2", selectedCheckbox = Some(messages("agent.income_source.rentUkProperty"))),
            checkboxSet(id = "IncomeSource-3", selectedCheckbox = Some(messages("agent.income_source.foreignProperty")))
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
          val serviceNameGovUK = " - Use software to report your client’s Income Tax - GOV.UK"
          Then("Should return a OK with the income source page")
          res must have(
            httpStatus(OK),
            pageTitle(messages("agent.income_source.heading") + serviceNameGovUK),
            checkboxSet(id = "IncomeSource", selectedCheckbox = None),
            checkboxSet(id = "IncomeSource-2", selectedCheckbox = None),
            checkboxSet(id = "IncomeSource-3", selectedCheckbox = None)
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
          val serviceNameGovUK = " - Use software to report your client’s Income Tax - GOV.UK"
          Then("Should return a OK with the income source page")
          res must have(
            httpStatus(OK),
            pageTitle(messages("agent.income_source.heading") + serviceNameGovUK),
            checkboxSet(id = "IncomeSource", selectedCheckbox = Some(messages("agent.income_source.selfEmployed"))),
            checkboxSet(id = "IncomeSource-2", selectedCheckbox = Some(messages("agent.income_source.rentUkProperty")))
          )

          val checkboxes = Jsoup.parse(res.body).select(".govuk-checkboxes__item")
          checkboxes.size() mustBe 2

          val checkboxTextForeignProperty = Jsoup.parse(res.body).select(s"label[for=IncomeSource-3]").text()
          checkboxTextForeignProperty mustBe empty

        }
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/income" when {
    "it is in edit mode" when {
      "the user selects self-employment and self-employment journey has not been completed before" when {
        "selected tax year page has been completed before" should {
          s"redirect to ${appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl}" in {
            val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)

            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                selectedTaxYear = Some(testAccountingYearNext)
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
            stubGetAllSubscriptionDetails(None, None, None, None, None)

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of self-employment frontend initialise")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
            )
          }
        }

        "selected tax year page has not been completed before and the user has no uk property and no overseas property income" should {
          s"redirect to ${appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl}" in {
            val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
              subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false))
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
            stubGetAllSubscriptionDetails(None, None, None, None, None)

            When("POST /details/income-receive is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of self-employment Frontend Initialise")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
            )
          }
        }

      }

      "the user selected UK property and UK property journey has not been completed before" should {
        s"redirect to ${controllers.agent.business.routes.PropertyStartDateController.show()}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false))
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(Some(testSummaryDataSelfEmploymentData), None, Some(testAccountingMethod), None, None)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of property start date")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(propertyStartDateURI)
          )
        }
      }

      "the user selected overseas property and overseas property journey has not been completed before" should {
        s"redirect to ${controllers.agent.business.routes.OverseasPropertyStartDateController.show().url}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true))
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(Some(testSummaryDataSelfEmploymentData), None, Some(testAccountingMethod), None, None)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of overseas property start date")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(overseasPropertyStartDateURI)
          )
        }
      }


      "the user select self-employment and self-employment journey has completed before" should {
        s"redirect to ${controllers.agent.routes.CheckYourAnswersController.show.url}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(testAccountingYearCurrent)
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(Some(testSummaryDataSelfEmploymentData), None, Some(testAccountingMethod), None, None)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select self-employment and UK property and both journeys have been completed before" should {
        s"redirect to ${controllers.agent.routes.CheckYourAnswersController.show}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false))
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(Some(testSummaryDataSelfEmploymentData), None, Some(testAccountingMethod), Some(testFullPropertyModel), Some(testFullOverseasPropertyModel))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select self-employment and overseas property and both journeys have been completed before" should {
        s" redirect to ${controllers.agent.routes.CheckYourAnswersController.show}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true))
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(Some(testSummaryDataSelfEmploymentData), None, Some(testAccountingMethod), None, Some(testFullOverseasPropertyModel))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select self-employment, UK property and overseas property and all three journeys have been completed before" should {
        s"return an SEE OTHER (303)" + s"${controllers.agent.routes.CheckYourAnswersController.show}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true))
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(Some(testSummaryDataSelfEmploymentData), None, Some(testAccountingMethod), Some(testFullPropertyModel), Some(testFullOverseasPropertyModel))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }


      "the user select UK property and UK property journeys has been completed before" should {
        s" redirect to ${controllers.agent.routes.CheckYourAnswersController.show}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false))
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(None, None, None, Some(testFullPropertyModel), Some(testFullOverseasPropertyModel))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select overseas property and overseas property journeys has been completed before" should {
        s"return an SEE OTHER (303)" + s" ${controllers.agent.routes.CheckYourAnswersController.submit}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true))
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(None, None, None, None, Some(testFullOverseasPropertyModel))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select UK property and overseas property and both journeys have been completed before" should {
        s"redirect to ${controllers.agent.routes.CheckYourAnswersController.show}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true))
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(None, None, None, Some(testFullPropertyModel), Some(testFullOverseasPropertyModel))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user selects self-employment and no UK property or overseas property and self-employment journey has been completed before" should {
        s"redirect to ${controllers.agent.routes.CheckYourAnswersController.show}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(testAccountingYearNext),
              accountingMethod = Some(testAccountingMethod)
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(Some(testSummaryDataSelfEmploymentData), Some(testBusinessName), Some(testAccountingMethod), None, None)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }
    }
  }

  "ForeignProperty is disabled" when {
    "not in edit mode" when {
      "the user is self-employed only" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        stubGetAllSubscriptionDetails(None, None, None, None, None)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of Business Name page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameSEURI)
        )
      }

      "the user is self-employed and has a UK property" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        stubGetAllSubscriptionDetails(None, None, None, None, None)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of self-employment client initialise page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
        )
      }

      "the user has a UK property only" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        stubGetAllSubscriptionDetails(None, None, None, None, None)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of UK property start date page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyStartDateURI)
        )
      }
    }
  }


  "ForeignProperty is enabled" when {
    "not in edit mode" when {
      "the user is self-employed only" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        stubGetAllSubscriptionDetails(None, None, None, None, None)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of Business Name page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameSEURI)
        )
      }

      "the user is self-employed and has a UK property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        stubGetAllSubscriptionDetails(None, None, None, None, None)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of self-employment client initialise page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
        )
      }

      "the user is self-employed and has a foreign property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        stubGetAllSubscriptionDetails(None, None, None, None, None)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of self-employment client initialise page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
        )
      }

      "the user is self-employed and has a UK property and a foreign property " in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        stubGetAllSubscriptionDetails(None, None, None, None, None)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of self-employment client initialise page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
        )
      }

      "the user has a UK property and a foreign property " in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        stubGetAllSubscriptionDetails(None, None, None, None, None)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of UK property start date page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyStartDateURI)
        )
      }

      "the user has a UK property only" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        stubGetAllSubscriptionDetails(None, None, None, None, None)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of UK property start date page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyStartDateURI)
        )
      }

      "the user has a foreign property only" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        stubGetAllSubscriptionDetails(None, None, None, None, None)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $INTERNAL_SERVER_ERROR with an internal server error")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(overseasPropertyStartDateURI)
        )
      }
    }
  }

  def stubGetAllSubscriptionDetails(maybeBusinesses: Option[Seq[SelfEmploymentData]],
                                    maybeBusinessName: Option[BusinessNameModel],
                                    maybeBusinessAccountingMethod: Option[AccountingMethodModel],
                                    maybeProperty: Option[PropertyModel],
                                    maybeOverseasProperty: Option[OverseasPropertyModel]): Unit = {
    maybeBusinesses match {
      case None => stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
      case Some(businesses) => stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(businesses))
    }
    maybeBusinessName match {
      case None => stubGetSubscriptionDetails(BusinessName, NO_CONTENT)
      case Some(businessName) => stubGetSubscriptionDetails(BusinessName, OK, Json.toJson(businessName))
    }
    maybeBusinessAccountingMethod match {
      case None => stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
      case Some(businessAccountingMethod) => stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(businessAccountingMethod))
    }
    maybeProperty match {
      case None => stubGetSubscriptionDetails(Property, NO_CONTENT)
      case Some(property) => stubGetSubscriptionDetails(Property, OK, Json.toJson(property))
    }
    maybeOverseasProperty match {
      case None => stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
      case Some(overseasProperty) => stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(overseasProperty))
    }
  }

}
