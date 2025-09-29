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
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants._
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.EligibilityStatus
import models.common.business.{BusinessStartDate, SelfEmploymentData}
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.{JsNumber, JsObject, JsString, Json}
import play.api.libs.ws.WSResponse
import utilities.SubscriptionDataKeys._

class ProgressSavedControllerISpec extends ComponentSpecBase {

  s"GET ${controllers.agent.tasklist.routes.ProgressSavedController.show().url}" should {
    "return SEE_OTHER" when {
      "the user is not authenticated" in {
        AuthStub.stubUnauthorised()

        val result: WSResponse = IncomeTaxSubscriptionFrontend.getProgressSaved()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/business/progress-saved"))
        )
      }
    }
    "return OK" when {
      "the last updated timestamp is successfully returned from the backend" when {
        "location was provided" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(
            OK,
            Seq(SelfEmploymentData(testId, Some(false), Some(BusinessStartDate(startDate))))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(lastUpdatedTimestamp, OK, JsObject(Seq(("$date", JsNumber(1)))))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))

          When(s"GET ${controllers.agent.tasklist.routes.ProgressSavedController.show(location = Some("test-location")).url} is called")
          val result = IncomeTaxSubscriptionFrontend.getProgressSaved(saveAndRetrieveLocation = Some("test-location"))

          Then("Return OK with the progress saved page content")
          result must have(
            httpStatus(OK),
            pageTitle(
              s"${messages("agent.business.progress-saved.title")} - Use software to report your client’s Income Tax - GOV.UK"
            )
          )
        }
        "location was not provided " in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(lastUpdatedTimestamp, OK, JsObject(Seq(("$date", JsNumber(1)))))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When(s"GET ${controllers.agent.tasklist.routes.ProgressSavedController.show().url} is called")
          val result = IncomeTaxSubscriptionFrontend.getProgressSaved()

          Then("Return OK with the progress saved page content")
          result must have(
            httpStatus(OK),
            pageTitle(
              s"${messages("agent.business.progress-saved.title")} - Use software to report your client’s Income Tax - GOV.UK"
            )
          )
        }
      }
    }
    "return an internal server error" when {
      "the last updated timestamp failed to return from the backend" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(lastUpdatedTimestamp, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When(s"GET ${controllers.agent.tasklist.routes.ProgressSavedController.show().url} is called")
        val result = IncomeTaxSubscriptionFrontend.getProgressSaved()

        Then("Return OK with the progress saved page content")
        result must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }


}
