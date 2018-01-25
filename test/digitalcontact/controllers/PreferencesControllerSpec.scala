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

package digitalcontact.controllers

import core.ITSASessionKeys
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import core.utils.TestConstants._
import digitalcontact.services.mocks.{MockPaperlessPreferenceTokenService, MockPreferencesService}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class PreferencesControllerSpec extends ControllerBaseSpec with MockPreferencesService with MockKeystoreService with MockPaperlessPreferenceTokenService {

  override val controllerName: String = "PreferencesControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "checkPreference" -> TestPreferencesController.checkPreferences
  )

  object TestPreferencesController extends PreferencesController(
    mockBaseControllerConfig(appConfig),
    messagesApi,
    mockPreferencesService,
    mockAuthService,
    MockKeystoreService,
    mockPaperlessPreferenceTokenService
  )


  "Calling the checkPreference action of the PreferencesController with an authorised user" should {
    implicit lazy val request = subscriptionRequest

    def result = TestPreferencesController.checkPreferences(request)

    "Redirect to Income Source if paperless is activated and in SignUp journey" in {
      mockStoreNinoSuccess(testNino)
      mockCheckPaperlessActivated(testToken)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(incometax.incomesource.controllers.routes.IncomeSourceController.show().url)
    }

    "Redirect to Create subscription controller if paperless is activated and in ConfirmAgentSubscription journey" in {
      mockStoreNinoSuccess(testNino)
      mockCheckPaperlessActivated(testToken)

      lazy val res = TestPreferencesController.checkPreferences(confirmAgentSubscriptionRequest)
      status(res) must be(Status.SEE_OTHER)
      redirectLocation(res).get must be(incometax.unauthorisedagent.controllers.routes.UnauthorisedSubscriptionController.subscribeUnauthorised().url)
    }


    "Redirect to returned preferences service if paperless was previously unspecified" in {
      mockStoreNinoSuccess(testNino)
      mockCheckPaperlessUnset(testToken, testUrl)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get mustBe testUrl
    }

  }

  "Calling the callback action of the PreferencesController with an authorised user" should {

    implicit lazy val request = subscriptionRequest

    def result = TestPreferencesController.callback(request)

    "Redirect to Income Source if paperless is activated and in SignUp journey" in {
      mockStoreNinoSuccess(testNino)
      mockCheckPaperlessActivated(testToken)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(incometax.incomesource.controllers.routes.IncomeSourceController.show().url)
    }

    "Redirect to Create Subscription controller if paperless is activated and in ConfirmAgentSubscription journey" in {
      mockStoreNinoSuccess(testNino)
      mockCheckPaperlessActivated(testToken)

      lazy val res = TestPreferencesController.callback(confirmAgentSubscriptionRequest)
      status(res) must be(Status.SEE_OTHER)
      redirectLocation(res).get must be(incometax.unauthorisedagent.controllers.routes.UnauthorisedSubscriptionController.subscribeUnauthorised().url)
    }

    "Redirect to the preferences error page if paperless preferences was not selected" in {
      mockStoreNinoSuccess(testNino)
      mockCheckPaperlessUnset(testToken, testUrl)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(routes.PreferencesController.showGoBackToPreferences().url)
      session(result).get(ITSASessionKeys.PreferencesRedirectUrl) must contain(testUrl)
    }

  }

  "Calling the showGoBackToPreferences action of the PreferencesController with an authorised user" should {

    lazy val result = TestPreferencesController.showGoBackToPreferences()(subscriptionRequest)
    lazy val document = Jsoup.parse(contentAsString(result))


    "return status (200)" in {
      status(result) must be(Status.OK)
    }

    "render the Contact Email address view" in {
      document.title() mustBe Messages("preferences_callback.title")
    }
  }

  "Calling the submitGoBackToPreferences action of the PreferencesController with an authorised user with yes" should {
    implicit lazy val request = subscriptionRequest

    def callShow() = TestPreferencesController.submitGoBackToPreferences()(request.withSession(ITSASessionKeys.PreferencesRedirectUrl -> testUrl))

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

  "Calling the submitGoBackToPreferences action of the PreferencesController with an authorised user with the redirect url in the session" should {
    implicit lazy val request = subscriptionRequest

    def callShow() = TestPreferencesController.submitGoBackToPreferences()(request.withSession(ITSASessionKeys.PreferencesRedirectUrl -> testUrl))

    "use the redirect location from the session" in {
      val goodRequest = callShow()

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest) mustBe Some(testUrl)
      session(goodRequest).get(ITSASessionKeys.PreferencesRedirectUrl) mustBe empty
      await(goodRequest)
    }
  }

  authorisationTests()

}