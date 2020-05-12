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

import config.featureswitch
import config.featureswitch.FeatureSwitching
import helpers.IntegrationTestConstants.checkYourAnswersURI
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import helpers.{ComponentSpecBase, IntegrationTestModels}
import models.individual.business.BusinessPhoneNumberModel
import models.individual.subscription.Both
import play.api.http.Status._
import play.api.i18n.Messages
import utilities.individual.CacheConstants

class BusinessPhoneNumberControllerISpec extends ComponentSpecBase with FeatureSwitching {

  enable(featureswitch.RegistrationFeature)

  "GET /report-quarterly/income-and-expenses/sign-up/business/phone-number" when {

    "keystore returns all data" should {
      "show the business phone number page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystoreBothPost()

        When("GET /business/phone-number is called")
        val res = IncomeTaxSubscriptionFrontend.businessPhoneNumber()

        Then("Should return a OK with the business phone number page with populated business number")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.phone_number.title")),
          textField("phoneNumber", testBusinessPhoneNumber.phoneNumber)
        )
      }
    }

    "keystore returns no data" should {
      "show the business phone number page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("GET /business/phone-number is called")
        val res = IncomeTaxSubscriptionFrontend.businessPhoneNumber()

        Then("Should return a OK with the business phone number page with no business phone number")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.phone_number.title")),
          textField("phoneNumber", "")
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/phone-number" when {

    "not in edit mode" should {

      "enter business phone number" in {
        val userInput: BusinessPhoneNumberModel = IntegrationTestModels.testBusinessPhoneNumber

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.BusinessPhoneNumber, userInput)

        When("POST /business/phone-number is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessPhoneNumber(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.business.routes.BusinessAddressController.show().url)
        )
      }

      "do not enter business phone number" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.BusinessPhoneNumber, "")

        When("POST /business/phone-number is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessPhoneNumber(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "select invalid business phone number option on the business phone number page as if the user it trying to manipulate the html" in {
        val userInput = BusinessPhoneNumberModel("ἄλφα")

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.BusinessPhoneNumber, userInput)

        When("POST /business/phone-number is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessPhoneNumber(inEditMode = false, Some(userInput))

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "in edit mode" should {
      "simulate not changing business phone number when calling page from Check Your Answers" in {
        val userInput: BusinessPhoneNumberModel = IntegrationTestModels.testBusinessPhoneNumber

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.BusinessPhoneNumber, userInput)

        When("POST /business/phone-number is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessPhoneNumber(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing business phone number when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Both
        val keystoreBusinessPhoneNumber = BusinessPhoneNumberModel("07890123456")
        val userInput: BusinessPhoneNumberModel = IntegrationTestModels.testBusinessPhoneNumber

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            businessPhoneNumber = Some(keystoreBusinessPhoneNumber)
          )
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.BusinessPhoneNumber, userInput)

        When("POST /business/phone-number is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessPhoneNumber(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }

  }
}
