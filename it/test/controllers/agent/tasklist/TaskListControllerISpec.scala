/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.agent.tasklist

import common.Constants.ITSASessionKeys
import connectors.stubs._
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.{testBusinessName => _, _}
import helpers.agent.servicemocks._
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.NO_CONTENT
import utilities.SubscriptionDataKeys._

class TaskListControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/task-list" should {
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

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.business.task-list.title") + serviceNameGovUk)
        )
      }
      "there is partial user data setup" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel.copy(accountingMethod = None, confirmed = false)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel.copy(accountingMethod = None, confirmed = false)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(IncomeSourceConfirmation, NO_CONTENT)

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.business.task-list.title") + serviceNameGovUk)
        )
      }
      "there is full user data setup" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(IncomeSourceConfirmation, OK, Json.toJson(true))

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.business.task-list.title") + serviceNameGovUk)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/task-list" when {
    "redirect to the global check your answers page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("I call POST /task-list")
      val res = IncomeTaxSubscriptionFrontend.submitTaskList()

      Then("The result must have a status of SEE_OTHER and redirect to the confirmation page")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(AgentURI.globalCheckYourAnswersURI)
      )
    }
  }
}
