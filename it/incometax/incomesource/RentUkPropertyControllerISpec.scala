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

package incometax.incomesource


import core.config.featureswitch.NewIncomeSourceFlowFeature
import core.services.CacheConstants
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import incometax.incomesource.forms.RentUkPropertyForm
import incometax.incomesource.models.RentUkPropertyModel
import play.api.http.Status._
import play.api.i18n.Messages
import helpers.IntegrationTestModels._
class RentUkPropertyControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(NewIncomeSourceFlowFeature)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    disable(NewIncomeSourceFlowFeature)
  }


  "GET /report-quarterly/income-and-expenses/sign-up/rent-uk-property" when {

    "keystore returns all data" should {
      "show the rent uk property page with the options selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.rentUkProperty()

        Then("Should return a OK with the rent uk property page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("rent_uk_property.title")),
          radioButtonSet(id = "rentUkProperty", selectedRadioButton = Some(Messages("base.yes"))),
          radioButtonSet(id = "onlySourceOfSelfEmployedIncome", selectedRadioButton = Some(Messages("base.no")))
        )
      }
    }

    "keystore returns no data" should {
      "show the rent uk property page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("GET /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.rentUkProperty()

        Then("Should return a OK with the rent uk property page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("rent_uk_property.title")),
          radioButtonSet(id = "rentUkProperty", selectedRadioButton = None),
          radioButtonSet(id = "onlySourceOfSelfEmployedIncome", selectedRadioButton = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/rent-uk-property" when {

    "not in edit mode" should {
      "select the No rent uk property radio button on the rent uk property page" in {
        val userInput = RentUkPropertyModel(RentUkPropertyForm.option_no, None)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, userInput)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = false, Some(userInput))


        /**todo**/
        Then("Should return a NOT_IMPLEMENTED")
        res should have(
          httpStatus(NOT_IMPLEMENTED)
        )
      }

      "select the Yes rent uk property radio button and No to only income source on the rent uk property page" in {
        val userInput = RentUkPropertyModel(RentUkPropertyForm.option_yes, Some(RentUkPropertyForm.option_no))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, userInput)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = false, Some(userInput))

        /**todo**/
        Then("Should return a NOT_IMPLEMENTED")
        res should have(
          httpStatus(NOT_IMPLEMENTED)
        )
      }


      "select the Yes rent uk property radio button and Yes to only income source on the rent uk property page" in {
        val userInput = RentUkPropertyModel(RentUkPropertyForm.option_yes, Some(RentUkPropertyForm.option_yes))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, userInput)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER and redirect to the other income page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(otherIncomeURI)
        )
      }
    }

    "when in edit mode" should {

      "simulate not changing rent uk property from No when calling page from Check Your Answers" in {
        val keystoreRentUkProperty = RentUkPropertyModel(RentUkPropertyForm.option_no, None)
        val userInput = RentUkPropertyModel(RentUkPropertyForm.option_no, None)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = Some(keystoreRentUkProperty)))
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, keystoreRentUkProperty)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate not changing rent uk property from Yes and only income source from No when calling page from Check Your Answers" in {
        val keystoreRentUkProperty = RentUkPropertyModel(RentUkPropertyForm.option_yes, Some(RentUkPropertyForm.option_no))
        val userInput =RentUkPropertyModel(RentUkPropertyForm.option_yes, Some(RentUkPropertyForm.option_no))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = Some(keystoreRentUkProperty)))
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, keystoreRentUkProperty)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate not changing rent uk property from Yes and only income source from Yes when calling page from Check Your Answers" in {
        val keystoreRentUkProperty = RentUkPropertyModel(RentUkPropertyForm.option_yes, Some(RentUkPropertyForm.option_yes))
        val userInput = RentUkPropertyModel(RentUkPropertyForm.option_yes, Some(RentUkPropertyForm.option_yes))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = Some(keystoreRentUkProperty)))
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, keystoreRentUkProperty)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing rent uk property from No when calling page from Check Your Answers" in {
        val keystoreRentUkProperty = RentUkPropertyModel(RentUkPropertyForm.option_no, None)
        val userInput = RentUkPropertyModel(RentUkPropertyForm.option_yes, Some(RentUkPropertyForm.option_no))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = Some(keystoreRentUkProperty)))
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, keystoreRentUkProperty)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = true, Some(userInput))

        Then("Should return a NOT_IMPLEMENTED")
        res should have(
          httpStatus(NOT_IMPLEMENTED)
        )
      }

      "simulate changing rent uk property from Yes and only income source from No when calling page from Check Your Answers" in {
        val keystoreRentUkProperty = RentUkPropertyModel(RentUkPropertyForm.option_yes, Some(RentUkPropertyForm.option_no))
        val userInput =RentUkPropertyModel(RentUkPropertyForm.option_no, None)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = Some(keystoreRentUkProperty)))
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, keystoreRentUkProperty)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = true, Some(userInput))

        Then("Should return a NOT_IMPLEMENTED")
        res should have(
          httpStatus(NOT_IMPLEMENTED)
        )
      }

      "simulate changing rent uk property from Yes and only income source from Yes when calling page from Check Your Answers" in {
        val keystoreRentUkProperty = RentUkPropertyModel(RentUkPropertyForm.option_yes, Some(RentUkPropertyForm.option_yes))
        val userInput = RentUkPropertyModel(RentUkPropertyForm.option_no, None)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = Some(keystoreRentUkProperty)))
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, keystoreRentUkProperty)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = true, Some(userInput))

        Then("Should return a NOT_IMPLEMENTED")
        res should have(
          httpStatus(NOT_IMPLEMENTED)
        )
      }
    }
  }
}