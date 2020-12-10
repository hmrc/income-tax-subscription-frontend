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

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.subscriptionData
import helpers.servicemocks.AuthStub
import models.common.IncomeSourceModel
import models.common.business.AccountingMethodModel
import models.{Accruals, Cash}
import play.api.http.Status._
import utilities.SubscriptionDataKeys

class BusinessAccountingMethodControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/accounting-method" when {

    "the Subscription Details Connector returns all data" should {
      "show the accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()

        When("GET /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingMethod()

        val expectedText = removeHtmlMarkup(messages("business.accounting_method.cash"))
        val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
        Then("Should return a OK with the accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "accountingMethod", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())

        When("GET /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingMethod()
        val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
        Then("Should return a OK with the accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.accounting_method.title") +serviceNameGovUk),
          radioButtonSet(id = "accountingMethod", selectedRadioButton = None)
        )
      }
    }

  }

  s"POST ${controllers.individual.business.routes.BusinessAccountingMethodController.submit().url}" when {

    "the user does not select an answer" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()

      When(s"POST ${controllers.individual.business.routes.BusinessAccountingMethodController.submit().url}")
      val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, request = None)

      Then(s"Should return a $BAD_REQUEST")
      res should have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }

    "not in edit mode" when {
      "the user rents a uk property " should {
        s"redirect to ${controllers.individual.business.routes.PropertyAccountingMethodController.show().url}" in {
          val userInput: AccountingMethodModel = AccountingMethodModel(Cash)

          Given("I setup the wiremock stubs and feature switches")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(IncomeSourceModel(true, true, false))))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingMethod)

          When(s"POST ${controllers.individual.business.routes.BusinessAccountingMethodController.submit().url}")
          val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, request = Some(userInput))

          Then(s"Should return a $SEE_OTHER with a redirect location of ${
            controllers.individual.business.routes.PropertyAccountingMethodController.show().url
          }")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingMethodPropertyURI)
          )
        }
      }
      "the user does not rent a uk property " should {
        s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
          val userInput: AccountingMethodModel = AccountingMethodModel(Cash)

          Given("I setup the wiremock stubs and feature switches")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingMethod)

          When(s"POST ${controllers.individual.business.routes.BusinessAccountingMethodController.submit().url}")
          val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, request = Some(userInput))

          Then(s"Should return a SEE_OTHER with a redirect location of ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }
    }

    "in edit mode" should {
      s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
        val userInput: AccountingMethodModel = AccountingMethodModel(Accruals)

        Given("I setup the wiremock stubs and feature switches")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingMethod)

        When(s"POST ${controllers.individual.business.routes.BusinessAccountingMethodController.submit().url}")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = true, request = Some(userInput))

        Then(s"Should return a $SEE_OTHER with a redirect location of ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }
  }
}
