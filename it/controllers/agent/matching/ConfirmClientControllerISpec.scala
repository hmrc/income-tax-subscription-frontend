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

package controllers.agent.matching

import _root_.common.Constants.ITSASessionKeys
import auth.agent.AgentUserMatched
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.UserMatchingIntegrationResultSupport
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.servicemocks.{AgentServicesStub, AuthStub}
import helpers.servicemocks.{AuthStub => _, _}
import play.api.http.Status._


class ConfirmClientControllerISpec extends ComponentSpecBase with UserMatchingIntegrationResultSupport {


  "POST /confirm-client" when {

    "general error occured" should {
      "show error page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        UserLockoutStub.stubUserIsNotLocked(testARN)
        AuthenticatorStub.stubMatchFailure()
        // n.b. failure is expected as the additional methods are not mocked

        When("I call POST /confirm-client")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

        Then("The result must have a status of INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, we are experiencing technical difficulties - 500")
        )
      }
    }

    "no client details had been filled in" should {
      "redirects to client details page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        UserLockoutStub.stubUserIsNotLocked(testARN)
        AuthenticatorStub.stubMatchNotFound()

        When("I call POST /confirm-client")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmClient(storedUserDetails = None)

        Then("The result must have a status of SEE_OTHER and redirect to client details page")
        res must have(
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
          UserLockoutStub.stubUserIsNotLocked(testARN)
          AuthenticatorStub.stubMatchNotFound()

          When("I call POST /confirm-client")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

          Then("The result must have a status of SEE_OTHER and redirect to client details error page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(clientDetailsErrorURI)
          )

          val cookie = getSessionMap(res)
          cookie.keys must contain(ITSASessionKeys.FailedClientMatching)
        }
      }

      "the failed attemps exceeded the maximum lockout threshold" should {
        "redirect the user to agent locked out page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionDeleteAll()
          UserLockoutStub.stubUserIsNotLocked(testARN)
          UserLockoutStub.stubLockAgent(testARN)
          AuthenticatorStub.stubMatchNotFound()

          When("I call POST /confirm-client")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmClient(previouslyFailedAttempts = 2)

          Then("The result must have a status of SEE_OTHER and redirect to agent locked out page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(agentLockedOutURI)
          )

          val cookie = getSessionMap(res)
          cookie.keys must not contain ITSASessionKeys.FailedClientMatching
        }
      }
    }

    "the client is already subscribed" should {
      "redirect the user to client already subscribed page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        AuthenticatorStub.stubMatchFound(testNino, Some(testUtr))
        AgentServicesStub.stubClientRelationship(testARN, testNino, exists = true)
        SubscriptionStub.stubGetSubscriptionFound()
        UserLockoutStub.stubUserIsNotLocked(testARN)


        When("I call POST /confirm-client")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

        Then("The result must have a status of SEE_OTHER and redirect to already subscribed page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(alreadySubscribedURI)
        )
      }
    }

    "there are no prior agent client relationships" when {
      "redirects to no agent client relationship page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        AuthenticatorStub.stubMatchFound(testNino, Some(testUtr))
        AgentServicesStub.stubClientRelationship(testARN, testNino, exists = false)
        SubscriptionStub.stubGetNoSubscription()
        UserLockoutStub.stubUserIsNotLocked(testARN)

        When("I call POST /confirm-client")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

        Then("The result must have a status of SEE_OTHER and redirect to check client relationship")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(noClientRelationshipURI)
        )

        Then("The client matching request must have been audited")
        AuditStub.verifyAudit()
      }
    }
    

    "the client does not have an SAUTR" should {
      "redirect to the sign up for self assessment page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        AgentServicesStub.stubClientRelationship(testARN, testNino, exists = true)
        AuthenticatorStub.stubMatchFound(testNino, None)
        SubscriptionStub.stubGetNoSubscription()
        UserLockoutStub.stubUserIsNotLocked(testARN)

        When("I call POST /confirm-client")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

        Then("The result must have a status of SEE_OTHER and redirect to index")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(indexURI)
        )

        val session = getSessionMap(res)
        session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatched.name)
        session.get(ITSASessionKeys.NINO) mustBe Some(testNino)
        session.get(ITSASessionKeys.UTR) mustBe None

        Then("The client matching request must have been audited")
        AuditStub.verifyAudit()
      }
    }

    "the agent is fully qualified" when {
      "the user is eligible" should {
        "redirect to the home page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          AuthenticatorStub.stubMatchFound(testNino, Some(testUtr))
          SubscriptionStub.stubGetNoSubscription()
          AgentServicesStub.stubClientRelationship(testARN, testNino, exists = true)
          EligibilityStub.stubEligibilityResponse(testUtr)(response = true)
          UserLockoutStub.stubUserIsNotLocked(testARN)

          When("I call POST /confirm-client")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

          Then("The result must have a status of SEE_OTHER and redirect to index")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(indexURI)
          )

          val session = getSessionMap(res)
          session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatched.name)
          session.get(ITSASessionKeys.NINO) mustBe Some(testNino)
          session.get(ITSASessionKeys.UTR) mustBe Some(testUtr)

          Then("The client matching request must have been audited")
          AuditStub.verifyAudit()
        }
      }
      "the user is ineligible" should {
        "redirect to the ineligible page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          AuthenticatorStub.stubMatchFound(testNino, Some(testUtr))
          SubscriptionStub.stubGetNoSubscription()
          AgentServicesStub.stubClientRelationship(testARN, testNino, exists = true)
          EligibilityStub.stubEligibilityResponse(testUtr)(response = false)
          UserLockoutStub.stubUserIsNotLocked(testARN)

          When("I call POST /confirm-client")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

          Then("The result must have a status of SEE_OTHER and redirect to cannot take part")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.eligibility.routes.CannotTakePartController.show.url)
          )

          Then("The client matching request must have been audited")
          AuditStub.verifyAudit()
        }
      }
    }
  }
}
