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
import helpers.{ComponentSpecBase, SessionCookieCrumbler}
import helpers.IntegrationTestConstants._
import helpers.servicemocks._
import play.api.http.Status._
import utilities.ITSASessionKeys


class ConfirmUserControllerISpec extends ComponentSpecBase with SessionCookieCrumbler with FeatureSwitching {

  "POST /confirm-user" when {

    "general error occured" should {
      "show error page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
        UserLockoutStub.stubUserIsNotLocked(testCredId)
        AuthenticatorStub.stubMatchFailure()
        // n.b. failure is expected as the additional methods are not mocked

        When("I call POST /confirm-user")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmUser()

        Then("The result should have a status of INTERNAL_SERVER_ERROR")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, we are experiencing technical difficulties - 500")
        )
      }
    }

    "no user details had been filled in" should {
      "redirects to user details page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        UserLockoutStub.stubUserIsNotLocked(testCredId)

        When("I call POST /confirm-user")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmUser(storedUserDetails = None)

        Then("The result should have a status of SEE_OTHER and redirect to user details page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(userDetailsURI)
        )
      }
    }

    "the user is not found" when {
      "the failed attempts does not exceeds the maximum lockout threshold" should {
        "redirect the user to user details error page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
          UserLockoutStub.stubUserIsNotLocked(testCredId)
          AuthenticatorStub.stubMatchNotFound()

          When("I call POST /confirm-user")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmUser()

          Then("The result should have a status of SEE_OTHER and redirect to user details error page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(userDetailsErrorURI)
          )

          val cookie = getSessionMap(res)
          cookie.keys should contain(ITSASessionKeys.FailedUserMatching)
        }
      }

      "the failed attempts exceeded the maximum lockout threshold" should {
        "redirect the user to agent locked out page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionDeleteAll()
          UserLockoutStub.stubUserIsNotLocked(testCredId)
          UserLockoutStub.stubLockAgent(testCredId)
          AuthenticatorStub.stubMatchNotFound()

          When("I call POST /confirm-user")
          val res = IncomeTaxSubscriptionFrontend.submitConfirmUser(previouslyFailedAttempts = 2)

          Then("The result should have a status of SEE_OTHER and redirect to agent locked out page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(userLockedOutURI)
          )

          val cookie = getSessionMap(res)
          cookie.keys should not contain ITSASessionKeys.FailedUserMatching
        }
      }
    }

    "the match is successful" should {
      "redirect to income source page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
        AuthenticatorStub.stubMatchFound(testNino)
        UserLockoutStub.stubUserIsNotLocked(testCredId)
        SubscriptionStub.stubGetNoSubscription()

        When("I call POST /confirm-user")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmUser()

        Then("The result should have a status of SEE_OTHER and redirect to income source")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(indexURI)
        )
      }
    }
  }
}
