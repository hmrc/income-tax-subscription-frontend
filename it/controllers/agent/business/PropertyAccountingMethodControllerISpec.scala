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

package controllers.agent.business

import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestModels.testFullPropertyModel
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.IntegrationTestModels._
import helpers.agent.servicemocks.AuthStub
import models.Cash
import models.common._
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys.Property

class PropertyAccountingMethodControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  "GET client/business/accounting-method-property" when {
    "the Subscription Details Connector returns all data" should {
      "show the property accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))

        When("GET /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.propertyAccountingMethod()

        val expectedText = removeHtmlMarkup(messages("agent.property.accounting_method.radio.cash"))

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        Then("Should return a OK with the property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.property.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "accountingMethodProperty", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "the Subscription Details Connector returns no property accounting method" should {
      "show the property accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
          incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true))
        ))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("GET /business/property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.propertyAccountingMethod()
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.property.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "accountingMethodProperty", selectedRadioButton = None)
        )
      }
    }
  }

  "POST client/business/accounting-method-property" when {
      "save and retrieve feature switch is enabled" when {
          "select an option of the accounting method on the agent Property Accounting Method page" should {
            "redirect to agent uk property Check Your Answers" in {
              enable(SaveAndRetrieve)
              val userInput = Cash

              Given("I setup the Wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false))
              ))
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
              IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(accountingMethod = Some(userInput)))

              When("POST /business/accounting-method-property is called")
              val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

              Then("Should return a SEE_OTHER with a redirect location of agent Uk Property Check Your Answers")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(ukPropertyCheckYourAnswersURI)
              )
            }
          }
      }

      "save and retrieve feature switch is disabled" when {
        "the user does not have foreign income" should {
          "select an option of the accounting method on the agent Property Accounting Method page" should {
            "redirect to agent Check Your Answers" in {

              val userInput = Cash

              Given("I setup the Wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false))
              ))
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
              IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(accountingMethod = Some(userInput)))

              When("POST /business/accounting-method-property is called")
              val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

              Then("Should return a SEE_OTHER with a redirect location of agent Check Your Answers")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(checkYourAnswersURI)
              )
            }
          }
        }

        "the user has foreign income" when {
          "select an option on the agent property accounting method page" should {
            "redirect to agent overseas property start date page" in {
              val userInput = Cash

              Given("I setup the Wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true))
              ))
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
              IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(accountingMethod = Some(userInput)))

              When("POST /business/accounting-method-property is called")
              val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

              Then("Should return a SEE_OTHER with a redirect location of agent property start date")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(overseasPropertyStartDateURI)
              )
            }
          }
        }
      }

      "not select an option on the Property Accounting Method page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
          incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true))
        ))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyAccountingMethod, "")

        When("POST /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }


  }
}
