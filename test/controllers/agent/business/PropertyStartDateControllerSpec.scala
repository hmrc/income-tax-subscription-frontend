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

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import controllers.agent.AgentControllerBaseSpec
import forms.agent.PropertyStartDateForm
import models.DateModel
import models.common.{IncomeSourceModel, PropertyModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{await, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.TestModels.{testCacheMap, testFullPropertyModel, testIncomeSourceBoth, testIncomeSourceProperty}
import views.html.agent.business.PropertyStartDate

import java.time.LocalDate
import scala.concurrent.Future

class PropertyStartDateControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService  {

  override val controllerName: String = "PropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestPropertyStartDateController$.show(isEditMode = false),
    "submit" -> TestPropertyStartDateController$.submit(isEditMode = false)
  )

  object TestPropertyStartDateController$ extends PropertyStartDateController(
    mock[PropertyStartDate],
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockLanguageUtils
  )

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  val incomeSourcePropertyOnly: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true,
    foreignProperty = false)

  val incomeSourceBoth: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
    foreignProperty = false)

  def propertyOnlyIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceProperty)

  def bothIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceBoth)


  "show" when {
    "there is income source details" should {
      "display the property start date view and return OK (200)" in withController { controller =>
        lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

        mockFetchAllFromSubscriptionDetails(testCacheMap(
          incomeSource = Some(incomeSourceBoth)
        ))
        mockFetchProperty(None)

        status(result) must be(Status.OK)
      }
    }

    "the Save and Retrieve feature is enabled" should {
      "display the property start date view and return OK (200)" in withController { controller =>
        enable(SaveAndRetrieve)
        lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

        mockFetchAllFromSubscriptionDetails(testCacheMap(
          incomeSource = None
        ))
        mockFetchProperty(None)

        status(result) must be(Status.OK)
      }
    }

    "there is noo income source details" should {
      "redirect to income source page" in withController { controller =>
        lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

        mockFetchAllFromSubscriptionDetails(testCacheMap(
          incomeSource = None
        ))
        mockFetchProperty(None)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.routes.IncomeSourceController.show().url)
      }
    }
  }

  "submit" should {

    val testValidMaxStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
    val testValidMinStartDate: DateModel = DateModel.dateConvert(LocalDate.of(1900, 1, 1))

    val testPropertyStartDateModel: DateModel = testValidMaxStartDate

    def callSubmit(controller: PropertyStartDateController, isEditMode: Boolean): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequest.post(
          PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString),
          testPropertyStartDateModel
        )
      )

    def callSubmitWithErrorForm(controller: PropertyStartDateController, isEditMode: Boolean): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequest
      )

    "When it is not in edit mode" when {
      "save and retrieve is disabled" should {
        "redirect to agent uk property accounting method page" in withController { controller =>
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchProperty(None)
          val goodRequest = callSubmit(controller, isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          await(goodRequest)
          redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.PropertyAccountingMethodController.show().url)

          verifyPropertySave(PropertyModel(startDate = Some(testValidMaxStartDate)))
        }

      }

      "save and retrieve is enabled" should {
        "redirect to agent uk property accounting method page" in withController { controller =>
          enable(SaveAndRetrieve)
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchProperty(None)

          val goodRequest = callSubmit(controller, isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          await(goodRequest)
          redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.PropertyAccountingMethodController.show().url)

          verifyPropertySave(PropertyModel(startDate = Some(testValidMaxStartDate)))

        }
      }
    }

    "When it is in edit mode" when {
      "save and retrieve is disabled" should {
        "redirect to agent final check your answer page" in withController { controller =>
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchProperty(testFullPropertyModel)
          val goodRequest = callSubmit(controller, isEditMode = true)
          await(goodRequest)
          redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show.url)


          verifyPropertySave(testFullPropertyModel.copy(startDate = Some(testValidMaxStartDate), confirmed = false))
        }
      }

      "save and retrieve is enabled" should {
        "redirect to agent uk property check your answers page" in withController { controller =>
          enable(SaveAndRetrieve)
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchProperty(testFullPropertyModel)
          val goodRequest = callSubmit(controller, isEditMode = true)
          await(goodRequest)
          redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.PropertyCheckYourAnswersController.show(true).url)

          verifyPropertySave(testFullPropertyModel.copy(startDate = Some(testValidMaxStartDate), confirmed = false))

        }
      }
    }

    "when there is an invalid submission with an error form" should {
      "redirect back to agent what income source page when incomeSource is missing" in withController { controller =>
        mockFetchAllFromSubscriptionDetails(testCacheMap(
          incomeSource = None
        ))

        val badRequest = callSubmitWithErrorForm(controller, isEditMode = false)

        await(badRequest)
        redirectLocation(badRequest) mustBe Some(controllers.agent.routes.IncomeSourceController.show().url)
      }

      "return bad request status (400) when save and retrieve is enabled" in withController { controller =>
        enable(SaveAndRetrieve)

        val badRequest = callSubmitWithErrorForm(controller, isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
      }

      "return bad request status (400)" in withController { controller =>

        mockFetchAllFromSubscriptionDetails(propertyOnlyIncomeSourceType)

        val badRequest = callSubmitWithErrorForm(controller, isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsFetchAll(1)
      }
    }

    "The back url is not in edit mode" when {
      "save and retrieve is enabled" should {
        "redirect back to agent what income source page" in withController { controller =>
          enable(SaveAndRetrieve)
          controller.backUrl(isEditMode = false, incomeSourcePropertyOnly) mustBe
            controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
        }
      }

      "save and retrieve is disabled" when {
        "there is at least one self-employed income source" should {
          "redirect back to agent business accounting method page" in withController { controller =>
            controller.backUrl(isEditMode = false, incomeSourceBoth) mustBe
              appConfig.incomeTaxSelfEmploymentsFrontendUrl + "client/details/business-accounting-method"
          }
        }

        "there is no self-employed income source" should {
          "redirect back to agent income source page" in withController { controller =>
            controller.backUrl(isEditMode = false, incomeSourcePropertyOnly) mustBe
              controllers.agent.routes.IncomeSourceController.show().url
          }
        }
      }
    }


    "The back url is in edit mode" when {
      "save and retrieve is enabled" should {
        "redirect back to agent uk property check your answers page" in withController { controller =>
          enable(SaveAndRetrieve)
          controller.backUrl(isEditMode = true, incomeSourcePropertyOnly) mustBe
            controllers.agent.business.routes.PropertyCheckYourAnswersController.show(true).url
        }
      }

      "save and retrieve is disabled" should {
        "redirect back to final check your answers page" in withController { controller =>
          controller.backUrl(isEditMode = true, incomeSourcePropertyOnly) mustBe
            controllers.agent.routes.CheckYourAnswersController.show.url
        }
      }
    }
  }

  private def withController(testCode: PropertyStartDateController => Any): Unit = {
    val mockView = mock[PropertyStartDate]

    when(mockView(any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new PropertyStartDateController(
      mockView,
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockLanguageUtils
    )

    testCode(controller)
  }
}
