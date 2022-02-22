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

package controllers.individual.business

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccessResponse
import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import controllers.ControllerBaseSpec
import forms.individual.business.RemoveOverseasPropertyForm
import forms.submapping.YesNoMapping
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.NotFoundException
import utilities.SubscriptionDataKeys
import views.html.individual.incometax.business.RemoveOverseasProperty

import scala.concurrent.Future

class RemoveOverseasPropertyControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockIncomeTaxSubscriptionConnector
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(SaveAndRetrieve)
  }

  "show" when {
    "the save & retrieve feature switch is enabled" must {
      "return OK and display the remove overseas property page" in withController { controller =>
        enable(SaveAndRetrieve)

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
    "the save & retrieve feature switch is disabled" must {
      "return a not found exception" in withController { controller =>
        val exception = intercept[NotFoundException](controller.show(subscriptionRequest).futureValue)
        exception.message mustBe "[RemoveOverseasPropertyController][show] - S&R feature switch is disabled"
      }
    }
  }

  "submit" when {
    "the save & retrieve feature switch is enabled" when {
      "the user does not select an option" must {
        "return BAD_REQUEST and display the overseas property page" in withController { controller =>
          enable(SaveAndRetrieve)

          val result: Future[Result] = controller.submit(subscriptionRequest)

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)

          verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.OverseasProperty, count = 0)
        }
      }
      "the user selects to remove the business" in withController { controller =>
        enable(SaveAndRetrieve)

        mockDeleteSubscriptionDetails(SubscriptionDataKeys.OverseasProperty)(DeleteSubscriptionDetailsSuccessResponse)

        val result: Result = controller.submit(
          subscriptionRequest.withFormUrlEncodedBody(RemoveOverseasPropertyForm.yesNo -> YesNoMapping.option_yes)
        ).futureValue

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.TaskListController.show().url)

        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.OverseasProperty, count = 1)
      }
      "the user selects to not remove the business" in withController { controller =>
        enable(SaveAndRetrieve)

        val result: Result = controller.submit(
          subscriptionRequest.withFormUrlEncodedBody(RemoveOverseasPropertyForm.yesNo -> YesNoMapping.option_no)
        ).futureValue

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.TaskListController.show().url)

        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.OverseasProperty, count = 0)
      }
    }
    "the save & retrieve feature switch is disabled" must {
      "return a not found exception" in withController { controller =>
        val exception = intercept[NotFoundException](controller.submit(subscriptionRequest).futureValue)
        exception.message mustBe "[RemoveOverseasPropertyController][submit] - S&R feature switch is disabled"
      }
    }
  }

  private def withController(testCode: RemoveOverseasPropertyController => Any): Unit = {
    val view = mock[RemoveOverseasProperty]

    when(view(any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new RemoveOverseasPropertyController(
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockIncomeTaxSubscriptionConnector,
      view,
    )

    testCode(controller)
  }

  override val controllerName: String = "RemoveOverseasPropertyController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestRemoveOverseasPropertyController.show,
    "submit" -> TestRemoveOverseasPropertyController.submit
  )

  val mockRemoveOverseasProperty: RemoveOverseasProperty = mock[RemoveOverseasProperty]

  object TestRemoveOverseasPropertyController extends RemoveOverseasPropertyController(
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockIncomeTaxSubscriptionConnector,
    mockRemoveOverseasProperty
  )

}