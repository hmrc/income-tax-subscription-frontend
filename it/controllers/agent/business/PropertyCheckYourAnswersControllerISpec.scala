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

package controllers.agent.business

import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import connectors.stubs.IncomeTaxSubscriptionConnectorStub.postUri
import helpers.IntegrationTestModels.subscriptionData
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants.taskListURI
import helpers.agent.WiremockHelper.verifyPost
import helpers.agent.servicemocks.AuthStub
import models.common.PropertyModel
import models.{Cash, DateModel}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.Property

class PropertyCheckYourAnswersControllerISpec extends ComponentSpecBase  {
  "GET /report-quarterly/income-and-expenses/sign-up/client/business/uk-property-check-your-answers" should {
    "return OK" when {
      "the save and retrieve feature switch is enabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(ukProperty = Some(PropertyModel(accountingMethod = Some(Cash)))))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(PropertyModel(accountingMethod = Some(Cash))))
        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("GET business/uk-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getPropertyCheckYourAnswers()

        Then("Should return OK with the property CYA page")
        res must have (
          httpStatus(OK),
          pageTitle(
            s"${messages("agent.business.check-your-answers.content.uk-property.title")} - Use software to report your client’s Income Tax - GOV.UK"
          )
        )
      }
    }

    "return NOT_FOUND" when {
      "the save and retrieve feature switch is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("GET business/uk-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getPropertyCheckYourAnswers()

        Then("Should return INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/uk-property-check-your-answers" should {
    "redirect to the agent task list page" when {
      "the save and retrieve feature switch is enabled" when {
        "the client has answered all the questions for uk property" should {
          "redirect to the agent tasks list page and save property answers" in {
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              Property,
              OK,
              Json.toJson(PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021"))))
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(
              PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")), confirmed = true)
            )
            And("save & retrieve feature switch is enabled")
            enable(SaveAndRetrieve)

            When("POST business/uk-property-check-your-answers is called")
            val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

            Then("Should return a SEE_OTHER with a redirect location of task list page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(taskListURI)
            )

            IncomeTaxSubscriptionConnectorStub.verifySaveProperty(PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")), confirmed = true), Some(1))
          }
        }

        "the client has answered partial questions for uk property" should {
          "redirect to the agent tasks list page and do not save property answers" in {
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              Property,
              OK,
              Json.toJson(PropertyModel(accountingMethod = Some(Cash)))
            )

            And("save & retrieve feature switch is enabled")
            enable(SaveAndRetrieve)

            When("POST business/uk-property-check-your-answers is called")
            val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

            Then("Should return a SEE_OTHER with a redirect location of task list page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(taskListURI)
            )
            verifyPost(postUri(Property), count= Some(0))

          }
        }
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the property details could not be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("POST business/uk-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "return NOT_FOUND" when {
      "the save and retrieve feature switch is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        And("save & retrieve feature switch is disabled")
        disable(SaveAndRetrieve)

        When("POST business/uk-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

        Then("Should return NOT_FOUND")
        res must have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }
}
