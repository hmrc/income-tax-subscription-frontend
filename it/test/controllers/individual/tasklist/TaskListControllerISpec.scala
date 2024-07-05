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

package controllers.individual.tasklist

import common.Constants.ITSASessionKeys
import connectors.stubs._
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.{testBusinessName => _, _}
import helpers._
import helpers.servicemocks._
import models.EligibilityStatus
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys._

class TaskListControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  "GET /report-quarterly/income-and-expenses/sign-up/business/task-list" should {
    "return OK" when {
      "there is no user data setup" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(IncomeSourceConfirmation, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.task-list.title") + serviceNameGovUk)
        )
      }

      "there is partial user data setup" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountingMethod.accountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel.copy(accountingMethod = None, confirmed = false)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel.copy(accountingMethod = None, confirmed = false)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(IncomeSourceConfirmation, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.task-list.title") + serviceNameGovUk)
        )
      }

      "there is full user data setup" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountingMethod.accountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(IncomeSourceConfirmation, OK, Json.toJson(true))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.task-list.title") + serviceNameGovUk)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/task-list" when {
    "the task list redesign feature switch is enabled" should {
      "redirect to the global check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountingMethod.accountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

        When("POST /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.submitTaskList()

        Then("Should return a SEE_OTHER with a redirect location of confirmation")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.globalCheckYourAnswersURI)
        )
      }
    }
  }
}
