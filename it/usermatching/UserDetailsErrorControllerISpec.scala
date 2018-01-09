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
import helpers.servicemocks.AuthStub
import play.api.http.Status.OK
import play.api.i18n.Messages

class UserDetailsErrorControllerISpec extends ComponentSpecBase with FeatureSwitching {

  // TODO remove this when the routes are moved into prod.routes
  override def config: Map[String, String] = super.config.+("application.router"->"testOnlyDoNotUseInAppConf.Routes")

  enable(UserMatchingFeature)

  "GET /error/user-details" should {
    "show the no matching user page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /error/user-details is called")
      val res = IncomeTaxSubscriptionFrontend.showUserDetailsError()

      Then("Should return a OK with the no matching user page")
      res should have(
        httpStatus(OK),
        pageTitle(Messages("user-details-error.title"))
      )
    }
  }
}
