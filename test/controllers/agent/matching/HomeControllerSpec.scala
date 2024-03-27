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

import common.Constants.ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY
import config.MockConfig
import config.featureswitch.FeatureSwitch.ThrottlingFeature
import config.featureswitch.FeatureSwitchingUtil
import controllers.agent.AgentControllerBaseSpec
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockSessionDataService, MockThrottlingConnector}

import scala.concurrent.Future

class HomeControllerSpec extends AgentControllerBaseSpec
  with MockAuditingService
  with MockThrottlingConnector
  with MockSessionDataService
  with FeatureSwitchingUtil {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> testHomeController().index()
  )

  private def testHomeController() = new HomeController(
    mockAuditingService,
    mockAuthService,
    MockConfig
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

  "index" when {
    "the journey is in an agent user matching state" should {
      "redirect to the enter client details page" in {
        val result: Future[Result] = testHomeController().index()(userMatchingRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsController.show().url)
      }
    }
    "the journey is in an agent sign up state" when {
      "the user is eligible to sign up for next year only" should {
        "redirect to the sign up next year only page" in {
          val result: Future[Result] = testHomeController().index()(agentSignUpRequest.addingToSession(ELIGIBLE_NEXT_YEAR_ONLY -> "true"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url)
        }
      }
      "the user is eligible to sign up for both tax years" should {
        "redirect to the client can sign up page" in {
          val result: Future[Result] = testHomeController().index()(agentSignUpRequest.addingToSession(ELIGIBLE_NEXT_YEAR_ONLY -> "false"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.eligibility.routes.ClientCanSignUpController.show().url)
        }
      }
    }
    "the journey has no state" should {
      "redirect to the add another client route" in {
        val result: Future[Result] = testHomeController().index()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.AddAnotherClientController.addAnother().url)
      }
    }
  }

  authorisationTests()
}
