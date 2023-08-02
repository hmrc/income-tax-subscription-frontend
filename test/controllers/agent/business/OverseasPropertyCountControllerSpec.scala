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

package controllers.agent.business

import controllers.agent.AgentControllerBaseSpec
import forms.agent.OverseasPropertyCountForm
import models.common.OverseasPropertyModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.business.OverseasPropertyCount

import scala.concurrent.Future

class OverseasPropertyCountControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService {

  override val controllerName: String = "UkPropertyController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOverseasPropertyCountController.show(false),
    "submit" -> TestOverseasPropertyCountController.submit(false)
  )

  object TestOverseasPropertyCountController extends OverseasPropertyCountController(
    mock[OverseasPropertyCount],
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService
  )

  trait Setup {
    val overseasPropertyCount: OverseasPropertyCount = mock[OverseasPropertyCount]

    val controller = new OverseasPropertyCountController(
      overseasPropertyCount,
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService
    )
  }

  "show" must {
    "return the overseas property count view as html" when {
      "the call to fetch the overseas property count value returns a valid value" in new Setup {
        val returnedCount: Int = 1

        mockFetchOverseasProperty(Some(OverseasPropertyModel(count = Some(returnedCount))))

        when(overseasPropertyCount(
          ArgumentMatchers.eq(OverseasPropertyCountForm.form.fill(returnedCount)),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(routes.OverseasPropertyStartDateController.show().url),
          ArgumentMatchers.any()
        )(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(false)(subscriptionRequestWithName)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the call to fetch the overseas property count value returns empty" in new Setup {
        mockFetchOverseasProperty(None)

        when(overseasPropertyCount(
          ArgumentMatchers.eq(OverseasPropertyCountForm.form),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(routes.OverseasPropertyStartDateController.show().url),
          ArgumentMatchers.any(),
        )(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(false)(subscriptionRequestWithName)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the page is in edit mode" in new Setup {
        mockFetchOverseasProperty(None)

        when(overseasPropertyCount(
          ArgumentMatchers.eq(OverseasPropertyCountForm.form),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(routes.OverseasPropertyCheckYourAnswersController.show(true).url),
          ArgumentMatchers.any()
        )(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(true)(subscriptionRequestWithName)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" must {
    "save the overseas property count and redirect to the overseas property accounting method page" when {
      "the form was submitted with a valid input" in new Setup {
        mockFetchOverseasProperty(None)
        mockSaveOverseasProperty("test-reference")

        val result: Future[Result] = controller.submit(false)(
          subscriptionRequest.withFormUrlEncodedBody(OverseasPropertyCountForm.fieldName -> "1")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.OverseasPropertyAccountingMethodController.show().url)

        verifyOverseasPropertySave(Some(OverseasPropertyModel(count = Some(1))))
      }
    }

    "save the overseas property count and redirect to the overseas property check your answers" when {
      "the form was submitted with a valid input and the page is in edit mode" in new Setup {
        mockFetchOverseasProperty(None)
        mockSaveOverseasProperty("test-reference")

        val result: Future[Result] = controller.submit(true)(
          subscriptionRequest.withFormUrlEncodedBody(OverseasPropertyCountForm.fieldName -> "1")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(true).url)

        verifyOverseasPropertySave(Some(OverseasPropertyModel(count = Some(1))))
      }
    }

    "throw an InternalServerException" when {
      "there was a problem saving the overseas property count" in new Setup {
        mockFetchOverseasProperty(None)
        setupMockSubscriptionDetailsSaveFunctionsFailure()

        val result: Future[Result] = controller.submit(false)(
          subscriptionRequest.withFormUrlEncodedBody(OverseasPropertyCountForm.fieldName -> "1")
        )

        intercept[InternalServerException](await(result))
          .message mustBe "[OverseasPropertyCountController][submit] - Could not save number of foreign properties"
      }
    }

    "return the overseas property page count with an error" when {
      "the form was submitted with an invalid input" in new Setup {
        when(overseasPropertyCount(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(routes.OverseasPropertyStartDateController.show().url),
          ArgumentMatchers.any(),
        )(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.submit(false)(
          subscriptionRequestWithName.withFormUrlEncodedBody(OverseasPropertyCountForm.fieldName -> "")
        )

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  authorisationTests()

}
