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

package controllers.agent.business

import agent.audit.mocks.MockAuditingService
//import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import controllers.agent.{AgentControllerBaseSpec, WhatYearToSignUpController}
import forms.agent.AccountingYearForm
import models.Current
import models.common.{AccountingYearModel, IncomeSourceModel}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockAccountingPeriodService, MockSubscriptionDetailsService}
import utilities.SubscriptionDataKeys.SelectedTaxYear
import views.agent.mocks.MockWhatYearToSignUp

import scala.concurrent.Future

class WhatYearToSignUpControllerSpec extends AgentControllerBaseSpec
  with MockWhatYearToSignUp
  with MockSubscriptionDetailsService
  with MockAccountingPeriodService
  with MockAuditingService
   {

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

//  override def beforeEach(): Unit = {
//    disable(SaveAndRetrieve)
//    super.beforeEach()
//  }

  "show" should {
    "display the What Year To Sign Up view with pre-saved tax year option and return OK (200)" when {
      "there is a pre-saved tax year option in Subscription Details " in {

        mockIncomeSource()
        lazy val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        mockFetchSelectedTaxYearFromSubscriptionDetails(Some(AccountingYearModel(Current)))

        status(result) must be(Status.OK)

        verifySubscriptionDetailsFetch(SelectedTaxYear, Some(1))
      }
    }

    "display the What Year To Sign Up view with empty form and return OK (200)" when {
      "there is a no pre-saved tax year option in Subscription Details " in {
        mockIncomeSource()
        lazy val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        mockFetchSelectedTaxYearFromSubscriptionDetails(None)

        status(result) must be(Status.OK)

        verifySubscriptionDetailsFetch(SelectedTaxYear, Some(1))
      }
    }
  }


  "submit" when {

    def callShow(isEditMode: Boolean): Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingYearForm.accountingYearForm, Current)
    )

    def callShowWithErrorForm(isEditMode: Boolean): Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "save and retrieve is enabled" should {
      "not in edit mode" should {
        "redirect to tax year check your answers page" in {
//          enable(SaveAndRetrieve)

          mockIncomeSource()
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchIncomeSourceFromSubscriptionDetails(Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)))
          val goodRequest = callShow(isEditMode = false)

          redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.TaxYearCheckYourAnswersController.show().url)

          status(goodRequest) must be(Status.SEE_OTHER)

          await(goodRequest)
          verifySubscriptionDetailsSave(SelectedTaxYear, 1)
        }
      }

      "in edit mode" should {
        "redirect to tax year check your answers page" in {
//          enable(SaveAndRetrieve)

          mockIncomeSource()
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchIncomeSourceFromSubscriptionDetails(Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)))
          val goodRequest = callShow(isEditMode = true)

          redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.TaxYearCheckYourAnswersController.show().url)

          status(goodRequest) must be(Status.SEE_OTHER)

          await(goodRequest)
          verifySubscriptionDetailsSave(SelectedTaxYear, 1)
        }
      }
    }

//    "save and retrieve is disabled" when {
//      "not in edit mode" should {
//        "redirect to Income Sources page" in {
//          disable(SaveAndRetrieve)
//
//          mockIncomeSource()
//          setupMockSubscriptionDetailsSaveFunctions()
//          mockFetchIncomeSourceFromSubscriptionDetails(Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)))
//          val goodRequest = callShow(isEditMode = false)
//
//          redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.IncomeSourceController.show().url)
//
//          status(goodRequest) must be(Status.SEE_OTHER)
//
//          await(goodRequest)
//          verifySubscriptionDetailsSave(SelectedTaxYear, 1)
//        }
//      }
//
//      "in edit mode" should {
//        "redirect to checkYourAnswer page" in {
//          disable(SaveAndRetrieve)
//
//          mockIncomeSource()
//          setupMockSubscriptionDetailsSaveFunctions()
//
//          val goodRequest = callShow(isEditMode = true)
//
//          status(goodRequest) must be(Status.SEE_OTHER)
//          redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show.url)
//
//          await(goodRequest)
//          verifySubscriptionDetailsSave(SelectedTaxYear, 1)
//        }
//      }
//    }

    "there is an invalid submission with an error form" should {
      "return bad request status (400)" in {
        mockIncomeSource()
        val badRequest = callShowWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

      }
    }
  }

  "backUrl" when {
    "the save and retrieve feature switch is enabled" when {
      "in edit mode" must {
        s"return ${controllers.agent.routes.TaxYearCheckYourAnswersController.show().url}" in {
//          enable(SaveAndRetrieve)
          TestWhatYearToSignUpController.backUrl(true) mustBe Some(controllers.agent.routes.TaxYearCheckYourAnswersController.show().url)
        }
      }
      "not in edit mode" must {
        s"return ${controllers.agent.routes.TaskListController.show().url}" in {
//          enable(SaveAndRetrieve)
          TestWhatYearToSignUpController.backUrl(false) mustBe Some(controllers.agent.routes.TaskListController.show().url)
        }
      }
    }
//    "the save and retrieve feature switch is disabled" when {
//      "in edit mode" must {
//        s"return ${controllers.agent.routes.CheckYourAnswersController.show.url}" in {
//          TestWhatYearToSignUpController.backUrl(true) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show.url)
//        }
//      }
//      "not in edit mode" must {
//        s"return None" in {
//          TestWhatYearToSignUpController.backUrl(false) mustBe None
//        }
//      }
//    }
  }

}
