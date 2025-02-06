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
import forms.individual.business.PropertyStartDateForm
import models.DateModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.individual.mocks.MockPropertyStartDate

import java.time.LocalDate
import scala.concurrent.Future

class PropertyStartDateControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAuditingService
  with MockReferenceRetrieval
  with MockPropertyStartDate {

  override val controllerName: String = "PropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestPropertyStartDateController.show(isEditMode = false, isGlobalEdit = false),
    "submit" -> TestPropertyStartDateController.submit(isEditMode = false, isGlobalEdit = false)
  )

  object TestPropertyStartDateController extends PropertyStartDateController(
    propertyStartDate,
    mockSubscriptionDetailsService,
    mockReferenceRetrieval
  )(
    mockAuditingService,
    mockAuthService,
    appConfig,
    mockLanguageUtils
  )

  trait Test {
    val controller = new PropertyStartDateController(
      propertyStartDate,
      mockSubscriptionDetailsService,
      mockReferenceRetrieval
    )(
      mockAuditingService,
      mockAuthService,
      appConfig,
      mockLanguageUtils
    )
  }

  "show" should {
    "display the property start date view and return OK (200)" when {
      "no start date is returned" in new Test {
        mockPropertyStartDate(
          postAction = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateController.submit(),
          backUrl = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateBeforeLimitController.show().url
        )
        mockFetchPropertyStartDate(None)

        val result: Result = await(controller.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }
      "a start date is returned" in new Test {
        mockPropertyStartDate(
          postAction = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateController.submit(),
          backUrl = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateBeforeLimitController.show().url
        )
        mockFetchPropertyStartDate(Some(testPropertyStartDate))

        val result: Result = await(controller.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }
      "in edit mode" in new Test {
        mockPropertyStartDate(
          postAction = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateController.submit(editMode = true),
          backUrl = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateBeforeLimitController.show(editMode = true).url
        )
        mockFetchPropertyStartDate(Some(testPropertyStartDate))

        val result: Result = await(controller.show(isEditMode = true, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }
      "in global edit mode" in new Test {
        mockPropertyStartDate(
          postAction = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateController.submit(isGlobalEdit = true),
          backUrl = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateBeforeLimitController.show(isGlobalEdit = true).url
        )
        mockFetchPropertyStartDate(Some(testPropertyStartDate))

        val result: Result = await(controller.show(isEditMode = false, isGlobalEdit = true)(subscriptionRequest))

        status(result) must be(Status.OK)
      }
    }
  }

  "submit" when {
    val maxDate = LocalDate.now.minusYears(1)
    val testValidMaxDate: DateModel = DateModel.dateConvert(maxDate)
    val minDate = LocalDate.of(1900, 1, 1)

    val testPropertyStartDateModel: DateModel = testValidMaxDate

    def callSubmit(isEditMode: Boolean, isGlobalEdit: Boolean): Future[Result] =
      TestPropertyStartDateController.submit(isEditMode = isEditMode, isGlobalEdit = isGlobalEdit)(
        subscriptionRequest.post(PropertyStartDateForm.propertyStartDateForm(minDate, maxDate, d => d.toString), testPropertyStartDateModel)
      )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestPropertyStartDateController.submit(isEditMode = isEditMode, isGlobalEdit = false)(
      subscriptionRequest
    )

    "in edit mode" should {
      "redirect to the uk property check your answers page" in {
        mockSavePropertyStartDate(testPropertyStartDateModel)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = await(callSubmit(isEditMode = true, isGlobalEdit = false))

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.PropertyCheckYourAnswersController.show(editMode = true).url)
      }
    }

    "in global edit mode" should {
      "redirect to the uk property check your answers page" in {
        mockSavePropertyStartDate(testPropertyStartDateModel)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = await(callSubmit(isEditMode = true, isGlobalEdit = true))

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.PropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url)
      }
    }

    "not in edit mode" must {
      "redirect to the uk property accounting method page" in {
        mockSavePropertyStartDate(testPropertyStartDateModel)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = await(callSubmit(isEditMode = false, isGlobalEdit = false))

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.PropertyAccountingMethodController.show().url)
      }
    }

    "return BAD_REQUEST" when {
      "there is an invalid submission with an error form" in {
        mockPropertyStartDate(
          postAction = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateController.submit(),
          backUrl = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateBeforeLimitController.show().url)

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)
      }
    }

    "throw an exception" when {
      "cannot save the start date" in {
        mockSavePropertyStartDate(testPropertyStartDateModel)(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val result: Future[Result] = callSubmit(isEditMode = false, isGlobalEdit = false)

        intercept[InternalServerException](await(result)).message mustBe "[PropertyStartDateController][submit] - Could not save start date"
      }
    }
  }

  private lazy val testPropertyStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))

}

