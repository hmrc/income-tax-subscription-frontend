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

package controllers.individual.business

import controllers.ControllerBaseSpec
import forms.individual.business.UkPropertyCountForm
import models.common.PropertyModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.incometax.business.UkPropertyCount

import scala.concurrent.Future

class UkPropertyCountControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService {

  override val controllerName: String = "UkPropertyController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUkPropertyCountController.show(false),
    "submit" -> TestUkPropertyCountController.submit(false)
  )

  object TestUkPropertyCountController extends UkPropertyCountController(
    mockAuditingService,
    mock[UkPropertyCount],
    mockAuthService,
    MockSubscriptionDetailsService
  )

  trait Setup {
    val ukPropertyCount: UkPropertyCount = mock[UkPropertyCount]

    val controller = new UkPropertyCountController(
      mockAuditingService,
      ukPropertyCount,
      mockAuthService,
      MockSubscriptionDetailsService
    )
  }

  "show" must {
    "return the uk property count view as html" when {
      "the call to fetch the uk property count value returns a valid value" in new Setup {
        val returnedCount: Int = 1

        mockFetchProperty(Some(PropertyModel(count = Some(returnedCount))))

        when(ukPropertyCount(
          ArgumentMatchers.eq(UkPropertyCountForm.form.fill(returnedCount)),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(routes.PropertyStartDateController.show().url)
        )(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(false)(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the call to fetch the uk property count value returns empty" in new Setup {
        mockFetchProperty(None)

        when(ukPropertyCount(
          ArgumentMatchers.eq(UkPropertyCountForm.form),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(routes.PropertyStartDateController.show().url)
        )(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(false)(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the page is in edit mode" in new Setup {
        mockFetchProperty(None)

        when(ukPropertyCount(
          ArgumentMatchers.eq(UkPropertyCountForm.form),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(routes.PropertyCheckYourAnswersController.show(true).url)
        )(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(true)(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" must {
    "save the uk property count and redirect to the uk property accounting method page" when {
      "the form was submitted with a valid input" in new Setup {
        mockFetchProperty(None)
        mockSaveUkProperty("test-reference")

        val result: Future[Result] = controller.submit(false)(
          subscriptionRequest.withFormUrlEncodedBody(UkPropertyCountForm.fieldName -> "1")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.PropertyAccountingMethodController.show().url)

        verifyPropertySave(Some(PropertyModel(count = Some(1))))
      }
    }

    "save the uk property count and redirect to the uk property check your answers" when {
      "the form was submitted with a valid input and the page is in edit mode" in new Setup {
        mockFetchProperty(None)
        mockSaveUkProperty("test-reference")

        val result: Future[Result] = controller.submit(true)(
          subscriptionRequest.withFormUrlEncodedBody(UkPropertyCountForm.fieldName -> "1")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show(true).url)

        verifyPropertySave(Some(PropertyModel(count = Some(1))))
      }
    }

    "throw an InternalServerException" when {
      "there was a problem saving the uk property count" in new Setup {
        mockFetchProperty(None)
        setupMockSubscriptionDetailsSaveFunctionsFailure()

        val result: Future[Result] = controller.submit(false)(
          subscriptionRequest.withFormUrlEncodedBody(UkPropertyCountForm.fieldName -> "1")
        )

        intercept[InternalServerException](await(result))
          .message mustBe "[PropertyCountController][submit] - Could not save number of properties"
      }
    }

    "return the uk property page count with an error" when {
      "the form was submitted with an invalid input" in new Setup {
        when(ukPropertyCount(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(routes.PropertyStartDateController.show().url)
        )(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.submit(false)(
          subscriptionRequest.withFormUrlEncodedBody(UkPropertyCountForm.fieldName -> "")
        )

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  authorisationTests()

}
