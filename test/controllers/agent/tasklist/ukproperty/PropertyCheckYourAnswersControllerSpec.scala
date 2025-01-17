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

import config.featureswitch.FeatureSwitching
import config.{AppConfig, MockConfig}
import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsSuccessResponse, UnexpectedStatusFailure}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import models.common.PropertyModel
import models.{Cash, DateModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.agent.tasklist.ukproperty.mocks.MockPropertyCheckYourAnswers

import scala.concurrent.Future

class PropertyCheckYourAnswersControllerSpec extends ControllerSpec
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockSubscriptionDetailsService
  with MockPropertyCheckYourAnswers
  with FeatureSwitching {

  override val appConfig: AppConfig = MockConfig

  "show" when {
    "no property data was returned" should {
      "throw an exception" in {
        mockFetchProperty(None)

        val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = false)(request)

        intercept[InternalServerException](await(result))
          .message mustBe "[PropertyCheckYourAnswersController] - Could not retrieve property details"
      }
    }
    "property data was returned" should {
      "return OK with the page contents" when {
        "not in edit mode" in {
          mockFetchProperty(Some(fullProperty))
          mockPropertyCheckYourAnswers(
            viewModel = fullProperty,
            postAction = routes.PropertyCheckYourAnswersController.submit(),
            isGlobalEdit = false,
            backUrl = routes.PropertyIncomeSourcesController.show().url,
            clientDetails = clientDetails
          )

          val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = false)(request)

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "in edit mode" in {
          mockFetchProperty(Some(fullProperty))
          mockPropertyCheckYourAnswers(
            viewModel = fullProperty,
            postAction = routes.PropertyCheckYourAnswersController.submit(),
            isGlobalEdit = false,
            backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
            clientDetails = clientDetails
          )

          val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = true, isGlobalEdit = false)(request)

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "in global edit mode" when {
          "the property business is confirmed" in {
            mockFetchProperty(Some(fullProperty.copy(confirmed = true)))
            mockPropertyCheckYourAnswers(
              viewModel = fullProperty.copy(confirmed = true),
              postAction = routes.PropertyCheckYourAnswersController.submit(true),
              isGlobalEdit = true,
              backUrl = controllers.agent.routes.GlobalCheckYourAnswersController.show.url,
              clientDetails = clientDetails
            )

            val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = true)(request)

            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }
          "the property business is not confirmed" in {
            mockFetchProperty(Some(fullProperty))
            mockPropertyCheckYourAnswers(
              viewModel = fullProperty,
              postAction = routes.PropertyCheckYourAnswersController.submit(true),
              isGlobalEdit = true,
              backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
              clientDetails = clientDetails
            )

            val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = true)(request)

            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }
        }
      }
    }

    "submit" when {
      "no property data was returned" should {
        "throw an exception" in {
          mockFetchProperty(None)

          val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

          intercept[InternalServerException](await(result))
            .message mustBe "[PropertyCheckYourAnswersController] - Could not retrieve property details"
        }
      }
      "property data was returned" which {
        "is missing the accounting method" should {
          "redirect to the your income sources page" in {
            mockFetchProperty(Some(fullProperty.copy(accountingMethod = None)))

            val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
          }
        }
        "is missing the start date" should {
          "redirect to the your income sources page" in {
            mockFetchProperty(Some(fullProperty.copy(startDate = None)))

            val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
          }
        }
      }
      "property data was returned and is complete" should {
        "save the property as confirmed and redirect to the global check your answers" when {
          "in global edit mode" in {
            mockFetchProperty(Some(fullProperty))
            mockSaveProperty(fullProperty.copy(confirmed = true))(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = true)(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.agent.routes.GlobalCheckYourAnswersController.show.url)
          }
        }
        "save the property as confirmed and redirect to the your income sources" when {
          "not in edit mode" in {
            mockFetchProperty(Some(fullProperty))
            mockSaveProperty(fullProperty.copy(confirmed = true))(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
          }
        }
        "throw an exception" when {
          "a failure occured when saving the confirmed property" in {
            mockFetchProperty(Some(fullProperty))
            mockSaveProperty(fullProperty.copy(confirmed = true))(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

            val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

            intercept[InternalServerException](await(result))
              .message mustBe "[PropertyCheckYourAnswersController][submit] - Could not confirm property"
          }
        }
      }
    }

    lazy val fullProperty: PropertyModel = PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("1", "1", "1980")))

    object TestPropertyCheckYourAnswersController extends PropertyCheckYourAnswersController(
      fakeIdentifierAction,
      fakeConfirmedClientJourneyRefiner,
      mockSubscriptionDetailsService,
      mockView,
      MockConfig
    )
  }
}