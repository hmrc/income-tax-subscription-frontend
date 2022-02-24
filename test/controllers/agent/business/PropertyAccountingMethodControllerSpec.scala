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
import config.featureswitch._
import controllers.agent.AgentControllerBaseSpec
import forms.agent.AccountingMethodPropertyForm
import models.Cash
import models.common.{IncomeSourceModel, PropertyModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks.{MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.agent.TestModels._
import views.html.agent.business.PropertyAccountingMethod

import scala.concurrent.Future

class PropertyAccountingMethodControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService with MockIncomeTaxSubscriptionConnector with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  override val controllerName: String = "PropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  val propertyAccountingMethodView = mock[PropertyAccountingMethod]

  private def withController(testCode: PropertyAccountingMethodController => Any): Unit = {
    val propertyAccountingMethodView = mock[PropertyAccountingMethod]

    when(propertyAccountingMethodView(any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new PropertyAccountingMethodController(
      propertyAccountingMethodView,
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService
    )

    testCode(controller)
  }

  def propertyOnlyIncomeSourceType: CacheMap = testCacheMap(incomeSource = Some(testIncomeSourceProperty))

  def bothPropertyAndBusinessIncomeSource: CacheMap = testCacheMap(
    incomeSource = Some(testIncomeSourceBusinessAndUkProperty)
  )

  def cacheMap(incomeSource: Option[IncomeSourceModel] = None): CacheMap = testCacheMap(
    incomeSource = incomeSource
  )

  "show" when {
    "there is no previous selected answer" should {
      "display the property accounting method view and return OK (200)" in withController { controller =>
        lazy val result = await(controller.show(isEditMode = false)(subscriptionRequest))

        mockFetchAllFromSubscriptionDetails(cacheMap(incomeSource = Some(testIncomeSourceProperty)))
        mockFetchProperty(None)

        status(result) must be(Status.OK)
      }
    }

    "there is a previous selected answer CASH" should {
      "display the property accounting method view with the previous selected answer CASH and return OK (200)" in withController { controller =>
        lazy val result = await(controller.show(isEditMode = false)(subscriptionRequest))

        mockFetchAllFromSubscriptionDetails(cacheMap(
          incomeSource = Some(testIncomeSourceProperty)
        ))
        mockFetchProperty(PropertyModel(Cash))

        status(result) must be(Status.OK)
      }
    }
  }

  "submit" should withController { controller =>

    def callSubmit(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Cash)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "save and retrieve is enabled" should {
      "redirect to agent uk property check your answers page" in {
        enable(SaveAndRetrieve)
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchAllFromSubscriptionDetails(cacheMap(incomeSource = Some(testIncomeSourceProperty)))
        mockFetchProperty(None)

        val goodRequest: Future[Result] = callSubmit(isEditMode = false)

        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.PropertyCheckYourAnswersController.show(false).url)

        await(goodRequest)
        verifyPropertySave(PropertyModel(accountingMethod = Some(Cash)))
      }
    }

    "save and retrieve is disabled" should {
      "the user doesn't have foreign property" should {
        "redirect to agent Check Your Answer page" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(cacheMap(incomeSource = Some(testIncomeSourceProperty)))
          mockFetchProperty(None)

          val goodRequest: Future[Result] = callSubmit(isEditMode = false)

          status(goodRequest) mustBe Status.SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show.url)

          await(goodRequest)
          verifyPropertySave(PropertyModel(accountingMethod = Some(Cash)))
        }
      }
      "the user has foreign property" should {
        "redirect to the agent overseas property commencement date" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(cacheMap(incomeSource = Some(testIncomeSourceProperty.copy(foreignProperty = true))))
          mockFetchProperty(None)


          val goodRequest: Future[Result] = callSubmit(isEditMode = false)

          status(goodRequest) mustBe Status.SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.OverseasPropertyStartDateController.show().url)

          await(goodRequest)
          verifyPropertySave(PropertyModel(accountingMethod = Some(Cash)))
        }
      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {
        mockFetchAllFromSubscriptionDetails(cacheMap(incomeSource = Some(testIncomeSourceProperty.copy(foreignProperty = true))))

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)

        verifyPropertySave(None)
      }
    }
  }

  "The back url" when {
    "in edit mode" should {
      "save and retrieve is enabled" should {
        "redirect to the agent uk property check your answers" in withController { controller =>
          enable(SaveAndRetrieve)

          controller.backUrl(
            isEditMode = true
          ) mustBe controllers.agent.business.routes.PropertyCheckYourAnswersController.show(true).url
        }
      }

      "save and retrieve is disabled" should {
        "redirect to the agent check your answers" in withController { controller =>
          controller.backUrl(
            isEditMode = true
          ) mustBe controllers.agent.routes.CheckYourAnswersController.show.url
        }
      }
    }

    "not in edit mode" should {
      "save and retrieve is enabled" should {
        "redirect to the uk property commencement date" in withController { controller =>
          enable(SaveAndRetrieve)
          controller.backUrl(
            isEditMode = false
          ) mustBe controllers.agent.business.routes.PropertyStartDateController.show().url
        }
      }

      "save and retrieve is disabled" should {
        "redirect to the uk property commencement date" in withController { controller =>
          controller.backUrl(
            isEditMode = false
          ) mustBe controllers.agent.business.routes.PropertyStartDateController.show().url
        }
      }
    }
  }
}
