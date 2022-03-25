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

package controllers.individual.business

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import controllers.ControllerBaseSpec
import forms.individual.business.OverseasPropertyStartDateForm
import models.DateModel
import models.common.{IncomeSourceModel, OverseasPropertyModel}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.individual.mocks.MockAuthService
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.OverseasPropertyStartDate
import utilities.TestModels.{testCacheMap, testIncomeSourceBoth, testIncomeSourceOverseasProperty}
import views.individual.mocks.MockOverseasPropertyStartDate

import java.time.LocalDate
import scala.concurrent.Future

class OverseasPropertyStartDateControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuthService with MockAuditingService  with MockOverseasPropertyStartDate {

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  override val controllerName: String = "OverseasPropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOverseasPropertyStartDateController.show(isEditMode = false),
    "submit" -> TestOverseasPropertyStartDateController.submit(isEditMode = false)
  )

  object TestOverseasPropertyStartDateController extends OverseasPropertyStartDateController(
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockLanguageUtils,
    overseasPropertyStartDate
  )

  val incomeSourceAllTypes: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)

  val incomeSourceSelfEmployAndOverseasProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)

  val incomeSourceUkAndOverseasProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)

  val incomeSourceOverseasPropertyOnly: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)

  def foreignPropertyIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceOverseasProperty)

  def bothIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceBoth)


  "show" should {
    "display the foreign property start date view and return OK (200)" in withController { controller =>
      disable(SaveAndRetrieve)
      mockOverseasPropertyStartDateView()
      mockFetchOverseasProperty(Some(OverseasPropertyModel(startDate = Some(DateModel("22", "11", "2021")))))
      mockIndividualWithNoEnrolments()
      mockFetchAllFromSubscriptionDetails(testCacheMap(
        incomeSource = Some(incomeSourceAllTypes)
      ))

      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      status(result) must be(Status.OK)
      verifyOverseasPropertySave(None)
      verifySubscriptionDetailsFetchAll(2)
    }

    "display the foreign property start date view and return OK (200) when Save & Retrieve feature is enabled" in withController { controller =>
      enable(SaveAndRetrieve)
      mockOverseasPropertyStartDateView()
      mockFetchOverseasProperty(Some(OverseasPropertyModel(startDate = Some(DateModel("22", "11", "2021")))))

      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockIndividualWithNoEnrolments()
      mockFetchAllFromSubscriptionDetails(testCacheMap())

      status(result) must be(Status.OK)
      verifyOverseasPropertySave(None)
      verifySubscriptionDetailsFetchAll(1)
    }
  }

  "submit" should {

    val testValidMaxStartDate: DateModel = DateModel.dateConvert( LocalDate.now.minusYears(1))

    def callPost(controller: OverseasPropertyStartDateController, isEditMode: Boolean): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequest.post(OverseasPropertyStartDateForm.overseasPropertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString),
          testValidMaxStartDate)
      )

    def callPostWithErrorForm(controller: OverseasPropertyStartDateController, isEditMode: Boolean): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequest
      )

    "When it is not in edit mode" when {
      "save and retrieve is enabled" should {
        "redirect to foreign property accounting method page" in withController { controller =>
          mockIndividualWithNoEnrolments()
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchOverseasProperty(Some(OverseasPropertyModel(startDate = Some(DateModel("22", "11", "2021")))))

          val goodRequest = callPost(controller, isEditMode = false)

          redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show().url)

          await(goodRequest)
          verifyOverseasPropertySave(Some(OverseasPropertyModel(startDate = Some(testValidMaxStartDate))))
        }
      }

      "save and retrieve is disabled" should {
        "redirect to foreign property accounting method page" in withController { controller =>
          mockIndividualWithNoEnrolments()
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchOverseasProperty(Some(OverseasPropertyModel(startDate = Some(DateModel("22", "11", "2021")))))
          val goodRequest = callPost(controller, isEditMode = false)

          redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show().url)

          await(goodRequest)
          verifyOverseasPropertySave(Some(OverseasPropertyModel(startDate = Some(testValidMaxStartDate))))
        }
      }
    }

    "When it is in edit mode" when {
      "save and retrieve is disabled" should {
        "redirect to checkYourAnswer page" in withController { controller =>
          disable(SaveAndRetrieve)
          mockIndividualWithNoEnrolments()
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchOverseasProperty(Some(OverseasPropertyModel(startDate = Some(DateModel("22", "11", "2021")))))

          val goodRequest = callPost(controller, isEditMode = true)

          redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show.url)

          await(goodRequest)
          verifyOverseasPropertySave(Some(OverseasPropertyModel(startDate = Some(testValidMaxStartDate))))

        }
      }

      "save and retrieve is enabled" should {
        "redirect to overseas property check your answers page" in withController { controller =>
          enable(SaveAndRetrieve)
          mockIndividualWithNoEnrolments()
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchOverseasProperty(Some(OverseasPropertyModel(startDate = Some(DateModel("22", "11", "2021")))))
          val goodRequest = callPost(controller, isEditMode = true)

          redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.show(true).url)

          await(goodRequest)
          verifyOverseasPropertySave(Some(OverseasPropertyModel(startDate = Some(testValidMaxStartDate))))
        }
      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in withController { controller =>
        mockOverseasPropertyStartDateView()
        mockIndividualWithNoEnrolments()
        mockFetchIndividualIncomeSourceFromSubscriptionDetails(Some(testIncomeSourceOverseasProperty))

        val badRequest = callPostWithErrorForm(controller, isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyOverseasPropertySave(None)
      }

      "return bad request status (400) when Save & Retrieve feature is enabled" in withController { controller =>
        enable(SaveAndRetrieve)
        mockOverseasPropertyStartDateView()

        val badRequest = callPostWithErrorForm(controller, isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsSave(OverseasPropertyStartDate, 0)
        verifySubscriptionDetailsFetchAll(0)
      }
    }


    "backUrl" when {
      "in edit mode" when {
        "save and retrieve is disabled" should {
          "redirect to the final check your answers page" in withController { controller =>
            val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)
            controller.backUrl(isEditMode = true, maybeIncomeSourceModel = Some(incomeSourceModel)) mustBe
              controllers.individual.subscription.routes.CheckYourAnswersController.show.url
          }
        }

        "save and retrieve is enabled" should {
          "redirect to overseas property check your answers page" in withController { controller =>
            enable(SaveAndRetrieve)
            controller.backUrl(isEditMode = true, maybeIncomeSourceModel = None) mustBe
              controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.show(true).url
          }
        }
      }

      "not in edit mode" when {
        "save and retrieve is disabled" when {
          "the user has uk property income" should {
            " redirect to uk property accounting method page" in withController { controller =>
              val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)
              controller.backUrl(isEditMode = false, maybeIncomeSourceModel = Some(incomeSourceModel)) mustBe
                controllers.individual.business.routes.PropertyAccountingMethodController.show().url
            }
          }

          "the user has self employment income but no uk property income" should {
            "redirect to self employment accounting method page" in withController { controller =>
              val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)
              controller.backUrl(isEditMode = false, maybeIncomeSourceModel = Some(incomeSourceModel)) mustBe
                appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/details/business-accounting-method"
            }
          }

          "the user has no self employment or uk property income" should {
            "redirect to income source page" in withController { controller =>
              val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)
              controller.backUrl(isEditMode = false, maybeIncomeSourceModel = Some(incomeSourceModel)) mustBe
                controllers.individual.incomesource.routes.IncomeSourceController.show().url
            }
          }
        }

        "save and retrieve is enabled" should {
          "redirect to what income source to sign up page" in withController { controller =>
            enable(SaveAndRetrieve)
            controller.backUrl(isEditMode = false, maybeIncomeSourceModel = None) mustBe
              controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show().url
          }
        }
      }
    }
  }

  private def withController(testCode: OverseasPropertyStartDateController => Any) = {
    val controller = new OverseasPropertyStartDateController(
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockLanguageUtils,
      overseasPropertyStartDate
    )

    testCode(controller)
  }

}
