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

import core.services.CacheConstants
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import models.individual.incomesource.RentUkPropertyModel
import play.api.http.Status._
import play.api.i18n.Messages

class AreYouSelfEmployedControllerISpec extends ComponentSpecBase {

  def setRentUkPropertyInKeystore(rentUkProperty: Option[RentUkPropertyModel]): Unit =
    KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = rentUkProperty))

  "GET /report-quarterly/income-and-expenses/sign-up/are-you-self-employed" when {

    "keystore returns all data" should {
      "show the income source page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystoreBothPost()

        When("GET /income is called")
        val res = IncomeTaxSubscriptionFrontend.areYouSelfEmployed()

        Then("Should return a OK with the are you self-employed page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("are_you_selfemployed.title")),
          radioButtonSet(id = "choice-Yes", selectedRadioButton = Some(Messages("base.yes")))
        )
      }
    }

    "keystore returns no data for are you self-employed" should {
      "show the income source page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        setRentUkPropertyInKeystore(Some(testRentUkProperty_property_and_other))

        When("GET /income is called")
        val res = IncomeTaxSubscriptionFrontend.areYouSelfEmployed()

        Then("Should return a OK with the are you self-employed page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("are_you_selfemployed.title")),
          radioButtonSet(id = "choice-Yes", selectedRadioButton = None)
        )
      }
    }

    "keystore returns no data for rent uk property" should {
      "show the income source page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        setRentUkPropertyInKeystore(None)

        When("GET /income is called")
        val res = IncomeTaxSubscriptionFrontend.areYouSelfEmployed()

        Then("Should return a SEE_OTHER to the rent uk property page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(rentUkPropertyURI)
        )
      }
    }

  }


  "POST /report-quarterly/income-and-expenses/sign-up/are-you-self-employed" when {

    "the user does not select a value" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("POST /are-you-self-employed is called")
      val res = IncomeTaxSubscriptionFrontend.submitAreYouSelfEmployed(inEditMode = false, None)

      Then(s"Should return a $BAD_REQUEST")
      res should have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }

    "not in edit mode" when {
      "the user rents a uk property and they select they are self employed" in {
        val userInput = testAreYouSelfEmployed_yes

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        setRentUkPropertyInKeystore(Some(testRentUkProperty_property_and_other))
        KeystoreStub.stubKeystoreSave(CacheConstants.AreYouSelfEmployed)

        When("POST /are you self-employed is called")
        val res = IncomeTaxSubscriptionFrontend.submitAreYouSelfEmployed(inEditMode = false, Some(userInput))

        Then(s"return a $SEE_OTHER with a redirect location of business name")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
      "the user rents a uk property and they select they are not self employed" in {
          val userInput = testAreYouSelfEmployed_no

          Given("I setup the wiremock stubs and feature switch")
          AuthStub.stubAuthSuccess()
          setRentUkPropertyInKeystore(Some(testRentUkProperty_property_and_other))
          KeystoreStub.stubKeystoreSave(CacheConstants.AreYouSelfEmployed)

          When("POST /are you self-employed is called")
          val res = IncomeTaxSubscriptionFrontend.submitAreYouSelfEmployed(inEditMode = false, Some(userInput))

          Then(s"return a $SEE_OTHER with a redirect location of property accounting method")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingMethodPropertyURI)
          )
      }
      "the user does not rent a uk property and they select they are self employed" in {
        val userInput = testAreYouSelfEmployed_yes

        Given("I setup the wiremock stubs and feature switch")
        AuthStub.stubAuthSuccess()
        setRentUkPropertyInKeystore(Some(testRentUkProperty_no_property))
        KeystoreStub.stubKeystoreSave(CacheConstants.AreYouSelfEmployed)

        When("POST /are you self-employed is called")
        val res = IncomeTaxSubscriptionFrontend.submitAreYouSelfEmployed(inEditMode = false, Some(userInput))

        Then(s"return a $SEE_OTHER with a redirect location of business name")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
      "the user does not rent a uk property and they select they are not self employed" in {
        val userInput = testAreYouSelfEmployed_no

        Given("I setup the wiremock stubs and feature switch")
        AuthStub.stubAuthSuccess()
        setRentUkPropertyInKeystore(Some(testRentUkProperty_no_property))
        KeystoreStub.stubKeystoreSave(CacheConstants.AreYouSelfEmployed)

        When("POST /are you self-employed is called")
        val res = IncomeTaxSubscriptionFrontend.submitAreYouSelfEmployed(inEditMode = false, Some(userInput))

        Then(s"return a $SEE_OTHER with a redirect location of cannot sign up")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(cannotSignUpURI)
        )
      }
    }

    "in edit mode" when {
      "the user changes their answer" in {
        val userInput = testAreYouSelfEmployed_yes

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = Some(testRentUkProperty_property_and_other),
          areYouSelfEmployed = Some(testAreYouSelfEmployed_no)))
        KeystoreStub.stubKeystoreSave(CacheConstants.AreYouSelfEmployed)

        When("POST /are you self-employed is called")
        val res = IncomeTaxSubscriptionFrontend.submitAreYouSelfEmployed(inEditMode = true, Some(userInput))

        Then(s"return a $SEE_OTHER with a redirect location of business name")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "the user keeps their answer the same" in {
        val userInput = testAreYouSelfEmployed_yes

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = Some(testRentUkProperty_property_and_other), areYouSelfEmployed = Some(userInput)))
        KeystoreStub.stubKeystoreSave(CacheConstants.AreYouSelfEmployed)

        When("POST /are you self-employed is called")
        val res = IncomeTaxSubscriptionFrontend.submitAreYouSelfEmployed(inEditMode = true, Some(userInput))

        Then(s"return a $SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }

  }

}
