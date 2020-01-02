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

package controllers.agent

import agent.assets.MessageLookup.FrontPage
import agent.audit.Logging
import agent.auth.{AgentSignUp, AgentUserMatching}
import core.config.{BaseControllerConfig, MockConfig}
import org.jsoup.Jsoup
import org.mockito.Mockito.reset
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._


class HomeControllerSpec extends AgentControllerBaseSpec {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> testHomeController(showGuidance = false).index()
  )

  def mockBaseControllerConfig(showStartPage: Boolean): BaseControllerConfig = {
    val mockConfig = new MockConfig {
      override val showGuidance: Boolean = showStartPage
    }
    mockBaseControllerConfig(mockConfig)
  }

  private def testHomeController(showGuidance: Boolean) = new HomeController(
    mockBaseControllerConfig(showGuidance),
    messagesApi,
    mockAuthService,
    app.injector.instanceOf[Logging]
  )

  "Calling the home action of the Home controller with an authorised user" should {

    "If the start page (showGuidance) is enabled" should {

      lazy val result = testHomeController(showGuidance = true).home()(FakeRequest())

      "Return status OK (200)" in {
        status(result) must be(Status.OK)
      }

      "Should have the page title" in {
        Jsoup.parse(contentAsString(result)).title mustBe FrontPage.title
      }
    }

    "If the start page (showGuidance) is disabled" should {
      lazy val result = testHomeController(showGuidance = false).home()(FakeRequest())

      "Return status SEE_OTHER (303) redirect" in {
        status(result) must be(Status.SEE_OTHER)
      }

      "Redirect to the 'Index' page" in {
        redirectLocation(result).get mustBe controllers.agent.routes.HomeController.index().url
      }
    }

  }

  "Calling the index action of the HomeController with an authorised user" when {

    "there is no journey state in session" should {
      lazy val request = FakeRequest()

      def result = testHomeController(showGuidance = false).index()(request)

      s"if the user has arn redirect to ${controllers.agent.matching.routes.ClientDetailsController.show().url}" in {
        reset(mockAuthService)
        mockAgent()
        status(result) must be(Status.SEE_OTHER)

        redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsController.show().url

        await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatching.name)
      }

      s"if the user does not have arn redirect to ${controllers.agent.routes.NotEnrolledAgentServicesController.show().url}" in {
        reset(mockAuthService)
        mockNotAgent()

        status(result) must be(Status.SEE_OTHER)

        redirectLocation(result).get mustBe controllers.agent.routes.NotEnrolledAgentServicesController.show().url

        await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe None
      }
    }

    "journey state is user matching" should {
      lazy val request = userMatchingRequest

      def result = testHomeController(showGuidance = false).index()(request)

      s"redirect user to ${controllers.agent.matching.routes.ClientDetailsController.show().url}" in {
        status(result) must be(Status.SEE_OTHER)

        redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsController.show().url

        await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatching.name)
      }
    }

    "journey state is user matched" when {
      "the user has a UTR" should {
        lazy val request = userMatchedRequest

        def result = testHomeController(showGuidance = false).index()(request)

        s"redirect user to ${controllers.agent.routes.IncomeSourceController.show().url}" in {
          status(result) must be(Status.SEE_OTHER)

          redirectLocation(result).get mustBe controllers.agent.routes.IncomeSourceController.show().url

          await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
        }
      }

      "the user does not have a UTR" should {
        lazy val request = userMatchedRequestNoUtr

        def result = testHomeController(showGuidance = false).index()(request)

        s"redirect user to ${controllers.agent.matching.routes.NoSAController.show().url}" in {
          status(result) must be(Status.SEE_OTHER)

          redirectLocation(result).get mustBe controllers.agent.matching.routes.NoSAController.show().url

          await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe None
        }
      }
    }

    "journey state is user matched" when {
      "the agent is unauthorised" should {
        lazy val request = unauthorisedUserMatchedRequest

        def result = testHomeController(showGuidance = false).index()(request)

        s"redirect user to ${controllers.agent.routes.IncomeSourceController.show().url}" in {
          status(result) must be(Status.SEE_OTHER)

          redirectLocation(result).get mustBe controllers.agent.routes.IncomeSourceController.show().url

          await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
        }
      }
    }

    "journey state is user matching" when {
      "the agent is unauthorised" should {
        lazy val request = unauthorisedUserMatchingRequest

        def result = testHomeController(showGuidance = false).index()(request)

        s"redirect user to ${controllers.agent.routes.AgentNotAuthorisedController.show().url}" in {
          status(result) must be(Status.SEE_OTHER)

          redirectLocation(result).get mustBe controllers.agent.routes.AgentNotAuthorisedController.show().url

          await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatching.name)
        }
      }
    }

    "the user has an mtd flag" when {
      "the agent is authorised" should {
        lazy val request = subscriptionRequest.withSession(ITSASessionKeys.MTDITID -> "any")

        def result = testHomeController(showGuidance = false).index()(request)

        s"redirect user to ${controllers.agent.routes.ConfirmationController.show().url}" in {
          status(result) must be(Status.SEE_OTHER)

          redirectLocation(result).get mustBe controllers.agent.routes.ConfirmationController.show().url
        }
      }

      "the agent is unauthorised" should {
        lazy val request = subscriptionRequest.withSession(ITSASessionKeys.MTDITID -> "any", ITSASessionKeys.UnauthorisedAgentKey -> true.toString)

        def result = testHomeController(showGuidance = false).index()(request)

        s"redirect user to ${controllers.agent.routes.UnauthorisedAgentConfirmationController.show().url}" in {
          status(result) must be(Status.SEE_OTHER)

          redirectLocation(result).get mustBe controllers.agent.routes.UnauthorisedAgentConfirmationController.show().url
        }
      }
    }
  }

  authorisationTests()

}
