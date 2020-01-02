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

import _root_.agent.helpers.IntegrationTestConstants.{agentLockedOutURI, testARN}
import _root_.agent.helpers.servicemocks.{AgentLockoutStub, AuthStub, KeystoreStub}
import _root_.agent.helpers.{ComponentSpecBase, IntegrationTestModels}
import helpers.UserMatchingIntegrationResultSupport
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.libs.ws.WSResponse
import usermatching.models.UserDetailsModel

class ClientDetailsControllerISpec extends ComponentSpecBase with UserMatchingIntegrationResultSupport {

  "GET /client-details" when {
    def fixture(agentLocked: Boolean): WSResponse = {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubFullKeystore()

      if (agentLocked) AgentLockoutStub.stubAgentIsLocked(testARN)
      else AgentLockoutStub.stubAgentIsNotLocked(testARN)

      When("I call GET /client-details")
      IncomeTaxSubscriptionFrontend.showClientDetails()
    }

    "the agent is locked out" should {
      "show the client details page" in {
        val res = fixture(agentLocked = true)

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(agentLockedOutURI)
        )
      }
    }

    "the agent is not locked out" should {
      "show the client details page" in {
        val res = fixture(agentLocked = false)

        Then("The result should have a status of OK")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.client-details.title"))
        )
      }
    }
  }

  "POST /client-details" when {

    "the agent is locked out" should {
      "show the agent lock out page" in {
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        AgentLockoutStub.stubAgentIsLocked(testARN)

        val res = IncomeTaxSubscriptionFrontend.submitClientDetails(newSubmission = None, storedSubmission = None)

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(agentLockedOutURI)
        )
      }
    }

    "An invalid form is submitted" should {
      "show the client details page with validation errors" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("I call POST /client-details")
        val res = IncomeTaxSubscriptionFrontend.submitClientDetails(newSubmission = None, storedSubmission = None)

        Then("The result should have a status of BadRequest")
        res should have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + Messages("agent.client-details.title"))
        )

        res.verifyStoredUserDetailsIs(None)
        KeystoreStub.verifyKeyStoreDelete(Some(0))
      }
    }

    "A valid form is submitted and there are no previous user details in keystore" should {
      "redirects to confirm client and saves the client details to keystore" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        val clientDetails: UserDetailsModel = IntegrationTestModels.testClientDetails
        KeystoreStub.stubEmptyKeystore()

        When("I call POST /client-details")
        val res = IncomeTaxSubscriptionFrontend.submitClientDetails(newSubmission = Some(clientDetails), storedSubmission = None)

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmClientController.show().url)
        )

        res.verifyStoredUserDetailsIs(Some(clientDetails))
        KeystoreStub.verifyKeyStoreDelete(Some(0))
      }
    }

    "A valid form is submitted and there there is already a user details in keystore which matches the new submission" should {
      "redirects to confirm client but do not modify the keystore" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        val clientDetails: UserDetailsModel = IntegrationTestModels.testClientDetails

        When("I call POST /client-details")
        val res = IncomeTaxSubscriptionFrontend.submitClientDetails(newSubmission = Some(clientDetails), storedSubmission = Some(clientDetails))

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmClientController.show().url)
        )

        res.verifyStoredUserDetailsIs(Some(clientDetails))
        KeystoreStub.verifyKeyStoreDelete(Some(0))
      }
    }

    "A valid form is submitted and there there is already a user details in keystore which does not match the new submission" should {
      "redirects to confirm client and wipe all previous keystore entries before saving the new client details" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        val clientDetails = IntegrationTestModels.testClientDetails
        KeystoreStub.stubKeystoreDelete()

        When("I call POST /client-details")
        val submittedUserDetails = clientDetails.copy(firstName = "NotMatching")
        val res = IncomeTaxSubscriptionFrontend.submitClientDetails(newSubmission = Some(submittedUserDetails), storedSubmission = Some(clientDetails))

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmClientController.show().url)
        )

        KeystoreStub.verifyKeyStoreDelete(Some(1))
        res.verifyStoredUserDetailsIs(Some(submittedUserDetails))

      }
    }
  }

}
