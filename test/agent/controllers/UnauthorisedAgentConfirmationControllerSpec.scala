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

      "for a user who has an mtd value in session and flagged as unauthorised return OK" in {
        val result = TestUnauthorisedAgentConfirmationController.show(
          subscriptionRequest.withSession(ITSASessionKeys.UnauthorisedAgentKey -> true.toString, ITSASessionKeys.MTDITID -> "any value")
        )

        status(result) shouldBe OK
      }

      "for a user who has an mtd value in session and flagged as authorised return SEE OTHER" in {
        val result = TestUnauthorisedAgentConfirmationController.show(
          subscriptionRequest.withSession(ITSASessionKeys.UnauthorisedAgentKey -> false.toString, ITSASessionKeys.MTDITID -> "any value")
        )

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.HomeController.index().url)
      }

      "for a user who does not have an mtd value in session return NOT FOUND" in {
        val result = TestUnauthorisedAgentConfirmationController.show(
          subscriptionRequest.withSession(ITSASessionKeys.UnauthorisedAgentKey -> true.toString)
        )

        intercept[NotFoundException] {
          await(result)
        }
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