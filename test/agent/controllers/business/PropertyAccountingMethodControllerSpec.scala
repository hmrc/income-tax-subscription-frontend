/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.controllers.business

import agent.controllers.AgentControllerBaseSpec
import agent.forms.AccountingMethodPropertyForm
import agent.models.AccountingMethodPropertyModel
import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels._
import core.config.MockConfig
import core.config.featureswitch._
import core.controllers.ControllerBaseSpec
import core.models.Cash
import incometax.incomesource.services.mocks.MockCurrentTimeService
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

class PropertyAccountingMethodControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService
  with MockCurrentTimeService
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EligibilityPagesFeature)
  }

  override val controllerName: String = "PropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestPropertyAccountingMethodController.show(isEditMode = false),
    "submit" -> TestPropertyAccountingMethodController.submit(isEditMode = false)
  )

  object TestPropertyAccountingMethodController extends PropertyAccountingMethodController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    MockConfig,
    mockCurrentTimeService
  )

  def propertyOnlyIncomeSourceType: CacheMap = testCacheMap(incomeSource = Some(testIncomeSourceProperty))

  def propertyOtherIncomeYes: CacheMap = testCacheMap(
    incomeSource = Some(testIncomeSourceProperty), otherIncome = Some(testOtherIncomeYes)
  )

  def propertyOtherIncomeNo: CacheMap = testCacheMap(
    incomeSource = Some(testIncomeSourceProperty), otherIncome = Some(testOtherIncomeNo)
  )

  def bothPropertyAndBusinessIncomeSource: CacheMap = testCacheMap(
    incomeSource = Some(testIncomeSourceBoth)
  )

  "show" when {
    "there is no previous selected answer" should {
      "display the property accounting method view and return OK (200)" in {
        lazy val result = await(TestPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequest))

        setupMockKeystore(
          fetchPropertyAccountingMethod = None,
          fetchAll = propertyOnlyIncomeSourceType // for the back url
        )

        status(result) must be(Status.OK)
        verifyKeystore(fetchPropertyAccountingMethod = 1, savePropertyAccountingMethod = 0, fetchAll = 1)

      }
    }

    "there is a previous selected answer CASH" should {
      "display the property accounting method view with the previous selected answer CASH and return OK (200)" in {
        lazy val result = await(TestPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequest))

        setupMockKeystore(
          fetchPropertyAccountingMethod = AccountingMethodPropertyModel(Cash),
          fetchAll = propertyOnlyIncomeSourceType // for the back url
        )

        status(result) must be(Status.OK)
        verifyKeystore(fetchPropertyAccountingMethod = 1, savePropertyAccountingMethod = 0, fetchAll = 1)

      }
    }
  }

  "submit" should {

    def callShow(isEditMode: Boolean) = TestPropertyAccountingMethodController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, AccountingMethodPropertyModel(Cash))
    )

    def callShowWithErrorForm(isEditMode: Boolean) = TestPropertyAccountingMethodController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" when {
      "FeatureSwitch EligibilityPagesFeature is enabled" should {
        "turn a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystoreSaveFunctions()
          enable(EligibilityPagesFeature)

          val goodRequest = callShow(isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)

          await(goodRequest)
          verifyKeystore(fetchAll = 0, savePropertyAccountingMethod = 1)
        }

        "redirect to CheckYourAnswer page" in {
          setupMockKeystoreSaveFunctions()
          enable(EligibilityPagesFeature)

          val goodRequest = callShow(isEditMode = false)

          redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.CheckYourAnswersController.show().url)

          await(goodRequest)
          verifyKeystore(fetchAll = 0, savePropertyAccountingMethod = 1)
        }
      }

      "FeatureSwitch EligibilityPagesFeature is disabled" should {
        "turn a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)

          await(goodRequest)
          verifyKeystore(fetchAll = 0, savePropertyAccountingMethod = 1)
        }

        "redirect to Terms page" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(isEditMode = false)

          redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.TermsController.show().url)

          await(goodRequest)
          verifyKeystore(fetchAll = 0, savePropertyAccountingMethod = 1)
        }
      }
    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchAll = 0, savePropertyAccountingMethod = 1)
      }

      "redirect to CheckYourAnswer page" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchAll = 0, savePropertyAccountingMethod = 1)

      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {
        setupMockKeystore(fetchAll = propertyOnlyIncomeSourceType)

        val badRequest = callShowWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyKeystore(savePropertyAccountingMethod = 0, fetchAll = 1)
      }
    }

    "The back url is not in edit mode" when {

      "the user has rental property and has other income source" should {
        "redirect to Other Income Error page" in {
          setupMockKeystore(fetchAll = propertyOtherIncomeYes)
          await(TestPropertyAccountingMethodController.backUrl(isEditMode = false)) mustBe
            agent.controllers.routes.OtherIncomeErrorController.show().url
        }
      }

      "the user has rental property and has no other income source" should {
        "redirect to Income Other Page" in {
          setupMockKeystore(fetchAll = propertyOtherIncomeNo)
          await(TestPropertyAccountingMethodController.backUrl(isEditMode = false)) mustBe
            agent.controllers.routes.OtherIncomeController.show().url
        }
      }

      "the user has both rental property and business" should {
        "redirect to Business Accounting Method page" in {
          setupMockKeystore(fetchAll = bothPropertyAndBusinessIncomeSource)
          await(TestPropertyAccountingMethodController.backUrl(isEditMode = false)) mustBe
            agent.controllers.business.routes.BusinessAccountingMethodController.show().url
        }
      }
    }
    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to Check Your Answer page" in {
          setupMockKeystore(fetchAll = propertyOtherIncomeNo)
          await(TestPropertyAccountingMethodController.backUrl(isEditMode = true)) mustBe
            agent.controllers.routes.CheckYourAnswersController.show().url
        }
      }
    }
  }


}