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

package usermatching.controllers

import auth.{MockConfig, UserMatching}
import config.AppConfig
import controllers.{ControllerBaseSpec, ITSASessionKeys}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class NinoResolverControllerSpec extends ControllerBaseSpec {
  override val controllerName: String = "NinoResolverController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "resolveNino" -> TestNinoResolverController.resolveNino
  )

  def createTestNinoResolverController(appConfig: AppConfig): NinoResolverController =
    new NinoResolverController(
      mockBaseControllerConfig(appConfig),
      messagesApi,
      mockAuthService
    )

  lazy val TestNinoResolverController = createTestNinoResolverController(
    new MockConfig {
      override val userMatchingFeature = true
    })

  "NinoResolverController.resolveNino" when {
    "if userMatchingFeature is set to true" should {
      lazy val callResolve = createTestNinoResolverController(
        new MockConfig {
          override val userMatchingFeature = true
        }).resolveNino(subscriptionRequest)

      "go to user details" in {
        mockIndividualWithNoEnrolments()

        val result = callResolve

        status(result) mustBe Status.SEE_OTHER

        redirectLocation(result).get mustBe usermatching.controllers.routes.UserDetailsController.show().url

        await(result).session(subscriptionRequest).get(ITSASessionKeys.JourneyStateKey) mustBe Some(UserMatching.name)
      }
    }

    "if userMatchingFeature is set to false" should {
      lazy val callResolve = createTestNinoResolverController(
        new MockConfig {
          override val userMatchingFeature = false
        }).resolveNino(subscriptionRequest)

      "go to IV" in {
        mockIndividualWithNoEnrolments()

        val result = callResolve

        status(result) mustBe Status.SEE_OTHER

        redirectLocation(result).get mustBe identityverification.controllers.routes.IdentityVerificationController.gotoIV().url
      }
    }
  }

  authorisationTests()

}
