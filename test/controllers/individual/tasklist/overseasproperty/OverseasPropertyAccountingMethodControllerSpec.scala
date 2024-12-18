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

package controllers.individual.tasklist.overseasproperty

import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.individual.ControllerBaseSpec
import forms.individual.business.AccountingMethodOverseasPropertyForm
import models.Cash
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.individual.mocks.MockOverseasPropertyAccountingMethod

import scala.concurrent.Future

class OverseasPropertyAccountingMethodControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockReferenceRetrieval
  with MockAuditingService
  with MockOverseasPropertyAccountingMethod {

  override val controllerName: String = "ForeignPropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private def withController(testCode: OverseasPropertyAccountingMethodController => Any): Unit = {

    val controller = new OverseasPropertyAccountingMethodController(
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
    "display the foreign property accounting method view and return OK (200)" in withController { controller =>
      mockOverseasPropertyAccountingMethod(
        postAction = routes.OverseasPropertyAccountingMethodController.submit(),
        backUrl = routes.OverseasPropertyStartDateController.show().url
      )
      mockFetchOverseasPropertyAccountingMethod(Some(Cash))

      val result = await(controller.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

      status(result) must be(Status.OK)
    }

    "display the correct backUrl" should {
      "in edit mode" in withController { controller =>
        mockOverseasPropertyAccountingMethod(
          postAction = routes.OverseasPropertyAccountingMethodController.submit(editMode = true),
          backUrl = routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
        )
        mockFetchOverseasPropertyAccountingMethod(Some(Cash))

        lazy val result: Result = await(controller.show(isEditMode = true, isGlobalEdit = false)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "in global edit mode" in withController { controller =>
        mockOverseasPropertyAccountingMethod(
          postAction = routes.OverseasPropertyAccountingMethodController.submit(editMode = true, isGlobalEdit = true),
          backUrl = routes.OverseasPropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url
        )
        mockFetchOverseasPropertyAccountingMethod(Some(Cash))

        lazy val result: Result = await(controller.show(isEditMode = true, isGlobalEdit = true)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "not in edit mode" in withController { controller =>
        mockOverseasPropertyAccountingMethod(
          postAction = routes.OverseasPropertyAccountingMethodController.submit(),
          backUrl = routes.OverseasPropertyStartDateController.submit().url
        )
        mockFetchOverseasPropertyAccountingMethod(Some(Cash))

        lazy val result: Result = await(controller.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" should withController { controller =>

    def callSubmit(isEditMode: Boolean, isGlobalEdit: Boolean): Future[Result] = controller.submit(isEditMode, isGlobalEdit)(
      subscriptionRequest.post(AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm, Cash)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean, isGlobalEdit: Boolean): Future[Result] = controller.submit(isEditMode, isGlobalEdit)(
      subscriptionRequest.withFormUrlEncodedBody()
    )

    "redirect to overseas property check your answer page" when {
      "not in edit mode" in {
        mockSaveOverseasAccountingMethodProperty(Cash)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = false, isGlobalEdit = false)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show().url)

        await(goodRequest)
      }

      "in edit mode" in {
        mockSaveOverseasAccountingMethodProperty(Cash)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = true, isGlobalEdit = false)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url)

        await(goodRequest)
      }

      "in global edit mode" in {
        mockSaveOverseasAccountingMethodProperty(Cash)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = true, isGlobalEdit = true)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url)

        await(goodRequest)
      }
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" when {
        "not in edit mode" in {
          mockOverseasPropertyAccountingMethod(
            postAction = routes.OverseasPropertyAccountingMethodController.submit(),
            backUrl = routes.OverseasPropertyStartDateController.show().url)

          val badRequest = callSubmitWithErrorForm(isEditMode = false, isGlobalEdit = false)

          status(badRequest) must be(Status.BAD_REQUEST)
        }

        "in edit mode" in {
          mockOverseasPropertyAccountingMethod(
            postAction = routes.OverseasPropertyAccountingMethodController.submit(editMode = true),
            backUrl = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url)

          val badRequest = callSubmitWithErrorForm(isEditMode = true, isGlobalEdit = false)

          status(badRequest) must be(Status.BAD_REQUEST)
        }

        "in globaledit mode" in {
          mockOverseasPropertyAccountingMethod(
            postAction = routes.OverseasPropertyAccountingMethodController.submit(editMode = true, isGlobalEdit = true),
            backUrl = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url)

          val badRequest = callSubmitWithErrorForm(isEditMode = true, isGlobalEdit = true)

          status(badRequest) must be(Status.BAD_REQUEST)
        }
      }
    }

    "throw an exception" when {
      "cannot save the accounting method" in {
        mockSaveOverseasAccountingMethodProperty(Cash)(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val exceptionRequest = callSubmit(isEditMode = false, isGlobalEdit = false)

        intercept[InternalServerException](await(exceptionRequest))
          .message mustBe "[OverseasPropertyAccountingMethodController][submit] - Could not save accounting method"
      }
    }

  }
}