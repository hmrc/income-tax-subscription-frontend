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

package controllers.individual.tasklist.ukproperty

import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccessResponse
import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import controllers.individual.ControllerBaseSpec
import forms.individual.business.RemoveUkPropertyForm
import forms.submapping.YesNoMapping
import models.Cash
import models.common.PropertyModel
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.{MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import utilities.SubscriptionDataKeys
import views.individual.mocks.MockRemoveUkProperty

import scala.concurrent.Future

class RemoveUkPropertyControllerSpec extends ControllerBaseSpec
  with MockRemoveUkProperty
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockReferenceRetrieval
  with MockIncomeTaxSubscriptionConnector {

  "show" should {
    "return OK and display the remove Uk property page" in {
      mockRemoveUkProperty()
      mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash))))

      val result: Future[Result] = TestRemoveUkPropertyController.show(subscriptionRequest)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }

    "redirect to Business Already Removed page" when {
      "no uk property business exists" in {
        mockFetchProperty(None)
        val result: Future[Result] = TestRemoveUkPropertyController.show(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.BusinessAlreadyRemovedController.show().url)
      }
    }
  }

  "submit" should {
    "return BAD_REQUEST and display the Uk property page" when {
      "the user does not select an option" in {
        mockRemoveUkProperty()

        val result: Future[Result] = TestRemoveUkPropertyController.submit(subscriptionRequest)

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)

        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.Property, count = 0)
        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.IncomeSourceConfirmation, count = 0)
      }
    }

    "redirect to the task list page" when {
      "the user selects to remove the business" in {
        mockDeleteSubscriptionDetails(SubscriptionDataKeys.Property)(Right(DeleteSubscriptionDetailsSuccessResponse))
        mockDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation)(Right(DeleteSubscriptionDetailsSuccessResponse))

        val result: Result = TestRemoveUkPropertyController.submit(
          subscriptionRequest.withFormUrlEncodedBody(RemoveUkPropertyForm.yesNo -> YesNoMapping.option_yes)
        ).futureValue

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)

        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.Property, count = 1)
        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.IncomeSourceConfirmation, count = 1)
      }

      "the user selects to not remove the business" in {
        val result: Result = TestRemoveUkPropertyController.submit(
          subscriptionRequest.withFormUrlEncodedBody(RemoveUkPropertyForm.yesNo -> YesNoMapping.option_no)
        ).futureValue

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)

        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.Property, count = 0)
        verifyDeleteSubscriptionDetails(id = SubscriptionDataKeys.IncomeSourceConfirmation, count = 0)
      }
    }

    "throw an exception" when {
      "cannot remove the UK property" in {
        mockDeleteSubscriptionDetailsFailure(SubscriptionDataKeys.Property)
        mockDeleteSubscriptionDetailsFailure(SubscriptionDataKeys.IncomeSourceConfirmation)

        val result = TestRemoveUkPropertyController.submit(
          subscriptionRequest.withFormUrlEncodedBody(RemoveUkPropertyForm.yesNo -> YesNoMapping.option_yes)
        )

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }
  }

  override val controllerName: String = "RemoveUkPropertyController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestRemoveUkPropertyController.show,
    "submit" -> TestRemoveUkPropertyController.submit
  )

  object TestRemoveUkPropertyController extends RemoveUkPropertyController(
    mockIncomeTaxSubscriptionConnector,
    mockReferenceRetrieval,
    mockSubscriptionDetailsService,
    removeUkProperty
  )(
    mockAuditingService,
    mockAuthService,
    appConfig
  )
}
