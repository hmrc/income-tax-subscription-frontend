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

import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestModels.subscriptionData
import helpers.servicemocks.AuditStub.verifyAudit
import helpers.servicemocks.AuthStub
import play.api.http.Status.{NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.{JsNumber, JsObject}
import utilities.SubscriptionDataKeys._

class ProgressSavedControllerISpec  extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/business/progress-saved" should {
    "return OK" when {
      "the save & retrieve feature switch is enabled and the location is not provided" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(lastUpdatedTimestamp, OK, JsObject(Seq(("$date", JsNumber(1)))))

        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("GET /business/progress-saved is called")
        val res = IncomeTaxSubscriptionFrontend.getProgressSaved()

        Then("Should return OK with progress saved page")
        res must have(
          httpStatus(OK),
          pageTitle(
            s"${messages("business.progress-saved.title")} - Use software to send Income Tax updates - GOV.UK"
          )
        )
        verifyAudit(Some(1))
      }

      "the save & retrieve feature switch is enabled and the location is provided" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(lastUpdatedTimestamp, OK, JsObject(Seq(("$date", JsNumber(1)))))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("GET /business/progress-saved is called")
        val res = IncomeTaxSubscriptionFrontend.getProgressSaved(saveAndRetrieveLocation = Some("test-location"))

        Then("Should return OK with progress saved page")
        res must have(
          httpStatus(OK),
          pageTitle(
            s"${messages("business.progress-saved.title")} - Use software to send Income Tax updates - GOV.UK"
          )
        )
        verifyAudit(Some(2))
      }
    }

    "return NOT_FOUND" when {
      "the save & retrieve feature switch is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        And("save & retrieve feature switch is enabled")
        disable(SaveAndRetrieve)

        When("GET /business/progress-saved is called")
        val res = IncomeTaxSubscriptionFrontend.getProgressSaved()

        Then("Should return NOT FOUND")
        res must have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }
}
