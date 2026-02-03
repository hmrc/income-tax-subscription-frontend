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
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants._
import helpers.UserMatchingIntegrationResultSupport
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.{AgentServicesStub, AuthStub}
import helpers.servicemocks.{AuthStub => _, _}
import play.api.http.Status._


class ConfirmClientControllerISpec extends ComponentSpecBase with UserMatchingIntegrationResultSupport {

  s"GET ${routes.ConfirmClientController.show().url}" when {
    "the user is not authenticated" should {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.confirmClient()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/confirm-client"))
        )
      }
    }
    "the user has no journey state" should {
      "redirect to the add another client controller" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.confirmClient(withJourneyState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.AddAnotherClientController.addAnother().url)
        )
      }
    }
    "authenticated and in the client details state" should {
      "display the page" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.confirmClient()

        result must have(
          httpStatus(OK)
        )
      }
    }
  }

  "POST /confirm-client" when {
    "the user is not authenticated" should {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.submitConfirmClient()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/confirm-client"))
        )
      }
    }
    "the user has no journey state" should {
      "redirect to the add another client controller" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.submitConfirmClient(withJourneyState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.AddAnotherClientController.addAnother().url)
        )
      }
    }
    "authenticated and in the client details state" when {
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
            pageTitle("Sorry, there is a problem with the service")
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
            redirectURI(AgentURI.clientDetailsURI)
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
              redirectURI(AgentURI.clientDetailsErrorURI)
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
              redirectURI(AgentURI.lockedOutURI)
            )

            val cookie = getSessionMap(res)
            cookie.keys must not contain ITSASessionKeys.FailedClientMatching
          }
        }
      }

      "the client is deceased, indicated by 424 status from authenticator" should {
        "redirect the user to the client details error page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          UserLockoutStub.stubUserIsNotLocked(testARN)
          AuthenticatorStub.stubMatchDeceased()

          When("I call POST /confirm-client")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

          Then("The result must have a status of SEE_OTHER and redirect to client details error page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.clientDetailsErrorURI)
          )

          val cookie = getSessionMap(res)
          cookie.keys must contain(ITSASessionKeys.FailedClientMatching)
        }
      }

      "the client is already subscribed" should {
        "redirect the user to client already subscribed page when user has a mtd relationship" in {

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          UserLockoutStub.stubUserIsNotLocked(testARN)
          AuthenticatorStub.stubMatchFound(testNino, Some(testUtr))
          AgentServicesStub.stubMTDRelationship(testARN, testMtdId, exists = true)
          SubscriptionStub.stubGetSubscriptionFound()



          When("I call POST /confirm-client")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

          Then("The result must have a status of SEE_OTHER and redirect to already subscribed page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.alreadySubscribedURI)
          )
        }

        "redirect the user to client already subscribed page when user has an mtdSupprelationship" in {

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          UserLockoutStub.stubUserIsNotLocked(testARN)
          AuthenticatorStub.stubMatchFound(testNino, Some(testUtr))
          AgentServicesStub.stubMTDRelationship(testARN, testMtdId, exists = true)
          SubscriptionStub.stubGetSubscriptionFound()


          When("I call POST /confirm-client")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

          Then("The result must have a status of SEE_OTHER and redirect to already subscribed page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.alreadySubscribedURI)
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
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.NINO, testNino)(OK)

          When("I call POST /confirm-client")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

          Then("The result must have a status of SEE_OTHER and redirect to check client relationship")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.clientRelationshipURI)
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
            redirectURI(AgentURI.registerForSAURI)
          )

          Then("The client matching request must have been audited")
          AuditStub.verifyAudit()
        }
      }

      "the agent is fully qualified" should {
        "redirect to the confirmed client resolver" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          AuthenticatorStub.stubMatchFound(testNino, Some(testUtr))
          SubscriptionStub.stubGetNoSubscription()
          AgentServicesStub.stubClientRelationship(testARN, testNino, exists = true)
          UserLockoutStub.stubUserIsNotLocked(testARN)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.NINO, testNino)(OK)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.UTR, testUtr)(OK)

          When("I call POST /confirm-client")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmClient()

          Then("The result must have a status of SEE_OTHER and redirect to the client can sign up controller")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.matching.routes.ConfirmedClientResolver.resolve.url)
          )

          Then("The client matching request must have been audited")
          AuditStub.verifyAudit()
        }
      }
    }
  }
}
