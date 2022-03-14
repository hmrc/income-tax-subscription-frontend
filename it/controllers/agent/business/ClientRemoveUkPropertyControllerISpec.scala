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
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.IntegrationTestModels.ukPropertySubscriptionData
import helpers.agent.servicemocks.AuthStub
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.Property

class ClientRemoveUkPropertyControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/remove-uk-property-business" when {

    "return OK" when {
      "save and retrieve feature switch is enabled" in {
        Given("I setup the Wiremock stubs")
        enable(SaveAndRetrieve)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(ukPropertySubscriptionData))

        When("GET client/business/remove-uk-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.getClientRemoveUkProperty
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the client remove Uk property confirmation page displaying")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.remove-uk-property-business.heading") + serviceNameGovUk)
        )
      }
    }

    "return NOT_FOUND" when {
      "save and retrieve is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(ukPropertySubscriptionData))

        When("GET client/business/remove-uk-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.getClientRemoveUkProperty

        Then("Should return NOT_FOUND")
        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/remove-uk-property-business" when {

    "save and retrieve is enabled" should {
      "redirect to the client task list page" when {
        "the user submits the 'yes' answer" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(ukPropertySubscriptionData))
          IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(Property)
          enable(SaveAndRetrieve)

          When("POST client/business/remove-uk-property-business is called")
          val res = IncomeTaxSubscriptionFrontend.submitClientRemoveUkProperty(Map("yes-no" -> Seq("Yes")))

          Then("Should return a SEE_OTHER with a redirect location of client task list page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(taskListURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(Property, Some(1))
        }

        "the user submits the 'no' answer" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(ukPropertySubscriptionData))
          enable(SaveAndRetrieve)

          When("POST client/business/remove-uk-property-business is called")
          val res = IncomeTaxSubscriptionFrontend.submitClientRemoveUkProperty(Map("yes-no" -> Seq("No")))

          Then("Should return a SEE_OTHER with a redirect location of client task list page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(taskListURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(Property, Some(0))
        }
      }
      "return a BAD_REQUEST" when {
        "no option was selected on the client remove Uk property page" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(ukPropertySubscriptionData))
          enable(SaveAndRetrieve)

          When("POST /business/remove-uk-property-business is called")
          val res = IncomeTaxSubscriptionFrontend.submitClientRemoveUkProperty(Map("yes-no" -> Seq("")))

          Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
          res should have(
            httpStatus(BAD_REQUEST),
            errorDisplayed()
          )
          IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(Property, Some(0))
        }
      }
    }

    "save and retrieve is disabled" should {
      "return NOT_FOUND" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(ukPropertySubscriptionData))

        When("POST business/remove-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitClientRemoveUkProperty(Map("yes-no" -> Seq("Yes")))

        Then("Should return NOT_FOUND")
        res should have(
          httpStatus(NOT_FOUND)
        )

        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(Property, Some(0))
      }
    }

  }
}
