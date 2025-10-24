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

package controllers.agent

import common.Constants.ITSASessionKeys
import connectors.agent.httpparsers.QueryUsersHttpParser.principalUserIdKey
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, MultipleIncomeSourcesSubscriptionAPIStub, SessionDataConnectorStub, UsersGroupsSearchStub}
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.WiremockHelper.verifyPost
import helpers.agent._
import helpers.agent.servicemocks.{AgentServicesStub, AuthStub}
import helpers.servicemocks.EnrolmentStoreProxyStub
import helpers.servicemocks.EnrolmentStoreProxyStub.jsonResponseBody
import models.common.subscription.{CreateIncomeSourcesModel, SignUpModel}
import models.sps.AgentSPSPayload
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import models.{DateModel, EligibilityStatus, Next, Yes}
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys._

class GlobalCheckYourAnswersControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  def testSignUpModel(taxYear: String): SignUpModel = SignUpModel(
    nino = testNino,
    utr = testUtr,
    taxYear = taxYear
  )

  "GET /report-quarterly/income-and-expenses/sign-up/client/final-check-your-answers" when {
    "return SEE_OTHER to the login page" when {
      "user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.getAgentGlobalCheckYourAnswers()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/final-check-your-answers"))
        )
      }
    }
    "the pre-pop feature switch is enabled" should {
      "return SEE OTHER to the your income sources page" when {
        "there is missing data from the users subscription" in {

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
            ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)),
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))

          When("GET /client/final-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.getAgentGlobalCheckYourAnswers()

          Then("Should redirect to the task list page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.yourIncomeSourcesURI)
          )
        }
        "there are unconfirmed income sources in the users subscription" in {

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel.copy(confirmed = false)))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
            ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)),
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))

          When("GET /client/final-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.getAgentGlobalCheckYourAnswers()

          Then("Should redirect to the task list page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.yourIncomeSourcesURI)
          )
        }
      }
    }
    "return OK" when {
      "all data was received and is complete" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)),
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))

        val serviceNameGovUk = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"

        When("GET /client/final-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getAgentGlobalCheckYourAnswers()

        Then("Should return OK with the global check your answers page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.global-check-your-answers.subheading") + serviceNameGovUk)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/final-check-your-answers" when {
    "return SEE_OTHER to the login page" when {
      "user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.getAgentGlobalCheckYourAnswers()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/final-check-your-answers"))
        )
      }
    }
    "there is complete data" should {
      "sign up and redirect to the confirmation page" when {
        "signing up for the current tax year" when {
          "all calls were successful" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              Property,
              OK,
              Json.toJson(testFullPropertyModel.copy(
                startDate = Some(testUkProperty().tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              OverseasProperty,
              OK,
              Json.toJson(testFullOverseasPropertyModel.copy(
                startDate = Some(testOverseasProperty().tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))
            SessionDataConnectorStub.stubGetAllSessionData(Map(
              ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)),
              ITSASessionKeys.NINO -> JsString(testNino),
              ITSASessionKeys.UTR -> JsString(testUtr)
            ))

            AgentServicesStub.stubMTDClientRelationship(testARN, testNino, exists = true)
            AgentServicesStub.stubMTDSuppAgentRelationship(testARN, testNino, exists = false)

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testSignUpModel(AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear))(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
              mtdbsa = testMtdId,
              request = CreateIncomeSourcesModel(
                nino = testNino,
                soleTraderBusinesses = Some(testSoleTraderBusinesses().copy(
                  businesses = testSoleTraderBusinesses().businesses.map(business =>
                    business.copy(confirmed = true, businessStartDate = business.businessStartDate.map(date => date.copy(startDate = DateModel.dateConvert(date.startDate.toLocalDate))), startDateBeforeLimit = Some(true))
                  )
                )),
                ukProperty = Some(testUkProperty().copy(tradingStartDate = DateModel.dateConvert(testUkProperty().tradingStartDate.toLocalDate), startDateBeforeLimit = Some(true))),
                overseasProperty = Some(testOverseasProperty().copy(tradingStartDate = DateModel.dateConvert(testOverseasProperty().tradingStartDate.toLocalDate), startDateBeforeLimit = Some(true)))
              )
            )(NO_CONTENT)

            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
            EnrolmentStoreProxyStub.stubUpsertEnrolment(testMtdId, testNino)(NO_CONTENT)
            EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testMtdId, testGroupId, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testMtdId, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testMtdId, testCredentialId2)(CREATED)

            When("POST /client/final-check-your-answers is called")
            val res = IncomeTaxSubscriptionFrontend.submitAgentGlobalCheckYourAnswers(Some(Yes))()

            Then("Should redirect to the confirmation page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(AgentURI.confirmationURI)
            )

            val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtrEnrolmentKey, testMTDIDEnrolmentKey)
            verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))
          }
        }
        "signing up for the next tax year" when {
          "all calls were successful" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              Property,
              OK,
              Json.toJson(testFullPropertyModel.copy(
                startDateBeforeLimit = Some(true),
                startDate = Some(testUkProperty(Next).tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              OverseasProperty,
              OK,
              Json.toJson(testFullOverseasPropertyModel.copy(
                startDateBeforeLimit = Some(true),
                startDate = Some(testOverseasProperty(Next).tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNextConfirmed))
            SessionDataConnectorStub.stubGetAllSessionData(Map(
              ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)),
              ITSASessionKeys.NINO -> JsString(testNino),
              ITSASessionKeys.UTR -> JsString(testUtr)
            ))

            AgentServicesStub.stubMTDClientRelationship(testARN, testNino, exists = false)
            AgentServicesStub.stubMTDSuppAgentRelationship(testARN, testNino, exists = true)

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testSignUpModel(AccountingPeriodUtil.getNextTaxYear.toLongTaxYear))(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
              mtdbsa = testMtdId,
              request = CreateIncomeSourcesModel(
                nino = testNino,
                soleTraderBusinesses = Some(testSoleTraderBusinesses(Next).copy(
                  businesses = testSoleTraderBusinesses(Next).businesses.map(business =>
                    business.copy(confirmed = true, businessStartDate = business.businessStartDate.map(date => date.copy(startDate = DateModel.dateConvert(date.startDate.toLocalDate))), startDateBeforeLimit = Some(true))
                  )
                )),
                ukProperty = Some(testUkProperty(Next).copy(tradingStartDate = DateModel.dateConvert(testUkProperty(Next).tradingStartDate.toLocalDate), startDateBeforeLimit = Some(true))),
                overseasProperty = Some(testOverseasProperty(Next).copy(tradingStartDate = DateModel.dateConvert(testOverseasProperty(Next).tradingStartDate.toLocalDate), startDateBeforeLimit = Some(true)))
              )
            )(NO_CONTENT)

            When("POST /client/final-check-your-answers is called")
            val res = IncomeTaxSubscriptionFrontend.submitAgentGlobalCheckYourAnswers(Some(Yes))()

            Then("Should redirect to the confirmation page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(AgentURI.confirmationURI)
            )
          }
        }
        "sign up returns a response indicating the customer is already signed up" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(testFullPropertyModel.copy(
              startDate = Some(testUkProperty(Next).tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(testFullOverseasPropertyModel.copy(
              startDate = Some(testOverseasProperty(Next).tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNextConfirmed))
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
            ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)),
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testSignUpModel(AccountingPeriodUtil.getNextTaxYear.toLongTaxYear))(UNPROCESSABLE_ENTITY)

          When("POST /client/final-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitAgentGlobalCheckYourAnswers(Some(Yes))()

          Then("Should redirect to the confirmation page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.confirmationURI)
          )
        }
      }
      "return INTERNAL SERVER ERROR" when {
        "sign up failed" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(testFullPropertyModel.copy(
              startDate = Some(testUkProperty().tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(testFullOverseasPropertyModel.copy(
              startDate = Some(testOverseasProperty().tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
            ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)),
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testSignUpModel(AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear))(INTERNAL_SERVER_ERROR)

          When("POST /client/final-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitAgentGlobalCheckYourAnswers(Some(Yes))()

          Then("Should show the internal service error page")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "create income sources failed" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(testFullPropertyModel.copy(
              startDate = Some(testUkProperty().tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(testFullOverseasPropertyModel.copy(
              startDate = Some(testOverseasProperty().tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
            ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)),
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testSignUpModel(AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear))(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              soleTraderBusinesses = Some(testSoleTraderBusinesses().copy(
                businesses = testSoleTraderBusinesses().businesses.map(business =>
                  business.copy(confirmed = true, businessStartDate = business.businessStartDate.map(date => date.copy(startDate = DateModel.dateConvert(date.startDate.toLocalDate))))
                )
              )),
              ukProperty = Some(testUkProperty().copy(tradingStartDate = DateModel.dateConvert(testUkProperty().tradingStartDate.toLocalDate))),
              overseasProperty = Some(testOverseasProperty().copy(tradingStartDate = DateModel.dateConvert(testOverseasProperty().tradingStartDate.toLocalDate)))
            )
          )(INTERNAL_SERVER_ERROR)

          When("POST /client/final-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitAgentGlobalCheckYourAnswers(Some(Yes))()

          Then("Should show the internal service error page")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }

}
