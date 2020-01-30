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

import core.config.featureswitch.PropertyCashOrAccruals
import core.models.{Accruals, Cash, No, Yes}
import core.services.CacheConstants
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.{keystoreData, testMatchTaxYearYes}
import helpers.servicemocks.{AuthStub, KeystoreStub}
import incometax.business.models.AccountingMethodModel
import incometax.incomesource.models.RentUkPropertyModel
import play.api.http.Status._
import play.api.i18n.Messages

class BusinessAccountingMethodControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    disable(PropertyCashOrAccruals)
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/business/accounting-method" when {

    "keystore returns all data" should {
      "show the accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystoreBothPost()

        When("GET /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingMethod()

        val expectedText = removeHtmlMarkup(Messages("business.accounting_method.cash"))

        Then("Should return a OK with the accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("business.accounting_method.title")),
          radioButtonSet(id = "accountingMethod", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "keystore returns no data" should {
      "show the accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(matchTaxYear = Some(testMatchTaxYearYes)))

        When("GET /business/accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingMethod()

        Then("Should return a OK with the accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("business.accounting_method.title")),
          radioButtonSet(id = "accountingMethod", selectedRadioButton = None)
        )
      }
    }

  }

  s"POST ${controllers.individual.business.routes.BusinessAccountingMethodController.submit().url}" when {

    "the user does not select an answer" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubFullKeystoreBothPost()

      When(s"POST ${controllers.individual.business.routes.BusinessAccountingMethodController.submit().url}")
      val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, request = None)

      Then(s"Should return a $BAD_REQUEST")
      res should have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }

    "not in edit mode" when {
      "the user rents a uk property and the property cash accruals feature switch is enabled" should {
        s"redirect to ${controllers.individual.business.routes.PropertyAccountingMethodController.show().url}" in {
          val userInput: AccountingMethodModel = AccountingMethodModel(Cash)

          enable(PropertyCashOrAccruals)

          Given("I setup the wiremock stubs and feature switches")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = Some(RentUkPropertyModel(Yes, Some(Yes)))))
          KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod)

          When(s"POST ${controllers.individual.business.routes.BusinessAccountingMethodController.submit().url}")
          val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, request = Some(userInput))

          Then(s"Should return a $SEE_OTHER with a redirect location of ${controllers.individual.business.routes.PropertyAccountingMethodController.show().url}")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingMethodPropertyURI)
          )
        }
      }
      "the user rents a uk property and the property cash accruals feature switch is disabled" should {
        s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
          val userInput: AccountingMethodModel = AccountingMethodModel(Accruals)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = Some(RentUkPropertyModel(Yes, Some(Yes)))))
          KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod)

          When(s"POST ${controllers.individual.business.routes.BusinessAccountingMethodController.submit().url}")
          val res = IncomeTaxSubscriptionFrontend.submitAccountingMethod(inEditMode = false, request = Some(userInput))

          Then(s"Should return a $SEE_OTHER with a redirect location of ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }
      "the user does not rent a uk property and the property cash accruals feature switch is enabled" should {
        s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
          val userInput: AccountingMethodModel = AccountingMethodModel(Cash)

          enable(PropertyCashOrAccruals)

          Given("I setup the wiremock stubs and feature switches")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = Some(RentUkPropertyModel(No, Some(Yes)))))
          KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod)

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
        KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = Some(RentUkPropertyModel(No, Some(Yes)))))
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingMethod)

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
