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

package controllers.individual

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, PreferencesFrontendConnectorStub, SessionDataConnectorStub}
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino, testUtr}
import helpers.IntegrationTestModels.{testAccountingYearCurrent, testAccountingYearNext}
import helpers.servicemocks.AuthStub
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import models.{EligibilityStatus, Yes}
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys._

class ConfirmationControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
  val currentTaxYearRange = s"6 April ${AccountingPeriodUtil.getCurrentTaxEndYear - 1} to 5 April ${AccountingPeriodUtil.getCurrentTaxEndYear}"
  val nextTaxYearRange = s"6 April ${AccountingPeriodUtil.getNextTaxEndYear - 1} to 5 April ${AccountingPeriodUtil.getNextTaxEndYear}"

  s"GET ${routes.ConfirmationController.show.url}" when {
    "the user is not authenticated" must {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.confirmation()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/confirmation"))
        )
      }
    }

    "the user does not have the confirmation journey state" must {
      "display a not found page" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.confirmation(includeConfirmationState = false)

        result must have(
          httpStatus(NOT_FOUND)
        )
      }
    }

    "the user is authenticated and is in a confirmation journey state" must {
      "display the sign up confirmation page" when {
        "the user is signed up for the current tax year" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
          PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(true))
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr),
            ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
            ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)),
            ITSASessionKeys.HAS_SOFTWARE -> JsString(Yes.toString)
          ))

          val result = IncomeTaxSubscriptionFrontend.confirmation()

          result must have(
            httpStatus(OK),
            pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk),
            elementTextBySelector(".govuk-panel__body--secondary")(messages("sign-up-confirmation.heading.panel.current", currentTaxYearRange))
          )
        }
        "the user is signed up for the next tax year" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNext))
          PreferencesFrontendConnectorStub.stubGetOptedInStatus(Some(true))
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr),
            ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
            ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)),
            ITSASessionKeys.HAS_SOFTWARE -> JsString(Yes.toString)
          ))

          val result = IncomeTaxSubscriptionFrontend.confirmation()

          result must have(
            httpStatus(OK),
            pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk),
            elementTextBySelector(".govuk-panel__body--secondary")(messages("sign-up-confirmation.heading.panel.next", nextTaxYearRange))
          )
        }
      }
    }
  }

  s"POST ${routes.ConfirmationController.submit.url}" when {
    "the user is unauthenticated" must {
      "redirect the user to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.submitConfirmation()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/confirmation"))
        )
      }
    }
    "the user is authenticated" must {
      "redirect the user to sign out" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.submitConfirmation()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.SignOutController.signOut.url)
        )
      }
    }
  }
}
