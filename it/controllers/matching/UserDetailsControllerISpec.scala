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

package controllers.matching

import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuthStub, KeystoreStub, UserLockoutStub}
import helpers.{ComponentSpecBase, IntegrationTestModels}
import models.matching.UserDetailsModel
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import services.CacheConstants

class UserDetailsControllerISpec extends ComponentSpecBase {

  // TODO remove this when the routes are moved into prod.routes
  override def config: Map[String, String] = super.config.+("application.router"->"testOnlyDoNotUseInAppConf.Routes")

  "GET /user-details" when {
    def fixture(agentLocked: Boolean): WSResponse = {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubFullKeystore()

      if (agentLocked) UserLockoutStub.stubUserIsLocked(testUserIdStripped)
      else UserLockoutStub.stubUserIsNotLocked(testUserIdStripped)

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

        Then("The result should have a status of OK")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("user-details.title"))
        )
      }
    }
  }

  "POST /user-details" when {

    "the user is locked out" should {
      "show the user lock out page" in {
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        UserLockoutStub.stubUserIsLocked(testUserIdStripped)

        val res = IncomeTaxSubscriptionFrontend.submitUserDetails(None)

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
        KeystoreStub.stubEmptyKeystore()
        UserLockoutStub.stubUserIsNotLocked(testUserIdStripped)

        When("I call POST /user-details")
        val res = IncomeTaxSubscriptionFrontend.submitUserDetails(None)

        Then("The result should have a status of BadRequest")
        res should have(
          httpStatus(BAD_REQUEST),
          pageTitle(Messages("user-details.title"))
        )
        KeystoreStub.verifyKeyStoreSave(CacheConstants.UserDetails, None, Some(0))
        KeystoreStub.verifyKeyStoreDelete(Some(0))
      }
    }

    "A valid form is submitted and there are no previous user details in keystore" should {
      "redirects to confirm user and saves the user details to keystore" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        val userDetails: UserDetailsModel = IntegrationTestModels.testUserDetails
        KeystoreStub.stubKeystoreSave(CacheConstants.UserDetails, userDetails)
        KeystoreStub.stubEmptyKeystore()
        UserLockoutStub.stubUserIsNotLocked(testUserIdStripped)

        When("I call POST /user-details")
        val res = IncomeTaxSubscriptionFrontend.submitUserDetails(Some(userDetails))

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmUserController.show().url)
        )

        KeystoreStub.verifyKeyStoreSave(CacheConstants.UserDetails, userDetails, Some(1))
        KeystoreStub.verifyKeyStoreDelete(Some(0))
      }
    }

    "A valid form is submitted and there there is already a user details in keystore which matches the new submission" should {
      "redirects to confirm user but do not modify the keystore" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        val userDetails: UserDetailsModel = IntegrationTestModels.testUserDetails
        val userDetailsJs = Json.toJson(userDetails)
        KeystoreStub.stubKeystoreData(Map(CacheConstants.UserDetails -> userDetailsJs))
        UserLockoutStub.stubUserIsNotLocked(testUserIdStripped)

        When("I call POST /user-details")
        val res = IncomeTaxSubscriptionFrontend.submitUserDetails(Some(userDetails))

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmUserController.show().url)
        )

        KeystoreStub.verifyKeyStoreSave(CacheConstants.UserDetails, userDetails, Some(0))
        KeystoreStub.verifyKeyStoreDelete(Some(0))
      }
    }

    "A valid form is submitted and there there is already a user details in keystore which does not match the new submission" should {
      "redirects to confirm user and wipe all previous keystore entries before saving the new user details" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        val userDetails = IntegrationTestModels.testUserDetails
        val userDetailsJs = Json.toJson(userDetails)
        KeystoreStub.stubKeystoreData(Map(CacheConstants.UserDetails -> userDetailsJs))
        KeystoreStub.stubKeystoreSave(CacheConstants.UserDetails, userDetailsJs)
        KeystoreStub.stubKeystoreDelete()
        UserLockoutStub.stubUserIsNotLocked(testUserIdStripped)

        When("I call POST /user-details")
        val submittedUserDetails = userDetails.copy(firstName = "NotMatching")
        val res = IncomeTaxSubscriptionFrontend.submitUserDetails(Some(submittedUserDetails))

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmUserController.show().url)
        )

        KeystoreStub.verifyKeyStoreDelete(Some(1))
        KeystoreStub.verifyKeyStoreSave(CacheConstants.UserDetails, submittedUserDetails, Some(1))
      }
    }
  }

}
