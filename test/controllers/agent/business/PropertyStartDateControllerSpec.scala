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

import java.time.LocalDate

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import controllers.agent.AgentControllerBaseSpec
import forms.agent.PropertyStartDateForm
import models.DateModel
import models.common.{IncomeSourceModel, PropertyStartDateModel}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{await, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.PropertyStartDate
import utilities.TestModels.{testCacheMap, testIncomeSourceBoth, testIncomeSourceProperty}

import scala.concurrent.Future

class PropertyStartDateControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService with FeatureSwitching {

  override val controllerName: String = "PropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestPropertyStartDateController$.show(isEditMode = false),
    "submit" -> TestPropertyStartDateController$.submit(isEditMode = false)
  )

  object TestPropertyStartDateController$ extends PropertyStartDateController(
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockLanguageUtils
  )

  trait Test {
    val controller = new PropertyStartDateController(
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockLanguageUtils
    )
  }

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    super.beforeEach()
  }

  val incomeSourcePropertyOnly: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true,
    foreignProperty = false)

  val incomeSourceBoth: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
    foreignProperty = false)

  def propertyOnlyIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceProperty)

  def bothIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceBoth)


  "show" should {
    "display the property accounting method view and return OK (200)" in new Test {
      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockFetchAllFromSubscriptionDetails(testCacheMap(
        incomeSource = Some(incomeSourceBoth)
      ))

      status(result) must be(Status.OK)
      verifySubscriptionDetailsFetchAll(1)

    }
  }

  "submit" should {

    val testValidMaxStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
    val testValidMinStartDate: DateModel = DateModel.dateConvert(LocalDate.of(1900, 1, 1))

    val testPropertyStartDateModel: PropertyStartDateModel = PropertyStartDateModel(testValidMaxStartDate)

    def callSubmit(isEditMode: Boolean): Future[Result] = TestPropertyStartDateController$.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(PropertyStartDateForm.propertyStartDateForm(testValidMinStartDate.toString, testValidMaxStartDate.toString),
        testPropertyStartDateModel)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestPropertyStartDateController$.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockSubscriptionDetailsSaveFunctions()
        val goodRequest = callSubmit(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsFetchAll(1)
        verifySubscriptionDetailsSave(PropertyStartDate, 1)

      }

      "redirect to businessAccountingMethod page" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callSubmit(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.PropertyAccountingMethodController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsFetchAll(1)
        verifySubscriptionDetailsSave(PropertyStartDate, 1)
      }

    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsFetchAll(1)
        verifySubscriptionDetailsSave(PropertyStartDate, 1)

      }

      "redirect to checkYourAnswer page" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callSubmit(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsFetchAll(1)
        verifySubscriptionDetailsSave(PropertyStartDate, 1)


      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {

        mockFetchAllFromSubscriptionDetails(propertyOnlyIncomeSourceType)

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsFetchAll(1)
      }
    }

    "The back url is not in edit mode" when {
      "the user has rental property and it is the only income source" should {
        "redirect to income source page when FS ReleaseFour is not enabled" in new Test {
          controller.backUrl(isEditMode = false, incomeSourcePropertyOnly) mustBe
            controllers.agent.routes.IncomeSourceController.show().url
        }

        "redirect to income source page when FS ReleaseFour is enabled" in new Test {
          enable(ReleaseFour)
          controller.backUrl(isEditMode = false, incomeSourcePropertyOnly) mustBe
            controllers.agent.routes.IncomeSourceController.show().url
        }
      }

      "the user has rental property and has a business" should {
        "redirect to income source page when FS ReleaseFour is not enabled" in new Test {
          controller.backUrl(isEditMode = false, incomeSourceBoth) mustBe
            controllers.agent.routes.IncomeSourceController.show().url
        }
        "redirect to Business Accounting Method page when FS ReleaseFour is enabled" in new Test {
          enable(ReleaseFour)
          controller.backUrl(isEditMode = false, incomeSourceBoth) mustBe
            controllers.agent.business.routes.BusinessAccountingMethodController.show().url
        }
      }
    }

    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to check your answer page" in new Test {
          controller.backUrl(isEditMode = true, incomeSourcePropertyOnly) mustBe
            controllers.agent.routes.CheckYourAnswersController.show().url
        }
      }
    }
  }
}
