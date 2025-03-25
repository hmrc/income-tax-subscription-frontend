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

package controllers.agent.tasklist.taxyear

import common.Constants.ITSASessionKeys
import config.featureswitch.FeatureSwitch.EmailCaptureConsent
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.testAccountingYearCurrent
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.common.AccountingYearModel
import models.status.MandationStatus.{Mandated, Voluntary}
import models.status.MandationStatusModel
import models.{Current, EligibilityStatus, Next}
import play.api.http.Status._
import play.api.libs.json.{JsBoolean, JsString, Json}
import utilities.SubscriptionDataKeys.SelectedTaxYear
import utilities.agent.TestConstants.testUtr
import utilities.{AccountingPeriodUtil, UserMatchingSessionUtil}

import java.time.LocalDate

class WhatYearToSignUpControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EmailCaptureConsent)
  }

  val serviceNameGovUk: String = "Use software to report your client’s Income Tax - GOV.UK"

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/what-year-to-sign-up" when {
    "the Subscription Details Connector returns some data" should {
      "return OK and show the What Tax Year To Sign Up page with current tax year radio selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(Some(testAccountingYearCurrent)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.accountingYear()

        val fromYear: String = (AccountingPeriodUtil.getTaxEndYear(LocalDate.now()) - 1).toString
        val toYear: String = AccountingPeriodUtil.getTaxEndYear(LocalDate.now()).toString

        val expectedText = removeHtmlMarkup(messages("agent.business.what-year-to-sign-up.option-1", fromYear, toYear))
        Then("Should return a OK with the What Year To Sign Up page")
        res must have(
          httpStatus(OK),
          pageTitle(s"${messages("agent.business.what-year-to-sign-up.heading")} - $serviceNameGovUk"),
          radioButtonSet(id = "accountingYear", selectedRadioButton = Some(expectedText)),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = None)
        )
      }
      "return SEE_OTHER" when {
        "The user is mandated for the current tax year" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Mandated, Voluntary)))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("GET /client/business/what-year-to-sign-up is called")
          val res = IncomeTaxSubscriptionFrontend.accountingYear(Map(
            UserMatchingSessionUtil.firstName -> testFirstName,
            UserMatchingSessionUtil.lastName -> testLastName
          ))

          Then("Should return SEE_OTHER to the what you need to do page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.whatYouNeedToDoURI)
          )
        }
        "The user is eligible for the next tax year only" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true)))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("GET /client/business/what-year-to-sign-up is called")
          val res = IncomeTaxSubscriptionFrontend.accountingYear(Map(
            UserMatchingSessionUtil.firstName -> testFirstName,
            UserMatchingSessionUtil.lastName -> testLastName
          ))

          Then("Should return SEE_OTHER to the what you need to do page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.whatYouNeedToDoURI)
          )
        }
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the What Year To Sign Up page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.accountingYear()
        Then("Should return a OK with the What Year To Sign Up page")
        res must have(
          httpStatus(200),
          pageTitle(s"${messages("agent.business.what-year-to-sign-up.heading")} - $serviceNameGovUk"),
          radioButtonSet(id = "accountingYear", selectedRadioButton = None),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = None)
        )
      }
    }

    "user is unauthorised" should {
      "return SEE_OTHER (303)" in {
        Given("I set up the Wiremock stubs")
        AuthStub.stubUnauthorised()

        When("GET /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.accountingYear()

        Then("Should return SEE_OTHER")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/business/what-year-to-sign-up"))
        )
      }
    }
  }

  s"POST ${routes.WhatYearToSignUpController.submit()}" when {
    "the user is unauthorised" must {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(Current))

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/business/what-year-to-sign-up"))
        )
      }
    }
    "the user has no state" must {
      "redirect to cannot go back to previous client page" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(Current), withJourneyState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }
    "in edit mode" must {
      "redirect to the global check your answers page" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SelectedTaxYear, AccountingYearModel(Current))

        val result = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, request = Some(Current))

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
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

          val result = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, request = None)

          result must have(
            httpStatus(BAD_REQUEST),
            pageTitle(s"Error: ${messages("agent.business.what-year-to-sign-up.heading")} - $serviceNameGovUk"),
            errorDisplayed()
          )
        }
      }
      "current tax year is selected" when {
        "the email capture consent feature switch is enabled" when {
          "the email passed flag is not present in session" must {
            "save the tax year and redirect to the capture consent page" in {
              enable(EmailCaptureConsent)

              AuthStub.stubAuthSuccess()
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.EMAIL_PASSED)(NO_CONTENT)
              IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SelectedTaxYear, AccountingYearModel(Current))

              val result = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(Current))

              result must have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.agent.email.routes.CaptureConsentController.show().url)
              )
            }
          }
          "the email passed flag is present in session" should {
            "save the tax year and redirect to the what you need to do page" in {
              enable(EmailCaptureConsent)

              AuthStub.stubAuthSuccess()
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
              SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.EMAIL_PASSED)(OK, JsBoolean(true))
              IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SelectedTaxYear, AccountingYearModel(Current))

              val result = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(Current))

              result must have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
              )
            }
          }
        }
        "the email capture consent feature switch is disabled" must {
          "save the tax year and redirect to the what you need to do page" in {
            AuthStub.stubAuthSuccess()
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.EMAIL_PASSED)(NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SelectedTaxYear, AccountingYearModel(Current))

            val result = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(Current))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
            )
          }
        }
      }
      "next tax year is selected" must {
        "save the tax year and redirect to the capture consent page" when {
          "the email capture consent feature switch is enabled" in {
            enable(EmailCaptureConsent)

            AuthStub.stubAuthSuccess()
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.EMAIL_PASSED)(NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SelectedTaxYear, AccountingYearModel(Next))

            val result = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(Next))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
            )
          }
          "the email capture consent feature switch is disabled" in {
            AuthStub.stubAuthSuccess()
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.EMAIL_PASSED)(NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SelectedTaxYear, AccountingYearModel(Next))

            val result = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(Next))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
            )
          }
        }
      }
    }
    "there was a problem saving the tax year selection" must {
      "return an internal server error page" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(SelectedTaxYear)

        val result = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(Current))

        result must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
    "there was a problem fetching the email passed flag from session" must {
      "return an internal server error page" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.EMAIL_PASSED)(INTERNAL_SERVER_ERROR)
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SelectedTaxYear, AccountingYearModel(Current))

        val result = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(Current))

        result must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
