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

import java.time.LocalDate

import controllers.ControllerBaseSpec
import forms.individual.business.PropertyCommencementDateForm
import models.DateModel
import models.individual.business.PropertyCommencementDateModel
import models.individual.incomesource.IncomeSourceModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.CacheConstants.PropertyCommencementDate
import utilities.TestModels.{testCacheMap, testIncomeSourceBoth, testIncomeSourceProperty}

import scala.concurrent.Future

class PropertyCommencementDateControllerSpec extends ControllerBaseSpec with MockKeystoreService {

  override val controllerName: String = "PropertyCommencementDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestPropertyCommencementDateController.show(isEditMode = false),
    "submit" -> TestPropertyCommencementDateController.submit(isEditMode = false)
  )

  object TestPropertyCommencementDateController extends PropertyCommencementDateController(
    mockAuthService,
    MockKeystoreService,
    mockLanguageUtils
  )

  trait Test {
    val controller = new PropertyCommencementDateController(
      mockAuthService,
      MockKeystoreService,
      mockLanguageUtils
    )
  }

  val incomeSourcePropertyOnly: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true,
    foreignProperty = false)

  val incomeSourceBoth: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
    foreignProperty = false)

  def propertyOnlyIncomeSourceType: CacheMap = testCacheMap(incomeSourceIndiv = testIncomeSourceProperty)

  def bothIncomeSourceType: CacheMap = testCacheMap(incomeSourceIndiv = testIncomeSourceBoth)


  "show" should {
    "display the property accounting method view and return OK (200)" in new Test {
      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockFetchAllFromKeyStore(testCacheMap(
        incomeSourceIndiv = Some(incomeSourceBoth)
      ))

      status(result) must be(Status.OK)
      verifyKeystoreSave(PropertyCommencementDate, 0)
      verifyKeyStoreFetchAll(1)

    }
  }

  "submit" should {

    val testValidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
    val testPropertyCommencementDateModel: PropertyCommencementDateModel = PropertyCommencementDateModel(testValidStartDate)

    def callShow(isEditMode: Boolean): Future[Result] = TestPropertyCommencementDateController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(PropertyCommencementDateForm.propertyCommencementDateForm(testValidStartDate.toString), testPropertyCommencementDateModel)
    )

    def callShowWithErrorForm(isEditMode: Boolean): Future[Result] = TestPropertyCommencementDateController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()
        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystoreSave(PropertyCommencementDate, 1)
        verifyKeyStoreFetchAll(0)
      }

      "redirect to businessAccountingMethod page" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.PropertyAccountingMethodController.show().url)

        await(goodRequest)
        verifyKeystoreSave(PropertyCommencementDate, 1)
        verifyKeyStoreFetchAll(0)
      }

    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystoreSave(PropertyCommencementDate, 1)
        verifyKeyStoreFetchAll(0)
      }

      "redirect to checkYourAnswer page" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystoreSave(PropertyCommencementDate, 1)
        verifyKeyStoreFetchAll(0)

      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {

        mockFetchAllFromKeyStore(propertyOnlyIncomeSourceType)

        val badRequest = callShowWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyKeystoreSave(PropertyCommencementDate, 0)
        verifyKeyStoreFetchAll(1)
      }
    }

    "The back url is not in edit mode" when {
      "the user has rental property and it is the only income source" should {
        "redirect to income source page" in new Test {
          controller.backUrl(isEditMode = false, incomeSourcePropertyOnly) mustBe
            controllers.individual.incomesource.routes.IncomeSourceController.show().url
        }
      }

      "the user has rental property and has a business" should {
        "redirect to business accounting method page" in new Test {
          controller.backUrl(isEditMode = false, incomeSourceBoth) mustBe
            appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/details/business-accounting-method"
        }
      }

    }
    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to check your answer page" in new Test {
          controller.backUrl(isEditMode = true, incomeSourcePropertyOnly) mustBe
            controllers.individual.subscription.routes.CheckYourAnswersController.show().url
        }
      }
    }
  }

}
