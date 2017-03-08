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

package controllers

import auth._
import forms.OtherIncomeForm
import models.OtherIncomeModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockKeystoreService
import utils.TestModels

class OtherIncomeControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "OtherIncomeController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showOtherIncome" -> TestOtherIncomeController.showOtherIncome,
    "submitOtherIncome" -> TestOtherIncomeController.submitOtherIncome
  )

  object TestOtherIncomeController extends OtherIncomeController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService
  )

  "Calling the showOtherIncome action of the OtherIncome controller with an authorised user" should {

    lazy val result = TestOtherIncomeController.showOtherIncome(authenticatedFakeRequest())

    "return ok (200)" in {
      setupMockKeystore(fetchOtherIncome = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchOtherIncome = 1, saveOtherIncome = 0)
    }
  }

  "Calling the submitOtherIncome action of the OtherIncome controller with an authorised user and saying yes to other income" should {

    def callSubmit = TestOtherIncomeController.submitOtherIncome(authenticatedFakeRequest()
      .post(OtherIncomeForm.otherIncomeForm, OtherIncomeModel(OtherIncomeForm.option_yes)))

    "return a redirect status (SEE_OTHER - 303)" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callSubmit

      status(goodRequest) must be(Status.SEE_OTHER)

      await(goodRequest)
      verifyKeystore(fetchOtherIncome = 0, saveOtherIncome = 1)
    }

    s"redirect to '${controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url}'" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callSubmit

      redirectLocation(goodRequest) mustBe Some(controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url)

      await(goodRequest)
      verifyKeystore(fetchOtherIncome = 0, saveOtherIncome = 1)
    }


  }

  "Calling the submitOtherIncome action of the OtherIncome controller with an authorised user and saying no to other income" should {

    def callSubmit = TestOtherIncomeController.submitOtherIncome(authenticatedFakeRequest()
      .post(OtherIncomeForm.otherIncomeForm, OtherIncomeModel(OtherIncomeForm.option_no)))

    "return a redirect status (SEE_OTHER - 303)" in {
      setupMockKeystoreSaveFunctions()

      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

      val goodRequest = callSubmit

      status(goodRequest) must be(Status.SEE_OTHER)

      await(goodRequest)
      verifyKeystore(fetchOtherIncome = 0, saveOtherIncome = 1)
    }

    s"redirect to '${controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url}' on the business journey" in {
      setupMockKeystoreSaveFunctions()

      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

      val goodRequest = callSubmit

      redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url)

      await(goodRequest)
      verifyKeystore(fetchOtherIncome = 0, saveOtherIncome = 1)
    }

    s"redirect to '${controllers.routes.TermsController.showTerms().url}' on the property journey" in {
      setupMockKeystoreSaveFunctions()

      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceProperty)

      val goodRequest = callSubmit

      redirectLocation(goodRequest) mustBe Some(controllers.routes.TermsController.showTerms().url)

      await(goodRequest)
      verifyKeystore(fetchOtherIncome = 0, saveOtherIncome = 1)
    }

    s"redirect to '${controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url}' on the both journey" in {
      setupMockKeystoreSaveFunctions()

      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

      val goodRequest = callSubmit

      redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url)

      await(goodRequest)
      verifyKeystore(fetchOtherIncome = 0, saveOtherIncome = 1)
    }

    "Calling the submitOtherIncome action of the OtherIncome controller with an authorised user and with an invalid choice" should {

      val dummy = "Invalid"

      def badrequest = TestOtherIncomeController.submitOtherIncome(authenticatedFakeRequest()
        .post(OtherIncomeForm.otherIncomeForm, OtherIncomeModel(dummy)))

      "return a bad request status (400)" in {
        setupMockKeystoreSaveFunctions()

        status(badrequest) must be(Status.BAD_REQUEST)

        await(badrequest)
        verifyKeystore(fetchOtherIncome = 0, saveOtherIncome = 0)
      }

    }
  }

  "The back url" should {
    s"point to ${controllers.routes.IncomeSourceController.showIncomeSource().url} on other income page" in {
      TestOtherIncomeController.backUrl mustBe controllers.routes.IncomeSourceController.showIncomeSource().url
    }
  }


}
