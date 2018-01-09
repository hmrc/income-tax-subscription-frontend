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
import core.services.mocks.MockAuthService
import incometax.unauthorisedagent.forms.ConfirmAgentForm
import incometax.unauthorisedagent.models.ConfirmAgentModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException

class ConfirmAgentControllerSpec extends ControllerBaseSpec
  with MockAuthService
  with FeatureSwitching {

  override val controllerName = "ConfirmAgentController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestConfirmAgentController.show(),
    "submit" -> TestConfirmAgentController.submit()
  )

  object TestConfirmAgentController extends ConfirmAgentController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService
  )

  "show" when {
    "the unauthorised agent flow is enabled" when {
      "the user is of the correct affinity group and has a nino in session" should {
        "return the confirm-agent-subscription page" in {
          enable(UnauthorisedAgentFeature)

          val result = await(TestConfirmAgentController.show().apply(confirmAgentSubscriptionRequest))
          val document = Jsoup.parse(contentAsString(result))

          status(result) mustBe OK
          document.title().replaceAll("  "," ") mustBe Messages("confirm-agent.title","").replaceAll("  "," ") // todo update agent name once it's mocked
        }
      }
    }
    "the unauthorised agent flow is disabled" should {
      "throw a NotFoundException" in {
        disable(UnauthorisedAgentFeature)

        intercept[NotFoundException](await(TestConfirmAgentController.show().apply(confirmAgentSubscriptionRequest)))
      }
    }
  }

  "submit" when {
    "the unauthorised agent flow is enabled" when {

      def submit(option: String) =
        TestConfirmAgentController.submit().apply(
          confirmAgentSubscriptionRequest.post(ConfirmAgentForm.confirmAgentForm, ConfirmAgentModel(option))
        )

      "the user is of the correct affinity group and has a nino in session" when {
        "the user answered yes" should {
          "" in {
            enable(UnauthorisedAgentFeature)


            val result = await(submit(ConfirmAgentForm.option_yes))

            status(result) must be(Status.NOT_IMPLEMENTED)
          }
        }

        "the user answered no" should {
          "" in {
            enable(UnauthorisedAgentFeature)


            val result = await(submit(ConfirmAgentForm.option_no))

            status(result) must be(Status.NOT_IMPLEMENTED)
          }
        }
      }

      "the unauthorised agent flow is disabled" should {
        "throw a NotFoundException" in {
          disable(UnauthorisedAgentFeature)

          intercept[NotFoundException](await(TestConfirmAgentController.submit().apply(confirmAgentSubscriptionRequest)))
        }
      }
    }
  }
}
