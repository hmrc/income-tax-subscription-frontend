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
import controllers.agent.ITSASessionKeys
import helpers.IntegrationTestModels.subscriptionData
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants.{taskListURI, testUtr}
import helpers.agent.WiremockHelper.verifyPost
import helpers.agent.servicemocks.AuthStub
import models.common.OverseasPropertyModel
import models.{Cash, DateModel}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.OverseasProperty

class OverseasPropertyCheckYourAnswersControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-check-your-answers" should {
    "return OK" when {
      "the save and retrieve feature switch is enabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(overseasProperty = Some(OverseasPropertyModel(accountingMethod = Some(Cash)))))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash))))
        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("GET business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getOverseasPropertyCheckYourAnswers(Map(ITSASessionKeys.UTR -> testUtr))

        Then("Should return OK with the property CYA page")
        res must have(
          httpStatus(OK),
          pageTitle(
            s"${messages("agent.business.check-your-answers.content.overseas-property.title")} - Use software to report your client’s Income Tax - GOV.UK"
          )
        )
      }
    }

    "return NOT_FOUND" when {
      "the save and retrieve feature switch is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        And("save & retrieve feature switch is disabled")
        disable(SaveAndRetrieve)

        When("GET business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getOverseasPropertyCheckYourAnswers(Map(ITSASessionKeys.UTR -> testUtr))

        Then("Should return NOT_FOUND")
        res must have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-check-your-answers" should {
    "redirect to the agent task list page" when {
      "the save and retrieve feature switch is enabled" when {
        "the user answered all the overseas property questions" should {
          "save the property answers" in {
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash),
              startDate = Some(DateModel("10", "11", "2021")))))
            IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(OverseasPropertyModel(accountingMethod = Some(Cash), confirmed = true,
              startDate = Some(DateModel("10", "11", "2021"))))

            And("save & retrieve feature switch is enabled")
            enable(SaveAndRetrieve)

            When("POST business/overseas-property-check-your-answers is called")
            val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers(Map(ITSASessionKeys.UTR -> testUtr))

            Then("Should return a SEE_OTHER with a redirect location of task list page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(taskListURI)
            )

            IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(OverseasPropertyModel(accountingMethod = Some(Cash),
              confirmed = true, startDate = Some(DateModel("10", "11", "2021"))), Some(1))
          }
        }

        "the user answered partial overseas property questions" should {
          "not save the property answers" in {
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash))))
            And("save & retrieve feature switch is enabled")
            enable(SaveAndRetrieve)

            When("POST business/overseas-property-check-your-answers is called")
            val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers(Map(ITSASessionKeys.UTR -> testUtr))

            Then("Should return a SEE_OTHER with a redirect location of task list page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(taskListURI)
            )

            verifyPost(postUri(OverseasProperty), count = Some(0))
          }
        }
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the property details could not be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("POST business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers()

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

        When("POST business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers(Map(ITSASessionKeys.UTR -> testUtr))

        Then("Should return NOT_FOUND")
        res must have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }
}
