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

import core.config.featureswitch.FeatureSwitching
import core.services.CacheConstants
import forms.individual.business.MatchTaxYearForm
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import models.individual.business.MatchTaxYearModel
import models.{No, Yes}
import play.api.http.Status._
import play.api.i18n.Messages

class MatchTaxYearControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "GET /report-quarterly/income-and-expenses/sign-up/business/match-to-tax-year" when {

    "keystore returns all data" should {
      "show the match tax year page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystoreBothPost()

        When("GET /business/match-to-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.matchTaxYear()

        Then("Should return a OK with the match tax year page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("business.match_tax_year.title.signup")),
          radioButtonSet(id = MatchTaxYearForm.matchTaxYear, selectedRadioButton = Some(Messages("base.no")))
        )
      }
    }

    "keystore returns no data" should {
      "show the other income page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("GET /business/match-to-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.matchTaxYear()

        Then("Should return a OK with the match tax year page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("business.match_tax_year.title.signup")),
          radioButtonSet(id = "matchTaxYear", selectedRadioButton = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/match-to-tax-year" when {

    "select the Yes radio button on the match tax year page" when {
      "the user has only business income" in {

        val userInput = MatchTaxYearModel(Yes)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(rentUkProperty = Some(testRentUkProperty_no_property), areYouSelfEmployed = Some(testAreYouSelfEmployed_yes))
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.MatchTaxYear, userInput)

        When("POST /business/match-to-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of what tax year do you want to sign up for")

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingYearURI)
        )
      }
    }

    "always" should {

      "select the No radio button on the match tax year page" in {
        val userInput = MatchTaxYearModel(No)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        KeystoreStub.stubKeystoreData(
          keystoreData(rentUkProperty = Some(testRentUkProperty_no_property), areYouSelfEmployed = Some(testAreYouSelfEmployed_yes))
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.MatchTaxYear, userInput)

        When("POST /business/match-to-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of accounting period dates")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingPeriodDatesURI)
        )
      }


      "select no option on the radio buttons on the match tax year page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.MatchTaxYear, "")

        When("POST /business/match-to-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }
  }

}