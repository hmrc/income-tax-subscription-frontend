/*
 * Copyright 2023 HM Revenue & Customs
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

import config.featureswitch.FeatureSwitch.EnableTaskListRedesign
import controllers.agent.AgentControllerBaseSpec
import forms.agent.AccountingMethodOverseasPropertyForm
import models.Cash
import models.common.OverseasPropertyModel
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys.OverseasPropertyAccountingMethod
import views.agent.mocks.MockOverseasPropertyAccountingMethod

import scala.concurrent.Future

class OverseasPropertyAccountingMethodControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService with MockOverseasPropertyAccountingMethod {

  override def beforeEach(): Unit = {
    disable(EnableTaskListRedesign)
    super.beforeEach()
  }

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

  "show" when {

    "there are missing client details" should {
      "throw an InternalServerException" in {
        mockFetchOverseasProperty(None)
        mockOverseasPropertyAccountingMethod()

        intercept[InternalServerException](await(TestOverseasPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequest)))
          .message mustBe "[IncomeTaxAgentUser][clientDetails] - could not retrieve client details from session"
      }
    }

    "display the overseas property accounting method view and return OK (200)" when {
      "there is no previously selected accounting method" in {
        lazy val result = await(TestOverseasPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequestWithName))
        mockFetchOverseasProperty(None)
        mockOverseasPropertyAccountingMethod()

        status(result) must be(Status.OK)
        verifyOverseasPropertySave(None)
      }

      "there is a previously selected answer of CASH" in {
        lazy val result = await(TestOverseasPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequestWithName))

        mockFetchOverseasProperty(Some(OverseasPropertyModel(
          accountingMethod = Some(Cash)
        )))
        mockOverseasPropertyAccountingMethod()

        status(result) must be(Status.OK)
        verifyOverseasPropertySave(None)
      }
    }
  }

  "submit" should {
    def callSubmit(isEditMode: Boolean): Future[Result] = TestOverseasPropertyAccountingMethodController.submit(isEditMode)(
      subscriptionRequestWithName.post(AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm, Cash)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestOverseasPropertyAccountingMethodController.submit(isEditMode)(
      subscriptionRequestWithName
    )

    "redirect to agent overseas property check your answers page" in {
      setupMockSubscriptionDetailsSaveFunctions()
      mockFetchOverseasProperty(Some(OverseasPropertyModel()))

      val goodRequest = callSubmit(isEditMode = false)

      status(goodRequest) must be(Status.SEE_OTHER)

      redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show().url)

      await(goodRequest)
      verifyOverseasPropertySave(Some(OverseasPropertyModel(accountingMethod = Some(Cash))))
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" in {
        mockOverseasPropertyAccountingMethod()

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsSave(OverseasPropertyAccountingMethod, 0)
        verifySubscriptionDetailsFetchAll(Some(0))
      }
    }

    "throw an exception" when {
      "cannot save the accounting method" in {
        mockFetchOverseasProperty(Some(OverseasPropertyModel()))
        setupMockSubscriptionDetailsSaveFunctionsFailure()

        val goodRequest = callSubmit(isEditMode = false)
        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }

    "The back url is in edit mode" should {
      "redirect to the agent overseas property check your answers page" in {
        TestOverseasPropertyAccountingMethodController.backUrl(isEditMode = true) mustBe
          controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(true).url
      }
    }

    "The back url is not in edit mode" should {
      "redirect to the Overseas Property Start Date page" in {
        TestOverseasPropertyAccountingMethodController.backUrl(isEditMode = false) mustBe
          controllers.agent.business.routes.OverseasPropertyStartDateController.show().url
      }
    }

  }
}
