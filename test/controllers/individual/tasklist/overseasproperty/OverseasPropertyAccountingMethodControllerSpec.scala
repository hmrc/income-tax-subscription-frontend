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
import forms.individual.business.AccountingMethodOverseasPropertyForm
import models.common.OverseasPropertyModel
import models.{Accruals, Cash}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.individual.mocks.MockOverseasPropertyAccountingMethod

import scala.concurrent.Future

class OverseasPropertyAccountingMethodControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockReferenceRetrieval
  with MockAuditingService
  with MockOverseasPropertyAccountingMethod {

  override val controllerName: String = "OverseasPropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestOverseasPropertyAccountingMethodController extends OverseasPropertyAccountingMethodController(
    mockView,
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
        mockFetchOverseasProperty(Some(OverseasPropertyModel(accountingMethod = Some(Cash))))
        mockOverseasPropertyAccountingMethod(
          postAction = routes.OverseasPropertyAccountingMethodController.submit(),
          backUrl = routes.ForeignPropertyStartDateBeforeLimitController.show().url
        )
        lazy val result = await(TestOverseasPropertyAccountingMethodController.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }

      "no property business is returned" in {
        mockFetchOverseasProperty(None)
        mockOverseasPropertyAccountingMethod(
          postAction = routes.OverseasPropertyAccountingMethodController.submit(),
          backUrl = routes.ForeignPropertyStartDateBeforeLimitController.show().url
        )

        val result = TestOverseasPropertyAccountingMethodController.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "no accounting method is returned" in {
        mockFetchOverseasProperty(Some(OverseasPropertyModel(accountingMethod = None)))
        mockOverseasPropertyAccountingMethod(
          postAction = routes.OverseasPropertyAccountingMethodController.submit(),
          backUrl = routes.ForeignPropertyStartDateBeforeLimitController.show().url
        )
        lazy val result = await(TestOverseasPropertyAccountingMethodController.show(isEditMode = false, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }

      "in edit mode" in {
        mockFetchOverseasProperty(None)
        mockOverseasPropertyAccountingMethod(
          postAction = routes.OverseasPropertyAccountingMethodController.submit(editMode = true),
          backUrl = routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
        )
        lazy val result = await(TestOverseasPropertyAccountingMethodController.show(isEditMode = true, isGlobalEdit = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }

      "in global edit mode" in {
        mockFetchOverseasProperty(None)
        mockOverseasPropertyAccountingMethod(
          postAction = routes.OverseasPropertyAccountingMethodController.submit(editMode = true, isGlobalEdit = true),
          backUrl = routes.OverseasPropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url
        )
        lazy val result = await(TestOverseasPropertyAccountingMethodController.show(isEditMode = true, isGlobalEdit = true)(subscriptionRequest))

        status(result) must be(Status.OK)
      }
    }
  }

  "submit" should {

    def callSubmit(isEditMode: Boolean): Future[Result] = TestOverseasPropertyAccountingMethodController.submit(isEditMode = isEditMode, isGlobalEdit = false)(
      subscriptionRequest.post(AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm, Cash)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean, isGlobalEdit: Boolean = false): Future[Result] =
      TestOverseasPropertyAccountingMethodController.submit(isEditMode, isGlobalEdit)(subscriptionRequest)

    "redirect to foreign property check your answers page" when {
      "not in edit mode" in {
        mockSaveOverseasAccountingMethodProperty(Cash)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = false)

        await(goodRequest)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show().url)
      }

      "in edit mode" in {
        mockSaveOverseasAccountingMethodProperty(Accruals)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = TestOverseasPropertyAccountingMethodController.submit(isEditMode = true, isGlobalEdit = false)(
          subscriptionRequest.post(AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm, Accruals)
        )
        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url)
      }

      "in global edit mode" in {
        mockSaveOverseasAccountingMethodProperty(Accruals)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = TestOverseasPropertyAccountingMethodController.submit(isEditMode = true, isGlobalEdit = true)(
          subscriptionRequest.post(AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm, Accruals)
        )
        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url)
      }
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" when {
        "not in edit mode" in {
          mockFetchOverseasProperty(None)
          mockOverseasPropertyAccountingMethod(
            postAction = routes.OverseasPropertyAccountingMethodController.submit(),
            backUrl = routes.ForeignPropertyStartDateBeforeLimitController.show().url
          )
          val badRequest = callSubmitWithErrorForm(isEditMode = false)

          status(badRequest) must be(Status.BAD_REQUEST)
        }

        "in global edit mode" in {
          mockFetchOverseasProperty(None)
          mockOverseasPropertyAccountingMethod(
            postAction = routes.OverseasPropertyAccountingMethodController.submit(editMode = true, isGlobalEdit = true),
            backUrl = routes.OverseasPropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url
          )
          val badRequest = callSubmitWithErrorForm(isEditMode = true, isGlobalEdit = true)

          status(badRequest) must be(Status.BAD_REQUEST)
        }
      }
    }

    "throw an exception" when {
      "cannot save the accounting method" in {
        mockFetchOverseasProperty(None)
        mockSaveOverseasAccountingMethodProperty(Cash)(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val result: Future[Result] = callSubmit(isEditMode = false)

        intercept[InternalServerException](await(result)).message mustBe "[OverseasPropertyAccountingMethodController][submit] - Could not save accounting method"
      }
    }
  }

  "backUrl" should {
    "return a link to the check your answers page" when {
      "in edit mode" in {
        TestOverseasPropertyAccountingMethodController.backUrl(
          isEditMode = true,
          isGlobalEdit = false,
          maybeStartDateBeforeLimit = None
        ) mustBe routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
      }
      "in global edit mode" in {
        TestOverseasPropertyAccountingMethodController.backUrl(
          isEditMode = false,
          isGlobalEdit = true,
          maybeStartDateBeforeLimit = None
        ) mustBe routes.OverseasPropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url
      }
    }
    "return a link to the start date page" when {
      "start date before limit is false" in {
        TestOverseasPropertyAccountingMethodController.backUrl(
          isEditMode = false,
          isGlobalEdit = false,
          maybeStartDateBeforeLimit = Some(false)
        ) mustBe routes.ForeignPropertyStartDateController.show().url
      }
    }
    "return a link to the start date before limit page" when {
      "the start date before limit value is true" in {
        TestOverseasPropertyAccountingMethodController.backUrl(
          isEditMode = false,
          isGlobalEdit = false,
          maybeStartDateBeforeLimit = Some(true)
        ) mustBe routes.ForeignPropertyStartDateBeforeLimitController.show().url
      }
      "the start date before limit has no value" in {
        TestOverseasPropertyAccountingMethodController.backUrl(
          isEditMode = false,
          isGlobalEdit = false,
          maybeStartDateBeforeLimit = None
        ) mustBe routes.ForeignPropertyStartDateBeforeLimitController.show().url
      }
    }
  }

}