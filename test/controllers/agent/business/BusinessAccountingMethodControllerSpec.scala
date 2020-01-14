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
import controllers.agent.AgentControllerBaseSpec
import core.config.featureswitch.{AgentPropertyCashOrAccruals, EligibilityPagesFeature, FeatureSwitching}
import core.models.{Cash, No, Yes}
import forms.agent.AccountingMethodForm
import incometax.business.models.MatchTaxYearModel
import incometax.subscription.models.{Both, Business}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._

import scala.concurrent.Future

class BusinessAccountingMethodControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EligibilityPagesFeature)
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

  "Calling the show action of the BusinessAccountingMethod with an authorised user" should {

    lazy val result = TestBusinessAccountingMethodController.show(isEditMode = false)(subscriptionRequest)

    "return ok (200)" in {
      setupMockKeystore(fetchAccountingMethod = None, fetchIncomeSource = Both, fetchMatchTaxYear = Some(MatchTaxYearModel(Yes)))

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchAccountingMethod = 1, saveAccountingMethod = 0, fetchIncomeSource = 1)
    }
  }

  "Calling the submit action of the BusinessAccountingMethod with an authorised user and valid submission" should {

    def callSubmit(isEditMode: Boolean): Future[Result] = TestBusinessAccountingMethodController.submit(isEditMode = isEditMode)(subscriptionRequest
      .post(AccountingMethodForm.accountingMethodForm, AccountingMethodModel(Cash)))

    "When it is not in edit mode" should {

      s"redirect to ${routes.PropertyAccountingMethodController.show().url}" when {
        "the property cash/accruals feature switch is enabled and the user has both business and property income" in {
          setupMockKeystoreSaveFunctions()
          setupMockKeystore(fetchIncomeSource = Both)
          enable(AgentPropertyCashOrAccruals)

          val goodRequest = await(callSubmit(isEditMode = false))

          status(goodRequest) mustBe SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(routes.PropertyAccountingMethodController.show().url)

          verifyKeystore(fetchIncomeSource = 1, saveAccountingMethod = 1)
        }
      }

      s"redirect to '${controllers.agent.routes.TermsController.show().url}'" in {
        setupMockKeystoreSaveFunctions()
        setupMockKeystore(fetchIncomeSource = Both)

        val goodRequest = callSubmit(isEditMode = false)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.TermsController.show().url)

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveAccountingMethod = 1)
      }

      s"redirect to '${controllers.agent.routes.CheckYourAnswersController.show().url}' when the eligibility pages feature switch is on" in {
        setupMockKeystoreSaveFunctions()
        setupMockKeystore(fetchIncomeSource = Both)
        enable(EligibilityPagesFeature)

        val goodRequest = callSubmit(isEditMode = false)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveAccountingMethod = 1)
      }
    }

    "When it is in edit mode" should {
      s"redirect to '${controllers.agent.routes.CheckYourAnswersController.show().url}'" in {
        setupMockKeystoreSaveFunctions()
        setupMockKeystore(fetchIncomeSource = Both)

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveAccountingMethod = 1)
      }
    }
  }

  "Calling the submit action of the BusinessAccountingMethod with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessAccountingMethodController.submit(isEditMode = false)(subscriptionRequest)

    "return a bad request status (400)" in {
      setupMockKeystore(fetchIncomeSource = Both, fetchMatchTaxYear = Some(MatchTaxYearModel(Yes)))

      status(badRequest) must be(Status.BAD_REQUEST)
      await(badRequest)

      verifyKeystore(fetchAccountingMethod = 0, saveAccountingMethod = 0, fetchIncomeSource = 1)
    }
  }

  "The back url" when {

    "in edit mode" should {
      s"point to ${controllers.agent.routes.CheckYourAnswersController.show().url}" in {
        TestBusinessAccountingMethodController.backUrl(isEditMode = true, None, None) mustBe controllers.agent.routes.CheckYourAnswersController.show().url
      }
    }

    "not in edit mode" when {
        "match tax year was answered with No" should {
          s"point to ${controllers.agent.business.routes.BusinessAccountingPeriodDateController.show().url}" in {

            TestBusinessAccountingMethodController.backUrl(isEditMode = false, None, MatchTaxYearModel(No)) mustBe
              controllers.agent.business.routes.BusinessAccountingPeriodDateController.show().url
          }
        }

        "income source type is both" should {
          s"point to ${controllers.agent.business.routes.MatchTaxYearController.show().url}" in {

            TestBusinessAccountingMethodController.backUrl(isEditMode = false, Some(Both), Some(MatchTaxYearModel(Yes))) mustBe
              controllers.agent.business.routes.MatchTaxYearController.show().url
          }
        }

        "income source type is business" should {
          s"point to ${controllers.agent.business.routes.WhatYearToSignUpController.show().url}" in {

            TestBusinessAccountingMethodController.backUrl(isEditMode = false, Some(Business), Some(MatchTaxYearModel(Yes))) mustBe
              controllers.agent.business.routes.WhatYearToSignUpController.show().url
          }
        }

        "the back url can't be determined" should {
          s"point to ${controllers.agent.routes.IncomeSourceController.show().url}" in {

            TestBusinessAccountingMethodController.backUrl(isEditMode = false, None, None) mustBe
              controllers.agent.routes.IncomeSourceController.show().url
          }
        }

    }
  }

  authorisationTests()

}
