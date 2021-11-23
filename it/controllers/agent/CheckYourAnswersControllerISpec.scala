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

import config.featureswitch.FeatureSwitch.{ReleaseFour, SPSEnabled}
import connectors.agent.httpparsers.QueryUsersHttpParser.principalUserIdKey
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, MultipleIncomeSourcesSubscriptionAPIStub, UsersGroupsSearchStub}
import helpers.IntegrationTestConstants.{checkYourAnswersURI => _, confirmationURI => _, incomeSourceURI => _, testNino => _, testUtr => _, _}
import helpers.IntegrationTestModels._
import helpers.WiremockHelper.verifyPost
import helpers.agent.IntegrationTestConstants._
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import helpers.servicemocks.EnrolmentStoreProxyStub.jsonResponseBody
import helpers.servicemocks.{ChannelPreferencesStub, EnrolmentStoreProxyStub, SubscriptionStub}
import models.common._
import models.common.business.BusinessSubscriptionDetailsModel
import models.sps.AgentSPSPayload
import models.{Cash, DateModel, Next}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.NO_CONTENT
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey, OverseasProperty, Property}

class CheckYourAnswersControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  override def beforeEach(): Unit = {
    disable(SPSEnabled)
    super.beforeEach()
  }

  "GET /check-your-answers" when {
    "the user has not answered the income sources question" should {
      "redirect the user to answer that question" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()

        When("GET /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()
        Then("Should return a OK with the check your answers page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourceURI)
        )
      }
    }

    "the Subscription Details Connector returns all data" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        When("GET /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the check your answers page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.summary.title") + serviceNameGovUk)
        )

      }
    }
  }

  "POST /check-your-answers" when {
    "property next tax year feature switch is disabled" when {
      "redirect to the confirmation page" should {
        "The whole subscription process was successful" when {
          "only self-employments selected and no other data" in {

            Given("I set the required feature switches")
            enable(ReleaseFour)

            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyStartDate = None,
                overseasAccountingMethodProperty = None
              )
            )(NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()


            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
            EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
            EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

          }

          "only self-employments selected and everything else answered" in {
            Given("I set the required feature switches")
            enable(ReleaseFour)

            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyStartDate = None,
                overseasAccountingMethodProperty = None
              )
            )(NO_CONTENT)


            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
            EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
            EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

          }

          "only UK property is answered" in {
            Given("I set the required feature switches")
            enable(ReleaseFour)

            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = None,
                accountingMethod = None,
                incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false),
                propertyStartDate = testFullPropertyModel.startDate.map(PropertyStartDateModel.apply),
                propertyAccountingMethod = testFullPropertyModel.accountingMethod.map(AccountingMethodPropertyModel.apply),
                overseasPropertyStartDate = None,
                overseasAccountingMethodProperty = None
              )
            )(NO_CONTENT)

            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
            EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
            EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

          }

          "everything has been answered but the user has only got uk property selected" in {
            Given("I set the required feature switches")
            enable(ReleaseFour)

            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = None,
                accountingMethod = None,
                incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false),
                propertyStartDate = testFullPropertyModel.startDate.map(PropertyStartDateModel.apply),
                propertyAccountingMethod = testFullPropertyModel.accountingMethod.map(AccountingMethodPropertyModel.apply),
                overseasPropertyStartDate = None,
                overseasAccountingMethodProperty = None
              )
            )(NO_CONTENT)


            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
            EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
            EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

          }

          "only foreign property has been answered" in {
            Given("I set the required feature switches")
            enable(ReleaseFour)

            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              OverseasProperty,
              OK,
              Json.toJson(
                OverseasPropertyModel(
                  accountingMethod = Some(Cash), startDate = Some(DateModel("21", "03", "2010"))
                )
              )
            )

            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = None,
                accountingMethod = None,
                incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true),
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(DateModel("21", "03", "2010"))),
                overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
              )
            )(NO_CONTENT)


            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
            EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
            EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

          }

          "everything has been answered but the user has only got foreign property selected" in {
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = None,
                accountingMethod = None,
                incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true),
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyStartDate = testFullOverseasPropertyModel.startDate.map(OverseasPropertyStartDateModel.apply),
                overseasAccountingMethodProperty = testFullOverseasPropertyModel.accountingMethod.map(OverseasAccountingMethodPropertyModel.apply),
              )
            )(NO_CONTENT)


            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
            EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
            EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

          }

          "successfully send the correct details to the backend for a user with all income" in {
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
                propertyStartDate = testFullPropertyModel.startDate.map(PropertyStartDateModel.apply),
                propertyAccountingMethod = testFullPropertyModel.accountingMethod.map(AccountingMethodPropertyModel.apply),
                overseasPropertyStartDate = testFullOverseasPropertyModel.startDate.map(OverseasPropertyStartDateModel.apply),
                overseasAccountingMethodProperty = testFullOverseasPropertyModel.accountingMethod.map(OverseasAccountingMethodPropertyModel.apply),
              )
            )(NO_CONTENT)


            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
            EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
            EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

          }
        }

        "subscription was successful but auto enrolment failed" when {
          "getting the group id the enrolment is allocated was not successful" in {
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyStartDate = None,
                overseasAccountingMethodProperty = None
              )
            )(NO_CONTENT)

            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(NO_CONTENT)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
          }

          "getting the users assigned to the enrolment was not successful" in {
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyStartDate = None,
                overseasAccountingMethodProperty = None
              )
            )(NO_CONTENT)

            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(NO_CONTENT)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
          }

          "getting the admin in a group was not successful" in {
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyStartDate = None,
                overseasAccountingMethodProperty = None
              )
            )(NO_CONTENT)

            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NOT_FOUND)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
          }

          "upserting the known facts was not successful" in {
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyStartDate = None,
                overseasAccountingMethodProperty = None
              )
            )(NO_CONTENT)

            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
            EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NOT_FOUND)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
          }

          "allocating the enrolment to a group was not successful" in {
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyStartDate = None,
                overseasAccountingMethodProperty = None
              )
            )(NO_CONTENT)

            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
            EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
            EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(NOT_FOUND)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
          }

          "assigning all the users to the enrolment was not successful" in {
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyStartDate = None,
                overseasAccountingMethodProperty = None
              )
            )(NO_CONTENT)

            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
            EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
            EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(NOT_FOUND)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
          }
        }
      }
    }

    "the signup of a user with next tax year property income is successful and creating of the enrolment is successful" when {
      "feature switch SPSEnabled is disabled" in {
        Given("I set the required feature switches")
        enable(ReleaseFour)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
          incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)),
          selectedTaxYear = Some(AccountingYearModel(Next)),
          businessName = None,
          accountingMethod = None
        ))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

        MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
        MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
          mtdbsa = testMtdId,
          request = BusinessSubscriptionDetailsModel(
            nino = testNino,
            accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
            selfEmploymentsData = None,
            accountingMethod = None,
            incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true),
            propertyStartDate = testFullPropertyModel.startDate.map(PropertyStartDateModel.apply),
            propertyAccountingMethod = testFullPropertyModel.accountingMethod.map(AccountingMethodPropertyModel.apply),
            overseasPropertyStartDate = testFullOverseasPropertyModel.startDate.map(OverseasPropertyStartDateModel.apply),
            overseasAccountingMethodProperty = testFullOverseasPropertyModel.accountingMethod.map(OverseasAccountingMethodPropertyModel.apply),
          )
        )(NO_CONTENT)


        And("The wiremock stubs for auto enrolment")
        EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)
        EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
        UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
        EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
        EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
        EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
        EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

        When("I call POST /check-your-answers")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(confirmationURI)
        )

        val expectedSPSBody: AgentSPSPayload = AgentSPSPayload(testARN, testNino, testUtr, testMtdId)
        verifyPost("/channel-preferences/enrolment", Some(Json.toJson(expectedSPSBody).toString), Some(0))

        val cookieMap = getSessionMap(res)
        cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
      }

      "feature switch SPSEnabled is enabled" in {
        Given("I set the required feature switches")
        enable(ReleaseFour)
        enable(SPSEnabled)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
          incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)),
          selectedTaxYear = Some(AccountingYearModel(Next)),
          businessName = None,
          accountingMethod = None
        ))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

        MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
        MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
          mtdbsa = testMtdId,
          request = BusinessSubscriptionDetailsModel(
            nino = testNino,
            accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
            selfEmploymentsData = None,
            accountingMethod = None,
            incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true),
            propertyStartDate = testFullPropertyModel.startDate.map(PropertyStartDateModel.apply),
            propertyAccountingMethod = testFullPropertyModel.accountingMethod.map(AccountingMethodPropertyModel.apply),
            overseasPropertyStartDate = testFullOverseasPropertyModel.startDate.map(OverseasPropertyStartDateModel.apply),
            overseasAccountingMethodProperty = testFullOverseasPropertyModel.accountingMethod.map(OverseasAccountingMethodPropertyModel.apply),
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

        ChannelPreferencesStub.stubAgentChannelPreferencesConfirm()

        When("I call POST /check-your-answers")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

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
  }
}

