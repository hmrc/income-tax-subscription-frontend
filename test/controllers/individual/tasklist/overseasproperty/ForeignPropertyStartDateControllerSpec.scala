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

import config.featureswitch.FeatureSwitch.RemoveAccountingMethod
import config.featureswitch.FeatureSwitching
import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.individual.ControllerBaseSpec
import forms.individual.business.ForeignPropertyStartDateForm
import models.DateModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.individual.mocks.MockAuthService
import services.mocks.{MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.individual.mocks.MockOverseasPropertyStartDate

import java.time.LocalDate
import scala.concurrent.Future

class ForeignPropertyStartDateControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAuthService
  with MockAuditingService
  with MockReferenceRetrieval
  with MockOverseasPropertyStartDate
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(RemoveAccountingMethod)
  }

  override val controllerName: String = "ForeignPropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestForeignPropertyStartDateController.show(isEditMode = false, isGlobalEdit = false),
    "submit" -> TestForeignPropertyStartDateController.submit(isEditMode = false, isGlobalEdit = false)
  )

  object TestForeignPropertyStartDateController extends ForeignPropertyStartDateController(
    foreignPropertyStartDate,
    mockSubscriptionDetailsService,
    mockReferenceRetrieval
  )(
    mockAuditingService,
    mockAuthService,
    appConfig,
    mockLanguageUtils
  )

  "show" must {
    "display the foreign property start date view and return OK (200)" when {
      "a start date is returned" in {
        mockForeignPropertyStartDateView(
          postAction = routes.ForeignPropertyStartDateController.submit(),
          backUrl = routes.ForeignPropertyStartDateBeforeLimitController.show().url
        )
        mockFetchOverseasPropertyStartDate(Some(DateModel("22", "11", "2021")))

        lazy val result: Result = await(TestForeignPropertyStartDateController.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }

      "no start date is returned" in withController { controller =>
        mockForeignPropertyStartDateView(
          postAction = routes.ForeignPropertyStartDateController.submit(),
          backUrl = routes.ForeignPropertyStartDateBeforeLimitController.show().url
        )
        mockFetchOverseasPropertyStartDate(None)

        lazy val result: Result = await(controller.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }

      "in edit mode" in withController { controller =>
        mockForeignPropertyStartDateView(
          postAction = routes.ForeignPropertyStartDateController.submit(editMode = true),
          backUrl = routes.ForeignPropertyStartDateBeforeLimitController.show(editMode = true).url
        )
        mockFetchOverseasPropertyStartDate(Some(DateModel("22", "11", "2021")))

        lazy val result: Result = await(controller.show(isEditMode = true, isGlobalEdit = false)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "in global edit mode" in withController { controller =>
        mockForeignPropertyStartDateView(
          postAction = routes.ForeignPropertyStartDateController.submit(editMode = true, isGlobalEdit = true),
          backUrl = routes.ForeignPropertyStartDateBeforeLimitController.show(editMode = true, isGlobalEdit = true).url
        )
        mockFetchOverseasPropertyStartDate(Some(DateModel("22", "11", "2021")))

        lazy val result: Result = await(controller.show(isEditMode = true, isGlobalEdit = true)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" when {
    val testValidMaxStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))

    def callPost(controller: ForeignPropertyStartDateController, isEditMode: Boolean, isGlobalEdit: Boolean): Future[Result] =
      controller.submit(isEditMode, isGlobalEdit)(
        subscriptionRequest.post(ForeignPropertyStartDateForm.startDateForm(_.toString),
          testValidMaxStartDate)
      )

    def callPostWithErrorForm(controller: ForeignPropertyStartDateController, isEditMode: Boolean, isGlobalEdit: Boolean): Future[Result] =
      controller.submit(isEditMode, isGlobalEdit)(
        subscriptionRequest.withFormUrlEncodedBody()
      )

    "in edit mode" should {
      "redirect to overseas property check your answers page" in withController { controller =>
        mockSaveOverseasPropertyStartDate(testValidMaxStartDate)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callPost(controller, isEditMode = true, isGlobalEdit = false)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url)
      }
    }

    "in global edit mode" should {
      "redirect to overseas property CYA page" in withController { controller =>
        mockSaveOverseasPropertyStartDate(testValidMaxStartDate)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callPost(controller, isEditMode = true, isGlobalEdit = true)

        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url)
      }
    }

    "not in edit mode" should {
      "redirect to the overseas property accounting method page" in withController { controller =>
        mockSaveOverseasPropertyStartDate(testValidMaxStartDate)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = await(callPost(controller, isEditMode = false, isGlobalEdit = false))

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyAccountingMethodController.show().url)
      }
    }

    "not in edit mode" should {
      "redirect to the overseas property check your answers page when feature switch is enabled" in withController { controller =>
        enable(RemoveAccountingMethod)

        mockSaveOverseasPropertyStartDate(testValidMaxStartDate)(Right(PostSubscriptionDetailsSuccessResponse))

        val result = await(callPost(controller, isEditMode = false, isGlobalEdit = false))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show().url
        )
      }
    }

    "there is an invalid submission with an error form" should {
      "return bad request status (400)" when {
        "in edit mode" in withController { controller =>
          mockForeignPropertyStartDateView(
            postAction = routes.ForeignPropertyStartDateController.submit(editMode = true),
            backUrl = routes.ForeignPropertyStartDateBeforeLimitController.show(editMode = true).url
          )

          val badRequest = callPostWithErrorForm(controller, isEditMode = true, isGlobalEdit = false)

          status(badRequest) must be(Status.BAD_REQUEST)
        }

        "in global mode" in withController { controller =>
          mockForeignPropertyStartDateView(
            routes.ForeignPropertyStartDateController.submit(editMode = true, isGlobalEdit = true),
            routes.ForeignPropertyStartDateBeforeLimitController.show(editMode = true, isGlobalEdit = true).url
          )

          val badRequest = callPostWithErrorForm(controller, isEditMode = true, isGlobalEdit = true)

          status(badRequest) must be(Status.BAD_REQUEST)
        }

        "not in edit mode" in withController { controller =>
          mockForeignPropertyStartDateView(
            routes.ForeignPropertyStartDateController.submit(),
            routes.ForeignPropertyStartDateBeforeLimitController.show().url
          )

          val badRequest = callPostWithErrorForm(controller, isEditMode = false, isGlobalEdit = false)

          status(badRequest) must be(Status.BAD_REQUEST)
        }
      }

    }

    "throw an exception" when {
      "cannot save the start date" in withController { controller =>
        mockSaveOverseasPropertyStartDate(testValidMaxStartDate)(
          Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
        )

        val exceptionRequest = callPost(controller, isEditMode = false, isGlobalEdit = true)

        intercept[InternalServerException](await(exceptionRequest))
          .message mustBe "[ForeignPropertyStartDateController][submit] - Could not save start date"
      }
    }

  }

  private def withController(testCode: ForeignPropertyStartDateController => Any) = {
    val controller = new ForeignPropertyStartDateController(
      foreignPropertyStartDate,
      mockSubscriptionDetailsService,
      mockReferenceRetrieval
    )(
      mockAuditingService,
      mockAuthService,
      appConfig,
      mockLanguageUtils
    )

    testCode(controller)
  }

}
