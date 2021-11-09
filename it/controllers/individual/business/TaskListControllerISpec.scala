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

package controllers.individual.business

import config.featureswitch.FeatureSwitch.{SPSEnabled, SaveAndRetrieve}
import connectors.stubs._
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.{testBusinessName, _}
import helpers.WiremockHelper.verifyPost
import helpers._
import helpers.servicemocks._
import models._
import models.common._
import models.common.subscription.CreateIncomeSourcesModel
import models.sps.SPSPayload
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.ITSASessionKeys.SPSEntityId
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey, Property}

class TaskListControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/business/task-list" should {
    "return OK" when {
      "there is no user data setup" in {
        enable(SaveAndRetrieve)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.task-list.title") + serviceNameGovUk)
        )
      }
      "there is partial user data setup" in {
        enable(SaveAndRetrieve)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
          selectedTaxYear = Some(testAccountingYearCurrent),
          overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty),
        ))

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel.copy(accountingMethod = None, confirmed = false)))

        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.task-list.title") + serviceNameGovUk)
        )
      }
      "there is full user data setup" in {
        enable(SaveAndRetrieve)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
          selectedTaxYear = Some(testAccountingYearCurrent),
          overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty),
          overseasPropertyStartDate = Some(testOverseasPropertyStartDate)
        ))

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))

        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.task-list.title") + serviceNameGovUk)
        )
      }
    }
    "return NOT_FOUND" when {
      "the save & retrieve feature switch is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
          incomeSource = None,
          selectedTaxYear = Some(testAccountingYearCurrent),
          businessName = None,
          accountingMethod = None,
          overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty),
          overseasPropertyStartDate = Some(testOverseasPropertyStartDate)
        ))

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return NOT FOUND")
        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/task-list" when {
    "the save and retrieve feature switch is enabled" when {
      "the subscription successfully" when {
        "the income source is only self employment" should {
          "send the correct details to the backend, call sps with the users details and redirect to the confirmation page" in {

            Given("I set the required feature switches")
            enable(SaveAndRetrieve)
            enable(SPSEnabled)

            And("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(testAccountingYearCurrentConfirmed),
              businessName = Some(testBusinessName),
              accountingMethod = Some(testAccountingMethod),
              overseasPropertyAccountingMethod = None,
              overseasPropertyStartDate = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
              mtdbsa = testMtdId,
              request = CreateIncomeSourcesModel(
                nino = testNino,
                selfEmployments = Some(testSoleTraderBusinesses)
              )
            )(NO_CONTENT)

            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, CREATED)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId(selfEmploymentSubscriptionData)

            ChannelPreferencesStub.stubChannelPreferenceConfirm()

            When("POST /business/task-list is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitTaskList(Map(SPSEntityId -> testEntityId))

            Then("Should return a SEE_OTHER with a redirect location of confirmation")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))
          }
        }

        "the income source is only uk property" should {
          "send the correct details to the backend, call sps with the users details and redirect to the confirmation page" in {

            Given("I set the required feature switches")
            enable(SaveAndRetrieve)
            enable(SPSEnabled)

            And("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
              selectedTaxYear = Some(testAccountingYearCurrentConfirmed),
              businessName = None,
              accountingMethod = None,
              overseasPropertyAccountingMethod = None,
              overseasPropertyStartDate = None
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

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
              mtdbsa = testMtdId,
              request = CreateIncomeSourcesModel(
                nino = testNino,
                selfEmployments = None,
                ukProperty = Some(testUkProperty),
                overseasProperty = None
              )
            )(NO_CONTENT)

            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, CREATED)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId(ukPropertySubscriptionData)

            ChannelPreferencesStub.stubChannelPreferenceConfirm()

            When("POST /business/task-list is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitTaskList(Map(SPSEntityId -> testEntityId))

            Then("Should return a SEE_OTHER with a redirect location of confirmation")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))
          }
        }

        "the income source is only overseas property" should {
          "send the correct details to the backend, call sps with the users details and redirect to the confirmation page" in {
            Given("I set the required feature switches")
            enable(SaveAndRetrieve)
            enable(SPSEnabled)

            And("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
              selectedTaxYear = Some(testAccountingYearCurrentConfirmed),
              businessName = None,
              accountingMethod = None,
              overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty),
              overseasPropertyStartDate = Some(testOverseasPropertyStartDateModel)
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
              mtdbsa = testMtdId,
              request = CreateIncomeSourcesModel(
                nino = testNino,
                selfEmployments = None,
                overseasProperty = Some(testOverseasProperty)
              )
            )(NO_CONTENT)

            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, CREATED)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId(overseasPropertySubscriptionData)

            ChannelPreferencesStub.stubChannelPreferenceConfirm()

            When("POST /business/task-list is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitTaskList(Map(SPSEntityId -> testEntityId))

            Then("Should return a SEE_OTHER with a redirect location of confirmation")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))
          }
        }

        "the income source contains self-employments, uk property and overseas property" should {
          "send the correct details to the backend, call sps with the users details and redirect to the confirmation page" in {

            Given("I set the required feature switches")
            enable(SaveAndRetrieve)
            enable(SPSEnabled)

            And("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
              selectedTaxYear = Some(testAccountingYearCurrentConfirmed),
              businessName = Some(testBusinessName),
              accountingMethod = Some(testAccountingMethod),
              overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty),
              overseasPropertyStartDate = Some(testOverseasPropertyStartDateModel)
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

            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, CREATED)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId(AllSubscriptionData)

            ChannelPreferencesStub.stubChannelPreferenceConfirm()

            When("POST /business/task-list is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitTaskList(Map(SPSEntityId -> testEntityId))

            Then("Should return a SEE_OTHER with a redirect location of confirmation")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))
          }
        }

      }

      "the subscription failed" should {
        "return a internalServer error and will not call sps" in {

          Given("I set the required feature switches")
          enable(SaveAndRetrieve)
          enable(SPSEnabled)

          And("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
            selectedTaxYear = Some(AccountingYearModel(Next, confirmed = true)),
            businessName = Some(testBusinessName),
            accountingMethod = None,
            overseasPropertyAccountingMethod = None,
            overseasPropertyStartDate = None
          ))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              selfEmployments = Some(testSoleTraderBusinesses)
            )
          )(INTERNAL_SERVER_ERROR)

          When("POST /business/task-list is called")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("Should return a INTERNAL SERVER ERROR status")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )

          verifyPost("/channel-preferences/confirm", count = Some(0))
        }
      }

      "save and retrieve feature switch is disabled" should {
        "throw NotFoundException and will not call sps" in {

          Given("I set the required feature switches")
          enable(SPSEnabled)

          And("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
            selectedTaxYear = Some(AccountingYearModel(Next, confirmed = true)),
            businessName = None,
            accountingMethod = None,
            overseasPropertyAccountingMethod = None,
            overseasPropertyStartDate = None
          ))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

          When("POST /business/task-list is called")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("Should return a Not Found Exception status")
          res should have(
            httpStatus(NOT_FOUND)
          )

          verifyPost("/channel-preferences/confirm", count = Some(0))
        }
      }


    }
  }
}