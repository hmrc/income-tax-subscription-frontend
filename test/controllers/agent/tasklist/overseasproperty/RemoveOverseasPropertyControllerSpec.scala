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

package controllers.agent.tasklist.overseasproperty

import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccessResponse
import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import controllers.agent.AgentControllerBaseSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.RemoveClientOverseasPropertyForm
import forms.submapping.YesNoMapping
import models.common.OverseasPropertyModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys
import views.html.agent.tasklist.overseasproperty.RemoveOverseasPropertyBusiness

import scala.concurrent.Future

class RemoveOverseasPropertyControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockIncomeTaxSubscriptionConnector
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner {

  override val controllerName: String = "RemoveOverseasPropertyController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "show" when {
    "return OK and display the client remove overseas property page" in withController { controller =>
      mockFetchOverseasProperty(Some(OverseasPropertyModel()))
      val result: Future[Result] = controller.show(subscriptionRequest)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }

    "redirect to Business Already Removed page" when {
      "no uk property business exists" in withController { controller =>
        mockFetchOverseasProperty(None)
        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.BusinessAlreadyRemovedController.show().url)
      }
    }
  }

  "submit" when {
    "redirect to your income sources page" when {
      "the user selects to remove the business" in withController { controller =>
        mockDeleteSubscriptionDetails(SubscriptionDataKeys.OverseasProperty)(Right(DeleteSubscriptionDetailsSuccessResponse))
        mockDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation)(Right(DeleteSubscriptionDetailsSuccessResponse))

        val result: Result = controller.submit(
          subscriptionRequest.withFormUrlEncodedBody(RemoveClientOverseasPropertyForm.yesNo -> YesNoMapping.option_yes)
        ).futureValue

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)

        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.OverseasProperty, count = 1)
        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.IncomeSourceConfirmation, count = 1)
      }


      "the user selects to not remove the business" in withController { controller =>
        val result: Result = controller.submit(
          subscriptionRequest.withFormUrlEncodedBody(RemoveClientOverseasPropertyForm.yesNo -> YesNoMapping.option_no)
        ).futureValue

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)

        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.OverseasProperty, count = 0)
        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.IncomeSourceConfirmation, count = 0)
      }
    }

    "return BAD_REQUEST and display the client remove overseas property page" when {
      "the user does not select an option" in withController { controller =>
        val result: Result = controller.submit(
          subscriptionRequest.withFormUrlEncodedBody()
        ).futureValue

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)
        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.OverseasProperty, count = 0)
        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.IncomeSourceConfirmation, count = 0)
      }
    }

    "throw an exception" when {
      "cannot remove the overseas property" in withController { controller =>
        mockDeleteSubscriptionDetailsFailure(SubscriptionDataKeys.OverseasProperty)
        mockDeleteSubscriptionDetailsFailure(SubscriptionDataKeys.IncomeSourceConfirmation)

        val result = controller.submit(
          subscriptionRequest.withFormUrlEncodedBody(RemoveClientOverseasPropertyForm.yesNo -> YesNoMapping.option_yes)
        )

        intercept[InternalServerException](await(result)).message mustBe "[RemoveOverseasPropertyController][submit] - Could not remove overseas property"
      }
      "cannot remove the income source confirmation" in withController { controller =>
        mockDeleteSubscriptionDetails(SubscriptionDataKeys.OverseasProperty)(Right(DeleteSubscriptionDetailsSuccessResponse))
        mockDeleteSubscriptionDetailsFailure(SubscriptionDataKeys.IncomeSourceConfirmation)

        val result = controller.submit(
          subscriptionRequest.withFormUrlEncodedBody(RemoveClientOverseasPropertyForm.yesNo -> YesNoMapping.option_yes)
        )

        intercept[InternalServerException](await(result)).message mustBe "[RemoveOverseasPropertyController][submit] - Failure to delete income source confirmation"
      }
    }
  }

  private def withController(testCode: RemoveOverseasPropertyController => Any): Unit = {
    val view = mock[RemoveOverseasPropertyBusiness]

    when(view(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)

    val controller = new RemoveOverseasPropertyController(
      mockIncomeTaxSubscriptionConnector,
      mockSubscriptionDetailsService,
      view,
      fakeIdentifierAction,
      fakeConfirmedClientJourneyRefiner
    )

    testCode(controller)
  }
}
