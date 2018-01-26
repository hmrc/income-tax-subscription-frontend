/*
 * Copyright 2018 HM Revenue & Customs
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

import core.config.featureswitch.{FeatureSwitching, NewIncomeSourceFlowFeature}
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import core.utils.TestModels._
import incometax.incomesource.forms.OtherIncomeForm
import incometax.subscription.models.{Both, Business, Property}
import incometax.util.AccountingPeriodUtil
import play.api.http.Status
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._

import scala.concurrent.Future

class TermsControllerNewIncomeSourceSpec extends ControllerBaseSpec
  with MockKeystoreService
  with FeatureSwitching {

  override val controllerName: String = "TermsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestTermsController.show(editMode = false),
    "submit" -> TestTermsController.submit()
  )

  object TestTermsController extends TermsController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(NewIncomeSourceFlowFeature)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    disable(NewIncomeSourceFlowFeature)
  }

  "Calling the showTerms action of the TermsController with an authorised user" when {

    implicit lazy val request = subscriptionRequest

    def result = await(TestTermsController.show(editMode = false)(subscriptionRequest))

    "The user selected business, and did not match the tax year" should {
      "return OK with the tax year from the accounting period date" in {
        setupMockKeystore(
          fetchAll = testCacheMap(
            rentUkProperty = testNewIncomeSourceBusiness.rentUkProperty,
            workForYourself = testNewIncomeSourceBusiness.workForYourself.get,
            matchTaxYear = testMatchTaxYearNo,
            accountingPeriodDate = testAccountingPeriod(),
            otherIncome = testOtherIncomeNo
          )
        )

        status(result) must be(Status.OK)

        val expectedPage = incometax.subscription.views.html.terms.apply(
          incometax.subscription.controllers.routes.TermsController.submit(),
          testAccountingPeriod().taxEndYear,
          incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
        )

        contentAsString(result) mustBe expectedPage.body
      }
    }
    "The user selected business, and matched the tax year" should {
      "return OK with the current tax year" in {
        setupMockKeystore(
          fetchAll = testCacheMap(
            rentUkProperty = testNewIncomeSourceBusiness.rentUkProperty,
            workForYourself = testNewIncomeSourceBusiness.workForYourself.get,
            matchTaxYear = testMatchTaxYearYes,
            accountingPeriodDate = testAccountingPeriod(),
            otherIncome = testOtherIncomeNo
          )
        )

        status(result) must be(Status.OK)

        val expectedPage = incometax.subscription.views.html.terms.apply(
          incometax.subscription.controllers.routes.TermsController.submit(),
          AccountingPeriodUtil.getCurrentTaxEndYear,
          incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
        )

        contentAsString(result) mustBe expectedPage.body
      }
    }

    "The user selected property with 1 page" should {
      "return OK with the current tax year" in {
        setupMockKeystore(
          fetchAll = testCacheMap(
            rentUkProperty = testNewIncomeSourceProperty_1page.rentUkProperty,
            workForYourself = testNewIncomeSourceProperty_1page.workForYourself,
            otherIncome = testOtherIncomeNo
          )
        )

        status(result) must be(Status.OK)

        val expectedPage = incometax.subscription.views.html.terms.apply(
          incometax.subscription.controllers.routes.TermsController.submit(),
          AccountingPeriodUtil.getCurrentTaxEndYear,
          incometax.incomesource.controllers.routes.OtherIncomeController.show().url
        )

        contentAsString(result) mustBe expectedPage.body
      }
    }

    "The user selected property with 2 pages" should {
      "return OK with the current tax year" in {
        setupMockKeystore(
          fetchAll = testCacheMap(
            rentUkProperty = testNewIncomeSourceProperty_2page.rentUkProperty,
            workForYourself = testNewIncomeSourceProperty_2page.workForYourself.get,
            otherIncome = testOtherIncomeNo
          )
        )

        status(result) must be(Status.OK)

        val expectedPage = incometax.subscription.views.html.terms.apply(
          incometax.subscription.controllers.routes.TermsController.submit(),
          AccountingPeriodUtil.getCurrentTaxEndYear,
          incometax.incomesource.controllers.routes.OtherIncomeController.show().url
        )

        contentAsString(result) mustBe expectedPage.body
      }
    }

    "The user selected business, and did not match the tax year but did not provide an accounting period" should {
      "return OK with the current tax year" in {
        setupMockKeystore(
          fetchAll = testCacheMap(
            rentUkProperty = testNewIncomeSourceBusiness.rentUkProperty,
            workForYourself = testNewIncomeSourceBusiness.workForYourself.get,
            matchTaxYear = testMatchTaxYearNo,
            otherIncome = testOtherIncomeNo
          )
        )

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) must contain(
          incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(
            editMode = false, editMatch = false)
            .url
        )
      }
    }
  }

  "Calling the submitTerms action of the TermsController with an authorised user and valid submission" when {

    def callShow(): Future[Result] = {
      setupMockKeystoreSaveFunctions()
      TestTermsController.submit()(subscriptionRequest)
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
    "edit mode is true" when {
      "match tax year is true" should {
        s"point to ${incometax.business.controllers.routes.MatchTaxYearController.show(editMode = true).url} on any journey" in {
          TestTermsController.getBackUrl(
            editMode = true,
            Property,
            OtherIncomeForm.option_yes,
            matchTaxYear = true
          )(subscriptionRequest) mustBe incometax.business.controllers.routes.MatchTaxYearController.show(editMode = true).url
        }
      }
      "match tax year is false" should {
        s"point to ${incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true).url} on any journey" in {
          TestTermsController.getBackUrl(
            editMode = true,
            Property,
            OtherIncomeForm.option_yes,
            matchTaxYear = false
          )(subscriptionRequest) mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true).url
        }
      }
    }
    "edit mode is false" should {
      s"point to ${incometax.business.controllers.routes.BusinessAccountingMethodController.show().url} on the business journey" in {
        TestTermsController.getBackUrl(
          editMode = false,
          Business,
          OtherIncomeForm.option_yes,
          matchTaxYear = false
        )(subscriptionRequest) mustBe incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
      }

      s"point to ${incometax.business.controllers.routes.BusinessAccountingMethodController.show().url} on the both journey" in {
        TestTermsController.getBackUrl(
          editMode = false,
          Both,
          OtherIncomeForm.option_yes,
          matchTaxYear = false
        )(subscriptionRequest) mustBe incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
      }

      s"point to ${incometax.incomesource.controllers.routes.OtherIncomeErrorController.show().url} on the property journey if they answered yes to other incomes" in {
        TestTermsController.getBackUrl(
          editMode = false,
          Property,
          OtherIncomeForm.option_yes,
          matchTaxYear = false
        )(subscriptionRequest) mustBe incometax.incomesource.controllers.routes.OtherIncomeErrorController.show().url
      }

      s"point to ${incometax.incomesource.controllers.routes.OtherIncomeController.show().url} on the property journey if they answered no to other incomes" in {
        TestTermsController.getBackUrl(
          editMode = false,
          Property,
          OtherIncomeForm.option_no,
          matchTaxYear = false
        )(subscriptionRequest) mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url
      }
    }

  }

  authorisationTests()
}
