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

package agent.controllers

import agent.audit.Logging
import agent.services.mocks.MockKeystoreService
import core.config.MockConfig
import core.config.featureswitch.{FeatureSwitching, UnauthorisedAgentFeature}
import org.scalatest.Matchers._
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException

class UnauthorisedAgentConfirmationControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with FeatureSwitching {

  object TestUnauthorisedAgentConfirmationController extends UnauthorisedAgentConfirmationController(
    mockBaseControllerConfig(
      new MockConfig {
        override val unauthorisedAgentEnabled = true
      }
    ),
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    app.injector.instanceOf[Logging]
  )

  override val controllerName: String = "UnauthorisedAgentConfirmationControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showUnauthorisedAgentConfirmation" -> TestUnauthorisedAgentConfirmationController.show
  )

  "show" when {
    "the unauthorised agent feature switch is enabled" should {
      "return OK" in {
        object TestUnauthorisedAgentConfirmationController extends UnauthorisedAgentConfirmationController(
          mockBaseControllerConfig(
            new MockConfig {
              override val unauthorisedAgentEnabled = true
            }
          ),
          messagesApi,
          MockKeystoreService,
          mockAuthService,
          app.injector.instanceOf[Logging]
        )

        val result = TestUnauthorisedAgentConfirmationController.show(
          subscriptionRequest.withSession(ITSASessionKeys.UnauthorisedAgentKey -> true.toString)
        )

        status(result) shouldBe OK

        await(result)
      }
    }

    "the feature switch is not enabled" should {
      "return NOT_FOUND" in {
        object TestUnauthorisedAgentConfirmationController extends UnauthorisedAgentConfirmationController(
          mockBaseControllerConfig(
            new MockConfig {
              override val unauthorisedAgentEnabled = false
            }
          ),
          messagesApi,
          MockKeystoreService,
          mockAuthService,
          app.injector.instanceOf[Logging]
        )

        intercept[NotFoundException](await(TestUnauthorisedAgentConfirmationController.show(subscriptionRequest)))
      }
    }

    authorisationTests()

  }
}