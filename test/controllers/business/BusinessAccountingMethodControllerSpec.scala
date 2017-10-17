/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.business

import controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import forms.AccountingMethodForm
import models.AccountingMethodModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class BusinessAccountingMethodControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessAccountingMethodController.show(isEditMode = false),
    "submit" -> TestBusinessAccountingMethodController.submit(isEditMode = false)
  )

  object TestBusinessAccountingMethodController extends BusinessAccountingMethodController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  "Calling the show action of the BusinessAccountingMethod with an authorised user" should {

    lazy val result = TestBusinessAccountingMethodController.show(isEditMode = false)(subscriptionRequest)

    "return ok (200)" in {
      setupMockKeystore(fetchAccountingMethod = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchAccountingMethod = 1, saveAccountingMethod = 0)
    }
  }

  "Calling the submit action of the BusinessAccountingMethod with an authorised user and valid submission" should {

    def callShow(isEditMode: Boolean) = TestBusinessAccountingMethodController.submit(isEditMode = isEditMode)(subscriptionRequest
      .post(AccountingMethodForm.accountingMethodForm, AccountingMethodModel(AccountingMethodForm.option_cash)))

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchAccountingMethod = 0, saveAccountingMethod = 1)
      }

      s"redirect to '${digitalcontact.controllers.routes.PreferencesController.checkPreferences().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.TermsController.showTerms().url)

        await(goodRequest)
        verifyKeystore(fetchAccountingMethod = 0, saveAccountingMethod = 1)
      }
    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchAccountingMethod = 0, saveAccountingMethod = 1)
      }

      s"redirect to '${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchAccountingMethod = 0, saveAccountingMethod = 1)
      }
    }
  }

  "Calling the submit action of the BusinessAccountingMethod with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessAccountingMethodController.submit(isEditMode = false)(subscriptionRequest)

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchAccountingMethod = 0, saveAccountingMethod = 0)
    }
  }

  "The back url not in edit mode" should {
    s"point to ${controllers.business.routes.BusinessAccountingPeriodDateController.show().url}" in {
      TestBusinessAccountingMethodController.backUrl(isEditMode = false) mustBe controllers.business.routes.BusinessAccountingPeriodDateController.show().url
    }
  }

  "The back url in edit mode" should {
    s"point to ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
      TestBusinessAccountingMethodController.backUrl(isEditMode = true) mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    }
  }

  authorisationTests()

}
