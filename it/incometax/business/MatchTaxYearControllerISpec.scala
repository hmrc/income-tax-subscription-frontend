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

package incometax.business

import core.services.CacheConstants
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import incometax.business.forms.MatchTaxYearForm
import incometax.business.models.MatchTaxYearModel
import play.api.http.Status._
import play.api.i18n.Messages

class MatchTaxYearControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/match-to-tax-year" when {

    "keystore returns all data" should {
      "show the match tax year page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /business/match-to-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.matchTaxYear()

        Then("Should return a OK with the match tax yearpage")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("business.match_tax_year.title")),
          radioButtonSet(id = "matchTaxYear", selectedRadioButton = Some(Messages("base.no")))
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
          pageTitle(Messages("business.match_tax_year.title")),
          radioButtonSet(id = "matchTaxYear", selectedRadioButton = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/match-to-tax-year" when {

    "always" should {

      "select the Yes radio button on the match tax year page" in {
        val userInput = MatchTaxYearModel(MatchTaxYearForm.option_yes)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.MatchTaxYear, userInput)

        When("POST /business/match-to-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of accounting methods")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessAccountingMethodURI)
        )
      }

      "select the No radio button on the match tax year page" in {
        val userInput = MatchTaxYearModel(MatchTaxYearForm.option_no)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.MatchTaxYear, userInput)

        When("POST /business/match-to-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of acocunting period dates")
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

      "select invalid option on the match tax year page as if the user it trying to manipulate the html" in {
        val userInput = MatchTaxYearModel("madeup")

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.MatchTaxYear, "madeup")

        When("POST /business/match-to-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(inEditMode = false, Some(userInput))

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }
  }
}