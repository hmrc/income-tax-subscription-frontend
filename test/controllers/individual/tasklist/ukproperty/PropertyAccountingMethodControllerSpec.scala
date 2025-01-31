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

import config.featureswitch.FeatureSwitch.StartDateBeforeLimit
import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.individual.ControllerBaseSpec
import forms.individual.business.AccountingMethodPropertyForm
import models.common.PropertyModel
import models.{Accruals, Cash}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.individual.mocks.MockPropertyAccountingMethod

import scala.concurrent.Future

class PropertyAccountingMethodControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockReferenceRetrieval
  with MockAuditingService
  with MockPropertyAccountingMethod {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(StartDateBeforeLimit)
  }

  override val controllerName: String = "PropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestPropertyAccountingMethodController extends PropertyAccountingMethodController(mockView,
    mockSubscriptionDetailsService,
    mockReferenceRetrieval
  )(
    mockAuditingService,
    mockAuthService,
    appConfig
  )

  "show" should {
    "display the property accounting method view and return OK (200)" when {
      "accounting method is returned" in {
        mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash))))
        mockPropertyAccountingMethodView(
          postAction = routes.PropertyAccountingMethodController.submit(),
          backUrl = routes.PropertyStartDateController.show().url
        )
        lazy val result = await(TestPropertyAccountingMethodController.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }

      "no property business is returned" in {
        mockFetchProperty(None)
        mockPropertyAccountingMethodView(
          postAction = routes.PropertyAccountingMethodController.submit(),
          backUrl = routes.PropertyStartDateController.show().url
        )

        val result = TestPropertyAccountingMethodController.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "no accounting method is returned" in {
        mockFetchProperty(Some(PropertyModel(accountingMethod = None)))
        mockPropertyAccountingMethodView(
          postAction = routes.PropertyAccountingMethodController.submit(),
          backUrl = routes.PropertyStartDateController.show().url
        )
        lazy val result = await(TestPropertyAccountingMethodController.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }

      "in edit mode" in {
        mockFetchProperty(None)
        mockPropertyAccountingMethodView(
          postAction = routes.PropertyAccountingMethodController.submit(editMode = true),
          backUrl = routes.PropertyCheckYourAnswersController.show(editMode = true).url
        )
        lazy val result = await(TestPropertyAccountingMethodController.show(isEditMode = true, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }

      "in global edit mode" in {
        mockFetchProperty(None)
        mockPropertyAccountingMethodView(
          postAction = routes.PropertyAccountingMethodController.submit(editMode = true, isGlobalEdit = true),
          backUrl = routes.PropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url
        )
        lazy val result = await(TestPropertyAccountingMethodController.show(isEditMode = true, isGlobalEdit = true)(subscriptionRequest))

        status(result) must be(Status.OK)
      }
    }
  }

  "submit" should {

    def callSubmit(isEditMode: Boolean): Future[Result] = TestPropertyAccountingMethodController.submit(isEditMode = isEditMode, isGlobalEdit = false)(
      subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Cash)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean, isGlobalEdit: Boolean = false): Future[Result] =
      TestPropertyAccountingMethodController.submit(isEditMode, isGlobalEdit)(subscriptionRequest)

    "redirect to uk property check your answers page" when {
      "not in edit mode" in {
        mockSavePropertyAccountingMethod(Cash)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = false)

        await(goodRequest)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.PropertyCheckYourAnswersController.show().url)
      }

      "in edit mode" in {
        mockSavePropertyAccountingMethod(Accruals)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = TestPropertyAccountingMethodController.submit(isEditMode = true, isGlobalEdit = false)(
          subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Accruals)
        )
        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.PropertyCheckYourAnswersController.show(editMode = true).url)
      }

      "in global edit mode" in {
        mockSavePropertyAccountingMethod(Accruals)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = TestPropertyAccountingMethodController.submit(isEditMode = true, isGlobalEdit = true)(
          subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Accruals)
        )
        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.PropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url)
      }
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" when {
        "not in edit mode" in {
          mockFetchProperty(None)
          mockPropertyAccountingMethodView(
            postAction = routes.PropertyAccountingMethodController.submit(),
            backUrl = routes.PropertyStartDateController.show().url
          )
          val badRequest = callSubmitWithErrorForm(isEditMode = false)

          status(badRequest) must be(Status.BAD_REQUEST)
        }

        "in global edit mode" in {
          mockFetchProperty(None)
          mockPropertyAccountingMethodView(
            postAction = routes.PropertyAccountingMethodController.submit(editMode = true, isGlobalEdit = true),
            backUrl = routes.PropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url
          )
          val badRequest = callSubmitWithErrorForm(isEditMode = true, isGlobalEdit = true)

          status(badRequest) must be(Status.BAD_REQUEST)
        }
      }
    }

    "throw an exception" when {
      "cannot save the accounting method" in {
        mockSavePropertyAccountingMethod(Cash)(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val result: Future[Result] = callSubmit(isEditMode = false)

        intercept[InternalServerException](await(result)).message mustBe "[PropertyAccountingMethodController][submit] - Could not save accounting method"
      }
    }
  }

  "backUrl" should {
    "return a link to the check your answers page" when {
      "in edit mode" in {
        TestPropertyAccountingMethodController.backUrl(
          isEditMode = true,
          isGlobalEdit = false,
          maybeStartDateBeforeLimit = None
        ) mustBe routes.PropertyCheckYourAnswersController.show(editMode = true).url
      }
      "in global edit mode" in {
        TestPropertyAccountingMethodController.backUrl(
          isEditMode = false,
          isGlobalEdit = true,
          maybeStartDateBeforeLimit = None
        ) mustBe routes.PropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url
      }
    }
    "return a link to the start date page" when {
      "the start date before limit feature switch is enabled" when {
        "start date before limit is false" in {
          enable(StartDateBeforeLimit)

          TestPropertyAccountingMethodController.backUrl(
            isEditMode = false,
            isGlobalEdit = false,
            maybeStartDateBeforeLimit = Some(false)
          ) mustBe routes.PropertyStartDateController.show(editMode = false).url
        }
      }
      "the start date before limit feature switch is disabled" in {
        TestPropertyAccountingMethodController.backUrl(
          isEditMode = false,
          isGlobalEdit = false,
          maybeStartDateBeforeLimit = Some(false)
        ) mustBe routes.PropertyStartDateController.show(editMode = false).url
      }
    }
    "return a link to the start date before limit page" when {
      "the start date before limit feature switch is enabled" when {
        "the start date before limit value is true" in {
          enable(StartDateBeforeLimit)

          TestPropertyAccountingMethodController.backUrl(
            isEditMode = false,
            isGlobalEdit = false,
            maybeStartDateBeforeLimit = Some(true)
          ) mustBe routes.PropertyStartDateBeforeLimitController.show().url
        }
        "the start date before limit has no value" in {
          enable(StartDateBeforeLimit)

          TestPropertyAccountingMethodController.backUrl(
            isEditMode = false,
            isGlobalEdit = false,
            maybeStartDateBeforeLimit = None
          ) mustBe routes.PropertyStartDateBeforeLimitController.show().url
        }
      }
    }
  }

}
