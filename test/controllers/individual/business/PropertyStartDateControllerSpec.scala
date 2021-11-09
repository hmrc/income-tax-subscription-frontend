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

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import forms.individual.business.PropertyStartDateForm
import models.DateModel
import models.common.{IncomeSourceModel, PropertyModel}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.PropertyStartDate
import utilities.TestModels.{testCacheMap, testFullPropertyModel, testIncomeSourceBoth, testIncomeSourceProperty}
import views.individual.mocks.MockPropertyStartDate

import java.time.LocalDate
import scala.concurrent.Future

class PropertyStartDateControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAuditingService
  with MockPropertyStartDate
  with FeatureSwitching {

  override val controllerName: String = "PropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestPropertyStartDateController.show(isEditMode = false),
    "submit" -> TestPropertyStartDateController.submit(isEditMode = false)
  )

  object TestPropertyStartDateController extends PropertyStartDateController(
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockLanguageUtils,
    propertyStartDate
  )

  trait Test {
    val controller = new PropertyStartDateController(
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockLanguageUtils,
      propertyStartDate
    )
  }

  val incomeSourcePropertyOnly: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true,
    foreignProperty = false)

  val incomeSourceBoth: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
    foreignProperty = false)

  def propertyOnlyIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceProperty)

  def bothIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceBoth)


  "show" should {
    "display the property accounting method view and return OK (200)" in new Test {
      disable(SaveAndRetrieve)
      mockPropertyStartDate()

      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockFetchAllFromSubscriptionDetails(testCacheMap(
        incomeSource = Some(incomeSourceBoth)
      ))
      mockFetchProperty(None)

      status(result) must be(Status.OK)
      verifySubscriptionDetailsSave(PropertyStartDate, 0)
    }

    "display the property accounting method view and return OK (200) when Save & Retrieve feature is enabled" in new Test {
      enable(SaveAndRetrieve)
      mockPropertyStartDate()

      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockFetchAllFromSubscriptionDetails(testCacheMap())
      mockFetchProperty(None)

      status(result) must be(Status.OK)
      verifySubscriptionDetailsSave(PropertyStartDate, 0)
    }
  }

  "submit" should {

    val testValidMaxDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
    val testValidMinDate: DateModel = DateModel.dateConvert(LocalDate.of(1900, 1, 1))

    val testPropertyStartDateModel: DateModel = testValidMaxDate

    def callShow(isEditMode: Boolean): Future[Result] = TestPropertyStartDateController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(PropertyStartDateForm.propertyStartDateForm(testValidMinDate.toString, testValidMaxDate.toString), testPropertyStartDateModel)
    )

    def callShowWithErrorForm(isEditMode: Boolean): Future[Result] = TestPropertyStartDateController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        disable(SaveAndRetrieve)
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchProperty(None)

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyPropertySave(Some(PropertyModel(startDate = testValidMaxDate)))
      }

      "redirect to property accounting method page" in {
        disable(SaveAndRetrieve)
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchProperty(None)

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.PropertyAccountingMethodController.show().url)

        await(goodRequest)
        verifyPropertySave(Some(PropertyModel(startDate = testValidMaxDate)))
      }

    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303) to check your answers" in {
        disable(SaveAndRetrieve)
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchProperty(testFullPropertyModel)

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyPropertySave(Some(testFullPropertyModel.copy(startDate = testValidMaxDate, confirmed = false)))
      }

      "redirect to taskListPage if save & retrieve feature is enabled page" in {
        enable(SaveAndRetrieve)
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchProperty(testFullPropertyModel)

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.PropertyAccountingMethodController.show().url)

        await(goodRequest)
        verifyPropertySave(Some(testFullPropertyModel.copy(startDate = testValidMaxDate, confirmed = false)))
      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {
        disable(SaveAndRetrieve)
        mockPropertyStartDate()

        mockFetchAllFromSubscriptionDetails(propertyOnlyIncomeSourceType)

        val badRequest = callShowWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyPropertySave(None)
        verifySubscriptionDetailsFetchAll(1)
      }

      "return bad request status (400) when Save & Retrieve feature is enabled" in {
        enable(SaveAndRetrieve)
        mockPropertyStartDate()

        val badRequest = callShowWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyPropertySave(None)
        verifySubscriptionDetailsFetchAll(0)
      }
    }

    "The back url is not in edit mode" when {
      "the user has rental property and it is the only income source" should {
        "redirect to income source page" in new Test {
          disable(SaveAndRetrieve)

          controller.backUrl(isEditMode = false, incomeSourcePropertyOnly) mustBe
            controllers.individual.incomesource.routes.IncomeSourceController.show().url
        }
      }

      "the user has rental property and has a business" should {
        "redirect to business accounting method page" in new Test {
          disable(SaveAndRetrieve)

          controller.backUrl(isEditMode = false, incomeSourceBoth) mustBe
            appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/details/business-accounting-method"
        }
      }

      "the Save & retrieve feature is enabled" should {
        "redirect to What Income Source To Sign Up page" in new Test {
          enable(SaveAndRetrieve)

          controller.backUrl(isEditMode = false, incomeSourcePropertyOnly) mustBe
            controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show().url
        }
      }
    }

    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to check your answer page if the Save & retrieve feature is disabled" in new Test {
          disable(SaveAndRetrieve)

          controller.backUrl(isEditMode = true, incomeSourcePropertyOnly) mustBe
            controllers.individual.subscription.routes.CheckYourAnswersController.show().url
        }

        "redirect to check your answer page if the Save & retrieve feature is enabled" in new Test {
          enable(SaveAndRetrieve)

          controller.backUrl(isEditMode = true, incomeSourcePropertyOnly) mustBe
            controllers.individual.business.routes.TaskListController.show().url
        }
      }
    }
  }

}