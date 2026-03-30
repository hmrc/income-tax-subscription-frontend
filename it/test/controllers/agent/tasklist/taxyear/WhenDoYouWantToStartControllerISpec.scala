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

package controllers.agent.tasklist.taxyear

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants.*
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.common.AccountingYearModel
import models.{Current, Next}
import play.api.http.Status.*
import models.*
import models.status.MandationStatus.Voluntary
import models.status.MandationStatus.Mandated
import models.status.MandationStatusModel
import play.api.libs.json.{JsString, Json}
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys.SelectedTaxYear

import java.time.LocalDate

class WhenDoYouWantToStartControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk: String = "Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"

  "GET /report-quarterly/income-and-expenses/sign-up/client/tax-year/select-tax-year" when {
    "the Subscription Details Connector returns some editable data" should {
      "return OK and show the page with current tax year selected" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(
            EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None)
          )
        ))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          SelectedTaxYear,
          OK,
          Json.toJson(Some(AccountingYearModel(Current, confirmed = false, editable = true)))
        )

        val res = IncomeTaxSubscriptionFrontend.whenDoYouWantToStart()

        val fromYear = (AccountingPeriodUtil.getTaxEndYear(LocalDate.now()) - 1).toString
        val toYear = AccountingPeriodUtil.getTaxEndYear(LocalDate.now()).toString
        val expectedText =
          removeHtmlMarkup(messages("agent.business.when-do-you-want-to-start.option1", fromYear, toYear))

        res must have(
          httpStatus(OK),
          pageTitle(s"${messages("agent.business.when-do-you-want-to-start.heading")} - $serviceNameGovUk"),
          radioButtonSet(id = "accountingYear", selectedRadioButton = Some(expectedText)),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = None)
        )
      }

      "return OK and show the page with next tax year selected" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(
            EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None)
          )
        ))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          SelectedTaxYear,
          OK,
          Json.toJson(Some(AccountingYearModel(Next, confirmed = false, editable = true)))
        )

        val res = IncomeTaxSubscriptionFrontend.whenDoYouWantToStart()

        val fromYear = AccountingPeriodUtil.getTaxEndYear(LocalDate.now()).toString
        val toYear = (AccountingPeriodUtil.getTaxEndYear(LocalDate.now()) + 1).toString
        val expectedText =
          removeHtmlMarkup(messages("agent.business.when-do-you-want-to-start.option2", fromYear, toYear))

        res must have(
          httpStatus(OK),
          pageTitle(s"${messages("agent.business.when-do-you-want-to-start.heading")} - $serviceNameGovUk"),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "the Subscription Details Connector returns some non-editable data" should {
      "redirect to the what you need to do page" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Mandated, Voluntary)),
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(
            EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None)
          )
        ))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          SelectedTaxYear,
          NO_CONTENT
        )

        val res = IncomeTaxSubscriptionFrontend.whenDoYouWantToStart()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
        )
      }
    }

    "the Subscription Details Connector returns no data" should {
      "return OK and show the page without an option selected" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(
            EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None)
          )
        ))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          SelectedTaxYear,
          NO_CONTENT
        )

        val res = IncomeTaxSubscriptionFrontend.whenDoYouWantToStart()

        res must have(
          httpStatus(OK),
          pageTitle(s"${messages("agent.business.when-do-you-want-to-start.heading")} - $serviceNameGovUk"),
          radioButtonSet(id = "accountingYear", selectedRadioButton = None),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = None)
        )
      }
    }

    "the user is unauthorised" should {
      "return SEE_OTHER" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.whenDoYouWantToStart()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/tax-year/select-tax-year"))
        )
      }
    }

    "the user has no state" should {
      "redirect to cannot go back to previous client page" in {
        AuthStub.stubAuthSuccess()

        val res = IncomeTaxSubscriptionFrontend.get(
          "/tax-year/select-tax-year",
          ClientData.basicClientData,
          withJourneyStateSignUp = false
        )

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }
  }

  s"POST ${controllers.agent.tasklist.taxyear.routes.WhenDoYouWantToStartController.submit(editMode = false).url}" when {
    "the user is unauthorised" must {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.submitWhenDoYouWantToStart(
          inEditMode = false,
          request = Some(Current)
        )

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/tax-year/select-tax-year"))
        )
      }
    }

    "the user has no state" must {
      "redirect to cannot go back to previous client page" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.submitWhenDoYouWantToStart(
          inEditMode = false,
          request = Some(Current),
          withJourneyState = false
        )

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }

    "in edit mode" must {
      "redirect to the global check your answers page" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(
          SelectedTaxYear,
          AccountingYearModel(Current)
        )

        val result = IncomeTaxSubscriptionFrontend.submitWhenDoYouWantToStart(
          inEditMode = true,
          request = Some(Current)
        )

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.GlobalCheckYourAnswersController.show.url)
        )
      }
    }

    "not in edit mode" when {
      "no option is selected" must {
        "return a bad request with the page content" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))

          val result = IncomeTaxSubscriptionFrontend.submitWhenDoYouWantToStart(
            inEditMode = false,
            request = None
          )

          result must have(
            httpStatus(BAD_REQUEST),
            pageTitle(s"Error: ${messages("agent.business.when-do-you-want-to-start.heading")} - $serviceNameGovUk"),
            errorDisplayed()
          )
        }
      }

      "current tax year is selected" must {
        "save the tax year and redirect to the what you need to do page" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(
            SelectedTaxYear,
            AccountingYearModel(Current)
          )

          val result = IncomeTaxSubscriptionFrontend.submitWhenDoYouWantToStart(
            inEditMode = false,
            request = Some(Current)
          )

          result must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
          )
        }
      }

      "next tax year is selected" must {
        "save the tax year and redirect to the what you need to do page" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(
            SelectedTaxYear,
            AccountingYearModel(Next)
          )

          val result = IncomeTaxSubscriptionFrontend.submitWhenDoYouWantToStart(
            inEditMode = false,
            request = Some(Next)
          )

          result must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
          )
        }
      }
    }

    "there was a problem saving the tax year selection" must {
      "return an internal server error page" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(SelectedTaxYear)

        val result = IncomeTaxSubscriptionFrontend.submitWhenDoYouWantToStart(
          inEditMode = false,
          request = Some(Current)
        )

        result must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}