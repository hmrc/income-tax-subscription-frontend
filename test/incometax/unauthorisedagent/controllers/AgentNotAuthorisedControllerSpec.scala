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

package incometax.unauthorisedagent.controllers

import agent.services.mocks.MockKeystoreService
import core.ITSASessionKeys
import core.config.featureswitch.{FeatureSwitching, UnauthorisedAgentFeature}
import core.connectors.mocks.MockAuth
import core.controllers.ControllerBaseSpec
import core.utils.TestConstants._
import incometax.unauthorisedagent.services.mocks.MockSubscriptionStoreRetrievalService
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HttpResponse, NotFoundException}


class AgentNotAuthorisedControllerSpec extends ControllerBaseSpec
  with MockAuth
  with MockSubscriptionStoreRetrievalService
  with FeatureSwitching
  with MockKeystoreService {
  override val controllerName = "AgentNotAuthorisedController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestAgentNotAuthorisedController.show()
  )

  object TestAgentNotAuthorisedController extends AgentNotAuthorisedController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService,
    mockSubscriptionStoreRetrievalService,
    MockKeystoreService
  )

  "show" when {
    "the feature switch is enabled" when {
      "the user is in the confirm agent subscription journey state" should {
        "show the agent not authorised page" in {
          enable(UnauthorisedAgentFeature)
          setupMockKeystore(deleteAll = HttpResponse(OK))
          mockDeleteSubscriptionData(testNino)

          implicit val request: FakeRequest[AnyContentAsEmpty.type] = confirmAgentSubscriptionRequest

          val res = await(TestAgentNotAuthorisedController.show(request))

          status(res) mustBe OK
          val document = Jsoup.parse(contentAsString(res))

          document.title() mustBe Messages("agent-not-authorised.title", testAgencyName)
          res.session.get(ITSASessionKeys.JourneyStateKey) mustBe empty

          verifyKeystore(deleteAll = 1)
        }
      }
      "the user is not in the correct journey state" should {
        "redirect to home" in {
          enable(UnauthorisedAgentFeature)

          val res = await(TestAgentNotAuthorisedController.show(fakeRequest))

          status(res) mustBe SEE_OTHER
        }
      }
    }
    "the feature switch is not enabled" should {
      "return NOT_FOUND" in {
        disable(UnauthorisedAgentFeature)

        intercept[NotFoundException](await(TestAgentNotAuthorisedController.show(confirmAgentSubscriptionRequest)))
      }
    }
  }
}
