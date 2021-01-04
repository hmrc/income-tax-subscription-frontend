/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.individual.business.AccountingMethodForm
import models.common.IncomeSourceModel
import models.common.business.AccountingMethodModel
import models.{Cash, No}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys._
import utilities.TestModels._

import scala.concurrent.Future

class BusinessAccountingMethodControllerSpec extends ControllerBaseSpec with MockSubscriptionDetailsService {

  override val controllerName: String = "BusinessAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessAccountingMethodController.show(isEditMode = false),
    "submit" -> TestBusinessAccountingMethodController.submit(isEditMode = false)
  )

  object TestBusinessAccountingMethodController extends BusinessAccountingMethodController(
    mockAuthService,
    MockSubscriptionDetailsService
  )

  "Calling the show action of the BusinessAccountingMethod with an authorised user" should {

    lazy val result = TestBusinessAccountingMethodController.show(isEditMode = false)(subscriptionRequest)

    "return ok (200)" in {
      mockFetchAccountingMethodFromSubscriptionDetails(None)
      mockFetchAllFromSubscriptionDetails(testCacheMap(incomeSource = testIncomeSourceBusiness))

      status(result) must be(Status.OK)

      await(result)
      verifySubscriptionDetailsSave(AccountingMethod, 0)
      verifySubscriptionDetailsFetchAll(2)
    }
  }

  "Calling the submit action of the BusinessAccountingMethod with an authorised user and valid submission" should {

    def callSubmit(isEditMode: Boolean): Future[Result] = TestBusinessAccountingMethodController.submit(isEditMode = isEditMode)(subscriptionRequest
      .post(AccountingMethodForm.accountingMethodForm, AccountingMethodModel(Cash)))

    "When it is not in edit mode" should {
      s"redirect to '${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}'" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)

        verifySubscriptionDetailsSave(AccountingMethod, 1)
        verifySubscriptionDetailsFetchAll(1)
      }

    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsSave(AccountingMethod, 1)
        verifySubscriptionDetailsFetchAll(1)
      }

      s"redirect to '${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}'" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callSubmit(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(AccountingMethod, 1)
        verifySubscriptionDetailsFetchAll(1)
      }
    }
  }

  "Calling the submit action of the BusinessAccountingMethod with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessAccountingMethodController.submit(isEditMode = false)(subscriptionRequest)

    "return a bad request status (400)" in {
      // for the back url
      mockFetchAllFromSubscriptionDetails(testCacheMap(incomeSource = testIncomeSourceBusiness))

      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifySubscriptionDetailsSave(AccountingMethod, 0)
      verifySubscriptionDetailsFetchAll(1)
    }
  }

  "The back url" when {
    "not in edit mode" when {
      "income source type is business" should {
        s"point to ${controllers.individual.business.routes.WhatYearToSignUpController.show().url}" in {
          mockFetchAllFromSubscriptionDetails(testCacheMap(incomeSource = testIncomeSourceBusiness))
          await(TestBusinessAccountingMethodController.backUrl(isEditMode = false)) mustBe
            controllers.individual.business.routes.WhatYearToSignUpController.show().url
        }

        s"point to ${controllers.individual.business.routes.BusinessNameController.show().url}" in {
          mockFetchAllFromSubscriptionDetails(testCacheMap(incomeSource = testIncomeSourceBoth))
          await(TestBusinessAccountingMethodController.backUrl(isEditMode = false)) mustBe
            controllers.individual.business.routes.BusinessNameController.show().url
        }
      }
    }
    "in edit mode" should {
      s"point to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
        await(TestBusinessAccountingMethodController.backUrl(isEditMode = true)) mustBe
          controllers.individual.subscription.routes.CheckYourAnswersController.show().url
      }
    }
  }

  authorisationTests()

}
