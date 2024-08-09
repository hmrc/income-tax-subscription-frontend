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

package controllers.agent.tasklist.ukproperty

import controllers.agent.AgentControllerBaseSpec
import forms.agent.AccountingMethodPropertyForm
import models.Cash
import models.common.PropertyModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockClientDetailsRetrieval, MockReferenceRetrieval, MockSubscriptionDetailsService}
import views.html.agent.tasklist.ukproperty.PropertyAccountingMethod

import scala.concurrent.Future

class PropertyAccountingMethodControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAuditingService
  with MockClientDetailsRetrieval
  with MockReferenceRetrieval {

  override val controllerName: String = "PropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private def withController(testCode: PropertyAccountingMethodController => Any): Unit = {
    val propertyAccountingMethodView = mock[PropertyAccountingMethod]

    when(propertyAccountingMethodView(any(), any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new PropertyAccountingMethodController(
      propertyAccountingMethodView,
      MockSubscriptionDetailsService,
      mockClientDetailsRetrieval,
      mockReferenceRetrieval
    )(
      mockAuditingService,
      appConfig,
      mockAuthService
    )

    testCode(controller)
  }

  "show" when {
    "there is no previous selected answer" should {
      "display the property accounting method view and return OK (200)" in withController { controller =>
        mockFetchProperty(None)

        val result = await(controller.show(isEditMode = false)(subscriptionRequestWithName))

        status(result) must be(Status.OK)
      }
    }

    "there is a previous selected answer CASH" should {
      "display the property accounting method view with the previous selected answer CASH and return OK (200)" in withController { controller =>
        mockFetchProperty(Some(PropertyModel(Some(Cash))))

        val result = await(controller.show(isEditMode = false)(subscriptionRequestWithName))

        status(result) must be(Status.OK)
      }
    }
  }

  "submit" should withController { controller =>

    def callSubmit(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequestWithName.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Cash)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequestWithName
    )

    "redirect to agent uk property check your answers page" in {
      mockFetchProperty(None)
      setupMockSubscriptionDetailsSaveFunctions()
      mockDeleteIncomeSourceConfirmationSuccess()

      val goodRequest: Future[Result] = callSubmit(isEditMode = false)

      status(goodRequest) mustBe Status.SEE_OTHER
      redirectLocation(goodRequest) mustBe Some(routes.PropertyCheckYourAnswersController.show().url)

      await(goodRequest)
      verifyPropertySave(Some(PropertyModel(accountingMethod = Some(Cash))))
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" in {
        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyPropertySave(None)
      }
    }

    "throw an exception" when {
      "cannot save the accounting method" in {
        setupMockSubscriptionDetailsSaveFunctionsFailure()
        mockFetchProperty(None)

        val goodRequest: Future[Result] = callSubmit(isEditMode = false)

        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }
  }

  "The back url" when {
    "in edit mode" should {
      "redirect to the agent uk property check your answers" in withController { controller =>
        controller.backUrl(
          isEditMode = true
        ) mustBe routes.PropertyCheckYourAnswersController.show(true).url
      }

    }

    "not in edit mode" should {
      "redirect back to uk property check your answers page" in withController { controller =>
        controller.backUrl(isEditMode = false) mustBe
          routes.PropertyStartDateController.show().url
      }
    }
  }
}