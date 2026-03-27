/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.individual.tasklist.taxyear

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.*
import helpers.servicemocks.AuthStub
import models.common.AccountingYearModel
import models.status.MandationStatus.{Mandated, Voluntary}
import models.status.MandationStatusModel
import models.{Current, EligibilityStatus, Next}
import play.api.http.Status.*
import play.api.libs.json.Json
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys.SelectedTaxYear

import java.time.LocalDate

class WhenDoYouWantToStartControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"

  "GET /report-quarterly/income-and-expenses/sign-up/tax-year/select-tax-year" when {
    "the Subscription Details Connector returns some data" should {
      "show the When Do You Want To Start page with current tax year selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          SelectedTaxYear,
          OK,
          Json.toJson(Some(AccountingYearModel(Current, confirmed = false, editable = true)))
        )
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(
            EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None)
          )
        ))

        When("GET /tax-year/select-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.whenDoYouWantToStart()

        val fromYear = (AccountingPeriodUtil.getTaxEndYear(LocalDate.now()) - 1).toString
        val toYear = AccountingPeriodUtil.getTaxEndYear(LocalDate.now()).toString
        val expectedText = removeHtmlMarkup(messages("business.when-do-you-want-to-start.option-1", fromYear, toYear))

        Then("Should return OK with the current tax year radio selected")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.when-do-you-want-to-start.heading") + serviceNameGovUk),
          radioButtonSet(id = "accountingYear", selectedRadioButton = Some(expectedText)),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = None)
        )
      }

      "show the When Do You Want To Start page with next tax year selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          SelectedTaxYear,
          OK,
          Json.toJson(Some(AccountingYearModel(Next, confirmed = false, editable = true)))
        )
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(
            EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None)
          )
        ))

        When("GET /tax-year/select-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.whenDoYouWantToStart()

        val fromYear = AccountingPeriodUtil.getTaxEndYear(LocalDate.now()).toString
        val toYear = (AccountingPeriodUtil.getTaxEndYear(LocalDate.now()) + 1).toString
        val expectedText = removeHtmlMarkup(messages("business.when-do-you-want-to-start.option-2", fromYear, toYear))

        Then("Should return OK with the next tax year radio selected")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.when-do-you-want-to-start.heading") + serviceNameGovUk),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the When Do You Want To Start page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(
            EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None)
          )
        ))

        When("GET /tax-year/select-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.whenDoYouWantToStart()

        Then("Should return OK with no radio option selected")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.when-do-you-want-to-start.heading") + serviceNameGovUk),
          radioButtonSet(id = "accountingYear", selectedRadioButton = None),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = None)
        )
      }
    }

    "return SEE_OTHER" when {
      "the user is mandated for the current tax year" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Mandated, Voluntary)),
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(
            EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None)
          )
        ))

        When("GET /tax-year/select-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.whenDoYouWantToStart()

        Then("Should redirect to what you need to do")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.whatYouNeedToDoURI)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/tax-year/select-tax-year" when {
    "redirect to the What You Need To Do page" when {
      "selecting the Current radio button" in {
        val userInput = Current

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary))
        ))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(
          SelectedTaxYear,
          AccountingYearModel(userInput)
        )

        When("POST /tax-year/select-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.submitWhenDoYouWantToStart(inEditMode = false, request = Some(userInput))

        Then("Should return SEE_OTHER with a redirect location of What You Need To Do")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.routes.WhatYouNeedToDoController.show.url)
        )
      }

      "selecting the Next radio button" in {
        val userInput = Next

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary))
        ))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(
          SelectedTaxYear,
          AccountingYearModel(userInput)
        )

        When("POST /tax-year/select-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.submitWhenDoYouWantToStart(inEditMode = false, request = Some(userInput))

        Then("Should return SEE_OTHER with a redirect location of What You Need To Do")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.routes.WhatYouNeedToDoController.show.url)
        )
      }
    }

    "return BAD_REQUEST" when {
      "no option has been selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary))
        ))

        When("POST /tax-year/select-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.submitWhenDoYouWantToStart(inEditMode = false, request = None)

        Then("Should return BAD_REQUEST and display an error box on screen without redirecting")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle(s"Error: ${messages("business.when-do-you-want-to-start.heading")}$serviceNameGovUk"),
          errorDisplayed()
        )
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "there is a failure while saving the tax year" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary))
        ))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(SelectedTaxYear)

        When("POST /tax-year/select-tax-year is called")
        val res = IncomeTaxSubscriptionFrontend.submitWhenDoYouWantToStart(inEditMode = false, request = Some(Current))

        Then("Should return INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
