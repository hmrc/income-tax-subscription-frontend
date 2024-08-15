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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import views.html.individual.tasklist.ukproperty.PropertyAccountingMethod

import scala.concurrent.Future

class PropertyAccountingMethodControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockReferenceRetrieval
  with MockAuditingService {

  override val controllerName: String = "PropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private def withController(testCode: PropertyAccountingMethodController => Any): Unit = {
    val propertyAccountingMethodView = mock[PropertyAccountingMethod]

    when(propertyAccountingMethodView(any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new PropertyAccountingMethodController(
      propertyAccountingMethodView,
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
    "display the property accounting method view and return OK (200)" in withController { controller =>
      mockFetchPropertyAccountingMethod(Some(Cash))

      lazy val result = await(controller.show(isEditMode = false)(subscriptionRequest))

      status(result) must be(Status.OK)
    }
  }

  "submit" should withController { controller =>

    def callSubmit(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Cash)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "redirect to uk property check your answers page" when {
      "not in edit mode" in {
        mockSavePropertyAccountingMethod(Cash)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = false)

        await(goodRequest)

        redirectLocation(goodRequest) mustBe Some(routes.PropertyCheckYourAnswersController.show().url)
      }

      "in edit mode" in {
        mockSavePropertyAccountingMethod(Accruals)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = controller.submit(isEditMode = true)(
          subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Accruals)
        )

        redirectLocation(goodRequest) mustBe Some(routes.PropertyCheckYourAnswersController.show(true).url)
      }
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" in {
        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
      }
    }

    "throw an exception" when {
      "cannot save the accounting method" in {
        mockSavePropertyAccountingMethod(Cash)(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val goodRequest: Future[Result] = callSubmit(isEditMode = false)

        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }

    "The back url" when {
      "is in edit mode" should {
        "redirect back to uk property start date page" in withController { controller =>
          controller.backUrl(isEditMode = true) mustBe
            routes.PropertyCheckYourAnswersController.show(true).url
        }
      }

      "is not in edit mode" when {
        "redirect back to uk property start date page" in withController { controller =>
          controller.backUrl(isEditMode = false) mustBe
            routes.PropertyStartDateController.show().url
        }
      }
    }
  }
}
