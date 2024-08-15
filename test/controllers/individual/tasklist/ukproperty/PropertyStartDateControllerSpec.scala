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
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
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
    "show" -> TestPropertyStartDateController.show(isEditMode = false),
    "submit" -> TestPropertyStartDateController.submit(isEditMode = false)
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
    "display the property accounting method view and return OK (200)" in new Test {
      mockPropertyStartDate()
      mockFetchPropertyStartDate(None)

      val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      status(result) must be(Status.OK)
    }
  }

  "submit" when {

    val maxDate = LocalDate.now.minusYears(1)
    val testValidMaxDate: DateModel = DateModel.dateConvert(maxDate)
    val minDate = LocalDate.of(1900, 1, 1)

    val testPropertyStartDateModel: DateModel = testValidMaxDate

    def callSubmit(isEditMode: Boolean): Future[Result] = TestPropertyStartDateController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(PropertyStartDateForm.propertyStartDateForm(minDate, maxDate, d => d.toString), testPropertyStartDateModel)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestPropertyStartDateController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "in edit mode" should {
      "redirect to the uk property check your answers page" in {
        mockSavePropertyStartDate(testPropertyStartDateModel)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = await(callSubmit(isEditMode = true))

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.PropertyCheckYourAnswersController.show(true).url)
      }
    }

    "not in edit mode" must {
      "redirect to the uk property accounting method page" in {
        mockSavePropertyStartDate(testPropertyStartDateModel)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = await(callSubmit(isEditMode = false))

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.PropertyAccountingMethodController.show().url)
      }
    }

    "return BAD_REQUEST" when {
      "there is an invalid submission with an error form" in {
        mockPropertyStartDate()

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
      }
    }

    "throw an exception" when {
      "cannot save the start date" in {
        mockSavePropertyStartDate(testPropertyStartDateModel)(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val goodRequest: Future[Result] = callSubmit(isEditMode = false)

        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }

    "The back url is in edit mode" should {
      "redirect back to uk property check your answers page" in new Test {
        controller.backUrl(isEditMode = true) mustBe
          routes.PropertyCheckYourAnswersController.show(true).url
      }
    }

    "The back url is not in edit mode" should {
      "redirect back to what income source page" in new Test {
        controller.backUrl(isEditMode = false) mustBe
          controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
      }
    }
  }
}

