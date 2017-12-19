/*
 * Copyright 2017 HM Revenue & Customs
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

package usermatching

import core.config.featureswitch.UnauthorisedAgentFeature
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{GGAuthenticationStub, _}
import play.api.http.Status._
import play.api.i18n.Messages

class ConfirmAgentSubscriptionControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/confirm-agent-subscription" when {
    "the unauthorised agent feature switch is enabled" should {
      "return the subscription confirmation page" in {
        Given("The feature switch is on")
        enable(UnauthorisedAgentFeature)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /confirm-agent-subscription is called")
        val res = IncomeTaxSubscriptionFrontend.confirmAgentSubscription()

        Then("Should return an OK with the error main income page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("confirm-agent-subscription.title"))
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/confirm-agent-subscription" when {
    "the unauthorised agent feature switch is enabled" should {
      "submit to DES and redirect to the confirmation page" in {
        Given("The feature switch is on")
        enable(UnauthorisedAgentFeature)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        SubscriptionStub.stubSuccessfulSubscription(confirmAgentSubscriptionUri)
        GGAdminStub.stubAddKnownFactsResult(OK)
        GGConnectorStub.stubEnrolResult(OK)
        GGAuthenticationStub.stubRefreshProfileResult(NO_CONTENT)
        KeystoreStub.stubPutMtditId()

        When("POST  confirm-agent-subscription is called")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmAgentSubscription()

        Then("Should return a SEE_OTHER with a redirect location of confirmation")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(confirmationURI)
        )
      }
    }
  }
}

