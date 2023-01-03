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

import controllers.agent.{AgentControllerBaseSpec, WhatYearToSignUpController}
import forms.agent.AccountingYearForm
import models.Current
import models.common.AccountingYearModel
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockAccountingPeriodService, MockAuditingService, MockSubscriptionDetailsService}
import views.agent.mocks.MockWhatYearToSignUp

import scala.concurrent.Future

class WhatYearToSignUpControllerSpec extends AgentControllerBaseSpec
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
    mockAuditingService,
    mockAuthService,
    mockAccountingPeriodService,
    MockSubscriptionDetailsService,
    whatYearToSignUp
  )

  "show" should {
    "display the What Year To Sign Up view with pre-saved tax year option and return OK (200)" when {
      "there is a pre-saved tax year option in Subscription Details " in {
        mockView()
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))

        val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        status(result) must be(Status.OK)
        verifyFetchSelectedTaxYear(1, "test-reference")
      }
    }

    "display the What Year To Sign Up view with empty form and return OK (200)" when {
      "there is a no pre-saved tax year option in Subscription Details " in {
        mockView()
        mockFetchSelectedTaxYear(None)

        val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        status(result) must be(Status.OK)
        verifyFetchSelectedTaxYear(1, "test-reference")
      }
    }
  }


  "submit" should {
    def callSubmit(isEditMode: Boolean): Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingYearForm.accountingYearForm, Current)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "redirect to tax year check your answers page" when {
      "not in edit mode" in {
        mockView()
        setupMockSubscriptionDetailsSaveFunctions()
        val goodRequest = callSubmit(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.TaxYearCheckYourAnswersController.show().url)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySaveSelectedTaxYear(1, "test-reference")
      }

      "in edit mode" in {
        mockView()
        setupMockSubscriptionDetailsSaveFunctions()
        val goodRequest = callSubmit(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.TaxYearCheckYourAnswersController.show().url)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySaveSelectedTaxYear(1, "test-reference")
      }
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" in {
        mockView()

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)
      }
    }

    "throw an exception" when {
      "there is a failure while saving the tax year" in {
        mockView()
        setupMockSubscriptionDetailsSaveFunctionsFailure()

        val goodRequest = callSubmit(isEditMode = false)

        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }
  }

  "backUrl" when {
    "in edit mode" must {
      s"return ${controllers.agent.routes.TaxYearCheckYourAnswersController.show().url}" in {
        TestWhatYearToSignUpController.backUrl(true) mustBe Some(controllers.agent.routes.TaxYearCheckYourAnswersController.show().url)
      }
    }
    "not in edit mode" must {
      s"return ${controllers.agent.routes.TaskListController.show().url}" in {
        TestWhatYearToSignUpController.backUrl(false) mustBe Some(controllers.agent.routes.TaskListController.show().url)
      }
    }
  }
}
