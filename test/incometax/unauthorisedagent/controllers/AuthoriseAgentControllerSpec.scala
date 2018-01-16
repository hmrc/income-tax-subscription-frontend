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

import core.ITSASessionKeys
import core.config.featureswitch.{FeatureSwitching, UnauthorisedAgentFeature}
import core.controllers.ControllerBaseSpec
import core.services.CacheUtil._
import core.services.mocks.{MockAuthService, MockKeystoreService}
import core.utils.TestConstants._
import core.utils.TestModels._
import incometax.subscription.services.mocks.MockSubscriptionOrchestrationService
import incometax.unauthorisedagent.forms.ConfirmAgentForm
import incometax.unauthorisedagent.models.ConfirmAgentModel
import incometax.unauthorisedagent.services.mocks.MockSubscriptionStoreRetrievalService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException

class AuthoriseAgentControllerSpec extends ControllerBaseSpec
  with MockAuthService
  with MockKeystoreService
  with MockSubscriptionOrchestrationService
  with MockSubscriptionStoreRetrievalService
  with FeatureSwitching {

  override val controllerName = "AuthoriseAgentController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestAuthoriseAgentController.show(),
    "submit" -> TestAuthoriseAgentController.submit()
  )

  object TestAuthoriseAgentController extends AuthoriseAgentController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService,
    MockKeystoreService,
    mockSubscriptionStoreRetrievalService,
    mockSubscriptionOrchestrationService
  )

  lazy val request = confirmAgentSubscriptionRequest.withSession(ITSASessionKeys.AgencyName -> testAgencyName)

  "show" when {
    "the unauthorised agent flow is enabled" when {
      "the user is of the correct affinity group and has a nino in session" should {
        "return the authorise-agent page" in {
          enable(UnauthorisedAgentFeature)

          val result = await(TestAuthoriseAgentController.show().apply(request))
          val document = Jsoup.parse(contentAsString(result))

          status(result) mustBe OK
          document.title() mustBe Messages("authorise-agent.title", testAgencyName)
        }
      }
    }
    "the unauthorised agent flow is disabled" should {
      "throw a NotFoundException" in {
        disable(UnauthorisedAgentFeature)

        intercept[NotFoundException](await(TestAuthoriseAgentController.show().apply(request)))
      }
    }
  }

  "submit" when {
    "the unauthorised agent flow is enabled" when {
      "the user is of the correct affinity group and has a nino in session" when {
        def submit(option: String) = TestAuthoriseAgentController.submit().apply(
          request.post(ConfirmAgentForm.confirmAgentForm, ConfirmAgentModel(option))
        )

        "the user answered yes" should {
          "submit to ETMP, store the MTDITID in keystore and redirect to the confirmation page" in {
            enable(UnauthorisedAgentFeature)
            setupMockKeystore(fetchAll = testCacheMap)
            mockCreateSubscriptionSuccess(testNino, testCacheMap.getSummary())
            mockDeleteSubscriptionData(testNino)

            val result = await(submit(ConfirmAgentForm.option_yes))

            verifyKeystore(fetchAll = 1, saveSubscriptionId = 1)

            status(result) must be(Status.SEE_OTHER)
            redirectLocation(result) mustBe Some(incometax.subscription.controllers.routes.ConfirmationController.show().url)
          }
        }

        "the user answered no" should {
          "redirects to agent not authorised" in {
            val result = await(submit(ConfirmAgentForm.option_no))
            status(result) must be(Status.SEE_OTHER)
            redirectLocation(result) mustBe Some(routes.AgentNotAuthorisedController.show().url)
          }
        }

        "the user answered badly" should {
          "return bad request" in {
            enable(UnauthorisedAgentFeature)

            val result = await(submit("this triggers a validation error"))

            status(result) must be(Status.BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include(Messages("authorise-agent.title", testAgencyName))
          }
        }
      }

      "the unauthorised agent flow is disabled" should {
        "throw a NotFoundException" in {
          disable(UnauthorisedAgentFeature)

          intercept[NotFoundException](await(TestAuthoriseAgentController.submit().apply(request)))
        }
      }
    }
  }
}
