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

import java.time.LocalDate

import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import controllers.agent.AgentControllerBaseSpec
import forms.agent.OverseasPropertyCommencementDateForm
import models.DateModel
import models.common.{IncomeSourceModel, OverseasPropertyCommencementDateModel}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.agent.mocks.MockAgentAuthService
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.OverseasPropertyCommencementDate
import utilities.TestModels.{testCacheMap, testIncomeSourceBoth, testIncomeSourceOverseasProperty}

import scala.concurrent.Future

class OverseasPropertyCommencementDateControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAgentAuthService with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    super.beforeEach()
  }

  override val controllerName: String = "OverseasPropertyCommencementDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOverseasPropertyCommencementDateController.show(isEditMode = false),
    "submit" -> TestOverseasPropertyCommencementDateController.submit(isEditMode = false)
  )

  object TestOverseasPropertyCommencementDateController extends OverseasPropertyCommencementDateController(
    mockAuthService,
    MockSubscriptionDetailsService,
    mockLanguageUtils
  )

  trait Test {
    val controller = new OverseasPropertyCommencementDateController(
      mockAuthService,
      MockSubscriptionDetailsService,
      mockLanguageUtils
    )
  }

  val incomeSourceAllTypes: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)

  val incomeSourceSelfEmployAndOverseasProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)

  val incomeSourceUkAndOverseasProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)

  val incomeSourceOverseasPropertyOnly: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)


  def foreignPropertyIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceOverseasProperty)

  def bothIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceBoth)


  "show" should {
    "display the foreign property commencement date view and return OK (200)" in new Test{
      lazy val result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockFetchAllFromSubscriptionDetails(testCacheMap(
        incomeSource = Some(incomeSourceAllTypes)
      ))

      status(result) must be(Status.OK)
      verifySubscriptionDetailsSave(OverseasPropertyCommencementDate, 0)
      verifySubscriptionDetailsFetchAll(1)
    }
  }

  "redirect to the income source page" when {
    "the user hasn't selected income sources" in new Test {
      lazy val result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockFetchAllFromSubscriptionDetails(testCacheMap(
        incomeSource = None
      ))

      status(result) must be(Status.SEE_OTHER)
      verifySubscriptionDetailsSave(OverseasPropertyCommencementDate, 0)
      verifySubscriptionDetailsFetchAll(1)
    }
  }

  "submit" should {

    val testValidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
    val testOverseasPropertyCommencementDateModel: OverseasPropertyCommencementDateModel = OverseasPropertyCommencementDateModel(testValidStartDate)

    def callSubmit(isEditMode: Boolean): Future[Result] = TestOverseasPropertyCommencementDateController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(OverseasPropertyCommencementDateForm.overseasPropertyCommencementDateForm(testValidStartDate.toString),
        testOverseasPropertyCommencementDateModel)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestOverseasPropertyCommencementDateController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {

        setupMockSubscriptionDetailsSaveFunctions()
        val goodRequest = callSubmit(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyCommencementDate, 1)
        verifySubscriptionDetailsFetchAll(1)
      }

      "redirect to foreign property accounting method page" in {

        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callSubmit(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.PropertyCommencementDateController.submit().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyCommencementDate, 1)
        verifySubscriptionDetailsFetchAll(1)
      }

    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {

        setupMockSubscriptionDetailsSaveFunctions()


        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyCommencementDate, 1)
        verifySubscriptionDetailsFetchAll(1)
      }

      "redirect to checkYourAnswer page" in {

        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callSubmit(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyCommencementDate, 1)
        verifySubscriptionDetailsFetchAll(1)

      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {

        mockFetchIncomeSourceFromSubscriptionDetails(Some(testIncomeSourceOverseasProperty))

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsSave(OverseasPropertyCommencementDate, 0)
        verifySubscriptionDetailsFetchAll(1)
      }
    }

    "The back url is not in edit mode and release four is disabled" when {
      "the user has a foreign property and it is the only income source" should {

        "redirect to income source page" in new Test {

          controller.backUrl(isEditMode = false, incomeSourceOverseasPropertyOnly) mustBe
            controllers.agent.routes.IncomeSourceController.show().url
        }
      }

      "the user has a business and a foreign property" should {
        "redirect to business accounting method page" in new Test {
          controller.backUrl(isEditMode = false, incomeSourceSelfEmployAndOverseasProperty) mustBe
            controllers.agent.business.routes.BusinessAccountingMethodController.show().url
        }
      }

      "the user has a UK and a foreign property" should {
        "redirect to business accounting method page" in new Test {

          controller.backUrl(isEditMode = false, incomeSourceUkAndOverseasProperty) mustBe
            controllers.agent.business.routes.PropertyAccountingMethodController.show().url
        }
      }

      "the user has a business, a UK and a foreign property" should {
        "redirect to property accounting method page" in new Test {

          controller.backUrl(isEditMode = false, incomeSourceAllTypes) mustBe
            controllers.agent.business.routes.PropertyAccountingMethodController.show().url
        }
      }

    }

    "The back url is not in edit mode and release four is enabled" when {
      "the user has a foreign property and it is the only income source" should {
        "redirect to income source page" in new Test {
          enable(ReleaseFour)
          controller.backUrl(isEditMode = false, incomeSourceOverseasPropertyOnly) mustBe
            controllers.agent.business.routes.WhatYearToSignUpController.show().url
        }
      }

      "the user has a business and a foreign property" should {
        "redirect to business accounting method page" in new Test {
          enable(ReleaseFour)
          controller.backUrl(isEditMode = false, incomeSourceSelfEmployAndOverseasProperty) mustBe
            "/report-quarterly/income-and-expenses/sign-up/self-employments/details/business-accounting-method"
        }
      }

      "the user has a UK and a foreign property" should {
        "redirect to business accounting method page" in new Test {
          enable(ReleaseFour)
          controller.backUrl(isEditMode = false, incomeSourceUkAndOverseasProperty) mustBe
            controllers.agent.business.routes.PropertyAccountingMethodController.show().url
        }
      }

      "the user has a business, a UK and a foreign property" should {
        "redirect to business accounting method page" in new Test {
          enable(ReleaseFour)
          controller.backUrl(isEditMode = false, incomeSourceAllTypes) mustBe
            controllers.agent.business.routes.PropertyAccountingMethodController.show().url
        }
      }

    }
    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to check your answer page" in new Test {
          controller.backUrl(isEditMode = true, incomeSourceOverseasPropertyOnly) mustBe
            controllers.agent.routes.CheckYourAnswersController.show().url
        }
      }
    }
  }

}