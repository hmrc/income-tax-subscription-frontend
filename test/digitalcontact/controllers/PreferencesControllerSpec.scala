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

package digitalcontact.controllers

import core.auth.MockConfig
import core.config.AppConfig
import controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import digitalcontact.services.mocks.{MockPaperlessPreferenceTokenService, MockPreferencesService}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import core.utils.TestConstants._

class PreferencesControllerSpec extends ControllerBaseSpec with MockPreferencesService with MockKeystoreService with MockPaperlessPreferenceTokenService {

  override val controllerName: String = "PreferencesControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "checkPreference" -> TestPreferencesController.checkPreferences
  )

  def createTestForPreferences(appConfig: AppConfig): PreferencesController = {
    new PreferencesController(
      mockBaseControllerConfig(appConfig),
      messagesApi,
      mockPreferencesService,
      mockAuthService,
      MockKeystoreService,
      mockPaperlessPreferenceTokenService
    )
  }

  lazy val TestPreferencesController = createTestForPreferences(MockConfig)

  "If newFeatures flag in core.config is false" when {
    "Calling the checkPreference action of the PreferencesController with an authorised user" should {
      implicit lazy val request = subscriptionRequest

      def result = TestPreferencesController.checkPreferences(request)

      "Redirect to Income Source if paperless is activated" in {
        mockStoreNinoSuccess(testNino)
        mockCheckPaperlessActivated(testToken)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get must be(controllers.routes.IncomeSourceController.showIncomeSource().url)
      }

      "Redirect to returned preferences service if paperless was previously unspecified" in {
        mockStoreNinoSuccess(testNino)
        mockCheckPaperlessUnset(testToken, Some(testUrl))

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get mustBe testUrl
      }

      "Redirect to default location of preferences service if paperless was previously unspecified and no redirect url is returned" in {
        mockStoreNinoSuccess(testNino)
        mockCheckPaperlessUnset(testToken, None)
        mockChoosePaperlessUrl(testUrl)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get mustBe testUrl
      }
    }

    "Calling the callback action of the PreferencesController with an authorised user" should {

      implicit lazy val request = subscriptionRequest

      def result = TestPreferencesController.callback(request)

      "Redirect to Terms and Conditions if paperless is activated" in {
        mockStoreNinoSuccess(testNino)
        mockCheckPaperlessActivated(testToken)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get must be(controllers.routes.IncomeSourceController.showIncomeSource().url)
      }

      "Redirect to do you still want to continue page if paperless was previously unspecified" in {
        mockStoreNinoSuccess(testNino)
        mockCheckPaperlessUnset(testToken, Some(testUrl))

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get must be(routes.PreferencesController.showGoBackToPreferences().url)
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

      def callShow() = TestPreferencesController.submitGoBackToPreferences()(request)

      "return a redirect status (SEE_OTHER - 303)" in {
        val goodRequest = callShow()

        status(goodRequest) must be(Status.SEE_OTHER)
      }

      s"redirects to the correct url" in {
        val goodRequest = callShow()
        redirectLocation(goodRequest) mustBe Some(TestPreferencesController.preferencesService.defaultChoosePaperlessUrl)

        await(goodRequest)
      }
    }
  }

  "If userMatchingFeature flag in core.config is true" when {
    lazy val TestNewFeaturesController = createTestForPreferences(
      new MockConfig {
        override val userMatchingFeature = true
      }
    )

    implicit lazy val request = subscriptionRequest

    def callCheck() = TestNewFeaturesController.checkPreferences()(request)

    def callShow() = TestNewFeaturesController.showGoBackToPreferences()(request)

    def callSubmit() = TestNewFeaturesController.submitGoBackToPreferences()(request)

    "Calling the checkPreferences controller should redirect us to income source" in {
      mockStoreNinoSuccess(testNino)
      val result = callCheck()

      status(result) must be(Status.SEE_OTHER)

      redirectLocation(result).get mustBe controllers.routes.IncomeSourceController.showIncomeSource().url
    }

    "Calling the showGoBackToPreferences controller should redirect us to income source" in {
      val result = callShow()

      status(result) must be(Status.SEE_OTHER)

      redirectLocation(result).get mustBe controllers.routes.IncomeSourceController.showIncomeSource().url
    }

    "Calling the submitGoBackToPreferences" in {
      val result = callSubmit()

      status(result) must be(Status.SEE_OTHER)

      redirectLocation(result).get mustBe controllers.routes.IncomeSourceController.showIncomeSource().url
    }

  }

  authorisationTests()

}
