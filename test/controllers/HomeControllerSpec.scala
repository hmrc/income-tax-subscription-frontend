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

import audit.Logging
import auth.{MockConfig, authenticatedFakeRequest}
import config.BaseControllerConfig
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockThrottlingService


class HomeControllerSpec extends ControllerBaseSpec
  with MockThrottlingService {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> TestHomeController(false).index()
  )

  def mockBaseControllerConfig(enableThrottling: Boolean): BaseControllerConfig = {
    val et = enableThrottling
    val mockConfig = new MockConfig {
      override val enableThrottling: Boolean = et
    }
    mockBaseControllerConfig(mockConfig)
  }

  def TestHomeController(enableThrottling: Boolean) = new HomeController(
    mockBaseControllerConfig(enableThrottling),
    messagesApi,
    TestThrottlingService,
    app.injector.instanceOf[Logging]
  )

  "Calling the index action of the Home controller with an authorised user" should {

    lazy val result = TestHomeController(false).index()(authenticatedFakeRequest())

    s"get a redirection (303) to ${controllers.preferences.routes.PreferencesController.checkPreferences().url}" in {

      status(result) must be(Status.SEE_OTHER)

      redirectLocation(result).get mustBe controllers.preferences.routes.PreferencesController.checkPreferences().url
    }
  }

  "If throttling is enabled when calling the index" should {
    lazy val result = TestHomeController(true).index()(authenticatedFakeRequest())

    "trigger a call to the throttling service" in {
      setupMockCheckAccess(auth.nino)(OK)

      status(result) must be(Status.SEE_OTHER)

      redirectLocation(result).get mustBe controllers.preferences.routes.PreferencesController.checkPreferences().url

      verifyMockCheckAccess(1)
    }
  }

  "If throttling is disabled when calling the index" should {
    lazy val result = TestHomeController(false).index()(authenticatedFakeRequest())

    "not trigger a call to the throttling service" in {
      setupMockCheckAccess(auth.nino)(OK)

      status(result) must be(Status.SEE_OTHER)

      redirectLocation(result).get mustBe controllers.preferences.routes.PreferencesController.checkPreferences().url

      verifyMockCheckAccess(0)
    }
  }

  authorisationTests()

}
