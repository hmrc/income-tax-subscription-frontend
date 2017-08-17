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

package controllers.preferences

import controllers.ControllerBaseSpec
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockPreferencesService
import utils.TestConstants._

class PreferencesControllerSpec extends ControllerBaseSpec with MockPreferencesService {

  override val controllerName: String = "PreferencesControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "checkPreference" -> TestPreferencesController.checkPreferences
  )

  object TestPreferencesController extends PreferencesController(
    MockBaseControllerConfig,
    messagesApi,
    mockPreferencesService,
    mockAuthService
  )

  "Calling the checkPreference action of the PreferencesController with an authorised user" should {

    implicit lazy val request = fakeRequest

    def result = TestPreferencesController.checkPreferences(request)

    "Redirect to Income Source if paperless is activated" in {
      mockCheckPaperlessActivated()

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(controllers.routes.IncomeSourceController.showIncomeSource().url)
    }

    "Redirect to preferences service if paperless is deactivated" in {
      mockCheckPaperlessDeclined()
      mockChoosePaperlessUrl(testUrl)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get mustBe testUrl
    }

    "Redirect to preferences service if paperless was previously unspecified" in {
      mockCheckPaperlessUnset()
      mockChoosePaperlessUrl(testUrl)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get mustBe testUrl
    }

  }

  "Calling the callback action of the PreferencesController with an authorised user" should {

    implicit lazy val request = fakeRequest

    def result = TestPreferencesController.callback(request)

    "Redirect to Terms and Conditions if paperless is activated" in {
      mockCheckPaperlessActivated()

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(controllers.routes.IncomeSourceController.showIncomeSource().url)
    }

    "Redirect to do you still want to continue page if paperless deactivated" in {
      mockCheckPaperlessDeclined()

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(routes.PreferencesController.showGoBackToPreferences().url)
    }

    "Redirect to do you still want to continue page if paperless was previously unspecified" in {
      mockCheckPaperlessUnset()

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(routes.PreferencesController.showGoBackToPreferences().url)
    }

  }

  "Calling the showGoBackToPreferences action of the PreferencesController with an authorised user" should {

    lazy val result = TestPreferencesController.showGoBackToPreferences()(fakeRequest)
    lazy val document = Jsoup.parse(contentAsString(result))


    "return status (200)" in {
      status(result) must be(Status.OK)
    }

    "render the Contact Email address view" in {
      document.title() mustBe Messages("preferences_callback.title")
    }
  }

  "Calling the submitGoBackToPreferences action of the PreferencesController with an authorised user with yes" should {
    implicit lazy val request = fakeRequest

    def callShow() = TestPreferencesController.submitGoBackToPreferences()(request)

    "return a redirect status (SEE_OTHER - 303)" in {
      val goodRequest = callShow()

      status(goodRequest) must be(Status.SEE_OTHER)
    }

    s"redirects to the correct url" in {
      val goodRequest = callShow()
      redirectLocation(goodRequest) mustBe Some(TestPreferencesController.preferencesService.choosePaperlessUrl)

      await(goodRequest)
    }
  }

  authorisationTests()

}
