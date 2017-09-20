/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.business

import controllers.ControllerBaseSpec
import forms.AccountingPeriodPriorForm
import forms.OtherIncomeForm._
import models.{AccountingPeriodPriorModel, OtherIncomeModel}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService
import utils.TestModels

import scala.concurrent.Future

class BusinessAccountingPeriodPriorControllerSpec extends ControllerBaseSpec with MockKeystoreService {

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
  val defaultOtherIncomeAnswer: OtherIncomeModel = TestModels.testOtherIncomeNo

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

    s"The back url should point to '${controllers.routes.OtherIncomeController.showOtherIncome().url}'" in {
      val document = Jsoup.parse(contentAsString(result))
      document.select("#back").attr("href") mustBe controllers.routes.OtherIncomeController.showOtherIncome().url
    }
  }

  "The back url" should {

    def result(choice: String): Future[Result] = {
      setupMockKeystore(
        fetchAccountingPeriodPrior = None,
        fetchOtherIncome = OtherIncomeModel(choice)
      )
      TestAccountingPeriodPriorController.show(isEditMode = false)(subscriptionRequest)
    }

    s"When the user previously answered yes to otherIncome, it should point to '${controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url}'" in {
      val document = Jsoup.parse(contentAsString(result(option_yes)))
      document.select("#back").attr("href") mustBe controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url
    }

    s"When the user previously answered no to otherIncome, it should point to '${controllers.routes.OtherIncomeController.showOtherIncome().url}'" in {
      val document = Jsoup.parse(contentAsString(result(option_no)))
      document.select("#back").attr("href") mustBe controllers.routes.OtherIncomeController.showOtherIncome().url
    }

  }

  "Calling the submit action of the BusinessAccountingPeriodPriorController with an authorised user and valid submission" when {

    def callShowCore(answer: String, isEditMode: Boolean): Future[Result] = TestAccountingPeriodPriorController.submit(isEditMode)(subscriptionRequest
      .post(AccountingPeriodPriorForm.accountingPeriodPriorForm, AccountingPeriodPriorModel(answer)))

    "Not in edit mode and " when {
      def callShow(answer: String): Future[Result] = callShowCore(answer, isEditMode = false)

      "Option 'Yes' is selected and there were no previous entries" in {
        setupMockKeystore(fetchAccountingPeriodPrior = None)
        val goodRequest = callShow(AccountingPeriodPriorForm.option_yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe controllers.business.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'Yes' is selected and there is previous entry" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes))
        val goodRequest = callShow(AccountingPeriodPriorForm.option_yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe controllers.business.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there were no previous entries" in {
        setupMockKeystore(fetchAccountingPeriodPrior = None)
        val goodRequest = callShow(AccountingPeriodPriorForm.option_no)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe controllers.business.routes.BusinessAccountingPeriodDateController.showAccountingPeriod().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there there is previous entry" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes))
        val goodRequest = callShow(AccountingPeriodPriorForm.option_no)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe controllers.business.routes.BusinessAccountingPeriodDateController.showAccountingPeriod().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }
    }

    "Is in edit mode and " when {
      def callShow(answer: String): Future[Result] = callShowCore(answer, isEditMode = true)

      "Option 'Yes' is selected and there were no previous entries" in {
        // this condition shouldn't happen, but no reason to break the journey, just proceed through the journey normally
        setupMockKeystore(fetchAccountingPeriodPrior = None)
        val goodRequest = callShow(AccountingPeriodPriorForm.option_yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe controllers.business.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'Yes' is selected and there is previous entry and it is the same as the current answer" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes))
        val goodRequest = callShow(AccountingPeriodPriorForm.option_yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe controllers.routes.CheckYourAnswersController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'Yes' is selected and there is previous entry and it is the different from the current answer" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no))
        val goodRequest = callShow(AccountingPeriodPriorForm.option_yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe  controllers.business.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there were no previous entries" in {
        // this condition shouldn't happen, but no reason to break the journey, just proceed through the journey normally
        setupMockKeystore(fetchAccountingPeriodPrior = None)
        val goodRequest = callShow(AccountingPeriodPriorForm.option_no)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe controllers.business.routes.BusinessAccountingPeriodDateController.showAccountingPeriod().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there there is previous entry and it is the same as the current answer" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no))
        val goodRequest = callShow(AccountingPeriodPriorForm.option_no)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe controllers.routes.CheckYourAnswersController.show().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there there is previous entry and it is the different from the current answer" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes))
        val goodRequest = callShow(AccountingPeriodPriorForm.option_no)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe controllers.business.routes.BusinessAccountingPeriodDateController.showAccountingPeriod().url
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
