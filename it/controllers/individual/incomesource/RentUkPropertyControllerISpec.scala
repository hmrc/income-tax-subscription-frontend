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

import core.config.featureswitch.PropertyCashOrAccruals
import core.models.{No, Yes}
import core.services.CacheConstants
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import incometax.incomesource.models.RentUkPropertyModel
import play.api.http.Status._
import play.api.i18n.Messages

class RentUkPropertyControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    disable(PropertyCashOrAccruals)
    super.beforeEach()
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
    "not in edit mode" when {
      "the user rents a uk property and has other income" in {
        val userInput: RentUkPropertyModel = RentUkPropertyModel(Yes, Some(No))

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, userInput)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of are you self employed")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(areYouSelfEmployedURI)
        )
      }
      "the user rents a uk property and doesn't have other income" when {
        "the property cash accruals feature switch is enabled" in {
          val userInput: RentUkPropertyModel = RentUkPropertyModel(Yes, Some(Yes))

          Given("I setup the wiremock stubs and feature switch")
          enable(PropertyCashOrAccruals)
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, userInput)

          When("POST /rent-uk-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = false, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of property accounting method")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingMethodPropertyURI)
          )
        }
        "the property cash accruals feature switch is disabled" in {
          val userInput: RentUkPropertyModel = RentUkPropertyModel(Yes, Some(Yes))

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, userInput)

          When("POST /rent-uk-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = false, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answers")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }
      "the user does not rent a uk property" in {
        val userInput: RentUkPropertyModel = RentUkPropertyModel(No, None)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, userInput)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of are you self employed")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(areYouSelfEmployedURI)
        )
      }
    }

    "in edit mode" when {
      "the user selects a different answer" in {
        val userInput: RentUkPropertyModel = RentUkPropertyModel(Yes, Some(No))

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreDataV2(rentUkProperty = Some(RentUkPropertyModel(No, None))))
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, userInput)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of are you self employed")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(areYouSelfEmployedURI)
        )
      }
      "the user selects the same answer" in {
        val userInput: RentUkPropertyModel = RentUkPropertyModel(Yes, Some(Yes))

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreDataV2(rentUkProperty = Some(userInput)))
        KeystoreStub.stubKeystoreSave(CacheConstants.RentUkProperty, userInput)

        When("POST /rent-uk-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitRentUkProperty(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }
  }

}