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

package controllers.individual.subscription

import common.Constants.ITSASessionKeys.MANDATED_CURRENT_YEAR
import config.featureswitch.FeatureSwitch.ConfirmationPage
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, PreferencesFrontendConnectorStub}
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.IndividualURI
import helpers.IntegrationTestModels.{testAccountingYearCurrent, testAccountingYearNext}
import helpers.servicemocks.AuthStub
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys._

class ConfirmationControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ConfirmationPage)
  }

  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

  "GET /confirmation" when {
    "the user is enrolled" when {
      "the confirmation page feature switch is disabled" should {
        "return the sign up complete page" when {
          "the user signed up for the current tax year" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubEnrolled()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))

            When("GET /confirmation is called")
            val res = IncomeTaxSubscriptionFrontend.confirmation()

            Then("Should return a OK with the confirmation page")
            res must have(
              httpStatus(OK),
              pageTitle(messages("sign-up-complete.heading") + serviceNameGovUk)
            )
          }
          "the user signed up for the next tax year" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubEnrolled()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNext))

            When("GET /confirmation is called")
            val res = IncomeTaxSubscriptionFrontend.confirmation()

            Then("Should return a OK with the confirmation page")
            res must have(
              httpStatus(OK),
              pageTitle(messages("sign-up-complete.heading") + serviceNameGovUk)
            )
          }
        }
      }
      "the confirmation page feature switch is enabled" should {
        "return the sign up confirmation page" when {
          "the user signed up for the current tax year" when {
            "the user is mandated for current year" when {
              "the user has no digital preference available" in {
                enable(ConfirmationPage)

                Given("I setup the Wiremock stubs")
                AuthStub.stubEnrolled()
                IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
                PreferencesFrontendConnectorStub.stubGetOptedInStatus(None)

                When("GET /confirmation is called")
                val res = IncomeTaxSubscriptionFrontend.confirmation(Map(MANDATED_CURRENT_YEAR -> "true"))

                Then("Should return a OK with the confirmation page")
                res must have(
                  httpStatus(OK),
                  pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
                )
              }
              "the user has a paper preference" in {
                enable(ConfirmationPage)

                Given("I setup the Wiremock stubs")
                AuthStub.stubEnrolled()
                IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
                PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(false))

                When("GET /confirmation is called")
                val res = IncomeTaxSubscriptionFrontend.confirmation(Map(MANDATED_CURRENT_YEAR -> "true"))

                Then("Should return a OK with the confirmation page")
                res must have(
                  httpStatus(OK),
                  pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
                )
              }
              "the user has a digital preference" in {
                enable(ConfirmationPage)

                Given("I setup the Wiremock stubs")
                AuthStub.stubEnrolled()
                IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
                PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(true))

                When("GET /confirmation is called")
                val res = IncomeTaxSubscriptionFrontend.confirmation(Map(MANDATED_CURRENT_YEAR -> "true"))

                Then("Should return a OK with the confirmation page")
                res must have(
                  httpStatus(OK),
                  pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
                )
              }
            }
            "the user is not mandated for current year" when {
              "the user has no digital preference available" in {
                enable(ConfirmationPage)

                Given("I setup the Wiremock stubs")
                AuthStub.stubEnrolled()
                IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
                PreferencesFrontendConnectorStub.stubGetOptedInStatus(None)

                When("GET /confirmation is called")
                val res = IncomeTaxSubscriptionFrontend.confirmation()

                Then("Should return a OK with the confirmation page")
                res must have(
                  httpStatus(OK),
                  pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
                )
              }
              "the user has a paper preference" in {
                enable(ConfirmationPage)

                Given("I setup the Wiremock stubs")
                AuthStub.stubEnrolled()
                IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
                PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(false))

                When("GET /confirmation is called")
                val res = IncomeTaxSubscriptionFrontend.confirmation()

                Then("Should return a OK with the confirmation page")
                res must have(
                  httpStatus(OK),
                  pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
                )
              }
              "the user has a digital preference" in {
                enable(ConfirmationPage)

                Given("I setup the Wiremock stubs")
                AuthStub.stubEnrolled()
                IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
                PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(true))

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
              enable(ConfirmationPage)

              Given("I setup the Wiremock stubs")
              AuthStub.stubEnrolled()
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNext))
              PreferencesFrontendConnectorStub.stubGetOptedInStatus(None)

              When("GET /confirmation is called")
              val res = IncomeTaxSubscriptionFrontend.confirmation()

              Then("Should return a OK with the confirmation page")
              res must have(
                httpStatus(OK),
                pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
              )
            }
            "the user has a paper preference" in {
              enable(ConfirmationPage)

              Given("I setup the Wiremock stubs")
              AuthStub.stubEnrolled()
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNext))
              PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(false))

              When("GET /confirmation is called")
              val res = IncomeTaxSubscriptionFrontend.confirmation()

              Then("Should return a OK with the confirmation page")
              res must have(
                httpStatus(OK),
                pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
              )
            }
            "the user has a digital preference" in {
              enable(ConfirmationPage)

              Given("I setup the Wiremock stubs")
              AuthStub.stubEnrolled()
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNext))
              PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(true))

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
