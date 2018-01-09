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

import core.config.featureswitch.{FeatureSwitching, UnauthorisedAgentFeature}
import core.controllers.ControllerBaseSpec
import core.services.CacheUtil._
import core.services.mocks.{MockAuthService, MockKeystoreService}
import core.utils.TestConstants._
import core.utils.TestModels._
import incometax.subscription.services.mocks.MockSubscriptionOrchestrationService
import incometax.unauthorisedagent.controllers.ConfirmAgentSubscriptionController
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException

class ConfirmAgentSubscriptionControllerSpec extends ControllerBaseSpec
  with MockAuthService
  with MockKeystoreService
  with MockSubscriptionOrchestrationService
  with FeatureSwitching {

  override val controllerName = "ConfirmAgentSubscriptionController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestConfirmAgentSubscriptionController.show(),
    "submit" -> TestConfirmAgentSubscriptionController.submit()
  )

  object TestConfirmAgentSubscriptionController extends ConfirmAgentSubscriptionController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService,
    MockKeystoreService,
    mockSubscriptionOrchestrationService
  )

  "show" when {
    "the unauthorised agent flow is enabled" when {
      "the user is of the correct affinity group and has a nino in session" should {
        "return the confirm-agent-subscription page" in {
          enable(UnauthorisedAgentFeature)

          val result = await(TestConfirmAgentSubscriptionController.show().apply(confirmAgentSubscriptionRequest))
          val document = Jsoup.parse(contentAsString(result))

          status(result) mustBe OK
          document.title() mustBe Messages("confirm-agent-subscription.title")
        }
      }
    }
    "the unauthorised agent flow is disabled" should {
      "throw a NotFoundException" in {
        disable(UnauthorisedAgentFeature)

        intercept[NotFoundException](await(TestConfirmAgentSubscriptionController.show().apply(confirmAgentSubscriptionRequest)))
      }
    }
  }

  "submit" when {
    "the unauthorised agent flow is enabled" when {
      "the user is of the correct affinity group and has a nino in session" should {
        "submit to ETMP, store the MTDITID in keystore and redirect to the confirmation page" in {
          enable(UnauthorisedAgentFeature)

          setupMockKeystore(fetchAll = testCacheMap)
          mockCreateSubscriptionSuccess(testNino, testCacheMap.getSummary())

          val result = await(TestConfirmAgentSubscriptionController.submit().apply(confirmAgentSubscriptionRequest))

          verifyKeystore(fetchAll = 1, saveSubscriptionId = 1)

          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result) mustBe Some(incometax.subscription.controllers.routes.ConfirmationController.show().url)
        }
      }

      "the unauthorised agent flow is disabled" should {
        "throw a NotFoundException" in {
          disable(UnauthorisedAgentFeature)

          intercept[NotFoundException](await(TestConfirmAgentSubscriptionController.submit().apply(confirmAgentSubscriptionRequest)))
        }
      }
    }
  }
}
