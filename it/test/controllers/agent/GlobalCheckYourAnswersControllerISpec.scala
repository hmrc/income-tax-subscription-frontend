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

import connectors.agent.httpparsers.QueryUsersHttpParser.principalUserIdKey
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, MultipleIncomeSourcesSubscriptionAPIStub, UsersGroupsSearchStub}
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.WiremockHelper.verifyPost
import helpers.agent._
import helpers.agent.servicemocks.AuthStub
import helpers.servicemocks.EnrolmentStoreProxyStub
import helpers.servicemocks.EnrolmentStoreProxyStub.jsonResponseBody
import models.common.subscription.CreateIncomeSourcesModel
import models.sps.AgentSPSPayload
import models.{DateModel, Next, Yes}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys._

class GlobalCheckYourAnswersControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/client/final-check-your-answers" should {
    "return SEE_OTHER to the task list page" when {
      "there is missing data from the users subscription" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountingMethod.accountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

        When("GET /client/final-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getAgentGlobalCheckYourAnswers()

        Then("Should redirect to the task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.taskListURI)
        )
      }
    }
    "return OK" when {
      "all data was received and is complete" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountingMethod.accountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        When("GET /client/final-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getAgentGlobalCheckYourAnswers()

        Then("Should return OK with the global check your answers page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.global-check-your-answers.heading") + serviceNameGovUk)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/final-check-your-answers" when {
    "there is missing data from the users subscription" should {
      "return SEE_OTHER to the task list page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountingMethod.accountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

        When("POST /final-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitAgentGlobalCheckYourAnswers(None)()

        Then("Should redirect to the task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.taskListURI)
        )
      }
    }
    "there is complete data" should {
      "sign up and redirect to the confirmation page" when {
        "signing up for the current tax year" when {
          "all calls were successful" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountingMethod.accountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              Property,
              OK,
              Json.toJson(testFullPropertyModel.copy(
                accountingMethod = Some(testUkProperty().accountingMethod),
                startDate = Some(testUkProperty().tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              OverseasProperty,
              OK,
              Json.toJson(testFullOverseasPropertyModel.copy(
                accountingMethod = Some(testOverseasProperty().accountingMethod),
                startDate = Some(testOverseasProperty().tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino, AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear)(OK)
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

            IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountingMethod.accountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              Property,
              OK,
              Json.toJson(testFullPropertyModel.copy(
                accountingMethod = Some(testUkProperty(Next).accountingMethod),
                startDate = Some(testUkProperty(Next).tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              OverseasProperty,
              OK,
              Json.toJson(testFullOverseasPropertyModel.copy(
                accountingMethod = Some(testOverseasProperty(Next).accountingMethod),
                startDate = Some(testOverseasProperty(Next).tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNextConfirmed))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino, AccountingPeriodUtil.getNextTaxYear.toLongTaxYear)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
              mtdbsa = testMtdId,
              request = CreateIncomeSourcesModel(
                nino = testNino,
                soleTraderBusinesses = Some(testSoleTraderBusinesses(Next).copy(
                  businesses = testSoleTraderBusinesses(Next).businesses.map(business =>
                    business.copy(confirmed = true, businessStartDate = business.businessStartDate.map(date => date.copy(startDate = DateModel.dateConvert(date.startDate.toLocalDate))))
                  )
                )),
                ukProperty = Some(testUkProperty(Next).copy(tradingStartDate = DateModel.dateConvert(testUkProperty(Next).tradingStartDate.toLocalDate))),
                overseasProperty = Some(testOverseasProperty(Next).copy(tradingStartDate = DateModel.dateConvert(testOverseasProperty(Next).tradingStartDate.toLocalDate)))
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
      }
      "return INTERNAL SERVER ERROR" when {
        "sign up failed" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountingMethod.accountingMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(testFullPropertyModel.copy(
              accountingMethod = Some(testUkProperty().accountingMethod),
              startDate = Some(testUkProperty().tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(testFullOverseasPropertyModel.copy(
              accountingMethod = Some(testOverseasProperty().accountingMethod),
              startDate = Some(testOverseasProperty().tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino, AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear)(INTERNAL_SERVER_ERROR)

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

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountingMethod.accountingMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(testFullPropertyModel.copy(
              accountingMethod = Some(testUkProperty().accountingMethod),
              startDate = Some(testUkProperty().tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(testFullOverseasPropertyModel.copy(
              accountingMethod = Some(testOverseasProperty().accountingMethod),
              startDate = Some(testOverseasProperty().tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino, AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear)(OK)
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
