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
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{testNino, testUtr}
import helpers.servicemocks.AuditStub.stubAuditing
import helpers.servicemocks.AuthStub
import models.EligibilityStatus
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.{JsNumber, JsObject, JsString, Json}
import utilities.SubscriptionDataKeys._

class ProgressSavedControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/progress-saved" should {
    "return OK" when {
      "the location is not provided" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(lastUpdatedTimestamp, OK, JsObject(Seq(("$date", JsNumber(1)))))

        When("GET /business/progress-saved is called")
        val res = IncomeTaxSubscriptionFrontend.getProgressSaved()

        Then("Should return OK with progress saved page")
        res must have(
          httpStatus(OK),
          pageTitle(
            s"${messages("business.progress-saved.title")} - Sign up for Making Tax Digital for Income Tax - GOV.UK"
          )
        )
      }

      "the location is provided" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        stubAuditing()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(lastUpdatedTimestamp, OK, JsObject(Seq(("$date", JsNumber(1)))))
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /business/progress-saved is called")
        val res = IncomeTaxSubscriptionFrontend.getProgressSaved(saveAndRetrieveLocation = Some("test-location"))

        Then("Should return OK with progress saved page")
        res must have(
          httpStatus(OK),
          pageTitle(
            s"${messages("business.progress-saved.title")} - Sign up for Making Tax Digital for Income Tax - GOV.UK"
          )
        )
      }
    }
  }
}
