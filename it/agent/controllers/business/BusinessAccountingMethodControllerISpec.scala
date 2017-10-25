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

package agent.controllers.business

import _root_.agent.services.CacheConstants
import agent.forms._
import agent.models._
import _root_.agent.helpers.ComponentSpecBase
import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.IntegrationTestModels._
import _root_.agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import play.api.http.Status._
import play.api.i18n.Messages

class BusinessAccountingMethodControllerISpec extends ComponentSpecBase {

  "GET /business/accounting-method" when {

    "keystore call fails" should {
      "not show page but return internal server error" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreFailure()

        When("GET /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingMethod()

        Then("should return an internal server error")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }


    "keystore returns all data" should {
      "show the accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingMethod()

        val expectedText = removeHtmlMarkup(Messages("agent.business.accounting_method.cash"))

        Then("Should return a OK with the accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.business.accounting_method.title")),
          radioButtonSet(id = "accountingMethod", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "keystore returns no data" should {
      "show the accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("GET /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingMethod()

        Then("Should return a OK with the accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.business.accounting_method.title")),
          radioButtonSet(id = "accountingMethod", selectedRadioButton = None)
        )
      }
    }

    "redirect to sign-in when auth fails" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubUnauthorised()

      When("GET /business/accounting-method is called")
      val res = IncomeTaxSubscriptionFrontend.businessAccountingMethod()

      Then("Should return a SEE_OTHER with a redirect location of sign-in")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(signInURI)
      )
    }
  }

  "POST /business/accounting-method" when {

    "not in edit mode" should {

      "select the Cash radio button on the accounting method page" in {
        val userInput = AccountingMethodModel(AccountingMethodForm.option_cash)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod, userInput)

        When("POST /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of terms")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(termsURI)
        )
      }

      "select the Accruals radio button on the accounting method page" in {
        val userInput = AccountingMethodModel(AccountingMethodForm.option_accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod, userInput)

        When("POST /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of terms")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(termsURI)
        )
      }
    }

    "not select an option on the accounting method page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod, "")

      When("POST /business/accounting-method is called")
      val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, None)

      Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
      res should have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }

    "select invalid other income option on the other income page as if the user it trying to manipulate the html" in {
      val userInput = AccountingMethodModel("madeup")

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod, "madeup")

      When("POST /business/accounting-method is called")
      val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, Some(userInput))

      Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
      res should have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }

    "not show the page correctly and return an internal server error" in {
      val userInput = AccountingMethodModel(AccountingMethodForm.option_cash)

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreFailure()

      When("POST /business/accounting-method is called")
      val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, Some(userInput))

      Then("should return an internal server error")
      res should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "redirect to sign-in when auth fails" in {
      val userInput = AccountingMethodModel(AccountingMethodForm.option_cash)

      Given("I setup the Wiremock stubs")
      AuthStub.stubUnauthorised()

      When("POST /business/accounting-method is called")
      val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, Some(userInput))

      Then("Should return a SEE_OTHER with a redirect location of sign-in")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(signInURI)
      )
    }

    "in edit mode" should {

      "changing to the Accruals radio button on the accounting method page" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_both)
        val keystoreIncomeOther = OtherIncomeModel(OtherIncomeForm.option_no)
        val keystoreAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)
        val keystoreAccountingPeriodDates: AccountingPeriodModel = testAccountingPeriod
        val keystoreAccountingMethod = AccountingMethodModel(AccountingMethodForm.option_cash)
        val userInput = AccountingMethodModel(AccountingMethodForm.option_accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther),
            accountingPeriodPrior = Some(keystoreAccountingPeriodPrior),
            accountingPeriodDate = Some(keystoreAccountingPeriodDates),
            accountingMethod = Some(keystoreAccountingMethod)
          )
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod, userInput)

        When("POST /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of terms")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate not changing accounting method when calling page from Check Your Answers" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_both)
        val keystoreIncomeOther = OtherIncomeModel(OtherIncomeForm.option_no)
        val keystoreAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)
        val keystoreAccountingPeriodDates: AccountingPeriodModel = testAccountingPeriod
        val keystoreAccountingMethod = AccountingMethodModel(AccountingMethodForm.option_cash)
        val userInput = AccountingMethodModel(AccountingMethodForm.option_cash)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther),
            accountingPeriodPrior = Some(keystoreAccountingPeriodPrior),
            accountingPeriodDate = Some(keystoreAccountingPeriodDates),
            accountingMethod = Some(keystoreAccountingMethod)
          )
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod, userInput)

        When("POST /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "not show the page correctly and return an internal server error" in {
        val userInput = AccountingMethodModel(AccountingMethodForm.option_cash)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreFailure()

        When("POST /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = true, Some(userInput))

        Then("should return an internal server error")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "redirect to sign-in when auth fails" in {
        val userInput = AccountingMethodModel(AccountingMethodForm.option_cash)

        Given("I setup the Wiremock stubs")
        AuthStub.stubUnauthorised()

        When("POST /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of sign-in")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(signInURI)
        )
      }
    }
  }
}
