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

import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import connectors.stubs.IncomeTaxSubscriptionConnectorStub.verifySubscriptionSave
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants.taskListURI
import helpers.agent.IntegrationTestModels.subscriptionData
import helpers.agent.servicemocks.AuthStub
import models.Current
import models.common.AccountingYearModel
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys.SelectedTaxYear

class TaxYearCheckYourAnswersControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/client/business/tax-year-check-your-answers" should {
    "return OK" when {
      "the save & retrieve feature switch is enabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("GET /business/tax-year-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getTaxYearCheckYourAnswers()

        Then("Should return OK with tax year CYA page")
        res must have(
          httpStatus(OK),
          pageTitle(
            s"${messages("business.check-your-answers.title")} - Use software to report your client’s Income Tax - GOV.UK"
          )
        )
      }
    }

    "return NOT_FOUND" when {
      "the save & retrieve feature switch is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        And("save & retrieve feature switch is disabled")
        disable(SaveAndRetrieve)

        When("GET /business/tax-year-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getTaxYearCheckYourAnswers()

        Then("Should return NOT FOUND")
        res must have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/tax-year-check-your-answers" should {
    "redirect to the task list" when {
      "the select tax has been confirmed" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(selectedTaxYear = Some(AccountingYearModel(Current)))
        )

        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails("subscriptionId")

        When("GET /business/tax-year-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitTaxYearCheckYourAnswers()

        Then("Should return a SEE_OTHER with a redirect location of confirmation")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(taskListURI)
        )

        val expectedCacheMap = CacheMap(
          "",
          Map(
            SubscriptionDataKeys.SelectedTaxYear -> Json.toJson(AccountingYearModel(Current, confirmed = true))
          )
        )

        verifySubscriptionSave("subscriptionId", expectedCacheMap, Some(1))
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the select tax could not be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(selectedTaxYear = None)
        )

        When("GET /business/tax-year-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitTaxYearCheckYourAnswers()

        Then("Should return a SEE_OTHER with a redirect location of confirmation")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
