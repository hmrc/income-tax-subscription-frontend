/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.agent.matching

import auth.agent.{AgentSignUp, AgentUserMatching}
import common.Constants.ITSASessionKeys
import config.MockConfig
import config.featureswitch.FeatureSwitch.ThrottlingFeature
import config.featureswitch.FeatureSwitchingUtil
import controllers.agent.AgentControllerBaseSpec
import org.mockito.Mockito.reset
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ThrottlingService
import services.mocks.{MockAuditingService, MockThrottlingConnector}

import scala.concurrent.Future

class HomeControllerSpec extends AgentControllerBaseSpec
  with MockAuditingService
  with MockThrottlingConnector
  with FeatureSwitchingUtil {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> testHomeController().index()
  )

  private def testHomeController() = new HomeController(
    mockAuditingService,
    mockAuthService,
    MockConfig,
    new ThrottlingService(mockThrottlingConnector, appConfig)
  )(executionContext, mockMessagesControllerComponents)

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ThrottlingFeature)
  }

  "Calling the home action of the Home controller with an authorised user" should {
    lazy val result = testHomeController().home()(FakeRequest())

    "Return status Redirect (303)" in {
      status(result) must be(Status.SEE_OTHER)
    }

    "Should have the redirect location to the index route" in {
      redirectLocation(result) mustBe Some(controllers.agent.matching.routes.HomeController.index.url)
    }
  }

  "Calling the index action of the HomeController with an authorised user" when {

    "there is no journey state in session" should {
      lazy val request = FakeRequest()

      def result: Future[Result] = testHomeController().index()(request)

      s"if the user has arn redirect to ${controllers.agent.matching.routes.ClientDetailsController.show().url}" in {
        reset(mockAuthService)
        mockAgent()
        status(result) must be(Status.SEE_OTHER)

        redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsController.show().url

        await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatching.name)
      }

      s"if the user does not have arn redirect to ${controllers.agent.matching.routes.NotEnrolledAgentServicesController.show.url}" in {
        reset(mockAuthService)
        mockNotAgent()

        status(result) must be(Status.SEE_OTHER)

        redirectLocation(result).get mustBe controllers.agent.matching.routes.NotEnrolledAgentServicesController.show.url

        await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe None
      }
    }

    "journey state is user matching" should {
      lazy val request = userMatchingRequest

      def result: Future[Result] = testHomeController().index()(request)

      s"redirect user to ${controllers.agent.matching.routes.ClientDetailsController.show().url}" in {
        status(result) must be(Status.SEE_OTHER)

        redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsController.show().url

        await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatching.name)
      }
    }

    "journey state is user matched" when {
      "the user has a UTR" should {
        lazy val request = userMatchedRequest

        def result: Future[Result] = testHomeController().index()(request)

        s"redirect user to ${controllers.agent.routes.WhatYouNeedToDoController.show().url}" in {
          status(result) must be(Status.SEE_OTHER)

          redirectLocation(result).get mustBe controllers.agent.routes.WhatYouNeedToDoController.show().url

          await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
        }
      }
    }

    "journey state is agent sign up" when {
      "the user has a UTR" should {
        lazy val request = agentSignUpRequest

        def result: Future[Result] = testHomeController().index()(request)

        s"redirect user to ${controllers.agent.routes.WhatYouNeedToDoController.show().url}" in {
          status(result) must be(Status.SEE_OTHER)

          redirectLocation(result).get mustBe controllers.agent.routes.WhatYouNeedToDoController.show().url

          await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
        }
      }

      "the user does not have a UTR" should {
        lazy val request = userMatchedRequestNoUtr

        def result: Future[Result] = testHomeController().index()(request)

        s"redirect user to ${controllers.agent.matching.routes.NoSAController.show.url}" in {
          status(result) must be(Status.SEE_OTHER)

          redirectLocation(result).get mustBe controllers.agent.matching.routes.NoSAController.show.url

          await(result).session(request).get(ITSASessionKeys.JourneyStateKey) mustBe None
        }
      }
    }

    "the user has an mtd flag" when {
      "the agent is authorised" should {
        lazy val request = subscriptionRequest.withSession(ITSASessionKeys.MTDITID -> "any")

        def result: Future[Result] = testHomeController().index()(request)

        s"redirect user to ${controllers.agent.routes.ConfirmationController.show.url}" in {
          status(result) must be(Status.SEE_OTHER)

          redirectLocation(result).get mustBe controllers.agent.routes.ConfirmationController.show.url
        }
      }
    }
  }

  authorisationTests()
}