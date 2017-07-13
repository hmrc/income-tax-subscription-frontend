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

package controllers

import config.FrontendAppConfig
import org.mockito.Mockito
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import play.api.{Configuration, Environment}

class SignInControllerSpec extends ControllerBaseSpec {

  val mockFrontendConfig = mock[FrontendAppConfig]

  object TestSignInController extends SignInController(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[Configuration],
    app.injector.instanceOf[Environment]
  )

  "navigating to SignIn page" should {
    lazy val result = TestSignInController.signIn(fakeRequest)

    "return OK (303)" in {
      status(result) mustBe Status.SEE_OTHER
    }

    "Redirect to GG Sign In on Company Auth Frontend" in {
      redirectLocation(result) must contain(
        "/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9561%2Freport-quarterly%2Fincome-and-expenses%2Fsign-up%2Findex&origin=income-tax-subscription-frontend"
      )
    }

  }
  override val controllerName: String = "SignInController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map.empty
}
