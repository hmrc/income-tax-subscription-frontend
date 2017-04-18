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

import auth.authenticatedFakeRequest
import org.scalatest.Matchers._
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._


class SignOutControllerSpec extends ControllerBaseSpec {

  object TestSignOutController extends SignOutController(
    app,
    appConfig
  )

  override val controllerName: String = "SignOutController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "signOut" -> TestSignOutController.signOut
  )

  "Authorised users" should {
    "be redirected to the gg signOut" in {
      val result = TestSignOutController.signOut(authenticatedFakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should be(appConfig.ggSignOutUrl)
    }
  }

  authorisationTests()

}
