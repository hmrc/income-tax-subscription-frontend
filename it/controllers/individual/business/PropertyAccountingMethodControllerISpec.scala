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

package controllers.individual.business

import core.models.{Accruals, Cash, No}
import core.services.CacheConstants
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.{keystoreData, testAccountingMethodProperty}
import helpers.servicemocks.{AuthStub, KeystoreStub}
import incometax.business.models.AccountingMethodPropertyModel
import incometax.subscription.models.Both
import play.api.http.Status._
import play.api.i18n.Messages

class PropertyAccountingMethodControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/accounting-method-property" when {

    "keystore returns pre-populated data" should {
      "show the accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        When("GET /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.propertyAccountingMethod()

        val expected = s"${Messages("property.accounting_method.radio.cash")} ${Messages("property.accounting_method.radio.cash.detail")}"

        Then("Should return a OK with the property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("property.accounting_method.title")),
          radioButtonSet(id = "accountingMethodProperty-Cash", selectedRadioButton = Some(expected))
        )
      }
    }

    "keystore returns with no pre-populated data" should {
      "show the property accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(propertyAccountingMethod = Some(testAccountingMethodProperty)))

        When("GET /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.propertyAccountingMethod()

        Then("Should return a OK with the property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("property.accounting_method.title")),
          radioButtonSet(id = "propertyAccountingMethod", selectedRadioButton = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/accounting-method-property" when {

    "not in edit mode" should {

      "select the Cash radio button on the property accounting method page" in {
        val userInput = AccountingMethodPropertyModel(Cash)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        KeystoreStub.stubKeystoreSave(CacheConstants.PropertyAccountingMethod, userInput)

        When("POST /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "select the Accruals radio button on the accounting method page" in {
        val userInput = AccountingMethodPropertyModel(Accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        KeystoreStub.stubKeystoreSave(CacheConstants.PropertyAccountingMethod, userInput)

        When("POST /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }

    "not select an option on the property accounting method page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubEmptyKeystore()
      KeystoreStub.stubKeystoreSave(CacheConstants.PropertyAccountingMethod, "")

      When("POST /business/accounting-method-property is called")

      val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, None)

      Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
      res should have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }

    "in edit mode" should {

      "changing to the Accruals radio button on the accounting method page" in {
        val keystoreIncomeSource = Both
        val keystoreIncomeOther = No
        val keystorePropertyAccountingMethod = AccountingMethodPropertyModel(Cash)
        val userInput = AccountingMethodPropertyModel(Accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther),
            propertyAccountingMethod = Some(keystorePropertyAccountingMethod)
          )
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.PropertyAccountingMethod, userInput)

        When("POST /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate not changing property accounting method when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Both
        val keystoreIncomeOther = No
        val keystorePropertyAccountingMethod = AccountingMethodPropertyModel(Cash)
        val userInput = AccountingMethodPropertyModel(Accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther),
            propertyAccountingMethod = Some(keystorePropertyAccountingMethod)
          )
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.PropertyAccountingMethod, userInput)

        When("POST /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }
  }
}
