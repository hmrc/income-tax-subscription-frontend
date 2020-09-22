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
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.IntegrationTestModels._
import helpers.agent.servicemocks.AuthStub
import models.common.{AccountingMethodModel, IncomeSourceModel}
import models.{Accruals, Cash}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys

class BusinessAccountingMethodControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "GET /business/accounting-method" when {
    "the Subscription Details Connector returns all data" should {
      "show the accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()

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

    "the Subscription Details Connector returns no data" should {
      "show the accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(IncomeSourceModel(true, true, false))))

          When ("GET /business/accounting-method is called")

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

          val expectedCacheMap = CacheMap("", Map(
            SubscriptionDataKeys.IncomeSource -> Json.toJson(IncomeSourceModel(true, true, false)),
            SubscriptionDataKeys.AccountingMethod -> Json.toJson(AccountingMethodModel(Cash))))

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData( subscriptionData(incomeSource = Some(IncomeSourceModel(true, true, false))))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingMethod, userInput)

          When ("POST /business/accounting-method is called")

          val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(propertyAccountingMethodURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySubscriptionSave(SubscriptionDataKeys.AccountingMethod, expectedCacheMap, Some(1))
        }
      }
    }
    "not in edit mode" should {
      "select the Cash radio button on the accounting method page" in {
        val userInput = AccountingMethodModel(Cash)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(IncomeSourceModel(true, true, false))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingMethod, userInput)

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
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData( subscriptionData(incomeSource = Some(IncomeSourceModel(true, true, false))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingMethod, userInput)

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
      IncomeTaxSubscriptionConnectorStub.stubSubscriptionData( subscriptionData(incomeSource = Some(IncomeSourceModel(true, true, false))))
      IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingMethod, "")

      When ("POST /business/accounting-method is called")

      val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, None)

      Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
      res should have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }

    "in edit mode" should {
      "changing to the Accruals radio button on the accounting method page" in {
        val SubscriptionDetailsIncomeSource = IncomeSourceModel(true, true, false)
        val SubscriptionDetailsAccountingMethod = AccountingMethodModel(Cash)
        val userInput = AccountingMethodModel(Accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingMethod, userInput)
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(
            incomeSource = Some(SubscriptionDetailsIncomeSource),
            accountingMethod = Some(SubscriptionDetailsAccountingMethod)
          )
        )

        When("POST /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate not changing accounting method when calling page from Check Your Answers" in {
        val SubscriptionDetailsIncomeSource = IncomeSourceModel(true, true, false)
        val SubscriptionDetailsAccountingMethod = AccountingMethodModel(Cash)
        val userInput = AccountingMethodModel(Accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            incomeSource = Some(SubscriptionDetailsIncomeSource),
            accountingMethod = Some(SubscriptionDetailsAccountingMethod)
          )
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingMethod, userInput)

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
