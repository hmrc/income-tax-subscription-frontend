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

package agent.controllers.matching

import _root_.agent.auth.AgentUserMatched
import _root_.agent.controllers.ITSASessionKeys
import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.servicemocks._
import _root_.agent.helpers.{ComponentSpecBase, SessionCookieCrumbler}
import play.api.http.Status._
import _root_.agent.services.CacheConstants


class ConfirmClientControllerISpec extends ComponentSpecBase {

  import IncomeTaxSubscriptionFrontend._

  "POST /confirm-client" when {

    "general error occured" should {
      "show error page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        // n.b. failure is expected as the additional methods are not mocked

        When("I call POST /confirm-client")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

        Then("The result should have a status of INTERNAL_SERVER_ERROR")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, we are experiencing technical difficulties - 500")
        )
      }
    }

    "no client details had been filled in" should {
      "redirects to client details page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("I call POST /confirm-client")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

        Then("The result should have a status of SEE_OTHER and redirect to client details page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(clientDetailsURI)
        )
      }
    }

    "the client is not found" when {
      "the failed attempts does not exceeds the maximum lockout threshold" should {
        "redirect the user to client details error page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubFullKeystore()
          AuthenticatorStub.stubMatchNotFound()

          When("I call POST /confirm-client")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

          Then("The result should have a status of SEE_OTHER and redirect to client details error page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(clientDetailsErrorURI)
          )

          val cookie = SessionCookieCrumbler.getSessionMap(res)
          cookie.keys should contain(ITSASessionKeys.FailedClientMatching)
        }
      }

      "the failed attemps exceeded the maximum lockout threshold" should {
        "redirect the user to agent locked out page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubFullKeystore()
          KeystoreStub.stubKeystoreDelete()
          AuthenticatorStub.stubMatchNotFound()

          When("I call POST /confirm-client")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmClient(previouslyFailedAttempts = 2)

          Then("The result should have a status of SEE_OTHER and redirect to agent locked out page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(agentLockedOutURI)
          )

          val cookie = SessionCookieCrumbler.getSessionMap(res)
          cookie.keys should not contain ITSASessionKeys.FailedClientMatching

          KeystoreStub.verifyKeyStoreDelete(Some(1))
        }
      }
    }

    "the client is already subscribed" should {
      "redirect the user to client already subscribed page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        AuthenticatorStub.stubMatchFound(testNino, testUtr)
        AgentServicesStub.stubClientRelationship(testARN, testNino, exists = true)
        SubscriptionStub.stubGetSubscriptionFound()

        When("I call POST /confirm-client")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

        Then("The result should have a status of SEE_OTHER and redirect to already subscribed page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(alreadySubscribedURI)
        )
      }
    }

    "there are no prior agent client relationships" should {
      "redirects to no agent client relationship page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        AuthenticatorStub.stubMatchFound(testNino, testUtr)
        AgentServicesStub.stubClientRelationship(testARN, testNino, exists = false)

        When("I call POST /confirm-client")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

        Then("The result should have a status of SEE_OTHER and redirect to check client relationship")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(noClientRelationshipURI)
        )

        Then("The client matching request should have been audited")
        AuditStub.verifyAudit()
      }
    }

    "the agent is fully qualified" should {
      "redirects to income source page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        AuthenticatorStub.stubMatchFound(testNino, testUtr)
        SubscriptionStub.stubGetNoSubscription()
        AgentServicesStub.stubClientRelationship(testARN, testNino, exists = true)
        KeystoreStub.stubKeystoreSave(CacheConstants.MatchedNino, testNino)

        When("I call POST /confirm-client")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

        Then("The result should have a status of SEE_OTHER and redirect to index")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(indexURI)
        )

        SessionCookieCrumbler.getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) shouldBe Some(AgentUserMatched.name)

        Then("The client matching request should have been audited")
        AuditStub.verifyAudit()
      }
    }
  }
}
