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

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.UserMatchingIntegrationResultSupport
import helpers.IntegrationTestConstants.{AgentURI, testARN}
import helpers.agent.servicemocks.{AgentLockoutStub, AuthStub}
import helpers.IntegrationTestModels
import helpers.agent.{ComponentSpecBase}
import models.usermatching.UserDetailsModel
import org.jsoup.Jsoup
import play.api.http.Status.{BAD_REQUEST, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.OK


class ClientDetailsControllerISpec extends ComponentSpecBase with UserMatchingIntegrationResultSupport {

  "GET /client-details" when {
    def fixture(agentLocked: Boolean): WSResponse = {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()

      if (agentLocked) AgentLockoutStub.stubAgentIsLocked(testARN)
      else AgentLockoutStub.stubAgentIsNotLocked(testARN)

      When("I call GET /client-details")
      IncomeTaxSubscriptionFrontend.showClientDetails()
    }

    "the agent is locked out" should {
      "show the client details page" in {
        val res = fixture(agentLocked = true)

        Then("The result must have a status of SEE_OTHER")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.lockedOutURI)
        )
      }
    }

    "the agent is not locked out" should {
      "show the client details page" in {
        val res = fixture(agentLocked = false)
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("The result must have a status of OK")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.client-details.title") + serviceNameGovUk)
        )
      }


      "return a view with appropriate national insurance hint" in {
        val res = fixture(agentLocked = false)
        val label = Jsoup.parse(res.body).selectOptionally("""label[for="clientNino"]""")
        label.isDefined mustBe true
        // label should not contain hint refs
        label.get.selectOptionally("""span[class="form-hint"]""").isDefined mustBe false
        // Check that input has hint ref, which is not nested
        val input = Jsoup.parse(res.body).selectOptionally("""input[aria-describedby="clientNino-hint"]""")
        input.isDefined mustBe true
        input.get.childrenSize() mustBe 0
        // Check that hint exists
        Jsoup.parse(res.body).selectOptionally("""div[id="clientNino-hint"]""").isDefined mustBe true
      }

      "return a view with the language selector" in {
        val res = fixture(agentLocked = false)
        val languageSelectNav = Jsoup.parse(res.body).selectOptionally("""nav[class="hmrc-language-select"]""")
        languageSelectNav.isDefined mustBe true
        languageSelectNav.get.selectOptionally("""a[href="/report-quarterly/income-and-expenses/sign-up/hmrc-frontend/language/cy"]""").isDefined mustBe true
      }
    }
  }

  "POST /client-details" when {

    "the agent is locked out" should {
      "show the agent lock out page" in {
        AuthStub.stubAuthSuccess()
        AgentLockoutStub.stubAgentIsLocked(testARN)

        val res = IncomeTaxSubscriptionFrontend.submitClientDetails(newSubmission = None, storedSubmission = None)

        Then("The result must have a status of SEE_OTHER")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.lockedOutURI)
        )
      }
    }

    "An invalid form is submitted" should {
      "show the client details page with validation errors" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        AgentLockoutStub.stubAgentIsNotLocked(testARN)

        When("I call POST /client-details")
        val res = IncomeTaxSubscriptionFrontend.submitClientDetails(newSubmission = None, storedSubmission = None)
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("The result must have a status of BadRequest")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("agent.client-details.title") + serviceNameGovUk)
        )

        res.verifyStoredUserDetailsIs(None)
      }
    }

    "A valid form is submitted and there are no previous user details in Subscription Details" should {
      "redirects to confirm client and saves the client details to Subscription Details" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        val clientDetails: UserDetailsModel = IntegrationTestModels.testClientDetails
        AgentLockoutStub.stubAgentIsNotLocked(testARN)

        When("I call POST /client-details")
        val res = IncomeTaxSubscriptionFrontend.submitClientDetails(newSubmission = Some(clientDetails), storedSubmission = None)

        Then("The result must have a status of SEE_OTHER")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmClientController.show().url)
        )

        res.verifyStoredUserDetailsIs(Some(clientDetails))
      }
    }

    "A valid form is submitted and there there is already a user details in Subscription Details which matches the new submission" should {
      "redirects to confirm client but do not modify the Subscription Details" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        AgentLockoutStub.stubAgentIsNotLocked(testARN)
        val clientDetails: UserDetailsModel = IntegrationTestModels.testClientDetails

        When("I call POST /client-details")
        val res = IncomeTaxSubscriptionFrontend.submitClientDetails(newSubmission = Some(clientDetails), storedSubmission = Some(clientDetails))

        Then("The result must have a status of SEE_OTHER")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmClientController.show().url)
        )

        res.verifyStoredUserDetailsIs(Some(clientDetails))
      }
    }

    "A valid form is submitted and there there is already a user details in Subscription Details which does not match the new submission" should {
      "redirects to confirm client and wipe all previous Subscription Details entries before saving the new client details" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        val clientDetails = IntegrationTestModels.testClientDetails
        AgentLockoutStub.stubAgentIsNotLocked(testARN)
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionDeleteAll()

        When("I call POST /client-details")
        val submittedUserDetails = clientDetails.copy(firstName = "NotMatching")
        val res = IncomeTaxSubscriptionFrontend.submitClientDetails(newSubmission = Some(submittedUserDetails), storedSubmission = Some(clientDetails))

        Then("The result must have a status of SEE_OTHER")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmClientController.show().url)
        )

        res.verifyStoredUserDetailsIs(Some(submittedUserDetails))

      }
    }
  }

}
