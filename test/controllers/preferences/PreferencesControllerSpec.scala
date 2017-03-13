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

import auth._
import controllers.ControllerBaseSpec
import forms.preferences.BackToPreferencesForm._
import models.preferences.BackToPreferencesModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockPreferencesService

class PreferencesControllerSpec extends ControllerBaseSpec
  with MockPreferencesService {

  override val controllerName: String = "PreferencesControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "checkPreference" -> TestPreferencesController.checkPreferences
  )

  object TestPreferencesController extends PreferencesController(
    MockBaseControllerConfig,
    messagesApi,
    TestPreferencesService
  )

  "Calling the checkPreference action of the PreferencesController with an authorised user" should {

    implicit lazy val request = authenticatedFakeRequest()

    def result = TestPreferencesController.checkPreferences(request)

    "Redirect to Income Source if paperless is activated" in {
      setupCheckPaperless(paperlessActivated)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(controllers.routes.IncomeSourceController.showIncomeSource().url)
    }

    "Redirect to preferences service if paperless is deactivated" in {
      setupCheckPaperless(paperlessDeclined)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(TestPreferencesService.choosePaperlessUrl)
    }

    "Redirect to preferences service if paperless was previously unspecified" in {
      setupCheckPaperless(paperlessPreconditionFailed)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(TestPreferencesService.choosePaperlessUrl)
    }

  }

  "Calling the callback action of the PreferencesController with an authorised user" should {

    implicit lazy val request = authenticatedFakeRequest()

    def result = TestPreferencesController.callback(request)

    "Redirect to Terms and Conditions if paperless is activated" in {
      setupCheckPaperless(paperlessActivated)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(controllers.routes.IncomeSourceController.showIncomeSource().url)
    }

    "Redirect to do you still want to continue page if paperless deactivated" in {
      setupCheckPaperless(paperlessDeclined)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(routes.PreferencesController.showGoBackToPreferences().url)
    }

    "Redirect to do you still want to continue page if paperless was previously unspecified" in {
      setupCheckPaperless(paperlessPreconditionFailed)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(routes.PreferencesController.showGoBackToPreferences().url)
    }

  }

  "Calling the showGoBackToPreferences action of the PreferencesController with an authorised user" should {

    lazy val result = TestPreferencesController.showGoBackToPreferences()(authenticatedFakeRequest())
    lazy val document = Jsoup.parse(contentAsString(result))


    "return status (200)" in {
      status(result) must be(Status.OK)
    }

    "render the Contact Email address view" in {
      document.title() mustBe Messages("preferences_callback.title")
    }
  }

  "Calling the submitGoBackToPreferences action of the PreferencesController with an authorised user with yes" should {
    implicit lazy val request = authenticatedFakeRequest().post(backToPreferencesForm, BackToPreferencesModel(option_yes))

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

  "Calling the submitGoBackToPreferences action of the PreferencesController with an authorised user with no" should {
    def callShow() = TestPreferencesController.submitGoBackToPreferences()(authenticatedFakeRequest()
      .post(backToPreferencesForm, BackToPreferencesModel(option_no)))

    "return a redirect status (NOT_IMPLEMENTED - 501)" in {
      val goodRequest = callShow()

      status(goodRequest) must be(Status.NOT_IMPLEMENTED)
    }

  }

  "Calling the submitGoBackToPreferences action of the PreferencesController with an authorised user and invalid submission" should {
    lazy val callShow = TestPreferencesController.submitGoBackToPreferences()(authenticatedFakeRequest()
      .post(backToPreferencesForm, BackToPreferencesModel("")))

    "return a redirect status (SEE_OTHER - 303)" in {
      val goodRequest = callShow

      status(goodRequest) must be(Status.BAD_REQUEST)
    }

  }

  authorisationTests()

}
