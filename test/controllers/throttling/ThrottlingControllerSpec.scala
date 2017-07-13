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

package controllers.throttling

import controllers.ControllerBaseSpec
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ThrottlingControllerSpec extends ControllerBaseSpec {

  override lazy val controllerName: String = "ThrottlingController"
  override lazy val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestThrottlingController.show
  )

  object TestThrottlingController extends ThrottlingController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService
  )

  "Calling the show action of the ThrottlingController" should {

    lazy val result = TestThrottlingController.show(fakeRequest)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

  }

  "Calling the submit action of the ThrottlingController" should {

    lazy val result = TestThrottlingController.submit(fakeRequest)

    s"return SEE_OTHER and redirects to ${controllers.routes.SignOutController.signOut().url}" in {
      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result) mustBe Some(controllers.routes.SignOutController.signOut().url)
    }

  }

  authorisationTests()

}
