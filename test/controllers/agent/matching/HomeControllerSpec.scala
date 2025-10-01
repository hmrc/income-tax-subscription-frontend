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
import controllers.ControllerSpec
import controllers.agent.AgentControllerBaseSpec
import controllers.agent.actions.mocks.MockIdentifierAction
import models.EligibilityStatus
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService, MockThrottlingConnector}

import scala.concurrent.Future

class HomeControllerSpec extends ControllerSpec
  with MockAuditingService
  with MockThrottlingConnector
  with MockReferenceRetrieval
  with FeatureSwitchingUtil
  with MockSubscriptionDetailsService
  with MockIdentifierAction{

 val controllerName: String = "HomeControllerSpec"

  val userMatchingRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name
  )

  val agentSignUpRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentSignUp.name,
    ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true"
  )

  private def testHomeController() = new HomeController(
    mockAuditingService
  )(
    mockGetEligibilityStatusService,
    mockSubscriptionDetailsService,
    mockReferenceRetrieval,
    fakeIdentifierAction
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
      "the user has previously confirmed to sign up their client" should {
        "redirect the user to the using software page" in {
          mockFetchEligibilityInterruptPassed(Some(true))

          val result: Future[Result] = testHomeController().index()(agentSignUpRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.UsingSoftwareController.show.url)
        }
      }
      "the user has not previously confirmed to sign up their client" when {
        "the user is eligible to sign up for next year only" should {
          "redirect to the sign up next year only page" in {
            mockFetchEligibilityInterruptPassed(None)
            mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))

            val result: Future[Result] = testHomeController().index()(agentSignUpRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url)
          }
        }
        "the user is eligible to sign up for both tax years" should {
          "redirect to the client can sign up page" in {
            mockFetchEligibilityInterruptPassed(None)
            mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))

            val result: Future[Result] = testHomeController().index()(agentSignUpRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.agent.eligibility.routes.ClientCanSignUpController.show().url)
          }
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
}
