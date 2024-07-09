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

package controllers.individual

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, PreferencesFrontendConnectorStub, SessionDataConnectorStub}
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{IndividualURI, testNino}
import helpers.IntegrationTestModels.{testAccountingYearCurrent, testAccountingYearNext}
import helpers.servicemocks.AuthStub
import models.EligibilityStatus
import models.status.MandationStatus.{Mandated, Voluntary}
import models.status.MandationStatusModel
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys._

class ConfirmationControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

  "GET /confirmation" when {
    "the user is enrolled" when {
      "the confirmation page feature switch is enabled" should {
        "return the sign up confirmation page" when {
          "the user signed up for the current tax year" when {
            "the user is mandated for current year" when {
              "the user has no digital preference available" in {
                Given("I setup the Wiremock stubs")
                AuthStub.stubEnrolled()
                IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
                PreferencesFrontendConnectorStub.stubGetOptedInStatus(None)
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Mandated, Voluntary)))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

                When("GET /confirmation is called")
                val res = IncomeTaxSubscriptionFrontend.confirmation()

                Then("Should return a OK with the confirmation page")
                res must have(
                  httpStatus(OK),
                  pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
                )
              }
              "the user has a paper preference" in {

                Given("I setup the Wiremock stubs")
                AuthStub.stubEnrolled()
                IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
                PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(false))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Mandated, Voluntary)))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

                When("GET /confirmation is called")
                val res = IncomeTaxSubscriptionFrontend.confirmation()

                Then("Should return a OK with the confirmation page")
                res must have(
                  httpStatus(OK),
                  pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
                )
              }
              "the user has a digital preference" in {

                Given("I setup the Wiremock stubs")
                AuthStub.stubEnrolled()
                IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
                PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(true))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Mandated, Voluntary)))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

                When("GET /confirmation is called")
                val res = IncomeTaxSubscriptionFrontend.confirmation()

                Then("Should return a OK with the confirmation page")
                res must have(
                  httpStatus(OK),
                  pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
                )
              }
            }
            "the user is not mandated for current year" when {
              "the user has no digital preference available" in {
                Given("I setup the Wiremock stubs")
                AuthStub.stubEnrolled()
                IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
                PreferencesFrontendConnectorStub.stubGetOptedInStatus(None)
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

                When("GET /confirmation is called")
                val res = IncomeTaxSubscriptionFrontend.confirmation()

                Then("Should return a OK with the confirmation page")
                res must have(
                  httpStatus(OK),
                  pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
                )
              }
              "the user has a paper preference" in {

                Given("I setup the Wiremock stubs")
                AuthStub.stubEnrolled()
                IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
                PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(false))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

                When("GET /confirmation is called")
                val res = IncomeTaxSubscriptionFrontend.confirmation()

                Then("Should return a OK with the confirmation page")
                res must have(
                  httpStatus(OK),
                  pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
                )
              }
              "the user has a digital preference" in {

                Given("I setup the Wiremock stubs")
                AuthStub.stubEnrolled()
                IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
                PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(true))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
                SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

                When("GET /confirmation is called")
                val res = IncomeTaxSubscriptionFrontend.confirmation()

                Then("Should return a OK with the confirmation page")
                res must have(
                  httpStatus(OK),
                  pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
                )
              }
            }
          }
          "the user signed up for the next tax year" when {
            "the user has no digital preference available" in {

              Given("I setup the Wiremock stubs")
              AuthStub.stubEnrolled()
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNext))
              PreferencesFrontendConnectorStub.stubGetOptedInStatus(None)
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

              When("GET /confirmation is called")
              val res = IncomeTaxSubscriptionFrontend.confirmation()

              Then("Should return a OK with the confirmation page")
              res must have(
                httpStatus(OK),
                pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
              )
            }
            "the user has a paper preference" in {

              Given("I setup the Wiremock stubs")
              AuthStub.stubEnrolled()
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNext))
              PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(false))
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

              When("GET /confirmation is called")
              val res = IncomeTaxSubscriptionFrontend.confirmation()

              Then("Should return a OK with the confirmation page")
              res must have(
                httpStatus(OK),
                pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
              )
            }
            "the user has a digital preference" in {

              Given("I setup the Wiremock stubs")
              AuthStub.stubEnrolled()
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNext))
              PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(true))
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

              When("GET /confirmation is called")
              val res = IncomeTaxSubscriptionFrontend.confirmation()

              Then("Should return a OK with the confirmation page")
              res must have(
                httpStatus(OK),
                pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
              )
            }
          }
        }
      }
    }
    "the user is not enrolled" should {
      "return a NOT_FOUND" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /confirmation is called")
        val res = IncomeTaxSubscriptionFrontend.confirmation()

        Then("Should return a NOT_FOUND status")
        res must have(
          httpStatus(NOT_FOUND))
      }
    }
  }

  "POST /confirmation" should {
    "redirect the user to sign out" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubEnrolled()

      When("POST /confirmation is called")
      val res = IncomeTaxSubscriptionFrontend.submitConfirmation()

      Then("Should redirect to sign out")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(IndividualURI.signOutURI)
      )
    }

    "return a NOT_FOUND" when {
      "the user is not enrolled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /confirmation is called")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmation()

        Then("Should return a NOT_FOUND status")
        res must have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

}
