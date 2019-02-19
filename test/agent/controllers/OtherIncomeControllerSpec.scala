/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.controllers

import agent.audit.Logging
import agent.forms.OtherIncomeForm
import agent.models.OtherIncomeModel
import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels
import core.config.featureswitch.FeatureSwitching
import core.models.{No, Yes}
import incometax.incomesource.services.mocks.MockCurrentTimeService
import incometax.subscription.models._
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{await, _}

class OtherIncomeControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with MockCurrentTimeService with FeatureSwitching {

  override val controllerName: String = "OtherIncomeController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOtherIncomeController.show(isEditMode = true),
    "submit" -> TestOtherIncomeController.submit(isEditMode = true)
  )

  object TestOtherIncomeController extends OtherIncomeController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    app.injector.instanceOf[Logging],
    mockCurrentTimeService
  )

  "Calling the showOtherIncome action of the OtherIncome controller with an authorised user" when {

    def call = TestOtherIncomeController.show(isEditMode = true)(subscriptionRequest)

    "income source is in keystore" should {
      "return ok (200)" in {
        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceBoth,
          fetchOtherIncome = None
        )

        val result = call

        status(result) must be(Status.OK)

        await(result)
        verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 1, saveOtherIncome = 0)
      }
    }

    "income source is not in keystore" should {
      s"return redirect (303) to ${agent.controllers.routes.IncomeSourceController.show().url}" in {
        setupMockKeystore(
          fetchIncomeSource = None,
          fetchOtherIncome = None
        )

        val result = call

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get mustBe agent.controllers.routes.IncomeSourceController.show().url

        await(result)
        verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 0, saveOtherIncome = 0)
      }
    }
  }


  "Calling the submitOtherIncome action of the OtherIncome controller with an authorised user" when {
    def callSubmit = TestOtherIncomeController.submit(isEditMode = true)(subscriptionRequest
      .post(OtherIncomeForm.otherIncomeForm, Yes))

    "income source is not in keystore" should {
      s"return redirect (303) to ${agent.controllers.routes.IncomeSourceController.show().url}" in {
        setupMockKeystore(
          fetchIncomeSource = None,
          fetchOtherIncome = None
        )

        val result = callSubmit

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get mustBe agent.controllers.routes.IncomeSourceController.show().url

        await(result)
        verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 0, saveOtherIncome = 0)
      }
    }


    "income source is in keystore and saying yes to other income" should {

      def callSubmit = TestOtherIncomeController.submit(isEditMode = true)(subscriptionRequest
        .post(OtherIncomeForm.otherIncomeForm, Yes))

      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceBoth,
          fetchOtherIncome = None
        )

        val goodRequest = callSubmit

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchOtherIncome = 1, saveOtherIncome = 1)
      }

      s"redirect to '${agent.controllers.routes.OtherIncomeErrorController.show().url}'" in {
        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceBoth,
          fetchOtherIncome = None
        )

        val goodRequest = callSubmit

        redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.OtherIncomeErrorController.show().url)

        await(goodRequest)
        verifyKeystore(fetchOtherIncome = 1, saveOtherIncome = 1)
      }

    }

    "Calling the submitOtherIncome action of the OtherIncome controller with an authorised user and saying no to other income" should {

      def callSubmit = TestOtherIncomeController.submit(isEditMode = true)(subscriptionRequest
        .post(OtherIncomeForm.otherIncomeForm, No))

      "return a redirect status (SEE_OTHER - 303)" in {

        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceBusiness,
          fetchOtherIncome = None
        )

        val goodRequest = callSubmit

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
      }

      s"redirect to '${agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url}' on the business journey" in {

        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceBusiness,
          fetchOtherIncome = None
        )

        val goodRequest = callSubmit

        redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url)

        await(goodRequest)
        verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
      }

      s"redirect to '${agent.controllers.routes.TermsController.show().url}' on the property journey" in {

        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceProperty,
          fetchOtherIncome = None
        )

        val goodRequest = callSubmit

        redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.TermsController.show().url)

        await(goodRequest)
        verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
      }

      s"redirect to '${agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url}' on the both journey" in {

        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceBoth,
          fetchOtherIncome = None
        )

        val goodRequest = callSubmit

        redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url)

        await(goodRequest)
        verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
      }

      "income source is in keystore and with an invalid choice" should {

        val dummy = "Invalid"

        def badrequest = TestOtherIncomeController.submit(isEditMode = true)(subscriptionRequest
          .postInvalid(OtherIncomeForm.otherIncomeForm, dummy))

        "return a bad request status (400)" in {
          setupMockKeystore(
            fetchIncomeSource = TestModels.testIncomeSourceBoth,
            fetchOtherIncome = None
          )

          status(badrequest) must be(Status.BAD_REQUEST)

          await(badrequest)
          verifyKeystore(fetchOtherIncome = 0, saveOtherIncome = 0)
        }

      }
    }
  }

  "backUrl" when {
    "edit mode is on" should {
      s"return ${agent.controllers.routes.CheckYourAnswersController.show().url}" in {
        TestOtherIncomeController.backUrl(isEditMode = true, Property) mustBe agent.controllers.routes.CheckYourAnswersController.show().url
      }
    }
  }

  authorisationTests()
}
