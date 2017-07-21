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
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.{MockSubscriptionService, MockThrottlingService}
import uk.gov.hmrc.play.http.InternalServerException
import utils.TestConstants


class HomeControllerSpec extends ControllerBaseSpec
  with MockThrottlingService
  with MockSubscriptionService {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> TestHomeController(enableThrottling = false, showGuidance = false, enableCheckSubscriptionCalls = true).index()
  )

  def mockBaseControllerConfig(isThrottled: Boolean, showStartPage: Boolean, enableCheckSubscriptionCalls: Boolean): BaseControllerConfig = {
    val mockConfig = new MockConfig {
      override val enableThrottling: Boolean = isThrottled
      override val showGuidance: Boolean = showStartPage
      override val enableCheckSubscription: Boolean = enableCheckSubscriptionCalls
    }
    mockBaseControllerConfig(mockConfig)
  }

  def TestHomeController(enableThrottling: Boolean, showGuidance: Boolean, enableCheckSubscriptionCalls: Boolean) = new HomeController(
    mockBaseControllerConfig(enableThrottling, showGuidance, enableCheckSubscriptionCalls),
    messagesApi,
    TestThrottlingService,
    TestSubscriptionService,
    mockAuthService,
    app.injector.instanceOf[Logging]
  )
  
  val testNino = TestConstants.testNino

  "Calling the home action of the Home controller with an authorised user" should {

    "If the start page (showGuidance) is enabled" should {

      lazy val result = TestHomeController(enableThrottling = false, showGuidance = true, enableCheckSubscriptionCalls = true).home()(fakeRequest)

      "Return status OK (200)" in {
        status(result) must be(Status.OK)
      }

      "Should have the page title" in {
        Jsoup.parse(contentAsString(result)).title mustBe FrontPage.title
      }
    }

    "If the start page (showGuidance) is disabled" should {
      lazy val result = TestHomeController(enableThrottling = false, showGuidance = false, enableCheckSubscriptionCalls = true).home()(fakeRequest)

      "Return status SEE_OTHER (303) redirect" in {
        status(result) must be(Status.SEE_OTHER)
      }

      "Redirect to the 'Index' page" in {
        redirectLocation(result).get mustBe controllers.routes.HomeController.index().url
      }
    }
  }

  "Calling the index action of the HomeController with an authorised user" should {
    def call() = TestHomeController(enableThrottling = true, showGuidance = false, enableCheckSubscriptionCalls = true).index()(fakeRequest)

    "redirect them to already subscribed page if they already has a subscription" in {
      setupGetSubscription(testNino)(subscribeSuccess)
      // this is mocked to check we don't call throttle as well
      setupMockCheckAccess(testNino)(OK)

      val result = call()
      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get mustBe controllers.routes.ClaimSubscriptionController.claim().url

      verifyGetSubscription(testNino)(1)
      verifyMockCheckAccess(testNino)(0)
    }

    "display the error page if there was an error checking the subscription" in {
      setupGetSubscription(testNino)(subscribeBadRequest)
      // this is mocked to check we don't call throttle as well
      setupMockCheckAccess(testNino)(OK)

      intercept[InternalServerException](await(call()))

      verifyGetSubscription(testNino)(1)
      verifyMockCheckAccess(testNino)(0)
    }

    // N.B. the subscribeNone case is covered below
  }

  for (enableCheckSubscription <- Seq(true, false)) {

    s"enableCheckSubscription is set to $enableCheckSubscription" when {

      val expectedGetSubscriptionCalls = enableCheckSubscription match {
        case true => 1
        case false => 0
      }

      "Calling the index action of the HomeController with an authorised user who does not already have a subscription" should {

        "If throttling is enabled when calling the index" should {
          lazy val result = TestHomeController(enableThrottling = true, showGuidance = false, enableCheckSubscriptionCalls = enableCheckSubscription).index()(fakeRequest)

          "trigger a call to the throttling service" in {
            setupGetSubscription(testNino)(subscribeNone)
            setupMockCheckAccess(testNino)(OK)

            status(result) must be(Status.SEE_OTHER)

            redirectLocation(result).get mustBe controllers.preferences.routes.PreferencesController.checkPreferences().url

            verifyGetSubscription(testNino)(expectedGetSubscriptionCalls)
            verifyMockCheckAccess(testNino)(1)
          }
        }

        "If throttling is disabled when calling the index" should {
          lazy val result = TestHomeController(enableThrottling = false, showGuidance = false, enableCheckSubscriptionCalls = enableCheckSubscription).index()(fakeRequest)

          "not trigger a call to the throttling service" in {
            setupGetSubscription(testNino)(subscribeNone)
            setupMockCheckAccess(testNino)(OK)

            status(result) must be(Status.SEE_OTHER)

            redirectLocation(result).get mustBe controllers.preferences.routes.PreferencesController.checkPreferences().url

            verifyGetSubscription(testNino)(expectedGetSubscriptionCalls)
            verifyMockCheckAccess(testNino)(0)
          }
        }
      }

    }
  }

  authorisationTests()

}
