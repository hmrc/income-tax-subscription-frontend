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

package agent.controllers

import _root_.agent.helpers.ComponentSpecBase
import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.IntegrationTestModels._
import _root_.agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import _root_.agent.services.CacheConstants
import core.config.featureswitch.{EligibilityPagesFeature, FeatureSwitching, AgentPropertyCashOrAccruals}
import core.models.{No, Yes}
import incometax.subscription.models.{Both, Business, Property}
import play.api.http.Status._
import play.api.i18n.Messages

class OtherIncomeControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EligibilityPagesFeature)
    disable(AgentPropertyCashOrAccruals)
  }

  "GET /income-other" when {
    "keystore returns all data" should {
      "show the other income page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.otherIncome()

        Then("Should return a OK with the other income page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.income-other.title")),
          radioButtonSet(id = "choice", selectedRadioButton = Some(Messages("base.no")))
        )
      }
    }

    "keystore returns no data for other income and" when {
      "income source is returned" should {
        "show the other income page without an option selected" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(testIncomeSourceBoth)))

          When("GET /income-other is called")
          val res = IncomeTaxSubscriptionFrontend.otherIncome()

          Then("Should return a OK with the income source page")
          res should have(
            httpStatus(OK),
            pageTitle(Messages("agent.income-other.title")),
            radioButtonSet(id = "choice", selectedRadioButton = None)
          )
        }
      }

      "Income source is not returned" should {
        "redirect to income source" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubEmptyKeystore()

          When("GET /income-other is called")
          val res = IncomeTaxSubscriptionFrontend.otherIncome()

          Then("Should return a OK with the income source page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(incomeSourceURI)
          )
        }
      }
    }

  }

  "POST /client/income-other" when {
    "not in edit mode" should {
      "if income source is not in key store redirect to income page" in {
        val keystoreIncomeSource = Business
        val keystoreOtherIncome = No
        val userInput = Yes

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = None, otherIncome = Some(keystoreOtherIncome)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of income source")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourceURI)
        )
      }

      "select the Yes other income radio button on the other income page" in {
        val userInput = Yes

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(testIncomeSourceBoth)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of error other income")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(errorOtherIncomeURI)
        )
      }

      "select the No other income radio button on the other income page while on Business journey" in {
        val keystoreIncomeSource = Business
        val userInput = No

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of match tax year")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(matchTaxYearURI)
        )
      }

      "select the No other income radio button on the other income page while on Both journey" in {
        val keystoreIncomeSource = Both
        val userInput = No

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of match tax year")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(matchTaxYearURI)
        )
      }

      "select the No other income radio button on the other income page while on Property journey" in {
        val keystoreIncomeSource = Property
        val userInput = No

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of terms")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(termsURI)
        )
      }

      "select the No other income radio button on the other income page while on Property journey and the cash accruals property feature switch is enabled" in {
        val keystoreIncomeSource = Property
        val userInput = No
        enable(AgentPropertyCashOrAccruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of terms")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyAccountingMethodURI)
        )
      }

      "select the No other income radio button on the other income page while on Property journey and the eligibility pages feature switch is enabled" in {
        val keystoreIncomeSource = Property
        val userInput = No
        enable(EligibilityPagesFeature)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of terms")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "income source is not present" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, testIncomeSourceBoth)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, None)

        Then("Should redirect user to income source")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourceURI)
        )
        KeystoreStub.verifyKeyStoreSave(CacheConstants.OtherIncome, "", Some(0))
      }

      "not select an option on the other income page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(testIncomeSourceBoth)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, "")

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "in edit mode" should {
      "if income source is not in key store redirect to income page" in {
        val keystoreIncomeSource = Business
        val keystoreOtherIncome = No
        val userInput = Yes

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = None, otherIncome = Some(keystoreOtherIncome)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of income source")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourceURI)
        )
      }

      "changing to the Yes other income radio button on the other income page" in {
        val keystoreIncomeSource = Business
        val keystoreOtherIncome = No
        val userInput = Yes

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource), otherIncome = Some(keystoreOtherIncome)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of error other income")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(errorOtherIncomeURI)
        )
      }

      "simulate not changing other income when already selected no on the other income page" in {
        val keystoreIncomeSource = Business
        val keystoreOtherIncome = No
        val userInput = No

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource), otherIncome = Some(keystoreOtherIncome)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }
  }
}