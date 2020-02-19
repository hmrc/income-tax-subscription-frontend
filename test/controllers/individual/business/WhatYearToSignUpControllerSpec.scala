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

import controllers.ControllerBaseSpec
import core.config.MockConfig
import core.config.featureswitch._
import core.services.mocks.{MockAccountingPeriodService, MockKeystoreService}
import forms.individual.business.AccountingYearForm
import models.Current
import models.individual.business.AccountingYearModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._

import scala.concurrent.Future

class WhatYearToSignUpControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with MockAccountingPeriodService
  with FeatureSwitching {

  override val controllerName: String = "WhatYearToSignUpMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestWhatYearToSignUpController.show(isEditMode = false),
    "submit" -> TestWhatYearToSignUpController.submit(isEditMode = false)
  )

  object TestWhatYearToSignUpController extends WhatYearToSignUpController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    MockConfig,
    mockAccountingPeriodService
  )

  "show" should {
    "display the What Year To Sign Up view with pre-saved tax year option and return OK (200)" when {
      "there is a pre-saved tax year option in keystore" in {

        lazy val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        setupMockKeystore(
          fetchSelectedTaxYear = Some(AccountingYearModel(Current))
        )

        status(result) must be(Status.OK)

        verifyKeystore(fetchSelectedTaxYear = 1)

      }
    }

    "display the What Year To Sign Up view with empty form and return OK (200)" when {
      "there is a no pre-saved tax year option in keystore" in {

        lazy val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        setupMockKeystore(
          fetchSelectedTaxYear = None
        )

        status(result) must be(Status.OK)

        verifyKeystore(fetchSelectedTaxYear = 1)

      }
    }
  }


  "submit" should {

    def callShow(isEditMode: Boolean):Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingYearForm.accountingYearForm, AccountingYearModel(Current))
    )

    def callShowWithErrorForm(isEditMode: Boolean):Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()
        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(saveSelectedTaxYear = 1)
      }

      "redirect to business accounting period page" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.BusinessAccountingMethodController.show().url)

        await(goodRequest)
        verifyKeystore(saveSelectedTaxYear = 1)
      }

    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(saveSelectedTaxYear = 1)
      }

      "redirect to checkYourAnswer page" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(saveSelectedTaxYear = 1)

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
        "redirect to Match Tax Year page" in {
          TestWhatYearToSignUpController.backUrl(isEditMode = false) mustBe
            controllers.individual.business.routes.MatchTaxYearController.show().url
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
