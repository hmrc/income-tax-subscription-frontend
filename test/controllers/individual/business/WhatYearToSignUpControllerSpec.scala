/*
 * Copyright 2020 HM Revenue & Customs
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

import config.featureswitch.FeatureSwitch.ReleaseFour
import controllers.ControllerBaseSpec
import config.featureswitch._
import forms.individual.business.AccountingYearForm
import models.Current
import models.common.{AccountingYearModel, IncomeSourceModel}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockAccountingPeriodService, MockSubscriptionDetailsService}
import utilities.SubscriptionDataKeys.SelectedTaxYear

import scala.concurrent.Future

class WhatYearToSignUpControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAccountingPeriodService
  with FeatureSwitching {

  override val controllerName: String = "WhatYearToSignUpMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestWhatYearToSignUpController.show(isEditMode = false),
    "submit" -> TestWhatYearToSignUpController.submit(isEditMode = false)
  )

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    super.beforeEach()
  }

  object TestWhatYearToSignUpController extends WhatYearToSignUpController(
    mockAuthService,
    mockAccountingPeriodService,
    MockSubscriptionDetailsService
  )

  "show" should {
    "display the What Year To Sign Up view with pre-saved tax year option and return OK (200)" when {
      "there is a pre-saved tax year option in Subscription Details " in {

        lazy val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        mockFetchSelectedTaxYearFromSubscriptionDetails(Some(AccountingYearModel(Current)))

        status(result) must be(Status.OK)

        verifySubscriptionDetailsFetch(SelectedTaxYear, 1)

      }
    }

    "display the What Year To Sign Up view with empty form and return OK (200)" when {
      "there is a no pre-saved tax year option in Subscription Details " in {

        lazy val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        mockFetchSelectedTaxYearFromSubscriptionDetails(None)

        status(result) must be(Status.OK)

        verifySubscriptionDetailsFetch(SelectedTaxYear, 1)

      }
    }
  }


  "submit" should {

    def callShow(isEditMode: Boolean): Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingYearForm.accountingYearForm, AccountingYearModel(Current))
    )

    def callShowWithErrorForm(isEditMode: Boolean): Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode and release four is disabled" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockSubscriptionDetailsSaveFunctions()
        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsSave(SelectedTaxYear, 1)
      }

      "redirect to business accounting period page" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.BusinessAccountingMethodController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(SelectedTaxYear, 1)
      }

    }

    "When it is not in edit mode and Release four is enabled" should {
      "return a redirect to self-employments initialise controller when self-employment is selected" in {
        enable(ReleaseFour)
        mockFetchIncomeSourceFromSubscriptionDetails(Some(IncomeSourceModel(true, false, false)))
        setupMockSubscriptionDetailsSaveFunctions()
        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some("/report-quarterly/income-and-expenses/sign-up/self-employments/details")
        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsSave(SelectedTaxYear, 1)
      }

      "redirect to Property start date page when only UK property is selected" in {
        enable(ReleaseFour)
        mockFetchIncomeSourceFromSubscriptionDetails(Some(IncomeSourceModel(false, true, false)))
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.PropertyStartDateController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(SelectedTaxYear, 1)
      }

      "redirect to Property start date page when only Overseas property is selected" in {
        enable(ReleaseFour)
        mockFetchIncomeSourceFromSubscriptionDetails(Some(IncomeSourceModel(false, false, true)))
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.OverseasPropertyStartDateController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(SelectedTaxYear, 1)
      }
    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsSave(SelectedTaxYear, 1)
      }

      "redirect to checkYourAnswer page" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(SelectedTaxYear, 1)

      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {

        val badRequest = callShowWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

      }
    }

    "The back url is not in edit mode" when {
      "the user click back url" should {
        "redirect to Income source page when release four is enabled" in {
          enable(ReleaseFour)
          TestWhatYearToSignUpController.backUrl(isEditMode = false) mustBe
            controllers.individual.incomesource.routes.IncomeSourceController.show().url
        }
        "redirect to Business name page when release four is disabled" in {
          TestWhatYearToSignUpController.backUrl(isEditMode = false) mustBe
            controllers.individual.business.routes.BusinessNameController.show().url
        }
      }
    }


    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to check your answer page" in {
          TestWhatYearToSignUpController.backUrl(isEditMode = true) mustBe
            controllers.individual.subscription.routes.CheckYourAnswersController.show().url
        }
      }
    }
  }
}
