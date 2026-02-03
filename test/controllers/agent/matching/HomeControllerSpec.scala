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
import config.featureswitch.FeatureSwitch.ThrottlingFeature
import config.featureswitch.FeatureSwitchingUtil
import controllers.ControllerSpec
import controllers.agent.actions.mocks.MockIdentifierAction
import models.EligibilityStatus
import play.api.http.Status
import play.api.mvc.{AnyContentAsEmpty, Result}
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
  with MockIdentifierAction {

  val userMatchingRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name
  )

  val agentSignUpRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentSignUp.name,
    ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true"
  )

  private def testHomeController() = new HomeController(
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
    "the journey is an agent with no state" should {
      "redirect user to the using software page" in {
        val result: Future[Result] = testHomeController().index()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.UsingSoftwareController.show().url)
      }
    }
  }
}
