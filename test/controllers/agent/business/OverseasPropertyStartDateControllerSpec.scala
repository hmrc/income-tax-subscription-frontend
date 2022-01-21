/*
 * Copyright 2022 HM Revenue & Customs
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

import agent.audit.mocks.{MockAuditingService, MockOverseasPropertyStartDate}
import config.featureswitch.FeatureSwitch.{ReleaseFour, SaveAndRetrieve}
import config.featureswitch.FeatureSwitching
import controllers.agent.AgentControllerBaseSpec
import forms.agent.OverseasPropertyStartDateForm
import models.DateModel
import models.common.{IncomeSourceModel, OverseasPropertyModel}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.agent.mocks.MockAgentAuthService
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.OverseasPropertyStartDate
import utilities.TestModels.{testAccountingMethodProperty, testCacheMap, testIncomeSourceBoth, testIncomeSourceOverseasProperty, testPropertyStartDateModel}

import java.time.LocalDate
import scala.concurrent.Future

class OverseasPropertyStartDateControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAgentAuthService with MockAuditingService with MockOverseasPropertyStartDate with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  override val controllerName: String = "OverseasPropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOverseasPropertyStartDateController$.show(isEditMode = false),
    "submit" -> TestOverseasPropertyStartDateController$.submit(isEditMode = false)
  )

  object TestOverseasPropertyStartDateController$ extends OverseasPropertyStartDateController(
    mockAuditingService,
    mockOverseasPropertyStartDate,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockLanguageUtils
  )

  trait Test {
    val controller = new OverseasPropertyStartDateController(
      mockAuditingService,
      mockOverseasPropertyStartDate,
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
    "display the foreign property start date view and return OK (200)" in new Test {
      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockFetchAllFromSubscriptionDetails(testCacheMap(
        incomeSource = Some(incomeSourceAllTypes)
      ))
      mockFetchOverseasProperty(Some(OverseasPropertyModel(
        accountingMethod = Some(testAccountingMethodProperty.propertyAccountingMethod),
        startDate = Some(testPropertyStartDateModel.startDate)
      )))

      status(result) must be(Status.OK)
      verifyOverseasPropertySave(None)
    }
  }

  "redirect to the income source page" when {
    "the user hasn't selected income sources" in new Test {
      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockFetchAllFromSubscriptionDetails(testCacheMap(
        incomeSource = None
      ))
      mockFetchOverseasProperty(Some(OverseasPropertyModel(
        accountingMethod = Some(testAccountingMethodProperty.propertyAccountingMethod),
        startDate = Some(testPropertyStartDateModel.startDate)
      )))

      status(result) must be(Status.SEE_OTHER)
      verifyOverseasPropertySave(None)
    }
  }

  "submit" should {

    val testValidMaxStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
    val testValidMinStartDate: DateModel = DateModel.dateConvert(LocalDate.of(1900, 1, 1))

    def callSubmit(isEditMode: Boolean): Future[Result] = TestOverseasPropertyStartDateController$.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(OverseasPropertyStartDateForm.overseasPropertyStartDateForm(testValidMinStartDate.toString, testValidMaxStartDate.toString),
        testValidMaxStartDate)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestOverseasPropertyStartDateController$.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" should {
      "redirect to agent foreign property accounting method page" in {

        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchOverseasProperty(Some(OverseasPropertyModel()))

        val goodRequest = callSubmit(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.OverseasPropertyAccountingMethodController.show().url)

        await(goodRequest)
        verifyOverseasPropertySave(Some(OverseasPropertyModel(startDate = Some(testValidMaxStartDate))))
      }
    }

    "When it is in edit mode" should {
      "save and retrieve is disabled" should {
        "redirect to agent final check your answers page" in {

          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchOverseasProperty(Some(OverseasPropertyModel()))

          val goodRequest = callSubmit(isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show.url)

          await(goodRequest)
          verifyOverseasPropertySave(Some(OverseasPropertyModel(startDate = Some(testValidMaxStartDate))))
        }
      }

      "save and retrieve is enabled" should {
        "redirect to agent overseas check your answers page" in {
          enable(SaveAndRetrieve)
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchOverseasProperty(Some(OverseasPropertyModel()))

          val goodRequest = callSubmit(isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(true).url)

          await(goodRequest)
          verifyOverseasPropertySave(Some(OverseasPropertyModel(startDate = Some(testValidMaxStartDate))))
        }
      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {

        mockFetchIncomeSourceFromSubscriptionDetails(Some(testIncomeSourceOverseasProperty))

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsSave(OverseasPropertyStartDate, 0)
        verifySubscriptionDetailsFetchAll(1)
      }
    }

    "The back url is not in edit mode" when {
      "save and retrieve is disabled" when {
        "the user has a foreign property and it is the only income source" should {
          "redirect to agent income source page" in new Test {

            controller.backUrl(isEditMode = false, incomeSourceOverseasPropertyOnly) mustBe
              controllers.agent.routes.IncomeSourceController.show().url
          }
        }

        "the user has a UK and a foreign property" should {
          "redirect to agent UK business accounting method page" in new Test {

            controller.backUrl(isEditMode = false, incomeSourceUkAndOverseasProperty) mustBe
              controllers.agent.business.routes.PropertyAccountingMethodController.show().url
          }
        }

        "the user has a self-employment a foreign property income sources" should {
          "redirect to agent sole trader accounting method page" in new Test {

            val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)
            controller.backUrl(isEditMode = false, maybeIncomeSourceModel = Some(incomeSourceModel)) mustBe
              appConfig.incomeTaxSelfEmploymentsFrontendUrl + "client/details/business-accounting-method"
          }
        }

      }

      "save and retrieve is enabled" when {
        //need to change to WhatIncomeSourceToSignUpController when it has been built
        "redirect to agent income source page" in new Test {
          enable(SaveAndRetrieve)
          controller.backUrl(isEditMode = false, incomeSourceUkAndOverseasProperty) mustBe
            controllers.agent.routes.IncomeSourceController.show().url
        }
      }
    }

    "The back url is in edit mode" when {
      "save and retrieve is enabled" when {
        "the user click back url" should {
          "redirect to agent overseas property check your answer page" in new Test {
            enable(SaveAndRetrieve)
            controller.backUrl(isEditMode = true, incomeSourceOverseasPropertyOnly) mustBe
              controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(true).url
          }
        }
      }

      "save and retrieve is disabled" when {
        "the user click back url" should {
          "redirect to agent check your answer page" in new Test {
            controller.backUrl(isEditMode = true, incomeSourceOverseasPropertyOnly) mustBe
              controllers.agent.routes.CheckYourAnswersController.show.url
          }
        }
      }
    }
  }

}
