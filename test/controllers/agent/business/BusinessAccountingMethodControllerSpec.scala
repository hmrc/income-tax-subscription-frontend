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

package controllers.agent.business

import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import utilities.agent.TestModels.testCacheMap
import controllers.agent.AgentControllerBaseSpec
import forms.agent.AccountingMethodForm
import models.common.business.AccountingMethodModel
import models.common.IncomeSourceModel
import models.{Cash, No, Yes, common}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys.AccountingMethod

class BusinessAccountingMethodControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with FeatureSwitching {

  override val controllerName: String = "BusinessAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessAccountingMethodController.show(isEditMode = false),
    "submit" -> TestBusinessAccountingMethodController.submit(isEditMode = false)
  )

  object TestBusinessAccountingMethodController extends BusinessAccountingMethodController(
    mockAuthService,
    MockSubscriptionDetailsService
  )

  trait Test {
    val controller = new BusinessAccountingMethodController(
      mockAuthService,
      MockSubscriptionDetailsService
    )
  }

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    super.beforeEach()
  }

  "show" must {
    s"return $OK" when {
      "the user has not entered an answer previously" in new Test {
        mockFetchAllFromSubscriptionDetails(testCacheMap(
          incomeSource = Some(IncomeSourceModel(true, false, false))
        ))

        val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

        status(result) mustBe OK

        verifySubscriptionDetailsFetchAll(1)
      }
      "the user has entered the answer previously" in new Test {
        mockFetchAllFromSubscriptionDetails(testCacheMap(
            incomeSource = Some(IncomeSourceModel(true, false, false)),
            accountingMethod = Some(AccountingMethodModel(Cash))
          )
        )

        val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

        status(result) mustBe OK

        verifySubscriptionDetailsFetchAll(1)
      }
    }
  }

  "submit" when {

    "the users submission is invalid" must {
      s"return $BAD_REQUEST" in new Test {
        mockFetchAllFromSubscriptionDetails(testCacheMap(
            incomeSource = Some(IncomeSourceModel(true, false, false))
          )
        )

        val result: Result = await(controller.submit(isEditMode = false)(subscriptionRequest))

        status(result) mustBe BAD_REQUEST

        verifySubscriptionDetailsFetchAll(1)
        verifySubscriptionDetailsSave(AccountingMethod, 0)
      }
    }

    "not in edit mode" should {
      s"redirect to ${routes.PropertyAccountingMethodController.show().url} when FS ReleaseFour is not enabled" when {
        "the user has both business and property income" in new Test {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(testCacheMap(
            incomeSource = Some(IncomeSourceModel(true, true, false))
          )
          )

          val result: Result = await(controller.submit(isEditMode = false)(subscriptionRequest.post(
            AccountingMethodForm.accountingMethodForm, AccountingMethodModel(Cash)
          )))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.PropertyAccountingMethodController.show().url)

          verifySubscriptionDetailsFetchAll(2)
          verifySubscriptionDetailsSave(AccountingMethod, 1)
        }
      }

      s"redirect to ${routes.PropertyStartDateController.show().url} when FS ReleaseFour is enabled" when {
        "the user has property income" in new Test {
          enable(ReleaseFour)
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(testCacheMap(
              incomeSource = Some(IncomeSourceModel(true, true, false))
            )
          )

          val result: Result = await(controller.submit(isEditMode = false)(subscriptionRequest.post(
            AccountingMethodForm.accountingMethodForm, AccountingMethodModel(Cash)
          )))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.PropertyStartDateController.show().url)

          verifySubscriptionDetailsFetchAll(2)
          verifySubscriptionDetailsSave(AccountingMethod, 1)
        }
      }

      s"redirect to ${routes.OverseasPropertyStartDateController.show().url}" when {
        "the user doesn't have uk property income and has foreign property income" in new Test {
          enable(ReleaseFour)
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true))
          )
          )

          val result: Result = await(controller.submit(isEditMode = false)(subscriptionRequest.post(
            AccountingMethodForm.accountingMethodForm, AccountingMethodModel(Cash)
          )))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.OverseasPropertyStartDateController.show().url)

          verifySubscriptionDetailsFetchAll(2)
          verifySubscriptionDetailsSave(AccountingMethod, 1)
        }
      }

      s"redirect to '${controllers.agent.routes.CheckYourAnswersController.show().url}'" when {

        "the user has business only income" in new Test {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(testCacheMap(
              incomeSource = Some(IncomeSourceModel(true, false, false))
            )
          )

          val result: Result = await(controller.submit(isEditMode = false)(subscriptionRequest.post(
            AccountingMethodForm.accountingMethodForm, AccountingMethodModel(Cash)
          )))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

          verifySubscriptionDetailsFetchAll(2)
          verifySubscriptionDetailsSave(AccountingMethod, 1)
        }
      }

    }

    "in edit mode" should {

      s"redirect to '${controllers.agent.routes.CheckYourAnswersController.show().url}'" in new Test {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchAllFromSubscriptionDetails(testCacheMap(
          incomeSource = Some(IncomeSourceModel(true, false, false))
        ))

        val result: Result = await(controller.submit(isEditMode = true)(
          subscriptionRequest.post(AccountingMethodForm.accountingMethodForm, AccountingMethodModel(Cash))
        ))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

        verifySubscriptionDetailsFetchAll(2)
        verifySubscriptionDetailsSave(AccountingMethod, 1)
      }

    }

  }

  "The back url" when {

    "in edit mode" should {
      s"point to ${controllers.agent.routes.CheckYourAnswersController.show().url}" in new Test {
        controller.backUrl(isEditMode = true) mustBe controllers.agent.routes.CheckYourAnswersController.show().url
      }
    }

    "not in edit mode" should {
      s"point to ${controllers.agent.business.routes.BusinessNameController.show().url}" in new Test {
        controller.backUrl(isEditMode = false) mustBe
          controllers.agent.business.routes.BusinessNameController.show().url
      }
    }
  }

  authorisationTests()

}
