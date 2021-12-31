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
import controllers.agent.AgentControllerBaseSpec
import forms.agent.AccountingMethodOverseasPropertyForm
import models.Cash
import models.common.OverseasPropertyModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.OverseasPropertyAccountingMethod
import utilities.agent.TestModels._
import views.agent.mocks.MockOverseasPropertyAccountingMethod

import scala.concurrent.Future

class OverseasPropertyAccountingMethodControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService with MockOverseasPropertyAccountingMethod {

  override val controllerName: String = "OverseasPropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOverseasPropertyAccountingMethodController.show(isEditMode = false),
    "submit" -> TestOverseasPropertyAccountingMethodController.submit(isEditMode = false)
  )

  object TestOverseasPropertyAccountingMethodController extends OverseasPropertyAccountingMethodController(
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    overseasPropertyAccountingMethod
  )

  def overseasPropertyOnlyIncomeSourceType: CacheMap = testCacheMap(incomeSource = Some(testIncomeSourceOverseasProperty))

  "show" when {
    "there is no previously selected accounting method" should {
      "display the overseas property accounting method view and return OK (200)" in {
        lazy val result = await(TestOverseasPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequest))
        mockFetchOverseasProperty(None)
        mockFetchAllFromSubscriptionDetails(overseasPropertyOnlyIncomeSourceType)
        mockOverseasPropertyAccountingMethod()

        status(result) must be(Status.OK)
        verifyOverseasPropertySave(None)
      }
    }
    "there is a previously selected answer of CASH" should {
      "display the overseas property accounting method view and return OK (200)" in {
        lazy val result = await(TestOverseasPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequest))

        mockFetchOverseasProperty(Some(OverseasPropertyModel(
          accountingMethod = Some(Cash)
        )))
        mockFetchAllFromSubscriptionDetails(overseasPropertyOnlyIncomeSourceType)
        mockOverseasPropertyAccountingMethod()

        status(result) must be(Status.OK)
        verifyOverseasPropertySave(None)
      }
    }
  }

  "submit" should {

    def callSubmit(isEditMode: Boolean): Future[Result] = TestOverseasPropertyAccountingMethodController.submit(isEditMode)(
      subscriptionRequest.post(AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm, Cash)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestOverseasPropertyAccountingMethodController.submit(isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" when {
      "redirect to CheckYourAnswer page" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchOverseasProperty(Some(OverseasPropertyModel()))

        val goodRequest = callSubmit(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show.url)

        await(goodRequest)
        verifyOverseasPropertySave(Some(OverseasPropertyModel(accountingMethod = Some(Cash))))
      }
    }

    "When it is in edit mode" should {
      "redirect to CheckYourAnswer page" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchOverseasProperty(Some(OverseasPropertyModel()))

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show.url)

        await(goodRequest)
        verifyOverseasPropertySave(Some(OverseasPropertyModel(accountingMethod = Some(Cash))))
      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {
        mockFetchAllFromSubscriptionDetails(overseasPropertyOnlyIncomeSourceType)
        mockOverseasPropertyAccountingMethod()

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsSave(OverseasPropertyAccountingMethod, 0)
        verifySubscriptionDetailsFetchAll(0)
      }
    }

    "The back url is not in edit mode" when {
      "the user clicks the back link" should {
        "redirect to the Overseas Property Start Date page" in {
          mockFetchAllFromSubscriptionDetails(overseasPropertyOnlyIncomeSourceType)
          TestOverseasPropertyAccountingMethodController.backUrl(isEditMode = false) mustBe
            controllers.agent.business.routes.OverseasPropertyStartDateController.show().url
        }
      }
    }

    "The back url is in edit mode" when {
      "the user clicks the back link" should {
        "redirect to the Check Your Answers page" in {
          mockFetchAllFromSubscriptionDetails(overseasPropertyOnlyIncomeSourceType)
          TestOverseasPropertyAccountingMethodController.backUrl(isEditMode = true) mustBe
            controllers.agent.routes.CheckYourAnswersController.show.url
        }
      }
    }
  }
}
