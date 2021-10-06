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
import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import forms.individual.business.AccountingMethodOverseasPropertyForm
import models.Cash
import models.common.{IncomeSourceModel, OverseasAccountingMethodPropertyModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.OverseasPropertyAccountingMethod
import utilities.TestModels._
import views.html.individual.incometax.business.{OverseasPropertyAccountingMethod, PropertyAccountingMethod}

import scala.concurrent.Future

class OverseasPropertyAccountingMethodControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    super.beforeEach()
  }

  override val controllerName: String = "ForeignPropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private def withController(testCode: OverseasPropertyAccountingMethodController => Any): Unit = {
    val overseasPropertyAccountingMethodView = mock[OverseasPropertyAccountingMethod]

    when(overseasPropertyAccountingMethodView(any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new OverseasPropertyAccountingMethodController(
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      overseasPropertyAccountingMethodView
    )

    testCode(controller)
  }

  val incomeSourceAllTypes: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)

  val incomeSourceSelfEmployAndForeignProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)

  val incomeSourceUkAndForeignProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)

  val incomeSourceForeignPropertyOnly: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)

  def overseasPropertyIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceOverseasProperty)

  def bothIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceBoth)

  "show" should {
    "display the foreign property accounting method view and return OK (200)" in withController { controller =>
      lazy val result = await(controller.show(isEditMode = false)(subscriptionRequest))


      mockFetchForeignPropertyAccountingFromSubscriptionDetails(None)
      mockFetchAllFromSubscriptionDetails(overseasPropertyIncomeSourceType)

      status(result) must be(Status.OK)
      verifySubscriptionDetailsSave(OverseasPropertyAccountingMethod, 0)
      verifySubscriptionDetailsFetchAll(1)
    }
  }

  "submit" should withController { controller =>

    def callShow(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm, OverseasAccountingMethodPropertyModel(Cash))
    )

    def callShowWithErrorForm(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockSubscriptionDetailsSaveFunctions()
        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyAccountingMethod, 1)
        verifySubscriptionDetailsFetchAll(1)
      }

      "redirect to checkYourAnswer page" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyAccountingMethod, 1)
        verifySubscriptionDetailsFetchAll(1)
      }

    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyAccountingMethod, 1)
        verifySubscriptionDetailsFetchAll(1)
      }

      "redirect to checkYourAnswer page" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyAccountingMethod, 1)
        verifySubscriptionDetailsFetchAll(1)

      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {

        mockFetchAllFromSubscriptionDetails(overseasPropertyIncomeSourceType)

        val badRequest = callShowWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsSave(OverseasPropertyAccountingMethod, 0)
        verifySubscriptionDetailsFetchAll(0)
      }
    }

    "The back url is not in edit mode" when {
      "the user has foreign property and it is the only income source" should {
        "redirect to overseas property start date page" in withController { controller =>
          mockFetchAllFromSubscriptionDetails(overseasPropertyIncomeSourceType)
          controller.backUrl(isEditMode = false) mustBe
            controllers.individual.business.routes.OverseasPropertyStartDateController.show().url
        }
      }

    }
    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to check your answer page" in withController { controller =>
          setupMockSubscriptionDetailsSaveFunctions()
          controller.backUrl(isEditMode = true) mustBe
            controllers.individual.subscription.routes.CheckYourAnswersController.show().url
        }
      }
    }

  }

}
