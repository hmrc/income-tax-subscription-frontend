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

import _root_.agent.helpers.ComponentSpecBase
import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.IntegrationTestModels._
import _root_.agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import _root_.agent.services.CacheConstants
import agent.models._
import core.config.featureswitch.{EligibilityPagesFeature, FeatureSwitching}
import core.models.{Accruals, Cash, No}
import incometax.business.models.AccountingPeriodModel
import incometax.subscription.models.Both
import play.api.http.Status._
import play.api.i18n.Messages

class PropertyAccountingMethodControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EligibilityPagesFeature)
  }

  "GET /business/accounting-method-property" when {
    "keystore returns all data" should {
      "show the property accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.propertyAccountingMethod()

        val expectedText = removeHtmlMarkup(Messages("agent.property.accounting_method.radio.cash"))

        Then("Should return a OK with the property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.property.accounting_method.title")),
          radioButtonSet(id = "accountingMethodProperty", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "keystore returns no data" should {
      "show the property accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(None))

        When("GET /business/property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.propertyAccountingMethod()

        Then("Should return a OK with the property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.property.accounting_method.title")),
          radioButtonSet(id = "accountingMethodProperty", selectedRadioButton = None)
        )
      }
    }
  }

  "POST /business/accounting-method-property" when {
    "not in edit mode"  should {
      "the eligibility pages feature switch is enabled"should {
        "select the Cash radio button on the Property Accounting Method page" in {
          val userInput = AccountingMethodPropertyModel(Cash)
          enable(EligibilityPagesFeature)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethodProperty, userInput)

          When("POST /business/accounting-method-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Check Your Answers")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }

        "select the Accruals radio button on the Property Accounting Method page" in {
          enable(EligibilityPagesFeature)
          val userInput = AccountingMethodPropertyModel(Accruals)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethodProperty, userInput)

          When("POST /business/accounting-method-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Check Your Answers")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the eligibility pages feature switch is disabled"should {
        "select the Cash radio button on the Property Accounting Method page" in {
          val userInput = AccountingMethodPropertyModel(Cash)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethodProperty, userInput)

          When("POST /business/accounting-method-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Terms page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(termsURI)
          )
        }

        "select the Accruals radio button on the Property Accounting Method page" in {
          val userInput = AccountingMethodPropertyModel(Accruals)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethodProperty, userInput)

          When("POST /business/accounting-method-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Terms page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(termsURI)
          )
        }
      }

      "not select an option on the Property Accounting Method page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethodProperty, "")

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
          val keystoreAccountingPeriodDates: AccountingPeriodModel = testAccountingPeriod
          val keystoreAccountingMethodProperty = AccountingMethodPropertyModel(Cash)
          val userInput = AccountingMethodPropertyModel(Accruals)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(
            keystoreData(
              incomeSource = Some(keystoreIncomeSource),
              otherIncome = Some(keystoreIncomeOther),
              accountingPeriodDate = Some(keystoreAccountingPeriodDates),
              accountingMethodProperty = Some(keystoreAccountingMethodProperty)
            )
          )
          KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethodProperty, userInput)

          When("POST /business/accounting-method-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = true, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }

        "simulate not changing Property Accounting Method when calling page from Check Your Answers" in {
          val keystoreIncomeSource = Both
          val keystoreIncomeOther = No
          val keystoreAccountingPeriodDates: AccountingPeriodModel = testAccountingPeriod
          val keystoreAccountingMethodProperty = AccountingMethodPropertyModel(Cash)
          val userInput = AccountingMethodPropertyModel(Cash)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(
            keystoreData(
              incomeSource = Some(keystoreIncomeSource),
              otherIncome = Some(keystoreIncomeOther),
              accountingPeriodDate = Some(keystoreAccountingPeriodDates),
              accountingMethodProperty = Some(keystoreAccountingMethodProperty)
            )
          )
          KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethodProperty, userInput)

          When("POST /business/accounting-method-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = true, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Check Your Answers")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }
    }
  }
}

