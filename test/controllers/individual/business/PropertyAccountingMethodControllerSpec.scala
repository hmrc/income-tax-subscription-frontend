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
import core.config.featureswitch._
import core.services.mocks.MockKeystoreService
import core.utils.TestModels._
import forms.individual.business.AccountingMethodPropertyForm
import incometax.incomesource.services.mocks.MockCurrentTimeService
import models.Cash
import models.individual.business.AccountingMethodPropertyModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class PropertyAccountingMethodControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with MockCurrentTimeService
  with FeatureSwitching {

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
    mockCurrentTimeService
  )

  def propertyOnlyIncomeSourceType: CacheMap = testCacheMap(rentUkProperty = Some(testRentUkProperty_property_only))

  def propertyOnlySelfEmNoIncomeSourceType: CacheMap = testCacheMap(
    rentUkProperty = Some(testRentUkProperty_property_and_other), areYouSelfEmployed = Some(testAreYouSelfEmployed_no)
  )

  def bothIncomeSourceType: CacheMap = testCacheMap(
    rentUkProperty = Some(testRentUkProperty_property_and_other),
    areYouSelfEmployed = Some(testAreYouSelfEmployed_yes)
  )

  "show" should {
    "display the property accounting method view and return OK (200)" in {
      lazy val result = await(TestPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequest))

      setupMockKeystore(
        fetchPropertyAccountingMethod = None,
        fetchAll = propertyOnlySelfEmNoIncomeSourceType // for the back url
      )

      status(result) must be(Status.OK)
      verifyKeystore(fetchPropertyAccountingMethod = 1, savePropertyAccountingMethod = 0, fetchAll = 1)

    }
  }

  "submit" should {

    def callShow(isEditMode: Boolean): Future[Result] = TestPropertyAccountingMethodController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, AccountingMethodPropertyModel(Cash))
    )

    def callShowWithErrorForm(isEditMode: Boolean): Future[Result] = TestPropertyAccountingMethodController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()
        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchAll = 0, savePropertyAccountingMethod = 1)
      }

      "redirect to checkYourAnswer page" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

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

      "redirect to checkYourAnswer page" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchAll = 0, savePropertyAccountingMethod = 1)

      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {
        setupMockKeystore(fetchAll = propertyOnlySelfEmNoIncomeSourceType)

        val badRequest = callShowWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyKeystore(savePropertyAccountingMethod = 0, fetchAll = 1)
      }
    }

    "The back url is not in edit mode" when {
      "the user has rental property and it is the only income source" should {
        "redirect to rent Uk property page" in {
          setupMockKeystore(fetchAll = propertyOnlyIncomeSourceType)
          await(TestPropertyAccountingMethodController.backUrl(isEditMode = false)) mustBe
            controllers.individual.incomesource.routes.RentUkPropertyController.show().url
        }
      }

      "the user has rental property and it is not the only income source and the user has a business" should {
        "redirect to business accounting method page" in {
          setupMockKeystore(fetchAll = bothIncomeSourceType)
          await(TestPropertyAccountingMethodController.backUrl(isEditMode = false)) mustBe
            controllers.individual.business.routes.BusinessAccountingMethodController.show().url
        }
      }

      "the user has rental property and it is not the only income source and the user does not have a business" should {
        "redirect to are you self employed page" in {
          setupMockKeystore(fetchAll = propertyOnlySelfEmNoIncomeSourceType)
          await(TestPropertyAccountingMethodController.backUrl(isEditMode = false)) mustBe
            controllers.individual.incomesource.routes.AreYouSelfEmployedController.show().url
        }
      }
    }
    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to check your answer page" in {
          setupMockKeystoreSaveFunctions()
          await(TestPropertyAccountingMethodController.backUrl(isEditMode = true)) mustBe
            controllers.individual.subscription.routes.CheckYourAnswersController.show().url
        }
      }
    }
  }

}
