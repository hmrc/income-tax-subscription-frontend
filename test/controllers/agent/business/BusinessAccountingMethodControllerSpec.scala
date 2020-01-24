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

package controllers.agent.business

import agent.models.AccountingMethodModel
import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels.testCacheMap
import controllers.agent.AgentControllerBaseSpec
import core.config.featureswitch.{AgentPropertyCashOrAccruals, FeatureSwitching}
import core.models.{Cash, No, Yes}
import forms.agent.AccountingMethodForm
import incometax.business.models.MatchTaxYearModel
import incometax.subscription.models.{Both, Business}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._

class BusinessAccountingMethodControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentPropertyCashOrAccruals)
  }

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

  trait Test {
    val controller = new BusinessAccountingMethodController(
      MockBaseControllerConfig,
      messagesApi,
      MockKeystoreService,
      mockAuthService
    )
  }

  "show" must {
    s"return $OK" when {
      "the user has not entered an answer previously" in new Test {
        setupMockKeystore(
          fetchAll = testCacheMap(
            incomeSource = Some(Business),
            matchTaxYear = Some(MatchTaxYearModel(Yes))
          )
        )

        val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

        status(result) mustBe OK

        verifyKeystore(fetchAll = 1)
      }
      "the user has entered the answer previously" in new Test {
        setupMockKeystore(
          fetchAll = testCacheMap(
            incomeSource = Some(Business),
            matchTaxYear = Some(MatchTaxYearModel(Yes)),
            accountingMethod = Some(incometax.business.models.AccountingMethodModel(Cash))
          )
        )

        val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

        status(result) mustBe OK

        verifyKeystore(fetchAll = 1)
      }
    }
  }

  "submit" when {

    "the users submission is invalid" must {
      s"return $BAD_REQUEST" in new Test {
        setupMockKeystore(
          fetchAll = testCacheMap(
            incomeSource = Some(Business),
            matchTaxYear = Some(MatchTaxYearModel(Yes))
          )
        )

        val result: Result = await(controller.submit(isEditMode = false)(subscriptionRequest))

        status(result) mustBe BAD_REQUEST

        verifyKeystore(fetchAll = 1, saveAccountingMethod = 0)
      }
    }

    "not in edit mode" should {
      s"redirect to ${routes.PropertyAccountingMethodController.show().url}" when {
        "the property cash/accruals feature switch is enabled and the user has both business and property income" in new Test {
          enable(AgentPropertyCashOrAccruals)
          setupMockKeystoreSaveFunctions()
          setupMockKeystore(
            fetchAll = testCacheMap(
              incomeSource = Some(Both),
              matchTaxYear = Some(MatchTaxYearModel(Yes))
            )
          )

          val result: Result = await(controller.submit(isEditMode = false)(subscriptionRequest.post(
            AccountingMethodForm.accountingMethodForm, AccountingMethodModel(Cash)
          )))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.PropertyAccountingMethodController.show().url)

          verifyKeystore(fetchAll = 1, saveAccountingMethod = 1)
        }
      }

      s"redirect to '${controllers.agent.routes.CheckYourAnswersController.show().url}'" when {
        "the property cash/accruals feature switch is disabled and the user has both business and property income" in new Test {
          setupMockKeystoreSaveFunctions()
          setupMockKeystore(
            fetchAll = testCacheMap(
              incomeSource = Some(Both),
              matchTaxYear = Some(MatchTaxYearModel(Yes))
            )
          )

          val result: Result = await(controller.submit(isEditMode = false)(subscriptionRequest.post(
            AccountingMethodForm.accountingMethodForm, AccountingMethodModel(Cash)
          )))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

          verifyKeystore(fetchAll = 1, saveAccountingMethod = 1)
        }

        "the property cash/accruals feature switch is enabled and the user has business only income" in new Test {
          enable(AgentPropertyCashOrAccruals)
          setupMockKeystoreSaveFunctions()
          setupMockKeystore(
            fetchAll = testCacheMap(
              incomeSource = Some(Business),
              matchTaxYear = Some(MatchTaxYearModel(Yes))
            )
          )

          val result: Result = await(controller.submit(isEditMode = false)(subscriptionRequest.post(
            AccountingMethodForm.accountingMethodForm, AccountingMethodModel(Cash)
          )))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

          verifyKeystore(fetchAll = 1, saveAccountingMethod = 1)
        }
      }

    }

    "in edit mode" should {

      s"redirect to '${controllers.agent.routes.CheckYourAnswersController.show().url}'" in new Test {
        setupMockKeystoreSaveFunctions()
        setupMockKeystore(
          fetchAll = testCacheMap(
            incomeSource = Some(Business),
            matchTaxYear = MatchTaxYearModel(Yes)
          )
        )

        val result: Result = await(controller.submit(isEditMode = true)(
          subscriptionRequest.post(AccountingMethodForm.accountingMethodForm, AccountingMethodModel(Cash))
        ))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

        verifyKeystore(fetchAll = 1, saveAccountingMethod = 1)
      }

    }

  }

  "The back url" when {

    "in edit mode" should {
      s"point to ${controllers.agent.routes.CheckYourAnswersController.show().url}" in new Test {
        controller.backUrl(isEditMode = true, Business, MatchTaxYearModel(Yes)) mustBe controllers.agent.routes.CheckYourAnswersController.show().url
      }
    }

    "not in edit mode" when {
      "the user does not match the tax year" should {
        s"point to ${controllers.agent.business.routes.BusinessAccountingPeriodDateController.show().url}" in new Test {
          controller.backUrl(isEditMode = false, Business, MatchTaxYearModel(No)) mustBe
            controllers.agent.business.routes.BusinessAccountingPeriodDateController.show().url
        }
      }

      "the user matches the tax year and only has business income" should {
        s"point to ${controllers.agent.business.routes.WhatYearToSignUpController.show().url}" in new Test {
          controller.backUrl(isEditMode = false, Business, MatchTaxYearModel(Yes)) mustBe
            controllers.agent.business.routes.WhatYearToSignUpController.show().url
        }
      }

      "the user matches the tax year and doesn't just have business income" should {
        s"point to ${controllers.agent.business.routes.MatchTaxYearController.show().url}" in new Test {
          controller.backUrl(isEditMode = false, Both, MatchTaxYearModel(Yes)) mustBe
            controllers.agent.business.routes.MatchTaxYearController.show().url
        }
      }
    }
  }

  authorisationTests()

}
