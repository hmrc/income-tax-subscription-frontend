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
import forms.individual.business.RemoveUkPropertyForm
import forms.submapping.YesNoMapping
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.NotFoundException
import utilities.SubscriptionDataKeys
import views.individual.mocks.MockRemoveUkProperty

import scala.concurrent.Future

class RemoveUkPropertyControllerSpec extends ControllerBaseSpec
  with MockRemoveUkProperty
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
      "return OK and display the remove Uk property page" in {
        enable(SaveAndRetrieve)

        mockRemoveUkProperty()

        val result: Future[Result] = TestRemoveUkPropertyController.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
    "the save & retrieve feature switch is disabled" must {
      "return a not found exception" in {
        val exception = intercept[NotFoundException](TestRemoveUkPropertyController.show(subscriptionRequest).futureValue)
        exception.message mustBe "[RemoveUkPropertyController][show] - S&R feature switch is disabled"
      }
    }
  }

  "submit" when {
    "the save & retrieve feature switch is enabled" when {
      "the user does not select an option" must {
        "return BAD_REQUEST and display the Uk property page" in {
          enable(SaveAndRetrieve)

          mockRemoveUkProperty()

          val result: Future[Result] = TestRemoveUkPropertyController.submit(subscriptionRequest)

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)

          verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.UkProperty, count = 0)
        }
      }
      "the user selects to remove the business" in {
        enable(SaveAndRetrieve)

        mockDeleteSubscriptionDetails(SubscriptionDataKeys.UkProperty)(DeleteSubscriptionDetailsSuccessResponse)

        val result: Result = TestRemoveUkPropertyController.submit(
          subscriptionRequest.withFormUrlEncodedBody(RemoveUkPropertyForm.yesNo -> YesNoMapping.option_yes)
        ).futureValue

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.TaskListController.show().url)

        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.UkProperty, count = 1)
      }
      "the user selects to not remove the business" in {
        enable(SaveAndRetrieve)

        val result: Result = TestRemoveUkPropertyController.submit(
          subscriptionRequest.withFormUrlEncodedBody(RemoveUkPropertyForm.yesNo -> YesNoMapping.option_no)
        ).futureValue

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.TaskListController.show().url)

        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.UkProperty, count = 0)
      }
    }
    "the save & retrieve feature switch is disabled" must {
      "return a not found exception" in {
        val exception = intercept[NotFoundException](TestRemoveUkPropertyController.submit(subscriptionRequest).futureValue)
        exception.message mustBe "[RemoveUkPropertyController][submit] - S&R feature switch is disabled"
      }
    }
  }

  override val controllerName: String = "RemoveUkPropertyController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestRemoveUkPropertyController.show,
    "submit" -> TestRemoveUkPropertyController.submit
  )

  object TestRemoveUkPropertyController extends RemoveUkPropertyController(
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockIncomeTaxSubscriptionConnector,
    removeUkProperty
  )

}
