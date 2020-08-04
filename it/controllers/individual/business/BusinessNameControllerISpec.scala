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

import helpers.IntegrationTestConstants.{accountingYearURI, businessAccountingMethodURI, checkYourAnswersURI}
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import helpers.{ComponentSpecBase, IntegrationTestModels}
import models.common.BusinessNameModel
import models.individual.incomesource.IncomeSourceModel
import models.individual.subscription.Both
import play.api.http.Status._
import utilities.CacheConstants

class BusinessNameControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/name" when {

    "keystore returns all data" should {
      "show the business name page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystoreBothPost()

        When("GET /business/name is called")
        val res = IncomeTaxSubscriptionFrontend.businessName()

        Then("Should return a OK with the business name page with populated business name")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.name.title")),
          textField("businessName", testBusinessName.businessName)
        )
      }
    }

    "keystore returns no data" should {
      "show the business name page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("GET /business/name is called")
        val res = IncomeTaxSubscriptionFrontend.businessName()

        Then("Should return a OK with the business name page with no business name")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.name.title")),
          textField("businessName", "")
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/name" when {
    "not in edit mode" when {
      "enter business name" should {
        "redirect to the accounting year page when the user is business only" in {
          val userInput: BusinessNameModel = IntegrationTestModels.testBusinessName

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(keystoreData(
            individualIncomeSource = Some(IncomeSourceModel(true, false, false))
          ))
          KeystoreStub.stubKeystoreSave(CacheConstants.BusinessName, userInput)

          When("POST /business/name is called")
          val res = IncomeTaxSubscriptionFrontend.submitBusinessName(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of match tax year")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingYearURI)
          )
        }
        "redirect to business accounting method page when the user has business and property" in {
          val userInput: BusinessNameModel = IntegrationTestModels.testBusinessName

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(keystoreData(
          ))
          KeystoreStub.stubKeystoreSave(CacheConstants.BusinessName, userInput)

          When("POST /business/name is called")
          val res = IncomeTaxSubscriptionFrontend.submitBusinessName(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of match tax year")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(businessAccountingMethodURI)
          )
        }
      }

      "do not enter business name" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()
        KeystoreStub.stubKeystoreSave(CacheConstants.BusinessName, "")

        When("POST /business/name is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessName(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "select invalid business name option on the business name page as if the user it trying to manipulate the html" in {
        val userInput = BusinessNameModel("ἄλφα")

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()
        KeystoreStub.stubKeystoreSave(CacheConstants.BusinessName, userInput)

        When("POST /business/name is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessName(inEditMode = false, Some(userInput))

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "in edit mode" should {
      "simulate not changing business name when calling page from Check Your Answers" in {
        val userInput: BusinessNameModel = IntegrationTestModels.testBusinessName

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.BusinessName, userInput)

        When("POST /business/name is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessName(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing business name when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Both
        val keystoreBusinessName = BusinessNameModel("testBusiness")
        val userInput: BusinessNameModel = IntegrationTestModels.testBusinessName

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            businessName = Some(keystoreBusinessName)
          )
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.BusinessName, userInput)

        When("POST /business/name is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessName(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }

  }
}
