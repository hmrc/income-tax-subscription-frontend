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

package incometax.incomesource

import core.services.CacheConstants
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import incometax.incomesource.forms.{IncomeSourceForm, OtherIncomeForm}
import incometax.incomesource.models.{IncomeSourceModel, OtherIncomeModel}
import play.api.http.Status._
import play.api.i18n.Messages

class OtherIncomeControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/income-other" when {

    "keystore call fails" should {
      "internal server error" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreFailure()

        When("GET /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.otherIncome()

        Then("Should return a INTERNAL_SERVER_ERROR")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

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
          pageTitle(Messages("income-other.title")),
          radioButtonSet(id = "choice", selectedRadioButton = Some(Messages("income-other.no")))
        )
      }
    }

    "keystore returns no data" should {
      "show the other income page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("GET /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.otherIncome()

        Then("Should return a OK with the income source page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("income-other.title")),
          radioButtonSet(id = "choice", selectedRadioButton = None)
        )
      }
    }

    "redirect to sign-in when auth fails" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubUnauthorised()

      When("GET /income-other is called")
      val res = IncomeTaxSubscriptionFrontend.otherIncome()

      Then("Should return a SEE_OTHER with a redirect location of sign-in")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(ggSignInURI)
      )
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/income-other" when {

    "not in edit mode" should {

      "select the Yes other income radio button on the other income page" in {
        val userInput = OtherIncomeModel(OtherIncomeForm.option_yes)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
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
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_business)
        val userInput = OtherIncomeModel(OtherIncomeForm.option_no)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of business name")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "select the No other income radio button on the other income page while on Both journey" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_both)
        val userInput = OtherIncomeModel(OtherIncomeForm.option_no)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of business name")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "select the No other income radio button on the other income page while on Property journey" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_property)
        val userInput = OtherIncomeModel(OtherIncomeForm.option_no)

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

      "not select an option on the other income page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, "")

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "select invalid other income option on the other income page as if the user it trying to manipulate the html" in {
        val userInput = OtherIncomeModel("madeup")

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, "madeup")

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, Some(userInput))

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "keystore call fails" in {
        val userInput = OtherIncomeModel(OtherIncomeForm.option_yes)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreFailure()

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, Some(userInput))

        Then("Should return a INTERNAL_SERVER_ERROR")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "redirect to sign-in when auth fails" in {
        val userInput = OtherIncomeModel(OtherIncomeForm.option_yes)

        Given("I setup the Wiremock stubs")
        AuthStub.stubUnauthorised()

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of sign-in")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(ggSignInURI)
        )
      }
    }

    "in edit mode" should {

      "changing to the Yes other income radio button on the other income page" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_business)
        val keystoreOtherIncome = OtherIncomeModel(OtherIncomeForm.option_no)
        val userInput = OtherIncomeModel(OtherIncomeForm.option_yes)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(otherIncome = Some(keystoreOtherIncome)))
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
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_business)
        val keystoreOtherIncome = OtherIncomeModel(OtherIncomeForm.option_no)
        val userInput = OtherIncomeModel(OtherIncomeForm.option_no)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(otherIncome = Some(keystoreOtherIncome)))
        KeystoreStub.stubKeystoreSave(CacheConstants.OtherIncome, userInput)

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "keystore call fails" in {
        val userInput = OtherIncomeModel(OtherIncomeForm.option_yes)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreFailure()

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = true, Some(userInput))

        Then("Should return a INTERNAL_SERVER_ERROR")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "redirect to sign-in when auth fails" in {
        val userInput = OtherIncomeModel(OtherIncomeForm.option_yes)

        Given("I setup the Wiremock stubs")
        AuthStub.stubUnauthorised()

        When("POST /income-other is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of sign-in")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(ggSignInURI)
        )
      }
    }
  }
}