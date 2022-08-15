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

package controllers.individual.business

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import connectors.stubs.IncomeTaxSubscriptionConnectorStub.verifySubscriptionSave
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.taskListURI
import helpers.servicemocks.AuthStub
import models.Current
import models.common.AccountingYearModel
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.SelectedTaxYear

class TaxYearCheckYourAnswersControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/business/tax-year-check-your-answers" should {
    "return OK" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

      When("GET /business/tax-year-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.getTaxYearCheckYourAnswers()

      Then("Should return OK with tax year CYA page")
      res must have(
        httpStatus(OK),
        pageTitle(
          s"${messages("business.check-your-answers.content.tax-year.title")} - Use software to send Income Tax updates - GOV.UK"
        )
      )

    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/tax-year-check-your-answers" should {
    "redirect to the task list" when {
      "the select tax has been confirmed" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        val unconfirmedTaxYear = AccountingYearModel(Current)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(unconfirmedTaxYear))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYearModel](SelectedTaxYear, unconfirmedTaxYear.copy(confirmed = true))

        When("GET /business/tax-year-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitTaxYearCheckYourAnswers()

        Then("Should return a SEE_OTHER with a redirect location of task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(taskListURI)
        )

        verifySubscriptionSave(SelectedTaxYear, unconfirmedTaxYear.copy(confirmed = true), Some(1))
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the select tax could not be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

        When("GET /business/tax-year-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitTaxYearCheckYourAnswers()

        Then("Should return an INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "the select tax could not be confirmed" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        val unconfirmedTaxYear = AccountingYearModel(Current)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(unconfirmedTaxYear))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(SelectedTaxYear)

        When("GET /business/tax-year-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitTaxYearCheckYourAnswers()

        Then("Should return an INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
