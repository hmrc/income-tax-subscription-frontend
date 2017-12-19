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
import incometax.util.AccountingPeriodUtil
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._

import scala.concurrent.Future

class TermsControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "TermsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showTerms" -> TestTermsController.showTerms(editMode = false),
    "submitTerms" -> TestTermsController.submitTerms()
  )

  object TestTermsController extends TermsController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  "The private getCurrentTaxYear" when {

    val testNextAccountingPeriod: AccountingPeriodModel =
      AccountingPeriodModel(
        DateModel("6", "4", AccountingPeriodUtil.getCurrentTaxEndYear.toString),
        DateModel("31", "3", (AccountingPeriodUtil.getCurrentTaxEndYear + 1).toString)
      )

    def call(editMode: Boolean) = TestTermsController.getCurrentTaxYear(editMode = editMode)(subscriptionRequest)

    "the user answered yes to match tax year" should {
      "return the current tax year" in {
        setupMockKeystore(fetchMatchTaxYear = TestModels.testMatchTaxYearYes)
        val result = call(editMode = false)
        await(result) mustBe Right(AccountingPeriodUtil.getCurrentTaxEndYear)
      }
    }

    "the user answered no to match tax year" should {
      "return the the text year for the accounting period date provided" in {
        setupMockKeystore(
          fetchMatchTaxYear = TestModels.testMatchTaxYearNo,
          fetchAccountingPeriodDate = testNextAccountingPeriod
        )
        val result = call(editMode = false)
        await(result) mustBe Right(AccountingPeriodUtil.getTaxEndYear(testNextAccountingPeriod))
      }
    }

    "the user answered no to match tax year but does not provide an answer" should {
      "when edit mode is false" in {
        setupMockKeystore(
          fetchMatchTaxYear = TestModels.testMatchTaxYearNo,
          fetchAccountingPeriodDate = None
        )
        val result = call(editMode = false)
        await(result.map(_.isLeft)) mustBe true
        redirectLocation(result.map(_.left.get)).get mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = false).url
      }
      "when edit mode is true" in {
        setupMockKeystore(
          fetchMatchTaxYear = TestModels.testMatchTaxYearNo,
          fetchAccountingPeriodDate = None
        )
        val result = call(editMode = true)
        await(result.map(_.isLeft)) mustBe true
        redirectLocation(result.map(_.left.get)).get mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true, editMatch = true).url
      }
    }
  }

  "Calling the showTerms action of the TermsController with an authorised user" should {

    lazy val result = TestTermsController.showTerms(editMode = false)(subscriptionRequest)

    "return ok (200)" in {
      setupMockKeystore(
        fetchIncomeSource = TestModels.testIncomeSourceBusiness,
        fetchMatchTaxYear = TestModels.testMatchTaxYearNo,
        fetchAccountingPeriodDate = TestModels.testAccountingPeriod(),
        fetchTerms = None
      )

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

  "The back url" when {
    "edit mode is true" should {
      s"point to ${incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true).url} on any journey" in {
        await(TestTermsController.backUrl(editMode = true)(subscriptionRequest)) mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true).url
      }
    }
    "edit mode is false" should {
      s"point to ${incometax.business.controllers.routes.BusinessAccountingMethodController.show().url} on the business journey" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)
        await(TestTermsController.backUrl(editMode = false)(subscriptionRequest)) mustBe incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
        verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 0)
      }

      s"point to ${incometax.business.controllers.routes.BusinessAccountingMethodController.show().url} on the both journey" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)
        await(TestTermsController.backUrl(editMode = false)(subscriptionRequest)) mustBe incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
        verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 0)
      }

      s"point to ${incometax.incomesource.controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url} on the property journey if they answered yes to other incomes" in {
        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceProperty,
          fetchOtherIncome = TestModels.testOtherIncomeYes
        )
        await(TestTermsController.backUrl(editMode = false)(subscriptionRequest)) mustBe incometax.incomesource.controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url
        verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 1)
      }

      s"point to ${incometax.incomesource.controllers.routes.OtherIncomeController.showOtherIncome().url} on the property journey if they answered no to other incomes" in {
        setupMockKeystore(
          fetchIncomeSource = TestModels.testIncomeSourceProperty,
          fetchOtherIncome = TestModels.testOtherIncomeNo
        )
        await(TestTermsController.backUrl(editMode = false)(subscriptionRequest)) mustBe incometax.incomesource.controllers.routes.OtherIncomeController.showOtherIncome().url
        verifyKeystore(fetchIncomeSource = 1, fetchOtherIncome = 1)
      }
    }

  }

  authorisationTests()
}
