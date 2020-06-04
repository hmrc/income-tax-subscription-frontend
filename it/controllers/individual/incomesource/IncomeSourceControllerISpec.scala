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

package controllers.individual.incomesource

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import models.individual.incomesource.{IncomeSourceModel, RentUkPropertyModel}
import models.{No, Yes}
import play.api.http.Status._
import utilities.CacheConstants

class IncomeSourceControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/details/income-receive" when {

    "keystore returns all data" should {
      "show the income source page with the options selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystoreBothPost()

        When("GET /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.incomeSource()

        Then("Should return a OK with the income source page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("income_source.title")),
          checkboxSet(id = "Business", selectedCheckbox = Some(messages("income_source.selfEmployed"))),
          checkboxSet(id = "UkProperty", selectedCheckbox = Some(messages("income_source.rentUkProperty")))
        )
      }
    }

    "keystore returns no data" should {
      "show the rent uk property page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("GET /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.incomeSource()

        Then("Should return a OK with the rent uk property page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("income_source.title")),
          checkboxSet(id = "Business", selectedCheckbox = None),
          checkboxSet(id = "UkProperty", selectedCheckbox = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/details/income-receive" when {
    "not in edit mode" when {
      "the user rents a uk property and has other income" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.IndividualIncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
      "the user rents a uk property and doesn't have other income" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(false, true)

          Given("I setup the wiremock stubs and feature switch")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.IndividualIncomeSource, userInput)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of property accounting method")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingMethodPropertyURI)
          )
      }
      "the user does not rent a uk property but have other income" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(true,false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.IndividualIncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
    }

    "in edit mode" when {
      "the user selects a different answer" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(individualIncomeSource = Some(IncomeSourceModel(true, true))))
        KeystoreStub.stubKeystoreSave(CacheConstants.IndividualIncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of property accounting method")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingMethodPropertyURI)
        )
      }
      "the user selects the same answer" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(individualIncomeSource = Some(userInput)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IndividualIncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }
  }

}