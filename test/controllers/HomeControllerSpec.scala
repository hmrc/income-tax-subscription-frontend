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
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._


class HomeControllerSpec extends ControllerBaseSpec {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> TestHomeController.index()
  )

  object TestHomeController extends HomeController(
    MockBaseControllerConfig,
    messagesApi
  )

  "Calling the index action of the Home controller with an authorised user" should {

    lazy val result = TestHomeController.index()(authenticatedFakeRequest())

    s"get a redirection (303) to ${controllers.routes.IncomeSourceController.showIncomeSource().url}" in {

      status(result) must be(Status.SEE_OTHER)

      redirectLocation(result).get mustBe controllers.routes.IncomeSourceController.showIncomeSource().url
    }
  }

  authorisationTests

}
