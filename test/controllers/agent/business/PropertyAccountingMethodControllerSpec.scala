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
import config.featureswitch._
import controllers.agent.AgentControllerBaseSpec
import forms.agent.AccountingMethodPropertyForm
import models.Cash
import models.common.{AccountingMethodPropertyModel, IncomeSourceModel}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.PropertyAccountingMethod
import utilities.agent.TestModels._

import scala.concurrent.Future

class PropertyAccountingMethodControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with FeatureSwitching {

  override val controllerName: String = "PropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestPropertyAccountingMethodController.show(isEditMode = false),
    "submit" -> TestPropertyAccountingMethodController.submit(isEditMode = false)
  )

  object TestPropertyAccountingMethodController extends PropertyAccountingMethodController(
    mockAuthService,
    MockSubscriptionDetailsService
  )

  def propertyOnlyIncomeSourceType: CacheMap = testCacheMap(incomeSource = Some(testIncomeSourceProperty))

  def bothPropertyAndBusinessIncomeSource: CacheMap = testCacheMap(
    incomeSource = Some(testIncomeSourceBusinessAndUkProperty)
  )

  def cacheMap(incomeSource: Option[IncomeSourceModel] = None, accountingMethodProperty: Option[AccountingMethodPropertyModel] = None): CacheMap = testCacheMap(
    incomeSource = incomeSource,
    accountingMethodProperty = accountingMethodProperty
  )

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    super.beforeEach()
  }

  "show" when {
    "the user hasn't answered their income source" should {
      "redirect to the income source page" in {
        lazy val result = await(TestPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequest))

        mockFetchAllFromSubscriptionDetails(cacheMap(incomeSource = None))

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.routes.IncomeSourceController.show().url)

        verifySubscriptionDetailsFetchAll(1)
      }
    }

    "there is no previous selected answer" should {
      "display the property accounting method view and return OK (200)" in {
        lazy val result = await(TestPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequest))

        mockFetchAllFromSubscriptionDetails(cacheMap(incomeSource = Some(testIncomeSourceProperty)))

        status(result) must be(Status.OK)
        verifySubscriptionDetailsFetchAll(1)
      }
    }

    "there is a previous selected answer CASH" should {
      "display the property accounting method view with the previous selected answer CASH and return OK (200)" in {
        lazy val result = await(TestPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequest))

        mockFetchAllFromSubscriptionDetails(cacheMap(
          incomeSource = Some(testIncomeSourceProperty),
          accountingMethodProperty = Some(AccountingMethodPropertyModel(Cash))
        ))

        status(result) must be(Status.OK)
        verifySubscriptionDetailsFetchAll(1)
      }
    }
  }

  "submit" when {

    def callSubmit(isEditMode: Boolean): Future[Result] = TestPropertyAccountingMethodController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, AccountingMethodPropertyModel(Cash))
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestPropertyAccountingMethodController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "it is not in edit mode" when {
      "the user doesn't have foreign property" should {
        "redirect to CheckYourAnswer page" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(cacheMap(incomeSource = Some(testIncomeSourceProperty)))

          val goodRequest: Future[Result] = callSubmit(isEditMode = false)

          status(goodRequest) mustBe Status.SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

          await(goodRequest)
          verifySubscriptionDetailsSave(PropertyAccountingMethod, 1)
          verifySubscriptionDetailsFetchAll(2)
        }
      }
      "the user has foreign property" should {
        "redirect to the overseas property commencement date" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(cacheMap(incomeSource = Some(testIncomeSourceProperty.copy(foreignProperty = true))))

          val goodRequest: Future[Result] = callSubmit(isEditMode = false)

          status(goodRequest) mustBe Status.SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.OverseasPropertyStartDateController.show().url)

          await(goodRequest)
          verifySubscriptionDetailsSave(PropertyAccountingMethod, 1)
          verifySubscriptionDetailsFetchAll(2)
        }
      }
    }

    "it is in edit mode" should {
      "redirect to CheckYourAnswer page" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchAllFromSubscriptionDetails(cacheMap(incomeSource = Some(testIncomeSourceProperty.copy(foreignProperty = true))))

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(PropertyAccountingMethod, 1)
        verifySubscriptionDetailsFetchAll(2)
      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {
        mockFetchAllFromSubscriptionDetails(cacheMap(incomeSource = Some(testIncomeSourceProperty.copy(foreignProperty = true))))

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsSave(PropertyAccountingMethod, 0)
        verifySubscriptionDetailsFetchAll(1)
      }
    }
  }

  "The back url" when {
    "in edit mode" should {
      "redirect to the check your answers" in {
        TestPropertyAccountingMethodController.backUrl(
          incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
          isEditMode = true
        ) mustBe controllers.agent.routes.CheckYourAnswersController.show().url
      }
    }

    "not in edit mode" when {
      "release four feature switch is enabled" should {
        "redirect to the uk property commencement date" in {
          enable(ReleaseFour)
          TestPropertyAccountingMethodController.backUrl(
            incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
            isEditMode = false
          ) mustBe controllers.agent.business.routes.PropertyStartDateController.show().url
        }
      }
      "release four feature switch is not enabled" should {
        "redirect to the business accounting method page" when {
          "the user has self employment income" in {
            TestPropertyAccountingMethodController.backUrl(
              incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
              isEditMode = false
            ) mustBe controllers.agent.business.routes.BusinessAccountingMethodController.show().url
          }
        }
        "redirect to the income sources page" when {
          "the user doesn't have self employment income" in {
            TestPropertyAccountingMethodController.backUrl(
              incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true),
              isEditMode = false
            ) mustBe controllers.agent.routes.IncomeSourceController.show().url
          }
        }
      }
    }
  }
}
