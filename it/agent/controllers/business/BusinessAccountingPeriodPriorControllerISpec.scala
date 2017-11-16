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
import _root_.agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import play.api.http.Status._
import play.api.i18n.Messages

class BusinessAccountingPeriodPriorControllerISpec extends ComponentSpecBase {

  "GET /business/accounting-period-prior" when {

    "keystore returns all data" should {
      "show the accounting period prior page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /business/accounting-period-prior is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingPeriodPrior()

        Then("Should return a OK with the accounting period prior page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.business.current_financial_period_prior.title")),
          radioButtonSet(id = "accountingPeriodPrior", selectedRadioButton = Some(Messages("agent.business.current_financial_period_prior.no")))
        )
      }
    }

    "keystore returns no data" should {
      "show the other income page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("GET /business/accounting-period-prior is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingPeriodPrior()

        Then("Should return a OK with the accounting period prior page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.business.current_financial_period_prior.title")),
          radioButtonSet(id = "accountingPeriodPrior", selectedRadioButton = None)
        )
      }
    }

  }

  "POST /business/accounting-period-prior" when {

    "always" should {

      "select the Yes current accounting period radio button on the accounting period prior page" in {
        val userInput = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodPrior, userInput)

        When("POST /business/accounting-period-prior is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessAccountingPeriodPrior(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of register next period")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(registerNextAccountingPeriodURI)
        )
      }

      "select the No current accounting period radio button on the accounting period prior page" in {
        val userInput = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodPrior, userInput)

        When("POST /business/accounting-period-prior is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessAccountingPeriodPrior(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of register next period")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingPeriodDatesURI)
        )
      }


      "select no option on the radio buttons on the accounting period prior page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodPrior, "")

        When("POST /business/accounting-period-prior is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessAccountingPeriodPrior(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "select invalid other income option on the accounting period prior page as if the user it trying to manipulate the html" in {
        val userInput = AccountingPeriodPriorModel("madeup")

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodPrior, "madeup")

        When("POST /business/accounting-period-prior is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessAccountingPeriodPrior(inEditMode = false, Some(userInput))

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }
  }
}