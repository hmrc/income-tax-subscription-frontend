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

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
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
  with MockSubscriptionDetailsService with MockAuditingService with FeatureSwitching {

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
    "display the property start date view and return OK (200)" in withController { controller =>
      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockFetchAllFromSubscriptionDetails(testCacheMap(
        incomeSource = Some(incomeSourceBoth)
      ))
      mockFetchProperty(None)

      status(result) must be(Status.OK)
    }
  }

  "submit" should {

    val testValidMaxStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
    val testValidMinStartDate: DateModel = DateModel.dateConvert(LocalDate.of(1900, 1, 1))

    val testPropertyStartDateModel: DateModel = testValidMaxStartDate

    def callSubmit(controller: PropertyStartDateController, isEditMode: Boolean): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequest.post(
          PropertyStartDateForm.propertyStartDateForm(testValidMinStartDate.toString, testValidMaxStartDate.toString),
          testPropertyStartDateModel
        )
      )

    def callSubmitWithErrorForm(controller: PropertyStartDateController, isEditMode: Boolean): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequest
      )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303) to the property accounting method page" in withController { controller =>
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchProperty(None)

        val goodRequest = callSubmit(controller, isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.PropertyAccountingMethodController.show().url)

        await(goodRequest)
        verifyPropertySave(PropertyModel(startDate = Some(testValidMaxStartDate)))
      }

    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303) to the check your answers page" in withController { controller =>
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchProperty(testFullPropertyModel)

        val goodRequest = callSubmit(controller, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show.url)

        await(goodRequest)
        verifyPropertySave(testFullPropertyModel.copy(startDate = Some(testValidMaxStartDate), confirmed = false))
      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in withController { controller =>

        mockFetchAllFromSubscriptionDetails(propertyOnlyIncomeSourceType)

        val badRequest = callSubmitWithErrorForm(controller, isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsFetchAll(1)
      }
    }

    "The back url is not in edit mode" when {
      "the user has rental property and it is the only income source" should {
        "redirect to income source page" in withController { controller =>
          controller.backUrl(isEditMode = false, incomeSourcePropertyOnly) mustBe
            controllers.agent.routes.IncomeSourceController.show().url
        }
      }

      "the user has rental property and has a business" should {
        "redirect to Business Accounting Method page" in withController { controller =>
          enable(ReleaseFour)
          controller.backUrl(isEditMode = false, incomeSourceBoth) mustBe
            appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/client/details/business-accounting-method"
        }
      }
    }

    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to check your answer page" in withController { controller =>
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
