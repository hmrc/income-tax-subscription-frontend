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

import config.MockConfig.mockLanguageUtils
import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.ControllerSpec
import controllers.individual.actions.mocks.{MockIdentifierAction, MockSignUpJourneyRefiner}
import forms.individual.business.ForeignPropertyStartDateForm
import models.DateModel
import play.api.mvc.Result
import play.api.test.Helpers.*
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.individual.mocks.MockOverseasPropertyStartDate

import java.time.LocalDate
import scala.concurrent.Future

class ForeignPropertyStartDateControllerSpec extends ControllerSpec
    with MockOverseasPropertyStartDate
    with MockSubscriptionDetailsService
    with MockIdentifierAction
    with MockSignUpJourneyRefiner {

  "show" must {
    "display the foreign property start date view and return OK (200)" when {
      "a start date is returned" in withController { controller =>
        mockForeignPropertyStartDateView(
          postAction = routes.ForeignPropertyStartDateController.submit()
        )
        mockFetchOverseasPropertyStartDate(Some(DateModel("22", "11", "2021")))

        val result: Future[Result] = controller.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "no start date is returned" in withController { controller =>
        mockForeignPropertyStartDateView(
          postAction = routes.ForeignPropertyStartDateController.submit()
        )
        mockFetchOverseasPropertyStartDate(None)

        val result: Future[Result] = controller.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "in edit mode" in withController { controller =>
        mockForeignPropertyStartDateView(
          postAction = routes.ForeignPropertyStartDateController.submit(editMode = true)
        )
        mockFetchOverseasPropertyStartDate(Some(DateModel("22", "11", "2021")))

        val result: Future[Result] = controller.show(isEditMode = true, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "in global edit mode" in withController { controller =>
        mockForeignPropertyStartDateView(
          postAction = routes.ForeignPropertyStartDateController.submit(editMode = true, isGlobalEdit = true)
        )

        mockFetchOverseasPropertyStartDate(Some(DateModel("22", "11", "2021")))

        val result: Future[Result] = controller.show(isEditMode = true, isGlobalEdit = true)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" when {
    val testValidMaxStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))

    def callPost(controller: ForeignPropertyStartDateController, isEditMode: Boolean, isGlobalEdit: Boolean): Future[Result] =
      controller.submit(isEditMode, isGlobalEdit)(
        request
          .withHeaders(
            "Content-Type" -> "application/x-www-form-urlencoded"
          )
          .withMethod(POST)
          .withFormUrlEncodedBody(
            s"${ForeignPropertyStartDateForm.startDate}-dateDay" ->
              testValidMaxStartDate.day,
            s"${ForeignPropertyStartDateForm.startDate}-dateMonth" ->
              testValidMaxStartDate.month,
            s"${ForeignPropertyStartDateForm.startDate}-dateYear" ->
              testValidMaxStartDate.year
          )
      )

    def callPostWithErrorForm(controller: ForeignPropertyStartDateController, isEditMode: Boolean, isGlobalEdit: Boolean): Future[Result] =
      controller.submit(isEditMode, isGlobalEdit)(
        request
          .withMethod(POST)
          .withFormUrlEncodedBody()
      )

    "in edit mode" should {
      "redirect to overseas property check your answers page" in withController { controller =>
        mockSaveOverseasPropertyStartDate(testValidMaxStartDate)(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = callPost(controller, isEditMode = true, isGlobalEdit = false)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url)
      }
    }

    "in global edit mode" should {
      "redirect to overseas property CYA page" in withController { controller =>
        mockSaveOverseasPropertyStartDate(testValidMaxStartDate)(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = callPost(controller, isEditMode = true, isGlobalEdit = true)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url)
      }
    }

    "not in edit mode" should {
      "redirect to the overseas property check your answers page" in withController { controller =>
        mockSaveOverseasPropertyStartDate(testValidMaxStartDate)(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = callPost(controller, isEditMode = false, isGlobalEdit = false)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show().url
        )
      }
    }

    "there is an invalid submission with an error form" should {
      "return bad request status (400)" when {
        "in edit mode" in withController { controller =>
          mockForeignPropertyStartDateView(postAction = routes.ForeignPropertyStartDateController.submit(editMode = true))

          val result: Future[Result] = callPostWithErrorForm(controller, isEditMode = true, isGlobalEdit = false)

          status(result) mustBe BAD_REQUEST
        }

        "in global mode" in withController { controller =>
          mockForeignPropertyStartDateView(postAction = routes.ForeignPropertyStartDateController.submit(editMode = true, isGlobalEdit = true))

          val result: Future[Result] = callPostWithErrorForm(controller, isEditMode = true, isGlobalEdit = true)

          status(result) mustBe BAD_REQUEST
        }

        "not in edit mode" in withController { controller =>
          mockForeignPropertyStartDateView(postAction = routes.ForeignPropertyStartDateController.submit())

          val result: Future[Result] = callPostWithErrorForm(controller, isEditMode = false, isGlobalEdit = false)

          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "throw an exception" when {
      "cannot save the start date" in withController { controller =>
        mockSaveOverseasPropertyStartDate(testValidMaxStartDate)(
          Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
        )

        val result: Future[Result] = callPost(controller, isEditMode = false, isGlobalEdit = true)

        intercept[InternalServerException] {
          await(result)
        }.message mustBe "[ForeignPropertyStartDateController][submit] - Could not save start date"
      }
    }
  }

  private def withController(testCode: ForeignPropertyStartDateController => Any): Any = {
    val controller = new ForeignPropertyStartDateController(
      foreignPropertyStartDate,
      mockSubscriptionDetailsService,
      fakeIdentifierAction,
      fakeSignUpJourneyRefiner
    )(
      mockLanguageUtils
    )

    testCode(controller)
  }
}
