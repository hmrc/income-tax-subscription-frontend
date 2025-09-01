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

import config.featureswitch.FeatureSwitch.RemoveAccountingMethod
import config.featureswitch.FeatureSwitching
import config.{AppConfig, MockConfig}
import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.ControllerSpec
import controllers.individual.actions.mocks.{MockIdentifierAction, MockSignUpJourneyRefiner}
import models.common.PropertyModel
import models.{Cash, DateModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.individual.mocks.MockPropertyCheckYourAnswers

import scala.concurrent.Future

class PropertyCheckYourAnswersControllerSpec extends ControllerSpec
  with MockIdentifierAction
  with MockSignUpJourneyRefiner
  with MockSubscriptionDetailsService
  with MockPropertyCheckYourAnswers
  with FeatureSwitching {

  override val appConfig: AppConfig = MockConfig

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(RemoveAccountingMethod)
  }


  "Show" should {
    "return ok and display the page" when {

      "return ok and display the page" when {
        "remove accounting method feature switch is enabled" which {
          "has full property data" in {
            enable(RemoveAccountingMethod)
            val propertyModel = PropertyModel(startDateBeforeLimit = Some(true))
            mockFetchProperty(Some(propertyModel))
            mockPropertyCheckYourAnswersView(
              viewModel = propertyModel,
              postAction = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit(),
              backUrl = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateBeforeLimitController.show().url,
              isGlobalEdit = false
            )
            val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = false)(request)
            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }
          "has partial property data" in {
            enable(RemoveAccountingMethod)
            val propertyModel = PropertyModel(startDateBeforeLimit = Some(false))
            mockFetchProperty(Some(propertyModel))
            mockPropertyCheckYourAnswersView(
              viewModel = propertyModel,
              postAction = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit(),
              backUrl = controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
              isGlobalEdit = false
            )
            val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = true, isGlobalEdit = false)(request)
            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }

          "is in Global Edit mode" in {
            enable(RemoveAccountingMethod)
            val propertyModel = PropertyModel(startDateBeforeLimit = Some(false), startDate = Some(DateModel("10", "11", "2021")), confirmed = true)
            mockFetchProperty(Some(propertyModel))
            mockPropertyCheckYourAnswersView(
              viewModel = propertyModel,
              postAction = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit(isGlobalEdit = true),
              backUrl = controllers.individual.routes.GlobalCheckYourAnswersController.show.url,
              isGlobalEdit = true
            )
            val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = true)(request)
            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }
        }
        "remove accounting method feature switch is disabled" which {
          "has full property data" in {
            val propertyModel = PropertyModel(startDateBeforeLimit = Some(true), accountingMethod = Some(Cash))
            mockFetchProperty(Some(propertyModel))
            mockPropertyCheckYourAnswersView(
              viewModel = propertyModel,
              postAction = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit(),
              backUrl = controllers.individual.tasklist.ukproperty.routes.PropertyAccountingMethodController.show().url,
              isGlobalEdit = false
            )
            val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = false)(request)
            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }
          "has partial property data" in {
            val propertyModel = PropertyModel(startDateBeforeLimit = Some(false), accountingMethod = Some(Cash))
            mockFetchProperty(Some(propertyModel))
            mockPropertyCheckYourAnswersView(
              viewModel = propertyModel,
              postAction = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit(),
              backUrl = controllers.individual.tasklist.ukproperty.routes.PropertyAccountingMethodController.show().url,
              isGlobalEdit = false
            )
            val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = false)(request)
            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }

          "is in Global Edit mode" in {
            val propertyModel = PropertyModel(startDateBeforeLimit = Some(false), startDate = Some(DateModel("10", "11", "2021")), accountingMethod = Some(Cash), confirmed = true)
            mockFetchProperty(Some(propertyModel))
            mockPropertyCheckYourAnswersView(
              viewModel = propertyModel,
              postAction = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit(isGlobalEdit = true),
              backUrl = controllers.individual.routes.GlobalCheckYourAnswersController.show.url,
              isGlobalEdit = true
            )
            val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = true)(request)
            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }
        }
      }
    }

    "throw an internal server exception" when {
      "property data is not available" in {
        mockFetchProperty(None)
        val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = false)(request)
        intercept[InternalServerException](await(result))
          .message mustBe "[PropertyCheckYourAnswersController] - Could not retrieve property details"
      }
    }
  }

  "Submit" when {
    "Remove Accounting Method feature switch is enabled" when {
      "property is complete" should {
        "save property and redirect" when {
          "in Global Edit mode" in {
            enable(RemoveAccountingMethod)
            val propertyModel = PropertyModel(startDateBeforeLimit = Some(true))
            mockFetchProperty(Some(propertyModel))
            mockSaveProperty(propertyModel.copy(confirmed = true))(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = true)(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url)
          }
          "not in Edit mode" in {
            enable(RemoveAccountingMethod)
            val propertyModel = PropertyModel(startDateBeforeLimit = Some(false), startDate = Some(DateModel("10", "11", "2021")))
            mockFetchProperty(Some(propertyModel))
            mockSaveProperty(propertyModel.copy(confirmed = true))(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
          }
        }
        "throw an Internal Server Exception when property cannot be saved" in {
          enable(RemoveAccountingMethod)
          val propertyModel = PropertyModel(startDateBeforeLimit = Some(true))
          mockFetchProperty(Some(propertyModel))
          mockSaveProperty(propertyModel.copy(confirmed = true))(
            Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
          )
          val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)
          intercept[InternalServerException](await(result))
            .message mustBe "[PropertyCheckYourAnswersController][submit] - Could not confirm property"
        }
      }
      "property is not complete" should {
        "redirect to Your Income Sources page" in {
          enable(RemoveAccountingMethod)
          val propertyModel = PropertyModel(startDateBeforeLimit = Some(false))
          mockFetchProperty(Some(propertyModel))

          val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        }
      }
    }

    "Remove Accounting Method feature switch is disabled" when {
      "property is complete" should {
        "save property and redirect" when {
          "in Global Edit mode" in {
            val propertyModel = PropertyModel(accountingMethod = Some(Cash), startDateBeforeLimit = Some(true))
            mockFetchProperty(Some(propertyModel))
            mockSaveProperty(propertyModel.copy(confirmed = true))(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = true)(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url)
          }
          "not in Edit mode" in {
            val propertyModel = PropertyModel(accountingMethod = Some(Cash),
              startDateBeforeLimit = Some(false), startDate = Some(DateModel("10", "11", "2021")))
            mockFetchProperty(Some(propertyModel))
            mockSaveProperty(propertyModel.copy(confirmed = true))(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
          }
        }
        "throw an Internal Server Exception when property cannot be saved" in {
          val propertyModel = PropertyModel(accountingMethod = Some(Cash), startDateBeforeLimit = Some(true))
          mockFetchProperty(Some(propertyModel))
          mockSaveProperty(propertyModel.copy(confirmed = true))(
            Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
          )
          val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)
          intercept[InternalServerException](await(result))
            .message mustBe "[PropertyCheckYourAnswersController][submit] - Could not confirm property"
        }
      }
      "property is not complete" should {
        "redirect to Your Income Sources page" in {
          val propertyModel = PropertyModel(accountingMethod = Some(Cash), startDateBeforeLimit = Some(false))
          mockFetchProperty(Some(propertyModel))

          val result: Future[Result] = TestPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        }
      }
    }
  }


  object TestPropertyCheckYourAnswersController extends PropertyCheckYourAnswersController(
    fakeIdentifierAction,
    fakeSignUpJourneyRefiner,
    mockSubscriptionDetailsService,
    mockView
  )(MockConfig)

}
