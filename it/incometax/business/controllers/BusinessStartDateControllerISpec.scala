/*
 * Copyright 2017 HM Revenue & Customs
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

package incometax.business.controllers

import core.config.featureswitch
import core.config.featureswitch.FeatureSwitching
import core.services.CacheConstants._
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.libs.json.Json

class BusinessStartDateControllerISpec extends ComponentSpecBase with FeatureSwitching {

  // TODO remove this when registration is enabled by default
  enable(featureswitch.RegistrationFeature)

  "GET /report-quarterly/income-and-expenses/sign-up/business/start-date" when {

    "the keystore is empty" should {
      "return the page with no prepopulated fields" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("GET /business/start-date is called")
        val res = IncomeTaxSubscriptionFrontend.businessStartDate()

        Then("should return an OK with the business start date page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("business.start_date.title"))
        )
      }
    }

    "keystore returns a previously filled in business start date" should {
      "show the current business start date page with date values entered" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(Map(BusinessStartDate -> Json.toJson(testBusinessStartDate)))

        When("GET /business/start-date is called")
        val res = IncomeTaxSubscriptionFrontend.businessStartDate()

        Then("should return an OK with the business start date page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("business.start_date.title")),
          dateField("startDate", testStartDate)
        )
      }
    }

    "redirect to sign-in when auth fails" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubUnauthorised()

      When("GET /business/start-date is called")
      val res = IncomeTaxSubscriptionFrontend.businessStartDate()

      Then("Should return a SEE_OTHER with a redirect location of sign-in")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(signInURI)
      )
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/start-date" when {
    "the form data is valid and keystore stores it successfully" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreSave(BusinessStartDate)

      When("POST /business/start-date is called")
      val res = IncomeTaxSubscriptionFrontend.submitBusinessStartDate(inEditMode = false, Some(testBusinessStartDate))

      Then("Should return a SEE_OTHER with a redirect location of accounting period dates")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(accountingPeriodDatesURI)
      )
    }

    "the form data is valid and keystore stores it successfully on editmode" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreSave(BusinessStartDate)

      When("POST /business/start-date is called")
      val res = IncomeTaxSubscriptionFrontend.submitBusinessStartDate(inEditMode = true, Some(testBusinessStartDate))

      Then("Should return a SEE_OTHER with a redirect location of check your answers")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(checkYourAnswersURI)
      )
    }
  }
}