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

import core.config.featureswitch.{NewIncomeSourceFlowFeature, TaxYearDeferralFeature}
import core.services.CacheConstants
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import incometax.incomesource.forms.WorkForYourselfForm
import incometax.incomesource.models.{RentUkPropertyModel, WorkForYourselfModel}
import play.api.http.Status._
import play.api.i18n.Messages

class WorkForYourselfControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(TaxYearDeferralFeature)
    enable(NewIncomeSourceFlowFeature)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    disable(TaxYearDeferralFeature)
    disable(NewIncomeSourceFlowFeature)
  }

  def setRentUkPropertyInKeystore(rentUkProperty: Option[RentUkPropertyModel]): Unit =
    KeystoreStub.stubKeystoreData(keystoreData(rentUkProperty = rentUkProperty))

  "GET /report-quarterly/income-and-expenses/sign-up/work-for-yourself" when {

    "keystore returns all data" should {
      "show the income source page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /income is called")
        val res = IncomeTaxSubscriptionFrontend.workForYourself()

        Then("Should return a OK with the work for yourself page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("work_for_yourself.title")),
          radioButtonSet(id = "choice-Yes", selectedRadioButton = Some(Messages("base.yes")))
        )
      }
    }

    "keystore returns no data for work for yourself" should {
      "show the income source page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        setRentUkPropertyInKeystore(Some(testNewIncomeSourceProperty_2page.rentUkProperty))

        When("GET /income is called")
        val res = IncomeTaxSubscriptionFrontend.workForYourself()

        Then("Should return a OK with the income source page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("work_for_yourself.title")),
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
        val res = IncomeTaxSubscriptionFrontend.workForYourself()

        Then("Should return a SEE_OTHER to the rent uk property page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(rentUkPropertyURI)
        )
      }
    }

  }


  "POST /report-quarterly/income-and-expenses/sign-up/work-for-yourself" when {

    "not in edit mode" when {
      "the user answered Yes and No on the rent uk property page" when {
        "select the Yes radio button on the work for yourself page" in {
          val userInput = WorkForYourselfModel(WorkForYourselfForm.option_yes)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          setRentUkPropertyInKeystore(Some(testRentUkProperty_property_and_other))
          KeystoreStub.stubKeystoreSave(CacheConstants.WorkForYourself, userInput)
          When("POST /work-for-yourself is called")
          val res = IncomeTaxSubscriptionFrontend.submitWorkForYourself(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of other income")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(otherIncomeURI)
          )
        }

        "select the No radio button on the work for yourself page" in {
          val userInput = WorkForYourselfModel(WorkForYourselfForm.option_no)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          setRentUkPropertyInKeystore(Some(testRentUkProperty_property_and_other))
          KeystoreStub.stubKeystoreSave(CacheConstants.WorkForYourself, userInput)

          When("POST /work-for-yourself is called")
          val res = IncomeTaxSubscriptionFrontend.submitWorkForYourself(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of other income")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(otherIncomeURI)
          )
        }
      }

      "the user answered No on the rent uk property page" when {
        "select the Yes radio button on the work for yourself page" in {
          val userInput = WorkForYourselfModel(WorkForYourselfForm.option_yes)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          setRentUkPropertyInKeystore(Some(testRentUkProperty_no_property))
          KeystoreStub.stubKeystoreSave(CacheConstants.WorkForYourself, userInput)
          When("POST /work-for-yourself is called")
          val res = IncomeTaxSubscriptionFrontend.submitWorkForYourself(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of other income")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(otherIncomeURI)
          )
        }

        "select the No radio button on the work for yourself page" in {
          val userInput = WorkForYourselfModel(WorkForYourselfForm.option_no)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          setRentUkPropertyInKeystore(Some(testRentUkProperty_no_property))
          KeystoreStub.stubKeystoreSave(CacheConstants.WorkForYourself, userInput)

          When("POST /work-for-yourself is called")
          val res = IncomeTaxSubscriptionFrontend.submitWorkForYourself(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of other income")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(cannotSignUpURI)
          )
        }
      }
    }

    "when in edit mode" when {
      "user does not change their answer be redirected back to check your answers page" in {
        val userInput = WorkForYourselfModel(WorkForYourselfForm.option_yes)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            rentUkProperty = Some(testRentUkProperty_property_and_other),
            workForYourself = Some(userInput)
          )
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.WorkForYourself, userInput)

        When("POST /work-for-yourself is called")
        val res = IncomeTaxSubscriptionFrontend.submitWorkForYourself(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answer")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }
  }
}