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

import assets.MessageLookup.FrontPage
import audit.Logging
import auth.MockConfig
import config.BaseControllerConfig
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockKeystoreService, MockSubscriptionService}
import uk.gov.hmrc.play.http.InternalServerException
import utils.TestConstants

import scala.concurrent.Future


class HomeControllerSpec extends ControllerBaseSpec
  with MockSubscriptionService
  with MockKeystoreService {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> TestHomeController(showGuidance = false).index()
  )

  def mockBaseControllerConfig(showStartPage: Boolean): BaseControllerConfig = {
    val mockConfig = new MockConfig {
      override val showGuidance: Boolean = showStartPage
    }
    mockBaseControllerConfig(mockConfig)
  }

  def TestHomeController(showGuidance: Boolean) = new HomeController(
    mockBaseControllerConfig(showGuidance),
    messagesApi,
    mockSubscriptionService,
    MockKeystoreService,
    mockAuthService,
    app.injector.instanceOf[Logging]
  )

  val testNino = TestConstants.testNino

  "Calling the home action of the Home controller with an authorised user" should {

    "If the start page (showGuidance) is enabled" should {

      lazy val result = TestHomeController(showGuidance = true).home()(fakeRequest)

      "Return status OK (200)" in {
        status(result) must be(Status.OK)
      }

      "Should have the page title" in {
        Jsoup.parse(contentAsString(result)).title mustBe FrontPage.title
      }
    }

    "If the start page (showGuidance) is disabled" should {
      lazy val result = TestHomeController(showGuidance = false).home()(fakeRequest)

      "Return status SEE_OTHER (303) redirect" in {
        status(result) must be(Status.SEE_OTHER)
      }

      "Redirect to the 'Index' page" in {
        redirectLocation(result).get mustBe controllers.routes.HomeController.index().url
      }
    }
  }

  "Calling the index action of the HomeController with an authorised user" should {
    def call() = TestHomeController(showGuidance = false).index()(fakeRequest)

    "redirect them to already subscribed page if they already has a subscription" in {
      setupMockGetSubscriptionFound(testNino)
      setupMockKeystoreSaveFunctions()

      val result = call()
      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get mustBe controllers.routes.ClaimSubscriptionController.claim().url

      verifyKeystore(saveSubscriptionId = 1)
    }

    "display the error page if there was an error checking the subscription" in {
      setupMockGetSubscriptionFailure(testNino)

      intercept[InternalServerException](await(call()))
    }

    // N.B. the subscribeNone case is covered below
  }

  "Calling the index action of the HomeController with an authorised user who does not already have a subscription" should {

    def getResult: Future[Result] = TestHomeController(showGuidance = false).index()(fakeRequest)

    "redirect to check preferences if the user qualifies" in {
      setupMockGetSubscriptionNotFound(testNino)

      val result = getResult

      status(result) must be(Status.SEE_OTHER)

      redirectLocation(result).get mustBe controllers.preferences.routes.PreferencesController.checkPreferences().url
    }

    "redirect when auth returns an org affinity" in {
      mockNinoRetrievalWithOrg()

      val result = getResult

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.AffinityGroupErrorController.show().url
    }

    "redirect when auth returns no affinity" in {
      mockNinoRetrievalWithNoAffinity()

      val result = getResult

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.AffinityGroupErrorController.show().url
    }
  }

  authorisationTests()

}
