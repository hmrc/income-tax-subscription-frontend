/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.individual.accountingperiod

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import models.common.AccountingYearModel
import models.status.MandationStatus.{Mandated, Voluntary}
import models.status.MandationStatusModel
import models._
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utilities.SubscriptionDataKeys.SelectedTaxYear
class AccountingPeriodNonStandardControllerISpec extends ComponentSpecBase {
  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
  s"GET ${controllers.individual.accountingperiod.routes.AccountingPeriodNonStandardController.show.url}" should {
    "show the Non Standard Accounting Period page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Mandated)))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.HAS_SOFTWARE)(NO_CONTENT)
      When(s"GET ${controllers.individual.accountingperiod.routes.AccountingPeriodNonStandardController.show.url}")
      val result = IncomeTaxSubscriptionFrontend.showNonStandardAccountingPeriod()
      Then("The result should be OK with page content")
      result must have(
        httpStatus(OK),
        pageTitle(messages("accounting-period-non-standard.heading") + serviceNameGovUk),
        radioButtonSet(id = "yes-no", selectedRadioButton = None),
        radioButtonSet(id = "yes-no-2", selectedRadioButton = None)
      )
    }
  }
  s"POST ${controllers.individual.accountingperiod.routes.AccountingPeriodNonStandardController.show.url}" when {
    s"return a redirect to ${controllers.individual.routes.WhatYouNeedToDoController.show.url}" when {
      "the user selects the Yes radio button" in {
        val userInput = Yes
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Mandated)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubSaveSessionData[YesNo](ITSASessionKeys.HAS_SOFTWARE, userInput)(OK)
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYearModel](SelectedTaxYear, AccountingYearModel(Next))
        When(s"POST ${controllers.individual.accountingperiod.routes.AccountingPeriodNonStandardController.submit.url} is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitNonStandardAccountingPeriod(request = Some(userInput))
        Then("Should return SEE_OTHER to the What You Need To Do controller")
        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.routes.WhatYouNeedToDoController.show.url)
        )
      }
    }
    s"return a redirect to ${controllers.individual.accountingperiod.routes.AccountingPeriodNotSupportedController.show.url}" when {
      "the user selects the No radio button" in {
        val userInput = No
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Mandated)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubSaveSessionData[YesNo](ITSASessionKeys.HAS_SOFTWARE, userInput)(OK)
        When(s"POST ${controllers.individual.accountingperiod.routes.AccountingPeriodNonStandardController.submit.url} is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitNonStandardAccountingPeriod(request = Some(userInput))
        Then("Should return SEE_OTHER to the Accounting Period Not Supported Controller")
        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.accountingperiod.routes.AccountingPeriodNotSupportedController.show.url)
        )
      }
    }
    "return BAD_REQUEST and display an error box on screen without redirecting" when {
      "the user does not select either option" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Mandated)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK)
        When(s"POST ${controllers.individual.accountingperiod.routes.AccountingPeriodNonStandardController.submit.url} is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitNonStandardAccountingPeriod(request = None)
        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        result must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("accounting-period-non-standard.heading") + serviceNameGovUk),
          errorDisplayed()
        )
      }
    }
    "return INTERNAL_SERVER_ERROR" when {
      "the Non Standard Accounting Period could not be saved" in {
        val userInput = Yes
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Mandated)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK)
        When(s"POST ${controllers.individual.accountingperiod.routes.AccountingPeriodNonStandardController.submit.url} is called")
        val result = IncomeTaxSubscriptionFrontend.submitNonStandardAccountingPeriod(request = Some(userInput))
        Then("Should return a INTERNAL_SERVER_ERROR")
        result must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
