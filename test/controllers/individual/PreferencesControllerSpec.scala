/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.individual

import controllers.ControllerBaseSpec
import config.featureswitch.FeatureSwitching
import utilities.individual.TestConstants._
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.{MockPaperlessPreferenceTokenService, MockPreferencesService, MockSubscriptionDetailsService}
import utilities.ITSASessionKeys

import scala.concurrent.Future

class PreferencesControllerSpec extends ControllerBaseSpec
  with MockPreferencesService
  with MockSubscriptionDetailsService
  with MockPaperlessPreferenceTokenService
  with FeatureSwitching {

  override val controllerName: String = "PreferencesController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "checkPreference" -> TestPreferencesController.checkPreferences
  )

  object TestPreferencesController extends PreferencesController(
    mockAuthService,
    mockPreferencesService,
    mockPaperlessPreferenceTokenService
  )(executionContext, appConfig, mockMessagesControllerComponents)

  "Calling the checkPreference action of the PreferencesController with an authorised user" when {
    implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = subscriptionRequest

    def result: Future[Result] = TestPreferencesController.checkPreferences(request)

    "Redirect to income source page if paperless is activated and in SignUp journey" in {
      mockStoreNinoSuccess(testNino)
      mockCheckPaperlessActivated(testToken)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(controllers.individual.incomesource.routes.IncomeSourceController.show().url)
    }

    "Redirect to returned preferences service if paperless was previously unspecified" in {
      mockStoreNinoSuccess(testNino)
      mockCheckPaperlessUnset(testToken, testUrl)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get mustBe testUrl
    }

  }

  "Calling the callback action of the PreferencesController with an authorised user" should {

    implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = subscriptionRequest

    def result: Future[Result] = TestPreferencesController.callback(request)

    "Redirect to rent uk property if paperless is activated and in SignUp journey" in {
      mockStoreNinoSuccess(testNino)
      mockCheckPaperlessActivated(testToken)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(controllers.individual.incomesource.routes.IncomeSourceController.show().url)
    }

    "Redirect to the preferences error page if paperless preferences was not selected" in {
      mockStoreNinoSuccess(testNino)
      mockCheckPaperlessUnset(testToken, testUrl)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(controllers.individual.routes.PreferencesController.show().url)
      session(result).get(ITSASessionKeys.PreferencesRedirectUrl) must contain(testUrl)
    }

  }

  "Calling the show action of the PreferencesController with an authorised user" should {

    lazy val result = TestPreferencesController.show()(subscriptionRequest)
    lazy val document = Jsoup.parse(contentAsString(result))


    "return status (200)" in {
      status(result) must be(Status.OK)
    }

    "render the Contact Email address view" in {
      document.title() mustBe Messages("preferences_callback.title")
    }
  }

  "Calling the submit action of the PreferencesController with an authorised user with yes" should {
    implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = subscriptionRequest

    def callShow(): Future[Result] = TestPreferencesController.submit()(request.withSession(ITSASessionKeys.PreferencesRedirectUrl -> testUrl))

    "return a redirect status (SEE_OTHER - 303)" in {
      val goodRequest = callShow()

      status(goodRequest) must be(Status.SEE_OTHER)
    }

    s"redirects to the correct url" in {
      val goodRequest = callShow()
      redirectLocation(goodRequest) must contain(testUrl)

      await(goodRequest)
    }
  }

  "Calling the submit action of the PreferencesController with an authorised user with the redirect url in the session" should {
    implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = subscriptionRequest

    def callShow(): Future[Result] = TestPreferencesController.submit()(request.withSession(ITSASessionKeys.PreferencesRedirectUrl -> testUrl))

    "use the redirect location from the session" in {
      val goodRequest = callShow()

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest) mustBe Some(testUrl)
      session(goodRequest).get(ITSASessionKeys.PreferencesRedirectUrl) mustBe empty
      await(goodRequest)
    }
  }

  "Calling submit with a fresh session should redirect to home" in {
    val res = TestPreferencesController.submit(FakeRequest())

    status(res) mustBe SEE_OTHER
    redirectLocation(res) must contain(controllers.usermatching.routes.HomeController.index().url)
  }

  authorisationTests()

}
