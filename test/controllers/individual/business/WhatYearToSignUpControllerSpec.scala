/*
 * Copyright 2022 HM Revenue & Customs
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

import agent.audit.mocks.MockAuditingService
import controllers.ControllerBaseSpec
import forms.individual.business.AccountingYearForm
import models.Current
import models.common.AccountingYearModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockAccountingPeriodService, MockSubscriptionDetailsService, MockWhatYearToSignUp}

import scala.concurrent.Future

class WhatYearToSignUpControllerSpec extends ControllerBaseSpec
  with MockWhatYearToSignUp
  with MockSubscriptionDetailsService
  with MockAccountingPeriodService
  with MockAuditingService {

  override val controllerName: String = "WhatYearToSignUpMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestWhatYearToSignUpController.show(isEditMode = false),
    "submit" -> TestWhatYearToSignUpController.submit(isEditMode = false)
  )

  object TestWhatYearToSignUpController extends WhatYearToSignUpController(
    whatYearToSignUp,
    mockAuditingService,
    mockAuthService,
    mockAccountingPeriodService,
    MockSubscriptionDetailsService
  )

  "show" should {
    "display the What Year To Sign Up view with pre-saved tax year option and return OK (200)" when {
      "there is a pre-saved tax year option in Subscription Details " in {
        mockIncomeSource()
        lazy val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))

        status(result) must be(Status.OK)

        verifyFetchSelectedTaxYear(1, "test-reference")

      }
    }

    "display the What Year To Sign Up view with empty form and return OK (200)" when {
      "there is a no pre-saved tax year option in Subscription Details " in {
        mockIncomeSource()
        lazy val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        mockFetchSelectedTaxYear(None)

        status(result) must be(Status.OK)

        verifyFetchSelectedTaxYear(1, "test-reference")

      }
    }
  }


  "submit" when {

    def callSubmit(isEditMode: Boolean): Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingYearForm.accountingYearForm, Current)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "not in edit mode" should {
      "redirect to taxYearCYA page" in {
        mockIncomeSource()
        setupMockSubscriptionDetailsSaveFunctions()
        val goodRequest = callSubmit(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.TaxYearCheckYourAnswersController.show().url)

        await(goodRequest)
        verifySaveSelectedTaxYear(1, "test-reference")
      }
    }

    "it is in edit mode" should {
      "redirect to the task list page" in {
        mockIncomeSource()
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.TaxYearCheckYourAnswersController.show().url)

        await(goodRequest)
        verifySaveSelectedTaxYear(1, "test-reference")
      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {
        mockIncomeSource()
        val badRequest = callSubmitWithErrorForm(isEditMode = false)
        status(badRequest) must be(Status.BAD_REQUEST)
      }
    }

    "The back url" should {
      "return the user to the task list page" in {
        mockIncomeSource()
        TestWhatYearToSignUpController.backUrl(isEditMode = false) mustBe Some(controllers.individual.business.routes.TaskListController.show().url)
      }

      "return the user to the taxYearCYA page when is in editMode" in {
        mockIncomeSource()
        TestWhatYearToSignUpController.backUrl(isEditMode = true) mustBe Some(controllers.individual.business.routes.TaxYearCheckYourAnswersController.show().url)
      }
    }
  }
}
