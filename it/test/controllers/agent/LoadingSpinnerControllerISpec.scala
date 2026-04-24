/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.agent

import common.Constants.ITSASessionKeys
import connectors.stubs.SessionDataConnectorStub
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino}
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import models.Status.{HandledError, InProgress, OtherError, Success}
import models.SubmissionStatus
import models.agent.JourneyStep
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.{JsString, Json}
import utilities.agent.TestConstants.testUtr

import java.time.LocalDateTime

class LoadingSpinnerControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  val serviceNameGovUk = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"

  s"GET ${routes.LoadingSpinnerController.show.url}" must {
    "return SEE_OTHER to the login page" when {
      "user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatus()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/confirming-please-wait"))
        )
      }
    }
    "return SEE_OTHER to the global check your answers" when {
      "there is no submission status available" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatus()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.GlobalCheckYourAnswersController.show.url)
        )
      }
    }
    "return SEE_OTHER to the confirmation page" when {
      "there is a submission status of Success" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.SUBMISSION_STATUS -> Json.toJson(SubmissionStatus(Success))
        ))

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatus()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.ConfirmationController.show.url)
        )

        getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(JourneyStep.Confirmation.key)
      }
    }
    "return SEE_OTHER to the contact hmrc page" when {
      "there is a submission status of HandledError" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.SUBMISSION_STATUS -> Json.toJson(SubmissionStatus(HandledError))
        ))

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatus()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.errors.routes.ContactHMRCController.show.url)
        )
      }
    }
    "return an INTERNAL_SERVER_ERROR with an error page content" when {
      "there is a submission status of OtherError" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.SUBMISSION_STATUS -> Json.toJson(SubmissionStatus(OtherError))
        ))

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatus()

        res must have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle(messages("service-error.title") + serviceNameGovUk)
        )
      }
      "there is a submission status of IN_PROGRESS, but the timestamp indicates it's been that way for longer than the max allowed wait" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.SUBMISSION_STATUS -> Json.toJson(SubmissionStatus(
            status = InProgress,
            timestamp = Some(LocalDateTime.now.minusSeconds(appConfig.confirmingSubmissionMaxWaitTimeSeconds))
          ))
        ))

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatus()

        res must have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle(messages("service-error.title") + serviceNameGovUk)
        )
      }
    }
    "return OK with the page content" in {
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubGetAllSessionData(Map(
        ITSASessionKeys.NINO -> JsString(testNino),
        ITSASessionKeys.UTR -> JsString(testUtr),
        ITSASessionKeys.SUBMISSION_STATUS -> Json.toJson(SubmissionStatus(
          status = InProgress,
          timestamp = Some(LocalDateTime.now)
        ))
      ))

      val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatus()

      res must have(
        httpStatus(OK),
        pageTitle(messages("loading-spinner.heading") + serviceNameGovUk)
      )
    }
  }

  s"GET ${routes.LoadingSpinnerController.query.url}" must {
    "return SEE_OTHER to the login page" when {
      "user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatus()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/confirming-please-wait"))
        )
      }
    }
    "return NO CONTENT" when {
      "the status of the subscription is InProgress and the max wait has not been reached" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.SUBMISSION_STATUS -> Json.toJson(SubmissionStatus(
            status = InProgress,
            timestamp = Some(LocalDateTime.now)
          ))
        ))

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatusQuery()

        res must have(
          httpStatus(NO_CONTENT)
        )
      }
    }
    "return OK" when {
      "the status of the subscription is InProgress and the max wait has been reached" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.SUBMISSION_STATUS -> Json.toJson(SubmissionStatus(
            status = InProgress,
            timestamp = Some(LocalDateTime.now.minusSeconds(appConfig.confirmingSubmissionMaxWaitTimeSeconds))
          ))
        ))

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatusQuery()

        res must have(
          httpStatus(OK)
        )
      }
      "the status of the subscription is Success" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.SUBMISSION_STATUS -> Json.toJson(SubmissionStatus(Success))
        ))

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatusQuery()

        res must have(
          httpStatus(OK)
        )
      }
      "the status of the subscription is HandledError" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.SUBMISSION_STATUS -> Json.toJson(SubmissionStatus(HandledError))
        ))

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatusQuery()

        res must have(
          httpStatus(OK)
        )
      }
      "the status of the subscription is OtherError" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.SUBMISSION_STATUS -> Json.toJson(SubmissionStatus(OtherError))
        ))

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatusQuery()

        res must have(
          httpStatus(OK)
        )
      }
    }
  }

}