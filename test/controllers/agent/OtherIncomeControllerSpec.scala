/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.agent

import agent.audit.Logging
import agent.forms.OtherIncomeForm
import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels
import core.config.featureswitch.{AgentPropertyCashOrAccruals, EligibilityPagesFeature, FeatureSwitching}
import core.models.{No, Yes}
import incometax.incomesource.services.mocks.MockCurrentTimeService
import incometax.subscription.models._
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{await, _}

import scala.concurrent.Future

class OtherIncomeControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with MockCurrentTimeService with FeatureSwitching {

  override val controllerName: String = "OtherIncomeController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOtherIncomeController.show(isEditMode = true),
    "submit" -> TestOtherIncomeController.submit(isEditMode = true)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EligibilityPagesFeature)
    disable(AgentPropertyCashOrAccruals)
  }

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
      s"return redirect (303) to ${controllers.agent.routes.IncomeSourceController.show().url}" in {
        setupMockKeystore(
          fetchIncomeSource = None,
          fetchOtherIncome = None
        )

        val result = call

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get mustBe controllers.agent.routes.IncomeSourceController.show().url

        await(result)
        verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 0, saveOtherIncome = 0)
      }
    }
  }


  "Calling the submitOtherIncome action of the OtherIncome controller with an authorised user" when {
    def callSubmit = TestOtherIncomeController.submit(isEditMode = true)(subscriptionRequest
      .post(OtherIncomeForm.otherIncomeForm, Yes))

    "income source is not in keystore" should {
      s"return redirect (303) to ${controllers.agent.routes.IncomeSourceController.show().url}" in {
        setupMockKeystore(
          fetchIncomeSource = None,
          fetchOtherIncome = None
        )

        val result = callSubmit

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get mustBe controllers.agent.routes.IncomeSourceController.show().url

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

      s"redirect to '${controllers.agent.routes.OtherIncomeErrorController.show().url}'" in {
        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceBoth,
          fetchOtherIncome = None
        )

        val goodRequest = callSubmit

        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.OtherIncomeErrorController.show().url)

        await(goodRequest)
        verifyKeystore(fetchOtherIncome = 1, saveOtherIncome = 1)
      }

    }

    "Calling the submitOtherIncome action of the OtherIncome controller with an authorised user and saying no to other income" should {

      def callSubmit: Future[Result] = TestOtherIncomeController.submit(isEditMode = true)(
        subscriptionRequest.post(OtherIncomeForm.otherIncomeForm, No)
      )

      s"redirect to '${controllers.agent.business.routes.MatchTaxYearController.show().url}' on the business journey" in {

        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceBusiness,
          fetchOtherIncome = None
        )

        val goodRequest = callSubmit

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.MatchTaxYearController.show().url)

        await(goodRequest)
        verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
      }

      s"redirect to ${business.routes.PropertyAccountingMethodController.show().url}" when {
        "the user is on a property only flow and the property cash accruals feature switch is enabled" in {
          enable(AgentPropertyCashOrAccruals)

          setupMockKeystore(
            fetchIncomeSource = Property,
            fetchOtherIncome = None
          )

          val goodRequest = await(callSubmit)

          status(goodRequest) mustBe SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(business.routes.PropertyAccountingMethodController.show().url)

          verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
        }
      }

      s"redirect to '${controllers.agent.routes.TermsController.show().url}' on the property journey" in {

        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceProperty,
          fetchOtherIncome = None
        )

        val goodRequest = callSubmit

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.TermsController.show().url)

        await(goodRequest)
        verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
      }

      s"redirect to '${controllers.agent.routes.CheckYourAnswersController.show().url}' on the property journey when the eligibility page feature switch is enabled" in {
        enable(EligibilityPagesFeature)
        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceProperty,
          fetchOtherIncome = None
        )

        val goodRequest = callSubmit

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
      }

      s"redirect to '${controllers.agent.business.routes.MatchTaxYearController.show().url}' on the both journey" in {

        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceBoth,
          fetchOtherIncome = None
        )

        val goodRequest = callSubmit

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.MatchTaxYearController.show().url)

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
      s"return ${controllers.agent.routes.CheckYourAnswersController.show().url}" in {
        TestOtherIncomeController.backUrl(isEditMode = true, Property) mustBe controllers.agent.routes.CheckYourAnswersController.show().url
      }
    }
  }

  authorisationTests()
}
