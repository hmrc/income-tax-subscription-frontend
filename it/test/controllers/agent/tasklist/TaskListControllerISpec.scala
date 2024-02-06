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

import _root_.config.featureswitch.FeatureSwitch.EnableTaskListRedesign
import common.Constants.ITSASessionKeys
import connectors.agent.httpparsers.QueryUsersHttpParser.principalUserIdKey
import connectors.stubs._
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.{testBusinessName => _, _}
import helpers.WiremockHelper.verifyPost
import helpers.agent.servicemocks._
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import helpers.servicemocks.EnrolmentStoreProxyStub
import helpers.servicemocks.EnrolmentStoreProxyStub.jsonResponseBody
import models.Next
import models.common.subscription.CreateIncomeSourcesModel
import models.sps.AgentSPSPayload
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.NO_CONTENT
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys._

class TaskListControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EnableTaskListRedesign)
  }

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/task-list" should {
    "return OK" when {
      "there is no user data setup" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(IncomeSourceConfirmation, NO_CONTENT)

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.business.task-list.title") + serviceNameGovUk)
        )
      }
      "there is partial user data setup" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel.copy(accountingMethod = None, confirmed = false)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel.copy(accountingMethod = None, confirmed = false)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(IncomeSourceConfirmation, NO_CONTENT)

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.business.task-list.title") + serviceNameGovUk)
        )
      }
      "there is full user data setup" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(IncomeSourceConfirmation, OK, Json.toJson(true))

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.business.task-list.title") + serviceNameGovUk)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/task-list" when {
    "the task list redesign feature switch is enabled" should {
      "redirect to the global check your answers page" in {
        enable(EnableTaskListRedesign)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          Property,
          OK,
          Json.toJson(testFullPropertyModel.copy(
            accountingMethod = Some(testUkProperty().accountingMethod),
            startDate = Some(testUkProperty().tradingStartDate)
          ))
        )
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

        When("I call POST /task-list")
        val res = IncomeTaxSubscriptionFrontend.submitTaskList()

        Then("The result must have a status of SEE_OTHER and redirect to the confirmation page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.globalCheckYourAnswersURI)
        )
      }
    }
    "the task list redesign feature switch is disabled" should {
      "sign up the client successfully" when {
        "they only have self employment income" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino, AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              soleTraderBusinesses = Some(testSoleTraderBusinesses())
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

          When("I call POST /task-list")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("The result must have a status of SEE_OTHER and redirect to the confirmation page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtrEnrolmentKey, testMTDIDEnrolmentKey)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) mustBe testMtdId

        }

        "they only have uk property income" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(testFullPropertyModel.copy(
              accountingMethod = Some(testUkProperty().accountingMethod),
              startDate = Some(testUkProperty().tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino, AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              ukProperty = Some(testUkProperty())
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

          When("I call POST /task-list")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("The result must have a status of SEE_OTHER and redirect to the confirmation page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtrEnrolmentKey, testMTDIDEnrolmentKey)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) mustBe testMtdId

        }
        "they only have overseas property income" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(testFullOverseasPropertyModel.copy(
              accountingMethod = Some(testOverseasProperty().accountingMethod),
              startDate = Some(testOverseasProperty().tradingStartDate)
            )))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino, AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              overseasProperty = Some(testOverseasProperty())
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

          When("I call POST /task-list")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("The result must have a status of SEE_OTHER and redirect to the confirmation page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtrEnrolmentKey, testMTDIDEnrolmentKey)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) mustBe testMtdId


        }
        "they have both self employment and uk property income" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(testFullPropertyModel.copy(
              accountingMethod = Some(testUkProperty().accountingMethod),
              startDate = Some(testUkProperty().tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino, AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              soleTraderBusinesses = Some(testSoleTraderBusinesses()),
              ukProperty = Some(testUkProperty())
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

          When("I call POST /task-list")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("The result must have a status of SEE_OTHER and redirect to the confirmation page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtrEnrolmentKey, testMTDIDEnrolmentKey)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) mustBe testMtdId


        }
        "they have both self employment and overseas property income" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(testFullOverseasPropertyModel.copy(
              accountingMethod = Some(testOverseasProperty().accountingMethod),
              startDate = Some(testOverseasProperty().tradingStartDate)
            )))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino, AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              soleTraderBusinesses = Some(testSoleTraderBusinesses()),
              overseasProperty = Some(testOverseasProperty())
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

          When("I call POST /task-list")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("The result must have a status of SEE_OTHER and redirect to the confirmation page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtrEnrolmentKey, testMTDIDEnrolmentKey)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) mustBe testMtdId


        }
        "they have both uk property and overseas property income" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(testFullPropertyModel.copy(
              accountingMethod = Some(testUkProperty().accountingMethod),
              startDate = Some(testUkProperty().tradingStartDate)
            )))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(testFullOverseasPropertyModel.copy(
              accountingMethod = Some(testOverseasProperty().accountingMethod),
              startDate = Some(testOverseasProperty().tradingStartDate)
            )))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino, AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              ukProperty = Some(testUkProperty()),
              overseasProperty = Some(testOverseasProperty())
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

          When("I call POST /task-list")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("The result must have a status of SEE_OTHER and redirect to the confirmation page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtrEnrolmentKey, testMTDIDEnrolmentKey)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) mustBe testMtdId


        }

        "they have self employment, uk property and overseas property income" when {
          "they are signing up to the current tax year" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              Property,
              OK,
              Json.toJson(testFullPropertyModel.copy(
                accountingMethod = Some(testUkProperty().accountingMethod),
                startDate = Some(testUkProperty().tradingStartDate)
              )))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              OverseasProperty,
              OK,
              Json.toJson(testFullOverseasPropertyModel.copy(
                accountingMethod = Some(testOverseasProperty().accountingMethod),
                startDate = Some(testOverseasProperty().tradingStartDate)
              )))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino, AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
              mtdbsa = testMtdId,
              request = CreateIncomeSourcesModel(
                nino = testNino,
                soleTraderBusinesses = Some(testSoleTraderBusinesses()),
                ukProperty = Some(testUkProperty()),
                overseasProperty = Some(testOverseasProperty())
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

            When("I call POST /task-list")
            val res = IncomeTaxSubscriptionFrontend.submitTaskList()

            Then("The result must have a status of SEE_OTHER and redirect to the confirmation page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(AgentURI.confirmationURI)
            )

            val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtrEnrolmentKey, testMTDIDEnrolmentKey)
            verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) mustBe testMtdId
          }
          "they are signing up to the next tax year" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              Property,
              OK,
              Json.toJson(testFullPropertyModel.copy(
                accountingMethod = Some(testUkProperty(Next).accountingMethod),
                startDate = Some(testUkProperty(Next).tradingStartDate)
              )))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              OverseasProperty,
              OK,
              Json.toJson(testFullOverseasPropertyModel.copy(
                accountingMethod = Some(testOverseasProperty(Next).accountingMethod),
                startDate = Some(testOverseasProperty(Next).tradingStartDate)
              )))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNextConfirmed))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino, AccountingPeriodUtil.getNextTaxYear.toLongTaxYear)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
              mtdbsa = testMtdId,
              request = CreateIncomeSourcesModel(
                nino = testNino,
                soleTraderBusinesses = Some(testSoleTraderBusinesses(accountingYear = Next)),
                ukProperty = Some(testUkProperty(accountingYear = Next)),
                overseasProperty = Some(testOverseasProperty(accountingYear = Next))
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

            When("I call POST /task-list")
            val res = IncomeTaxSubscriptionFrontend.submitTaskList()

            Then("The result must have a status of SEE_OTHER and redirect to the confirmation page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(AgentURI.confirmationURI)
            )

            val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtrEnrolmentKey, testMTDIDEnrolmentKey)
            verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) mustBe testMtdId
          }
        }
      }
      "fail to sign up the client" when {
        "the user has not selected their tax year" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty), Some(testAccountMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

          When("I call POST /task-list")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("The result must have a status of INTERNAL_SERVER_ERROR")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )

        }
        "the user has not got any business income sources" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

          When("I call POST /task-list")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("The result must have a status of INTERNAL_SERVER_ERROR")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }
}
