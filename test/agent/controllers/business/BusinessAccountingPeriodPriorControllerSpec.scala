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

package agent.controllers.business

import agent.controllers.AgentControllerBaseSpec
import agent.forms.AccountingPeriodPriorForm
import agent.models.AccountingPeriodPriorModel
import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels
import core.config.featureswitch.{EligibilityPagesFeature, FeatureSwitching}
import core.models.{No, Yes, YesNo}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{contentAsString, _}

import scala.concurrent.Future

class BusinessAccountingPeriodPriorControllerSpec extends AgentControllerBaseSpec with MockKeystoreService with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EligibilityPagesFeature)
  }

  override val controllerName: String = "BusinessAccountingPeriodPriorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestAccountingPeriodPriorController.show(isEditMode = false),
    "submit" -> TestAccountingPeriodPriorController.submit(isEditMode = false)
  )

  object TestAccountingPeriodPriorController extends BusinessAccountingPeriodPriorController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  // answer to other income is only significant for testing the backurl.
  val defaultOtherIncomeAnswer: YesNo = TestModels.testOtherIncomeNo

  "The back url" when {
    "the eligibility pages feature switch is enabled" should {
      s"redirect to ${agent.controllers.routes.IncomeSourceController.show().url}" in {
        enable(EligibilityPagesFeature)
        await(TestAccountingPeriodPriorController.backUrl(subscriptionRequest)) mustBe agent.controllers.business.routes.BusinessNameController.show().url
      }
    }
    "the eligibility pages feature switch is disabled" when {
      "the user answered 'Yes' to the other income question" should {
        s"redirect to ${agent.controllers.routes.OtherIncomeErrorController.show().url}" in {
          setupMockKeystore(fetchOtherIncome = Some(Yes))
          await(TestAccountingPeriodPriorController.backUrl(subscriptionRequest)) mustBe agent.controllers.routes.OtherIncomeErrorController.show().url
        }
      }
      "the user answered 'No' to the other income question" should {
        s"redirect to ${agent.controllers.routes.OtherIncomeController.show().url}" in {
          setupMockKeystore(fetchOtherIncome = Some(No))
          await(TestAccountingPeriodPriorController.backUrl(subscriptionRequest)) mustBe agent.controllers.routes.OtherIncomeController.show().url
        }
      }
    }
  }

  "Calling the show action of the BusinessAccountingPeriodPriorController with an authorised user" should {

    def result: Future[Result] = {
      setupMockKeystore(
        fetchAccountingPeriodPrior = None,
        fetchOtherIncome = defaultOtherIncomeAnswer
      )
      TestAccountingPeriodPriorController.show(isEditMode = true)(subscriptionRequest)
    }

    "return ok (200)" in {
      status(result) must be(Status.OK)
    }

    "retrieve one value from keystore" in {
      await(result)
      verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 0)
    }

    s"The back url should point to '${agent.controllers.routes.OtherIncomeController.show().url}'" in {
      val document = Jsoup.parse(contentAsString(result))
      document.select("#back").attr("href") mustBe agent.controllers.routes.OtherIncomeController.show().url
    }
  }

  "Calling the submit action of the BusinessAccountingPeriodPriorController with an authorised user and valid submission" when {

    def callShowCore(answer: YesNo, isEditMode: Boolean): Future[Result] = TestAccountingPeriodPriorController.submit(isEditMode)(subscriptionRequest
      .post(AccountingPeriodPriorForm.accountingPeriodPriorForm, AccountingPeriodPriorModel(answer)))

    "Not in edit mode and " when {
      def callShow(answer: YesNo): Future[Result] = callShowCore(answer, isEditMode = false)


      "Option 'Yes' is selected and there were no previous entries" in {
        setupMockKeystore(fetchAccountingPeriodPrior = None)
        val goodRequest = callShow(Yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'Yes' is selected and there is previous entry" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(Yes))
        val goodRequest = callShow(Yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there were no previous entries" in {
        setupMockKeystore(fetchAccountingPeriodPrior = None)
        val goodRequest = callShow(No)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe agent.controllers.business.routes.MatchTaxYearController.show().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there there is previous entry which is different" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(Yes))
        val goodRequest = callShow(No)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe agent.controllers.business.routes.MatchTaxYearController.show().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

    }

    "Is in edit mode and " when {
      def callShow(answer: YesNo): Future[Result] = callShowCore(answer, isEditMode = true)

      "Option 'Yes' is selected and there were no previous entries" in {
        // this condition shouldn't happen, but no reason to break the journey, just proceed through the journey normally
        setupMockKeystore(fetchAccountingPeriodPrior = None)
        val goodRequest = callShow(Yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'Yes' is selected and there is previous entry and it is the same as the current answer" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(Yes))
        val goodRequest = callShow(Yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.CheckYourAnswersController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'Yes' is selected and there is previous entry and it is the different from the current answer" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(No))
        val goodRequest = callShow(Yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there were no previous entries" in {
        // this condition shouldn't happen, but no reason to break the journey, just proceed through the journey normally
        setupMockKeystore(fetchAccountingPeriodPrior = None)
        val goodRequest = callShow(No)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe agent.controllers.business.routes.MatchTaxYearController.show().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there there is previous entry and it is the same as the current answer" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(No))
        val goodRequest = callShow(No)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.CheckYourAnswersController.show().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there there is previous entry and it is the different from the current answer" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(Yes))
        val goodRequest = callShow(No)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe agent.controllers.business.routes.MatchTaxYearController.show().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }
    }
  }

  "Calling the submit action of the BusinessAccountingPeriodPriorController with an authorised user and invalid submission" should {

    def badRequest: Future[Result] = {
      setupMockKeystore(fetchOtherIncome = defaultOtherIncomeAnswer)
      TestAccountingPeriodPriorController.submit(isEditMode = false)(subscriptionRequest)
    }

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)
    }

    "not update or retrieve anything from keystore" in {
      await(badRequest)
      verifyKeystore(fetchAccountingPeriodPrior = 0, saveAccountingPeriodPrior = 0)
    }
  }

  authorisationTests()

}
