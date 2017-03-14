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

import auth._
import controllers.ControllerBaseSpec
import forms.CurrentFinancialPeriodPriorForm
import forms.OtherIncomeForm._
import models.{CurrentFinancialPeriodPriorModel, OtherIncomeModel}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{contentAsString, _}
import services.mocks.MockKeystoreService
import utils.TestModels

import scala.concurrent.Future

class CurrentFinancialPeriodPriorControllerSpec extends ControllerBaseSpec with MockKeystoreService {

  override val controllerName: String = "CurrentFinancialPeriodPriorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestCurrentFinancialPeriodPriorController.show,
    "submit" -> TestCurrentFinancialPeriodPriorController.submit
  )

  object TestCurrentFinancialPeriodPriorController extends CurrentFinancialPeriodPriorController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService
  )

  // answer to other income is only significant for testing the backurl.
  val defaultOtherIncomeAnswer: OtherIncomeModel = TestModels.testOtherIncomeNo

  "Calling the show action of the CurrentFinancialPeriodPrior with an authorised user" should {

    def result: Future[Result] = {
      setupMockKeystore(
        fetchCurrentFinancialPeriodPrior = None,
        fetchOtherIncome = defaultOtherIncomeAnswer
      )
      TestCurrentFinancialPeriodPriorController.show(authenticatedFakeRequest())
    }

    "return ok (200)" in {
      status(result) must be(Status.OK)
    }

    "retrieve one value from keystore" in {
      await(result)
      verifyKeystore(fetchCurrentFinancialPeriodPrior = 1, saveCurrentFinancialPeriodPrior = 0)
    }

    s"The back url should point to '${controllers.routes.OtherIncomeController.showOtherIncome().url}'" in {
      val document = Jsoup.parse(contentAsString(result))
      document.select("#back").attr("href") mustBe controllers.routes.OtherIncomeController.showOtherIncome().url
    }
  }

  "The back url" should {

    def result(choice: String): Future[Result] = {
      setupMockKeystore(
        fetchCurrentFinancialPeriodPrior = None,
        fetchOtherIncome = OtherIncomeModel(choice)
      )
      TestCurrentFinancialPeriodPriorController.show(authenticatedFakeRequest())
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

  "Calling the submit action of the CurrentFinancialPeriodPrior with an authorised user and valid submission" when {

    def callShow(answer: String): Future[Result] = TestCurrentFinancialPeriodPriorController.submit(authenticatedFakeRequest()
      .post(CurrentFinancialPeriodPriorForm.currentFinancialPeriodPriorForm, CurrentFinancialPeriodPriorModel(answer)))

    "Option 'Yes' is selected" should {

      def goodRequest: Future[Result] = {
        setupMockKeystoreSaveFunctions()
        callShow(CurrentFinancialPeriodPriorForm.option_yes)
      }

      "return status SEE_OTHER (303)" in {
        status(goodRequest) mustBe Status.SEE_OTHER
      }

      s"redirect to ${controllers.business.routes.RegisterNextAccountingPeriodController.show()}" in {
        redirectLocation(goodRequest).get mustBe controllers.business.routes.RegisterNextAccountingPeriodController.show().url
      }

      "save one value into keystore" in {
        await(goodRequest)
        verifyKeystore(fetchCurrentFinancialPeriodPrior = 0, saveCurrentFinancialPeriodPrior = 1)
      }
    }

    "Option 'No' is selected" should {

      def goodRequest: Future[Result] = {
        setupMockKeystoreSaveFunctions()
        callShow(CurrentFinancialPeriodPriorForm.option_no)
      }

      "return status SEE_OTHER (303)" in {
        status(goodRequest) mustBe Status.SEE_OTHER
      }

      s"redirect to ${controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url}" in {
        redirectLocation(goodRequest).get mustBe controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url
      }

      "save one value into keystore" in {
        await(goodRequest)
        verifyKeystore(fetchCurrentFinancialPeriodPrior = 0, saveCurrentFinancialPeriodPrior = 1)
      }
    }
  }

  "Calling the submit action of the CurrentFinancialPeriodPrior with an authorised user and invalid submission" should {

    def badRequest: Future[Result] = {
      setupMockKeystore(fetchOtherIncome = defaultOtherIncomeAnswer)
      TestCurrentFinancialPeriodPriorController.submit(authenticatedFakeRequest())
    }

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)
    }

    "not update or retrieve anything from keystore" in {
      await(badRequest)
      verifyKeystore(fetchCurrentFinancialPeriodPrior = 0, saveCurrentFinancialPeriodPrior = 0)
    }
  }

  authorisationTests()

}
