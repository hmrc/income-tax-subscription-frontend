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

package incometax.subscription.controllers

import core.controllers.ControllerBaseSpec
import core.models.DateModel
import core.services.mocks.MockKeystoreService
import core.utils.TestModels
import incometax.business.models.AccountingPeriodModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._

import scala.concurrent.Future

class TermsControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "TermsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showTerms" -> TestTermsController.showTerms(),
    "submitTerms" -> TestTermsController.submitTerms()
  )

  object TestTermsController extends TermsController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  "Calling the showTerms action of the TermsController with an authorised user" should {

    lazy val result = TestTermsController.showTerms()(subscriptionRequest)

    "return ok (200)" in {
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness, fetchAccountingPeriodDate = TestModels.testAccountingPeriod())

      setupMockKeystore(fetchTerms = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchTerms = 0, saveTerms = 0, fetchAccountingPeriodDate = 1)
    }
  }

  "Calling the submitTerms action of the TermsController with an authorised user and valid submission" when {

    def callShow(): Future[Result] = {
      setupMockKeystoreSaveFunctions()
      TestTermsController.submitTerms()(subscriptionRequest)
    }

    "submit" should {

      "return a redirect status (SEE_OTHER - 303)" in {

        val goodResult = callShow()

        status(goodResult) must be(Status.SEE_OTHER)

        await(goodResult)
        verifyKeystore(fetchTerms = 0, saveTerms = 1)
      }

      s"redirect to '${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodResult = callShow()

        redirectLocation(goodResult) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)

        await(goodResult)
        verifyKeystore(fetchTerms = 0, saveTerms = 1)
      }
    }

  }

  "The back url" should {
    s"point to ${incometax.business.controllers.routes.BusinessAccountingMethodController.show().url} on the business journey" in {
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)
      await(TestTermsController.backUrl(subscriptionRequest)) mustBe incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
      verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 0)
    }

    s"point to ${incometax.business.controllers.routes.BusinessAccountingMethodController.show().url} on the both journey" in {
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)
      await(TestTermsController.backUrl(subscriptionRequest)) mustBe incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
      verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 0)
    }

    s"point to ${incometax.incomesource.controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url} on the property journey if they answered yes to other incomes" in {
      setupMockKeystore(
        fetchIncomeSource = TestModels.testIncomeSourceProperty,
        fetchOtherIncome = TestModels.testOtherIncomeYes
      )
      await(TestTermsController.backUrl(subscriptionRequest)) mustBe incometax.incomesource.controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url
      verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 1)
    }

    s"point to ${incometax.incomesource.controllers.routes.OtherIncomeController.showOtherIncome().url} on the property journey if they answered no to other incomes" in {
      setupMockKeystore(
        fetchIncomeSource = TestModels.testIncomeSourceProperty,
        fetchOtherIncome = TestModels.testOtherIncomeNo
      )
      await(TestTermsController.backUrl(subscriptionRequest)) mustBe incometax.incomesource.controllers.routes.OtherIncomeController.showOtherIncome().url
      verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 1)
    }

  }

  "TermsController.getTaxEndYear" should {
    "return 2018 if the tax year ends between 6th April 2017 and 5th April 2018" in {
      val testPeriodLowBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("6", "4", "2017"))
      val testPeriodBelowLowBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("5", "4", "2017"))
      TermsController.getTaxEndYear(testPeriodLowBound) mustBe 2018
      TermsController.getTaxEndYear(testPeriodBelowLowBound) mustBe 2017

      val testPeriodUpperBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("5", "4", "2018"))
      val testPeriodAboveUpperBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("6", "4", "2018"))
      TermsController.getTaxEndYear(testPeriodUpperBound) mustBe 2018
      TermsController.getTaxEndYear(testPeriodAboveUpperBound) mustBe 2019
    }

    "return 2019 if the tax year ends between 6th April 2018 and 5th April 2019" in {
      val testPeriodLowBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("6", "4", "2018"))
      val testPeriodBelowLowBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("5", "4", "2018"))
      TermsController.getTaxEndYear(testPeriodLowBound) mustBe 2019
      TermsController.getTaxEndYear(testPeriodBelowLowBound) mustBe 2018

      val testPeriodUpperBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("5", "4", "2019"))
      val testPeriodAboveUpperBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("6", "4", "2019"))
      TermsController.getTaxEndYear(testPeriodUpperBound) mustBe 2019
      TermsController.getTaxEndYear(testPeriodAboveUpperBound) mustBe 2020
    }
  }

  authorisationTests()
}
