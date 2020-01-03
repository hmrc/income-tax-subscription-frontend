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

import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels
import core.config.featureswitch.{AgentPropertyCashOrAccruals, FeatureSwitching}
import incometax.subscription.models.{Both, Property}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class TermsControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with FeatureSwitching {

  override val controllerName: String = "TermsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showTerms" -> TestTermsController.show(editMode = false),
    "submitTerms" -> TestTermsController.submit()
  )

  override def beforeEach(): Unit = {
    disable(AgentPropertyCashOrAccruals)
    super.beforeEach()
  }

  object TestTermsController extends TermsController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  "Calling the showTerms action of the TermsController with an authorised user" should {

    lazy val result = TestTermsController.show(editMode = false)(subscriptionRequest)

    "return ok (200)" in {
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness, fetchAccountingPeriodDate = TestModels.testAccountingPeriod())

      setupMockKeystore(fetchTerms = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchTerms = 0, saveTerms = 0, fetchAccountingPeriodDate = 1)
    }
  }

  "Calling the submitTerms action of the TermsController with an authorised user and valid submission" when {

    def callSubmit(): Future[Result] = {
      setupMockKeystoreSaveFunctions()
      TestTermsController.submit()(subscriptionRequest)
    }

    "submit" should {

      "return a redirect status (SEE_OTHER - 303)" in {

        val goodResult = callSubmit()

        status(goodResult) must be(Status.SEE_OTHER)

        await(goodResult)
        verifyKeystore(fetchTerms = 0, saveTerms = 1)
      }

      s"redirect to '${controllers.agent.routes.CheckYourAnswersController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodResult = callSubmit()

        redirectLocation(goodResult) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

        await(goodResult)
        verifyKeystore(fetchTerms = 0, saveTerms = 1)
      }
    }
  }

  "The back url" when {
    "edit mode is true" should {
      s"point to ${controllers.agent.business.routes.BusinessAccountingPeriodDateController.show(editMode = true).url} on any journey" in {
        await(TestTermsController.backUrl(editMode = true)(subscriptionRequest)) mustBe controllers.agent.business.routes.BusinessAccountingPeriodDateController.show(editMode = true).url
      }
    }
    "edit mode is false" should {

      s"point to ${business.routes.PropertyAccountingMethodController.show().url}" when {
        "the property cash/accruals feature switch is enabled" when {
          "the user is on a property only flow" in {
            enable(AgentPropertyCashOrAccruals)
            setupMockKeystore(fetchIncomeSource = Property)
            await(TestTermsController.backUrl(editMode = false)(subscriptionRequest)) mustBe business.routes.PropertyAccountingMethodController.show().url
            verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 0)
          }
          "the user is on a property and business flow" in {
            enable(AgentPropertyCashOrAccruals)
            setupMockKeystore(fetchIncomeSource = Both)
            await(TestTermsController.backUrl(editMode = false)(subscriptionRequest)) mustBe business.routes.PropertyAccountingMethodController.show().url
            verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 0)
          }
        }
      }

      s"point to ${controllers.agent.business.routes.BusinessAccountingMethodController.show().url} on the business journey" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)
        await(TestTermsController.backUrl(editMode = false)(subscriptionRequest)) mustBe controllers.agent.business.routes.BusinessAccountingMethodController.show().url
        verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 0)
      }

      s"point to ${controllers.agent.business.routes.BusinessAccountingMethodController.show().url} on the both journey" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)
        await(TestTermsController.backUrl(editMode = false)(FakeRequest())) mustBe controllers.agent.business.routes.BusinessAccountingMethodController.show().url
        verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 0)
      }

      s"point to ${controllers.agent.routes.IncomeSourceController.show().url} on the property journey if they answered yes to other incomes" in {
        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceProperty
        )
        await(TestTermsController.backUrl(editMode = false)(FakeRequest())) mustBe controllers.agent.routes.IncomeSourceController.show().url
        verifyKeystore(fetchIncomeSource = 1)
      }
    }
  }
  authorisationTests()
}
