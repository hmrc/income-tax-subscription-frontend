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

import core.ITSASessionKeys.AgencyName
import core.config.featureswitch.{FeatureSwitching, UnauthorisedAgentFeature}
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockAuthService
import core.utils.TestConstants._
import incometax.unauthorisedagent.forms.ConfirmAgentForm
import incometax.unauthorisedagent.models.ConfirmAgentModel
import incometax.unauthorisedagent.services.mocks.MockAgencyNameService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException

class ConfirmAgentControllerSpec extends ControllerBaseSpec
  with MockAuthService
  with MockAgencyNameService
  with FeatureSwitching {

  override val controllerName = "ConfirmAgentController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestConfirmAgentController.show(),
    "submit" -> TestConfirmAgentController.submit()
  )

  object TestConfirmAgentController extends ConfirmAgentController(
    MockBaseControllerConfig,
    mockAgencyNameService,
    messagesApi,
    mockAuthService
  )

  "show" when {
    "the unauthorised agent flow is enabled" when {
      "the user is of the correct affinity group and has a nino in session" should {
        "return the authorise-agent page" in {
          enable(UnauthorisedAgentFeature)
          mockGetAgencyNameSuccess(testArn)

          val testRequest = confirmAgentSubscriptionRequest

          testRequest.session.get(AgencyName) mustBe None

          val result = await(TestConfirmAgentController.show().apply(testRequest))
          val document = Jsoup.parse(contentAsString(result))

          status(result) mustBe OK
          document.title() mustBe Messages("confirm-agent.title", testAgencyName)
          result.session(testRequest).get(AgencyName) mustBe Some(testAgencyName)

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
          "redirects to authorise agent" in {
            enable(UnauthorisedAgentFeature)
            mockGetAgencyNameSuccess(testArn)

            val result = await(submit(ConfirmAgentForm.option_yes))

            status(result) must be(Status.SEE_OTHER)
            redirectLocation(result) mustBe Some(incometax.unauthorisedagent.controllers.routes.AuthoriseAgentController.show().url)
          }
        }

        "the user answered no" should {
          "redirect to agent not authorised" in {
            enable(UnauthorisedAgentFeature)

            val result = await(submit(ConfirmAgentForm.option_no))

            status(result) must be(Status.SEE_OTHER)
            redirectLocation(result) mustBe Some(routes.AgentNotAuthorisedController.show().url)
          }
        }

        "the user answered badly" should {
          "return bad request" in {
            enable(UnauthorisedAgentFeature)
            mockGetAgencyNameSuccess(testArn)

            val result = await(submit("this triggers a validation error"))

            status(result) must be(Status.BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include(Messages("confirm-agent.title", testAgencyName))
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

  "getAgentName" should {
    def getAgencyName(request: Request[AnyContent] = confirmAgentSubscriptionRequest) = TestConfirmAgentController.getAgentName(request)

    "return the agency name if it's stored in session" in {
      val name = getAgencyName(confirmAgentSubscriptionRequest.withSession(AgencyName -> testAgencyName))
      await(name) mustBe testAgencyName
    }

    "return the agency name by calling the agency name service if it's not in session" in {
      mockGetAgencyNameSuccess(testArn)

      val name = getAgencyName()

      await(name) mustBe testAgencyName
    }
  }

}
