/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.agent.business

import agent.audit.mocks.MockAuditingService
import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccessResponse
import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import controllers.agent.AgentControllerBaseSpec
import forms.agent.ClientRemoveUkPropertyForm
import forms.submapping.YesNoMapping
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys
import views.html.agent.business.ClientRemoveUkProperty

import scala.concurrent.Future

class RemoveUkPropertyControllerSpec extends AgentControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockIncomeTaxSubscriptionConnector {

  override val controllerName: String = "RemoveUkPropertyController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "show" when {
    "return OK and display the client remove Uk property page" in withController { controller =>
      val result: Future[Result] = controller.show(subscriptionRequest)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }
  }

  "submit" when {
    "the user does not select an option" must {
      "return BAD_REQUEST and display the client remove Uk property page" in withController { controller =>
        val result: Result = controller.submit(
          subscriptionRequest.withFormUrlEncodedBody()
        ).futureValue

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)

        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.Property, count = 0)
      }
    }

    "the user selects to remove the business" must {
      "remove the UK property business and redirect to agent task list page" in withController { controller =>
        mockDeleteSubscriptionDetails(SubscriptionDataKeys.Property)(Right(DeleteSubscriptionDetailsSuccessResponse))

        val result: Result = controller.submit(
          subscriptionRequest.withFormUrlEncodedBody(ClientRemoveUkPropertyForm.yesNo -> YesNoMapping.option_yes)
        ).futureValue

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.TaskListController.show().url)

        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.Property, count = 1)
      }
    }

    "the user selects to not remove the business" must {
      "does not remove UK property business and redirect to agent task list page" in withController { controller =>
        val result: Result = controller.submit(
          subscriptionRequest.withFormUrlEncodedBody(ClientRemoveUkPropertyForm.yesNo -> YesNoMapping.option_no)
        ).futureValue

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.TaskListController.show().url)

        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.Property, count = 0)
      }
    }
  }

  private def withController(testCode: RemoveUkPropertyController => Any): Unit = {
    val view = mock[ClientRemoveUkProperty]

    when(view(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)

    val controller = new RemoveUkPropertyController(
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockIncomeTaxSubscriptionConnector,
      view
    )

    testCode(controller)
  }
}
