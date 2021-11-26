/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.individual.business

import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import controllers.Assets.INTERNAL_SERVER_ERROR
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.taskListURI
import helpers.IntegrationTestModels.subscriptionData
import helpers.servicemocks.AuthStub
import models.Cash
import models.common.{OverseasPropertyModel, PropertyModel}
import play.api.http.Status.{NOT_FOUND, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.{OverseasProperty, Property}

class OverseasPropertyCheckYourAnswersControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/business/overseas-property-check-your-answers" should {
    "return OK" when {
      "save and retrieve is enabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(overseasProperty = Some(OverseasPropertyModel(accountingMethod = Some(Cash)))))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash))))
        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("GET business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getOverseasPropertyCheckYourAnswers()

        Then("Should return OK with the overseas property CYA page")
        res should have (
          httpStatus(OK),
          pageTitle(
            s"${messages("business.check-your-answers.title")} - Use software to send Income Tax updates - GOV.UK"
          )
        )
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "overseas property data cannot be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("GET business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getOverseasPropertyCheckYourAnswers()

        Then("Should return INTERNAL_SERVER_ERROR")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "return NOT_FOUND" when {
      "save and retrieve is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        And("save & retrieve feature switch is disabled")
        disable(SaveAndRetrieve)

        When("GET business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getOverseasPropertyCheckYourAnswers()

        Then("Should return NOT_FOUND")
        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/overseas-property-check-your-answers" should {
    "redirect to the task list page" when {
      "save and retrieve is enabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash))))
        IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(OverseasPropertyModel(accountingMethod = Some(Cash), confirmed = true))

        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("POST business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers()

        Then("Should return a SEE_OTHER with a redirect location of task list page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(taskListURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(OverseasPropertyModel(accountingMethod = Some(Cash), confirmed = true), Some(1))
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "overseas property data cannot be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("POST business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers()

        Then("Should return a INTERNAL_SERVER_ERROR")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "return NOT_FOUND" when {
      "save and retrieve is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        And("save & retrieve feature switch is disabled")
        disable(SaveAndRetrieve)

        When("POST business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers()

        Then("Should return NOT_FOUND")
        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }
}
