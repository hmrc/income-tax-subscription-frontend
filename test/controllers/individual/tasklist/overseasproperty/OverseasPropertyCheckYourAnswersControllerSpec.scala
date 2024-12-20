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
import models.common.OverseasPropertyModel
import models.{Cash, DateModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.{MockAccountingPeriodService, MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.individual.mocks.MockOverseasPropertyCheckYourAnswers

import scala.concurrent.Future

class OverseasPropertyCheckYourAnswersControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockAccountingPeriodService
  with MockReferenceRetrieval
  with MockSubscriptionDetailsService
  with MockOverseasPropertyCheckYourAnswers {

  override val controllerName: String = "OverseasPropertyCheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "show" should {
    "return an OK status with the property CYA page" when {
      "not in edit mode" in withController { controller =>
        mockOverseasPropertyCheckYourAnswersView(
          viewModel = testFullOverseasProperty,
          postAction = routes.OverseasPropertyCheckYourAnswersController.submit(),
          backUrl = routes.OverseasPropertyAccountingMethodController.show().url,
          isGlobalEdit = false
        )
        mockFetchOverseasProperty(Some(testFullOverseasProperty))

        val result: Future[Result] = await(controller.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }

      "in edit mode" in withController { controller =>
        mockOverseasPropertyCheckYourAnswersView(
          viewModel = testFullOverseasProperty,
          postAction = routes.OverseasPropertyCheckYourAnswersController.submit(),
          backUrl = controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          isGlobalEdit = false
        )
        mockFetchOverseasProperty(Some(testFullOverseasProperty))

        val result: Future[Result] = await(controller.show(true, isGlobalEdit = false)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }

      "in global edit mode and property not confirmed" in withController { controller =>
        mockOverseasPropertyCheckYourAnswersView(
          viewModel = testFullOverseasProperty,
          postAction = routes.OverseasPropertyCheckYourAnswersController.submit(isGlobalEdit = true),
          backUrl = controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          isGlobalEdit = true
        )
        mockFetchOverseasProperty(Some(testFullOverseasProperty))

        val result: Future[Result] = await(controller.show(false, isGlobalEdit = true)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }

      "in global edit mode and property is confirmed" in withController { controller =>
        mockOverseasPropertyCheckYourAnswersView(
          viewModel = testFullOverseasProperty.copy(confirmed = true),
          postAction = routes.OverseasPropertyCheckYourAnswersController.submit(isGlobalEdit = true),
          backUrl = controllers.individual.routes.GlobalCheckYourAnswersController.show.url,
          isGlobalEdit = true
        )
        mockFetchOverseasProperty(Some(testFullOverseasProperty.copy(confirmed = true)))

        val result: Future[Result] = await(controller.show(false, isGlobalEdit = true)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }
    }

    "throw an exception if cannot retrieve overseas property details" in withController { controller =>
      mockFetchOverseasProperty(None)

      val result: Future[Result] = await(controller.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

      intercept[InternalServerException](await(result)).message mustBe "[OverseasPropertyCheckYourAnswersController] - Could not retrieve property details"

    }
  }

  "submit" when {
    "not in global edit mode" should {
      "redirect to the your income sources page" when {
        "the user submits valid full data and overseas property is confirmed and saved" in withController { controller =>

          mockFetchOverseasProperty(Some(testFullOverseasProperty))
          mockSaveOverseasProperty(testFullOverseasProperty.copy(confirmed = true))(
            Right(PostSubscriptionDetailsSuccessResponse)
          )

          val result: Future[Result] = await(controller.submit(isGlobalEdit = false)(subscriptionRequest))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        }

        "the user submits valid partial data and overseas property answers is not saved" in withController { controller =>
          mockFetchOverseasProperty(Some(OverseasPropertyModel(accountingMethod = Some(Cash))))

          val result: Future[Result] = await(controller.submit(isGlobalEdit = false)(subscriptionRequest))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        }
      }
    }

    "in global edit mode" should {
      "redirect to the global CYA page and confirm the overseas property details" when {
        "the user submits a start date and accounting method" in withController { controller =>

          mockFetchOverseasProperty(Some(testFullOverseasProperty))
          mockSaveOverseasProperty(testFullOverseasProperty.copy(confirmed = true))(
            Right(PostSubscriptionDetailsSuccessResponse)
          )

          val result: Future[Result] = await(controller.submit(isGlobalEdit = true)(subscriptionRequest))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url)
        }

        "redirect to the your income sources page and not save overseas property" when {
          "the user submits valid partial data" in withController { controller =>
            mockFetchOverseasProperty(Some(OverseasPropertyModel(accountingMethod = Some(Cash))))

            val result: Future[Result] = await(controller.submit(isGlobalEdit = true)(subscriptionRequest))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
          }
        }
      }
    }

  }

  "submit" should {
    "throw an exception" when {
      "cannot retrieve property details" in withController { controller =>
        mockFetchOverseasProperty(None)

        val result: Future[Result] = controller.submit(isGlobalEdit = false)(subscriptionRequest)

        intercept[InternalServerException](await(result)).message mustBe "[OverseasPropertyCheckYourAnswersController] - Could not retrieve property details"
      }

      "cannot confirm overseas property details" in withController { controller =>
        mockFetchOverseasProperty(Some(testFullOverseasProperty))
        mockSaveOverseasProperty(testFullOverseasProperty.copy(confirmed = true))(
          Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
        )

        val result: Future[Result] = controller.submit(isGlobalEdit = false)(subscriptionRequest)

        intercept[InternalServerException](await(result))
          .message mustBe "[OverseasPropertyCheckYourAnswersController][submit] - Could not confirm property details"
      }
    }
  }


  private val testFullOverseasProperty: OverseasPropertyModel = OverseasPropertyModel(
    accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")))

  private def withController(testCode: OverseasPropertyCheckYourAnswersController => Any): Unit = {
    val controller = new OverseasPropertyCheckYourAnswersController(
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
}
