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

package controllers.unauthorisedagent

import agent.services.CacheUtil._
import controllers.ControllerBaseSpec
import core.ITSASessionKeys
import core.config.MockConfig
import core.config.featureswitch.{FeatureSwitching, UnauthorisedAgentFeature}
import core.services.mocks.{MockAuthService, MockKeystoreService}
import core.utils.TestConstants._
import core.utils.TestModels._
import incometax.subscription.services.mocks.MockSubscriptionOrchestrationService
import incometax.unauthorisedagent.services.mocks.MockSubscriptionStoreRetrievalService
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException

class UnauthorisedSubscriptionControllerSpec extends ControllerBaseSpec
  with MockAuthService
  with MockKeystoreService
  with MockSubscriptionOrchestrationService
  with MockSubscriptionStoreRetrievalService
  with FeatureSwitching {

  override val controllerName = "CreateSubscriptionController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] =
    Map("createSubscription" -> TestUnauthorisedSubscriptionController(true).subscribeUnauthorised())

  private def TestUnauthorisedSubscriptionController(enableUnauthorisedAgent: Boolean) = new UnauthorisedSubscriptionController(
    mockBaseControllerConfig(new MockConfig {
      override val unauthorisedAgentEnabled = enableUnauthorisedAgent
    }),
    messagesApi,
    mockAuthService,
    MockKeystoreService,
    mockSubscriptionStoreRetrievalService,
    mockSubscriptionOrchestrationService
  )

  lazy val request = confirmAgentSubscriptionRequest.withSession(
    ITSASessionKeys.ConfirmedAgent -> true.toString
  )

  "createSubscription" when {
    "the unauthorised agent flow is enabled" when {
      "the user is of the correct affinity group and has a nino in session" should {

        "submit to ETMP, store the MTDITID in keystore and redirect to the confirmation page" in {
          enable(UnauthorisedAgentFeature)
          setupMockKeystore(fetchAll = testCacheMap)
          mockCreateSubscriptionFromUnauthorisedAgentSuccess(testArn, testNino, testCacheMap.getSummary())
          mockDeleteSubscriptionData(testNino)
          mockEnrolAndRefreshSuccess(testMTDID, testNino)

          val result = await(TestUnauthorisedSubscriptionController(enableUnauthorisedAgent = true).subscribeUnauthorised().apply(request))

          verifyKeystore(fetchAll = 1, saveSubscriptionId = 1)

          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.ConfirmationController.show().url)
        }
      }
    }

    "the unauthorised agent flow is disabled" should {
      "throw a NotFoundException" in {
        disable(UnauthorisedAgentFeature)

        intercept[NotFoundException](await(TestUnauthorisedSubscriptionController(enableUnauthorisedAgent = false).subscribeUnauthorised().apply(request)))
      }
    }
  }

}