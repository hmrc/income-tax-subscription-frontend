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
import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch._
import controllers.ControllerBaseSpec
import forms.individual.business.AccountingMethodPropertyForm
import models.Cash
import models.common.AccountingMethodPropertyModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.PropertyAccountingMethod
import utilities.TestModels._
import views.html.individual.incometax.business.PropertyAccountingMethod

import scala.concurrent.Future

class PropertyAccountingMethodControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService with FeatureSwitching {

  override val controllerName: String = "PropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private def withController(testCode: PropertyAccountingMethodController => Any): Unit = {
    val propertyAccountingMethodView = mock[PropertyAccountingMethod]

    when(propertyAccountingMethodView(any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new PropertyAccountingMethodController(
      mockAuditingService,
      propertyAccountingMethodView,
      mockAuthService,
      MockSubscriptionDetailsService
    )

    testCode(controller)
  }

  def propertyOnlyIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceProperty)

  def bothIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceBoth)

  "show" should {
    "display the property accounting method view and return OK (200)" in withController { controller =>
      lazy val result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockFetchPropertyAccountingFromSubscriptionDetails(None)
      mockFetchAllFromSubscriptionDetails(propertyOnlyIncomeSourceType) // for the back url

      status(result) must be(Status.OK)
      verifySubscriptionDetailsSave(PropertyAccountingMethod, 0)
      verifySubscriptionDetailsFetchAll(2)

    }
  }

  "submit" should withController { controller =>

    def callShow(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, AccountingMethodPropertyModel(Cash))
    )

    def callShowWithErrorForm(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in withController { controller =>
        setupMockSubscriptionDetailsSaveFunctions()
        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsSave(PropertyAccountingMethod, 1)
        verifySubscriptionDetailsFetchAll(2)
      }

      "redirect to checkYourAnswer page" in withController { controller =>
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(PropertyAccountingMethod, 1)
        verifySubscriptionDetailsFetchAll(2)
      }

    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in withController { controller =>
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsSave(PropertyAccountingMethod, 1)
        verifySubscriptionDetailsFetchAll(1)
      }

      "redirect to checkYourAnswer page" in withController { controller =>
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(PropertyAccountingMethod, 1)
        verifySubscriptionDetailsFetchAll(1)

      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in withController { controller =>

        mockFetchAllFromSubscriptionDetails(propertyOnlyIncomeSourceType)

        val badRequest = callShowWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsSave(PropertyAccountingMethod, 0)
        verifySubscriptionDetailsFetchAll(1)
      }
    }

    "The back url is not in edit mode" when {
      "the user has rental property and it is the only income source" should {
        "redirect to income source page" in withController { controller =>
          mockFetchAllFromSubscriptionDetails(propertyOnlyIncomeSourceType)
          await(controller.backUrl(isEditMode = false)) mustBe
            controllers.individual.incomesource.routes.IncomeSourceController.show().url
        }
      }

      "the user has rental property and it is not the only income source and the user has a business" should {
        "redirect to business accounting method page" in withController { controller =>
          mockFetchAllFromSubscriptionDetails(bothIncomeSourceType)
          await(controller.backUrl(isEditMode = false)) mustBe
            controllers.individual.business.routes.BusinessAccountingMethodController.show().url
        }
      }

    }
    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to check your answer page" in withController { controller =>
          setupMockSubscriptionDetailsSaveFunctions()
          await(controller.backUrl(isEditMode = true)) mustBe
            controllers.individual.subscription.routes.CheckYourAnswersController.show().url
        }
      }
    }
    "The back URL with Release Four enabled" when {
      "the user clicks the back url" should {
        "redirect to the Property Start Date page" in withController { controller =>
          enable(ReleaseFour)
          mockFetchAllFromSubscriptionDetails(bothIncomeSourceType)
          await(controller.backUrl(isEditMode = false)) mustBe
            controllers.individual.business.routes.PropertyStartDateController.show().url
        }
      }
    }
  }

}
