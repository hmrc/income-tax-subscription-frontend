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

package controllers.usermatching

import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuthStub, UserLockoutStub}
import helpers.{ComponentSpecBase, IntegrationTestModels, UserMatchingIntegrationResultSupport, ViewSpec}
import models.usermatching.UserDetailsModel
import org.jsoup.Jsoup
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse

class UserDetailsControllerISpec extends ComponentSpecBase with FeatureSwitching with UserMatchingIntegrationResultSupport {

  "GET /user-details" when {
    def fixture(agentLocked: Boolean): WSResponse = {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()

      if (agentLocked) UserLockoutStub.stubUserIsLocked(testCredId)
      else UserLockoutStub.stubUserIsNotLocked(testCredId)

      When("I call GET /user-details")
      IncomeTaxSubscriptionFrontend.showUserDetails()
    }

    "the user is locked out" should {
      "show the user details page" in {
        val res = fixture(agentLocked = true)

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(userLockedOutURI)
        )
      }
    }

    "the user is not locked out" should {
      "show the user details page" in {
        val res = fixture(agentLocked = false)
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("The result should have a status of OK")
        res should have(
          httpStatus(OK),
          pageTitle(messages("user-details.title") + serviceNameGovUk)
        )
      }
    }

    "return a view with appropriate national insurance hint" in {
      val res = fixture(agentLocked = false)
      val label = Jsoup.parse(res.body).selectOptionally("""label[for="userNino"]""")
      label.isDefined mustBe true
      // label should not contain hint refs
      label.get.selectOptionally("""div[class="form-hint"]""").isDefined mustBe false
      // Check that input has hint ref, which is not nested
      val input = Jsoup.parse(res.body).selectOptionally("""input[aria-describedby="userNino-hint"]""")
      input.isDefined mustBe true
      input.get.childrenSize() mustBe (0)
      // Check that hint exists
      Jsoup.parse(res.body).selectOptionally("""div[id="userNino-hint"]""").isDefined mustBe true
    }
  }

  "POST /user-details" when {

    "the user is locked out" should {
      "show the user lock out page" in {
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
        UserLockoutStub.stubUserIsLocked(testCredId)

        val res = IncomeTaxSubscriptionFrontend.submitUserDetails(newSubmission = None, storedSubmission = None)

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(userLockedOutURI)
        )
      }
    }

    "An invalid form is submitted" should {
      "show the user details page with validation errors" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        UserLockoutStub.stubUserIsNotLocked(testCredId)

        When("I call POST /user-details")
        val res = IncomeTaxSubscriptionFrontend.submitUserDetails(newSubmission = None, storedSubmission = None)
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("The result should have a status of BadRequest")
        res should have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("user-details.title") + serviceNameGovUk)
        )
        res.verifyStoredUserDetailsIs(None)
        IncomeTaxSubscriptionConnectorStub.verifySubscriptionDelete(Some(0))
      }
    }

    "A valid form is submitted and there are no previous user details in Subscription Details" should {
      "redirects to confirm user and saves the user details to Subscription Details" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        val userDetails: UserDetailsModel = IntegrationTestModels.testUserDetails
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        UserLockoutStub.stubUserIsNotLocked(testCredId)

        When("I call POST /user-details")
        val res = IncomeTaxSubscriptionFrontend.submitUserDetails(newSubmission = Some(userDetails), storedSubmission = Some(userDetails))

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmUserController.show().url)
        )

        res.verifyStoredUserDetailsIs(Some(userDetails))
        IncomeTaxSubscriptionConnectorStub.verifySubscriptionDelete(Some(0))
      }
    }

    "A valid form is submitted and there there is already a user details in Subscription Details which matches the new submission" should {
      "redirects to confirm user but do not modify the Subscription Details" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        val userDetails: UserDetailsModel = IntegrationTestModels.testUserDetails
        UserLockoutStub.stubUserIsNotLocked(testCredId)

        When("I call POST /user-details")
        val res = IncomeTaxSubscriptionFrontend.submitUserDetails(newSubmission = Some(userDetails), storedSubmission = Some(userDetails))

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmUserController.show().url)
        )

        res.verifyStoredUserDetailsIs(Some(userDetails))
        IncomeTaxSubscriptionConnectorStub.verifySubscriptionDelete(Some(0))
      }
    }

    "A valid form is submitted and there there is already a user details in Subscription Details  which does not match the new submission" should {
      "redirects to confirm user and wipe all previous Subscription Details  entries before saving the new user details" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        val userDetails = IntegrationTestModels.testUserDetails
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionDeleteAll()
        UserLockoutStub.stubUserIsNotLocked(testCredId)

        When("I call POST /user-details")
        val submittedUserDetails = userDetails.copy(firstName = "NotMatching")
        val res = IncomeTaxSubscriptionFrontend.submitUserDetails(newSubmission = Some(submittedUserDetails), storedSubmission = Some(userDetails))

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmUserController.show().url)
        )

        IncomeTaxSubscriptionConnectorStub.verifySubscriptionDelete(Some(1))
        res.verifyStoredUserDetailsIs(Some(submittedUserDetails))
      }
    }
  }

}
