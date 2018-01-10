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

package usermatching

import core.config.featureswitch.{FeatureSwitching, UserMatchingFeature}
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuthStub, UserLockoutStub}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.Messages

class UserDetailsLockoutControllerISpec extends ComponentSpecBase with FeatureSwitching {

  // TODO remove this when the routes are moved into prod.routes
  override def config: Map[String, String] = super.config.+("application.router"->"testOnlyDoNotUseInAppConf.Routes")

  enable(UserMatchingFeature)

  "GET /error/lockout" when {
    "the agent is still locked out" should {
      "show the locked out page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        UserLockoutStub.stubUserIsLocked(testUserIdEncoded)

        When("I call GET /error/lockout")
        val res = IncomeTaxSubscriptionFrontend.showUserDetailsLockout()

        Then("The result should have a status of OK")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("user-details-lockout.title"))
        )
      }
    }

    "the agent is no longer locked out" should {
      "show the user details page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        UserLockoutStub.stubUserIsNotLocked(testUserIdEncoded)

        When("I call GET /error/lockout")
        val res = IncomeTaxSubscriptionFrontend.showUserDetailsLockout()

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(userDetailsURI)
        )
      }
    }
  }

}
