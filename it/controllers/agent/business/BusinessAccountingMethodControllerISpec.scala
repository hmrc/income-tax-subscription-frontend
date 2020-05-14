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

package controllers.agent.business

import config.featureswitch.FeatureSwitching
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.IntegrationTestModels._
import helpers.agent.servicemocks.{AuthStub, KeystoreStub}
import models.common.AccountingMethodModel
import models.individual.business.{AccountingPeriodModel, MatchTaxYearModel}
import models.individual.subscription.Both
import models.{Accruals, Cash, Yes}
import play.api.http.Status._
import utilities.CacheConstants

class BusinessAccountingMethodControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "GET /business/accounting-method" when {
    "keystore returns all data" should {
      "show the accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingMethod()

        val expectedText = removeHtmlMarkup(messages("agent.business.accounting_method.cash"))

        Then("Should return a OK with the accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.business.accounting_method.title")),
          radioButtonSet(id = "accountingMethod", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "keystore returns no data" should {
      "show the accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(Both), matchTaxYear = Some(MatchTaxYearModel(Yes))))

        When("GET /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingMethod()

        Then("Should return a OK with the accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.business.accounting_method.title")),
          radioButtonSet(id = "accountingMethod", selectedRadioButton = None)
        )
      }
    }
  }

  "POST /business/accounting-method" when {
    "the user is in the both flow" when {
      "an option is selected on the accounting method page" should {
        "redirect the user to the property accounting method page" in {
          val userInput = AccountingMethodModel(Cash)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod, userInput)
          KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(Both), matchTaxYear = Some(MatchTaxYearModel(Yes))))

          When("POST /business/accounting-method is called")
          val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(propertyAccountingMethodURI)
          )

          KeystoreStub.verifyKeyStoreSave(CacheConstants.AccountingMethod, userInput, Some(1))
        }
      }
    }
    "not in edit mode" should {
      "select the Cash radio button on the accounting method page" in {
        val userInput = AccountingMethodModel(Cash)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(Both), matchTaxYear = Some(MatchTaxYearModel(Yes))))
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod, userInput)

        When("POST /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyAccountingMethodURI)
        )
      }

      "select the Accruals radio button on the accounting method page" in {
        val userInput = AccountingMethodModel(Accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(Both), matchTaxYear = Some(MatchTaxYearModel(Yes))))
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod, userInput)

        When("POST /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyAccountingMethodURI)
        )
      }
    }

    "not select an option on the accounting method page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(Both), matchTaxYear = Some(MatchTaxYearModel(Yes))))
      KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod, "")

      When("POST /business/accounting-method is called")
      val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, None)

      Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
      res should have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }

    "in edit mode" should {
      "changing to the Accruals radio button on the accounting method page" in {
        val keystoreIncomeSource = Both
        val keystoreAccountingPeriodDates: AccountingPeriodModel = testAccountingPeriod
        val keystoreAccountingMethod = AccountingMethodModel(Cash)
        val userInput = AccountingMethodModel(Accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            accountingPeriodDate = Some(keystoreAccountingPeriodDates),
            accountingMethod = Some(keystoreAccountingMethod),
            matchTaxYear = Some(MatchTaxYearModel(Yes))
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

      "simulate not changing accounting method when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Both
        val keystoreAccountingPeriodDates: AccountingPeriodModel = testAccountingPeriod
        val keystoreAccountingMethod = AccountingMethodModel(Cash)
        val userInput = AccountingMethodModel(Accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            accountingPeriodDate = Some(keystoreAccountingPeriodDates),
            accountingMethod = Some(keystoreAccountingMethod),
            matchTaxYear = Some(MatchTaxYearModel(Yes))
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
    }
  }
}
