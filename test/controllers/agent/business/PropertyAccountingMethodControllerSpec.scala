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

import utilities.agent.TestModels._
import controllers.agent.AgentControllerBaseSpec
import config.featureswitch._
import forms.agent.AccountingMethodPropertyForm
import models.Cash
import models.common.AccountingMethodPropertyModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class PropertyAccountingMethodControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService
  with FeatureSwitching {

  override val controllerName: String = "PropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestPropertyAccountingMethodController.show(isEditMode = false),
    "submit" -> TestPropertyAccountingMethodController.submit(isEditMode = false)
  )

  object TestPropertyAccountingMethodController extends PropertyAccountingMethodController(
    mockAuthService,
    MockKeystoreService
  )

  def propertyOnlyIncomeSourceType: CacheMap = testCacheMap(incomeSource = Some(testIncomeSourceProperty))

  def bothPropertyAndBusinessIncomeSource: CacheMap = testCacheMap(
    incomeSource = Some(testIncomeSourceBoth)
  )

  "show" when {
    "there is no previous selected answer" should {
      "display the property accounting method view and return OK (200)" in {
        lazy val result = await(TestPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequest))

        mockFetchPropertyAccountingFromKeyStore(None)
        mockFetchAllFromKeyStore(propertyOnlyIncomeSourceType)

        status(result) must be(Status.OK)
        verifyKeystore(fetchPropertyAccountingMethod = 1, savePropertyAccountingMethod = 0, fetchAll = 1)

      }
    }

    "there is a previous selected answer CASH" should {
      "display the property accounting method view with the previous selected answer CASH and return OK (200)" in {
        lazy val result = await(TestPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequest))

        mockFetchPropertyAccountingFromKeyStore(AccountingMethodPropertyModel(Cash))
        mockFetchAllFromKeyStore(propertyOnlyIncomeSourceType)

        status(result) must be(Status.OK)
        verifyKeystore(fetchPropertyAccountingMethod = 1, savePropertyAccountingMethod = 0, fetchAll = 1)

      }
    }
  }

  "submit" should {

    def callShow(isEditMode: Boolean): Future[Result] = TestPropertyAccountingMethodController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, AccountingMethodPropertyModel(Cash))
    )

    def callShowWithErrorForm(isEditMode: Boolean): Future[Result] = TestPropertyAccountingMethodController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" when {
      "turn a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchAll = 0, savePropertyAccountingMethod = 1)
      }

      "redirect to CheckYourAnswer page" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchAll = 0, savePropertyAccountingMethod = 1)
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

        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchAll = 0, savePropertyAccountingMethod = 1)

      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {
        mockFetchAllFromKeyStore(propertyOnlyIncomeSourceType)

        val badRequest = callShowWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyKeystore(savePropertyAccountingMethod = 0, fetchAll = 1)
      }
    }

    "The back url is not in edit mode" when {

      "the user has rental property" should {
        s"return ${controllers.agent.routes.IncomeSourceController.show().url}" in {
          mockFetchAllFromKeyStore(propertyOnlyIncomeSourceType)
          await(TestPropertyAccountingMethodController.backUrl(isEditMode = false)) mustBe controllers.agent.routes.IncomeSourceController.show().url
        }
      }

      "the user has both rental property and business" should {
        "redirect to Business Accounting Method page" in {
          mockFetchAllFromKeyStore(bothPropertyAndBusinessIncomeSource)
          await(TestPropertyAccountingMethodController.backUrl(isEditMode = false)) mustBe
            controllers.agent.business.routes.BusinessAccountingMethodController.show().url
        }
      }
    }
    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to Check Your Answer page" in {
          mockFetchAllFromKeyStore(propertyOnlyIncomeSourceType)
          await(TestPropertyAccountingMethodController.backUrl(isEditMode = true)) mustBe
            controllers.agent.routes.CheckYourAnswersController.show().url
        }
      }
    }
  }
}
