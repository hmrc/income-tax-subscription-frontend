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

import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.individual.ControllerBaseSpec
import forms.individual.business.AccountingMethodPropertyForm
import models.{Accruals, Cash}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.individual.mocks.MockPropertyAccountingMethod

import scala.concurrent.Future

class PropertyAccountingMethodControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockReferenceRetrieval
  with MockAuditingService
  with MockPropertyAccountingMethod {

  override val controllerName: String = "PropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private def withController(testCode: PropertyAccountingMethodController => Any): Unit = {

    val controller = new PropertyAccountingMethodController(
      mockView,
      mockSubscriptionDetailsService,
      mockReferenceRetrieval
    )(
      mockAuditingService,
      mockAuthService,
      appConfig
    )

    testCode(controller)
  }

  "show" should {
    "display the property accounting method view and return OK (200)" when {
      "accounting method is returned" in withController { controller =>
        mockFetchPropertyAccountingMethod(Some(Cash))
        mockPropertyAccountingMethodView(
          routes.PropertyAccountingMethodController.submit(),
          backUrl = routes.PropertyStartDateController.show().url
        )
        lazy val result = await(controller.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }

      "no accounting method is returned" in withController { controller =>
        mockFetchPropertyAccountingMethod(None)
        mockPropertyAccountingMethodView(
          routes.PropertyAccountingMethodController.submit(),
          backUrl = routes.PropertyStartDateController.show().url
        )
        lazy val result = await(controller.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }

      "in edit mode" in withController { controller =>
        mockFetchPropertyAccountingMethod(None)
        mockPropertyAccountingMethodView(
          routes.PropertyAccountingMethodController.submit(editMode = true),
          backUrl = routes.PropertyCheckYourAnswersController.show(editMode = true).url
        )
        lazy val result = await(controller.show(isEditMode = true, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }

      "in global edit mode" in withController { controller =>
        mockFetchPropertyAccountingMethod(None)
        mockPropertyAccountingMethodView(
          routes.PropertyAccountingMethodController.submit(editMode = true, isGlobalEdit = true),
          backUrl = routes.PropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url
        )
        lazy val result = await(controller.show(isEditMode = true, isGlobalEdit = true)(subscriptionRequest))

        status(result) must be(Status.OK)
      }
    }
  }

  "submit" should withController { controller =>

    def callSubmit(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode, isGlobalEdit = false)(
      subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Cash)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean, isGlobalEdit: Boolean = false): Future[Result] =
      controller.submit(isEditMode, isGlobalEdit)(subscriptionRequest)

    "redirect to uk property check your answers page" when {
      "not in edit mode" in {
        mockSavePropertyAccountingMethod(Cash)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = false)

        await(goodRequest)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.PropertyCheckYourAnswersController.show().url)
      }

      "in edit mode" in {
        mockSavePropertyAccountingMethod(Accruals)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = controller.submit(isEditMode = true, isGlobalEdit = false)(
          subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Accruals)
        )
        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.PropertyCheckYourAnswersController.show(true).url)
      }

      "in global edit mode" in {
        mockSavePropertyAccountingMethod(Accruals)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = controller.submit(isEditMode = true, isGlobalEdit = true)(
          subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Accruals)
        )
        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.PropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url)
      }
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" when {
        "not in edit mode" in {
          mockPropertyAccountingMethodView(
            routes.PropertyAccountingMethodController.submit(),
            backUrl = routes.PropertyStartDateController.show().url
          )
          val badRequest = callSubmitWithErrorForm(isEditMode = false)

          status(badRequest) must be(Status.BAD_REQUEST)
        }

        "in global edit mode" in {
          mockPropertyAccountingMethodView(
            routes.PropertyAccountingMethodController.submit(editMode = true, isGlobalEdit = true),
            backUrl = routes.PropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url
          )
          val badRequest = callSubmitWithErrorForm(isEditMode = true, isGlobalEdit = true)

          status(badRequest) must be(Status.BAD_REQUEST)
        }
      }
    }

    "throw an exception" when {
      "cannot save the accounting method" in {
        mockSavePropertyAccountingMethod(Cash)(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val result: Future[Result] = callSubmit(isEditMode = false)

        intercept[InternalServerException](await(result)).message mustBe "[PropertyAccountingMethodController][submit] - Could not save accounting method"
      }
    }
  }
}
