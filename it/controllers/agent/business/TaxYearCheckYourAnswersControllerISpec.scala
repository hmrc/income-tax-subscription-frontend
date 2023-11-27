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

package controllers.agent.business

import common.Constants.ITSASessionKeys
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import connectors.stubs.IncomeTaxSubscriptionConnectorStub.verifySubscriptionSave
import helpers.IntegrationTestConstants.{AgentURI, testFirstName, testLastName, testNino}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.Current
import models.common.AccountingYearModel
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.SelectedTaxYear
import utilities.UserMatchingSessionUtil

class TaxYearCheckYourAnswersControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/client/business/tax-year-check-your-answers" should {
    "return OK" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

      When("GET /client/business/tax-year-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.getTaxYearCheckYourAnswers()

      Then("Should return OK with tax year CYA page")
      res must have(
        httpStatus(OK),
        pageTitle(
          s"${messages("agent.business.check-your-answers.content.tax-year.title")} - Use software to report your client’s Income Tax - GOV.UK"
        )
      )
    }
    "return SEE_OTHER" when {
      "The user is mandated for the current tax year" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

        When("GET /client/business/tax-year-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getTaxYearCheckYourAnswers(Map(
          UserMatchingSessionUtil.firstName -> testFirstName,
          UserMatchingSessionUtil.lastName -> testLastName,
          ITSASessionKeys.NINO -> testNino,
          ITSASessionKeys.MANDATED_CURRENT_YEAR -> "true"
        ))

        Then("Should return SEE_OTHER to task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.taskListURI)
        )
      }
      "The user is eligible for the next tax year only" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

        When("GET /client/business/tax-year-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getTaxYearCheckYourAnswers(Map(
          UserMatchingSessionUtil.firstName -> testFirstName,
          UserMatchingSessionUtil.lastName -> testLastName,
          ITSASessionKeys.NINO -> testNino,
          ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY -> "true"
        ))

        Then("Should return SEE_OTHER to task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.taskListURI)
        )
      }
    }
  }


  "POST /report-quarterly/income-and-expenses/sign-up/client/business/tax-year-check-your-answers" should {
    "redirect to the task list" when {
      "the select tax has been confirmed" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        val unconfirmedTaxYear = AccountingYearModel(Current)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(unconfirmedTaxYear))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYearModel](SelectedTaxYear, unconfirmedTaxYear.copy(confirmed = true))

        When("GET /client/business/tax-year-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitTaxYearCheckYourAnswers()

        Then("Should return a SEE_OTHER with a redirect location of task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.taskListURI)
        )

        verifySubscriptionSave(SelectedTaxYear, Json.toJson(unconfirmedTaxYear.copy(confirmed = true)), Some(1))
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the select tax could not be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

        When("GET /client/business/tax-year-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitTaxYearCheckYourAnswers()

        Then("Should return an INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "the select tax could not be confirmed" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(AccountingYearModel(Current)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(SelectedTaxYear)

        When("GET /client/business/tax-year-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitTaxYearCheckYourAnswers()

        Then("Should return an INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
