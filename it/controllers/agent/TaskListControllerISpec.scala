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

package controllers.agent

import config.featureswitch.FeatureSwitch.{SPSEnabled, SaveAndRetrieve}
import connectors.agent.httpparsers.QueryUsersHttpParser.principalUserIdKey
import connectors.stubs._
import helpers.IntegrationTestConstants.{confirmationURI => _, _}
import helpers.IntegrationTestModels.{testBusinessName => _, _}
import helpers.WiremockHelper.verifyPost
import helpers.agent.IntegrationTestConstants.{testNino => _, testUtr => _, _}
import helpers.agent.servicemocks._
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import helpers.servicemocks.EnrolmentStoreProxyStub
import helpers.servicemocks.EnrolmentStoreProxyStub.jsonResponseBody
import models.common.subscription.CreateIncomeSourcesModel
import models.sps.AgentSPSPayload
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.NO_CONTENT
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey, OverseasProperty, Property}

class TaskListControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/task-list" should {
    "return OK" when {
      "there is no user data setup" in {
        enable(SaveAndRetrieve)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.business.task-list.title") + serviceNameGovUk)
        )
      }
      "there is partial user data setup" in {
        enable(SaveAndRetrieve)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
          selectedTaxYear = Some(testAccountingYearCurrent)
        ))

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel.copy(accountingMethod = None, confirmed = false)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel.copy(accountingMethod = None, confirmed = false)))

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.business.task-list.title") + serviceNameGovUk)
        )
      }
      "there is full user data setup" in {
        enable(SaveAndRetrieve)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
          selectedTaxYear = Some(testAccountingYearCurrent)
        ))

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.business.task-list.title") + serviceNameGovUk)
        )
      }
    }

    "return NOT_FOUND" when {
      "the save & retrieve feature switch is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return NOT FOUND")
        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/task-list" when {
    "the save and retrieve feature switch is enabled" should {
      "sign up the client successfully" when {
        "they only have self employment income" in {
          Given("I set the required feature switches")
          enable(SaveAndRetrieve)
          enable(SPSEnabled)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            selectedTaxYear = Some(testAccountingYearCurrentConfirmed)
          ))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              selfEmployments = Some(testSoleTraderBusinesses)
            )
          )(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

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

          Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtr, testMtdId)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

        }
        "they only have uk property income" in {
          Given("I set the required feature switches")
          enable(SaveAndRetrieve)
          enable(SPSEnabled)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            selectedTaxYear = Some(testAccountingYearCurrentConfirmed)
          ))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(testFullPropertyModel.copy(
              accountingMethod = Some(testUkProperty.accountingMethod),
              startDate = Some(testUkProperty.tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              ukProperty = Some(testUkProperty)
            )
          )(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

          And("The wiremock stubs for auto enrolment")
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
          EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
          UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
          EnrolmentStoreProxyStub.stubUpsertEnrolment(testMTDID, testNino)(NO_CONTENT)
          EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testMTDID, testGroupId, testCredentialId)(CREATED)
          EnrolmentStoreProxyStub.stubAssignEnrolment(testMTDID, testCredentialId)(CREATED)
          EnrolmentStoreProxyStub.stubAssignEnrolment(testMTDID, testCredentialId2)(CREATED)

          When("I call POST /task-list")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtr, testMtdId)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

        }
        "they only have overseas property income" in {
          Given("I set the required feature switches")
          enable(SaveAndRetrieve)
          enable(SPSEnabled)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            selectedTaxYear = Some(testAccountingYearCurrentConfirmed)
          ))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(testFullOverseasPropertyModel.copy(
              accountingMethod = Some(testOverseasProperty.accountingMethod),
              startDate = Some(testOverseasProperty.tradingStartDate)
            )))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              overseasProperty = Some(testOverseasProperty)
            )
          )(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

          And("The wiremock stubs for auto enrolment")
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
          EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
          UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
          EnrolmentStoreProxyStub.stubUpsertEnrolment(testMTDID, testNino)(NO_CONTENT)
          EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testMTDID, testGroupId, testCredentialId)(CREATED)
          EnrolmentStoreProxyStub.stubAssignEnrolment(testMTDID, testCredentialId)(CREATED)
          EnrolmentStoreProxyStub.stubAssignEnrolment(testMTDID, testCredentialId2)(CREATED)

          When("I call POST /task-list")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtr, testMtdId)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID


        }
        "they have both self employment and uk property income" in {
          Given("I set the required feature switches")
          enable(SaveAndRetrieve)
          enable(SPSEnabled)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            selectedTaxYear = Some(testAccountingYearCurrentConfirmed)
          ))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(testFullPropertyModel.copy(
              accountingMethod = Some(testUkProperty.accountingMethod),
              startDate = Some(testUkProperty.tradingStartDate)
            ))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              selfEmployments = Some(testSoleTraderBusinesses),
              ukProperty = Some(testUkProperty)
            )
          )(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

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

          Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtr, testMtdId)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID


        }
        "they have both self employment and overseas property income" in {
          Given("I set the required feature switches")
          enable(SaveAndRetrieve)
          enable(SPSEnabled)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            selectedTaxYear = Some(testAccountingYearCurrentConfirmed)
          ))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(testFullOverseasPropertyModel.copy(
              accountingMethod = Some(testOverseasProperty.accountingMethod),
              startDate = Some(testOverseasProperty.tradingStartDate)
            )))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              selfEmployments = Some(testSoleTraderBusinesses),
              overseasProperty = Some(testOverseasProperty)
            )
          )(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

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

          Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtr, testMtdId)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID


        }
        "they have both uk property and overseas property income" in {
          Given("I set the required feature switches")
          enable(SaveAndRetrieve)
          enable(SPSEnabled)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            selectedTaxYear = Some(testAccountingYearCurrentConfirmed)
          ))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(testFullPropertyModel.copy(
              accountingMethod = Some(testUkProperty.accountingMethod),
              startDate = Some(testUkProperty.tradingStartDate)
            )))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(testFullOverseasPropertyModel.copy(
              accountingMethod = Some(testOverseasProperty.accountingMethod),
              startDate = Some(testOverseasProperty.tradingStartDate)
            )))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              ukProperty = Some(testUkProperty),
              overseasProperty = Some(testOverseasProperty)
            )
          )(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

          And("The wiremock stubs for auto enrolment")
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
          EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
          UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
          EnrolmentStoreProxyStub.stubUpsertEnrolment(testMTDID, testNino)(NO_CONTENT)
          EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testMTDID, testGroupId, testCredentialId)(CREATED)
          EnrolmentStoreProxyStub.stubAssignEnrolment(testMTDID, testCredentialId)(CREATED)
          EnrolmentStoreProxyStub.stubAssignEnrolment(testMTDID, testCredentialId2)(CREATED)

          When("I call POST /task-list")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtr, testMtdId)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID


        }

        "they have self employment, uk property and overseas property income" in {
          Given("I set the required feature switches")
          enable(SaveAndRetrieve)
          enable(SPSEnabled)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            selectedTaxYear = Some(testAccountingYearCurrentConfirmed)
          ))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(testFullPropertyModel.copy(
              accountingMethod = Some(testUkProperty.accountingMethod),
              startDate = Some(testUkProperty.tradingStartDate)
            )))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(testFullOverseasPropertyModel.copy(
              accountingMethod = Some(testOverseasProperty.accountingMethod),
              startDate = Some(testOverseasProperty.tradingStartDate)
            )))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              selfEmployments = Some(testSoleTraderBusinesses),
              ukProperty = Some(testUkProperty),
              overseasProperty = Some(testOverseasProperty)
            )
          )(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

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

          Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )

          val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtr, testMtdId)
          verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(1))

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

        }
      }
      "fail to sign up the client" when {
        "the user has not selected their tax year" in {
          Given("I set the required feature switches")
          enable(SaveAndRetrieve)
          enable(SPSEnabled)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            selectedTaxYear = None
          ))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              selfEmployments = Some(testSoleTraderBusinesses)
            )
          )(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

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

          Then("The result should have a status of INTERNAL_SERVER_ERROR and redirect to the confirmation page")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )

        }
        "the user has not got any business income sources" in {
          Given("I set the required feature switches")
          enable(SaveAndRetrieve)
          enable(SPSEnabled)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            selectedTaxYear = Some(testAccountingYearCurrentConfirmed)
          ))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              selfEmployments = Some(testSoleTraderBusinesses)
            )
          )(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

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

          Then("The result should have a status of INTERNAL_SERVER_ERROR and redirect to the confirmation page")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )

        }
      }
    }
    "the save and retrieve feature switch is disabled" should {
      "Return a NOT_FOUND page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return NOT_FOUND with the task list page")
        res should have(
          httpStatus(NOT_FOUND)
        )
      }

    }
  }
}
