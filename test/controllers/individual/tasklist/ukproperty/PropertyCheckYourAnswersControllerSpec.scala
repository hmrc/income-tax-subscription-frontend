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
import models.common.PropertyModel
import models.{Cash, DateModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.{MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.individual.mocks.MockPropertyCheckYourAnswers

import scala.concurrent.Future

class PropertyCheckYourAnswersControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockReferenceRetrieval
  with MockSubscriptionDetailsService
  with MockPropertyCheckYourAnswers {

  override val controllerName: String = "PropertyCheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "show" should {
    "return an OK status with the property CYA page" when {
      "with partial property data" in withController { controller =>
        val partialPropertyModel: PropertyModel = PropertyModel(accountingMethod = Some(Cash))
        mockPropertyCheckYourAnswersView(
          viewModel = partialPropertyModel,
          postAction = routes.PropertyCheckYourAnswersController.submit(),
          backUrl = routes.PropertyAccountingMethodController.show().url,
          isGlobalEdit = false
        )
        mockFetchProperty(Some(partialPropertyModel))

        val result: Future[Result] = await(controller.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }

      "with complete property data" in withController { controller =>
        mockPropertyCheckYourAnswersView(
          viewModel = testFullProperty,
          postAction = routes.PropertyCheckYourAnswersController.submit(),
          backUrl = routes.PropertyAccountingMethodController.show().url,
          isGlobalEdit = false
        )
        mockFetchProperty(Some(testFullProperty))

        val result: Future[Result] = await(controller.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }

      "in edit mode" in withController { controller =>
        mockPropertyCheckYourAnswersView(
          viewModel = testFullProperty,
          postAction = routes.PropertyCheckYourAnswersController.submit(),
          backUrl = controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          isGlobalEdit = false
        )
        mockFetchProperty(Some(testFullProperty))

        val result: Future[Result] = await(controller.show(isEditMode = true, isGlobalEdit = false)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }

      "in global edit mode with confirmed property" in withController { controller =>
        mockPropertyCheckYourAnswersView(
          viewModel = testFullProperty.copy(confirmed = true),
          postAction = routes.PropertyCheckYourAnswersController.submit(isGlobalEdit = true),
          backUrl = controllers.individual.routes.GlobalCheckYourAnswersController.show.url,
          isGlobalEdit = true
        )
        mockFetchProperty(Some(testFullProperty.copy(confirmed = true)))

        val result: Future[Result] = await(controller.show(isEditMode = false, isGlobalEdit = true)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }

      "in global edit mode with property not confirmed" in withController { controller =>
        mockPropertyCheckYourAnswersView(
          viewModel = testFullProperty,
          postAction = routes.PropertyCheckYourAnswersController.submit(isGlobalEdit = true),
          backUrl = controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          isGlobalEdit = true
        )
        mockFetchProperty(Some(testFullProperty))

        val result: Future[Result] = await(controller.show(isEditMode = false, isGlobalEdit = true)(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }
    }

    "throw an exception if cannot retrieve property details" in withController { controller =>
      mockFetchProperty(None)

      val result: Future[Result] = await(controller.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

      intercept[InternalServerException](await(result)).message mustBe "[PropertyCheckYourAnswersController] - Could not retrieve property details"
    }
  }

  "submit" when {
    "not in global edit mode" should {
      "redirect to the your income sources page and confirm the uk property details" when {
        "the user submits a start date and accounting method" in withController { controller =>
          mockFetchProperty(Some(testFullProperty))
          mockSaveProperty(testFullProperty.copy(confirmed = true))(
            Right(PostSubscriptionDetailsSuccessResponse)
          )

          val result: Future[Result] = await(controller.submit(isGlobalEdit = false)(subscriptionRequest))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        }
      }

      "redirect to the your income sources page but don't confirm the uk property details" when {
        "the user submits partial data" in withController { controller =>
          mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash))))

          val result: Future[Result] = await(controller.submit(isGlobalEdit = false)(subscriptionRequest))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        }
      }

      "throw an exception" when {
        "cannot confirm property details" in withController { controller =>
          mockFetchProperty(Some(testFullProperty))
          mockSaveProperty(testFullProperty.copy(confirmed = true))(
            Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
          )

          val result: Future[Result] = await(controller.submit(isGlobalEdit = false)(subscriptionRequest))

          intercept[InternalServerException](await(result)).message mustBe "[PropertyCheckYourAnswersController][submit] - Could not confirm property"
        }

        "cannot retrieve property details" in withController { controller =>
          mockFetchProperty(None)

          val result: Future[Result] = await(controller.submit(isGlobalEdit = false)(subscriptionRequest))

          intercept[InternalServerException](await(result)).message mustBe "[PropertyCheckYourAnswersController] - Could not retrieve property details"
        }
      }
    }

    "in global edit mode" should {
      "redirect to the global CYA page and confirm the uk property details" when {
        "the user submits complete valid data" in withController { controller =>
          mockFetchProperty(Some(testFullProperty))
          mockSaveProperty(testFullProperty.copy(confirmed = true))(
            Right(PostSubscriptionDetailsSuccessResponse)
          )

          val result: Future[Result] = await(controller.submit(isGlobalEdit = true)(subscriptionRequest))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url)
        }
      }

      "throw an exception" when {
        "cannot confirm property details" in withController { controller =>
          mockFetchProperty(Some(testFullProperty))
          mockSaveProperty(testFullProperty.copy(confirmed = true))(
            Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
          )

          val result: Future[Result] = await(controller.submit(isGlobalEdit = true)(subscriptionRequest))

          intercept[InternalServerException](await(result)).message mustBe "[PropertyCheckYourAnswersController][submit] - Could not confirm property"
        }

        "cannot retrieve property details" in withController { controller =>
          mockFetchProperty(None)

          val result: Future[Result] = await(controller.submit(isGlobalEdit = true)(subscriptionRequest))

          intercept[InternalServerException](await(result)).message mustBe "[PropertyCheckYourAnswersController] - Could not retrieve property details"
        }
      }
    }
  }

  private val testFullProperty: PropertyModel = PropertyModel(
    startDateBeforeLimit = Some(false),
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("10", "11", "2021"))
  )

  private def withController(testCode: PropertyCheckYourAnswersController => Any): Unit = {

    val controller = new PropertyCheckYourAnswersController(
      mockView,
      mockSubscriptionDetailsService,
      mockReferenceRetrieval
    )(
      mockAuditingService,
      mockAuthService,
      appConfig,
    )

    testCode(controller)
  }
}
