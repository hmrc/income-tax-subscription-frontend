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

import _root_.common.Constants.ITSASessionKeys.SPSEntityId
import config.featureswitch.FeatureSwitch.ThrottlingFeature
import connectors.stubs._
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.{testBusinessName => _, _}
import helpers.WiremockHelper.verifyPost
import helpers._
import helpers.servicemocks._
import models._
import models.common.{AccountingYearModel, IncomeSourceModel}
import models.common.subscription.CreateIncomeSourcesModel
import models.sps.SPSPayload
import play.api.http.Status._
import play.api.libs.json.Json
import services.IndividualEndOfJourneyThrottle
import utilities.SubscriptionDataKeys._

class TaskListControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  override def beforeEach(): Unit = {
    disable(ThrottlingFeature)
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/business/task-list" should {
    "return OK" when {
      "there is no user data setup" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.task-list.title") + serviceNameGovUk)
        )
      }

      "there is partial user data setup" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel.copy(accountingMethod = None, confirmed = false)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel.copy(accountingMethod = None, confirmed = false)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))

        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.task-list.title") + serviceNameGovUk)
        )
      }

      "there is full user data setup" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))

        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

        When("GET /business/task-list is called")
        val res = IncomeTaxSubscriptionFrontend.getTaskList()

        Then("Should return OK with the task list page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.task-list.title") + serviceNameGovUk)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/task-list" when {
    "throttling is enabled and the user is throttled" should {
      s"redirect the user to ${controllers.individual.routes.ThrottlingController.end.url} without calling any sign up APIs" in {
        Given("I set the required feature switches")
        enable(ThrottlingFeature)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        ThrottlingStub.stubThrottle(IndividualEndOfJourneyThrottle.throttleId)(throttled = true)

        val testEntityId: String = "testEntityId"
        val res = IncomeTaxSubscriptionFrontend.submitTaskList(Map(SPSEntityId -> testEntityId))

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(endOfJourneyThrottleURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifyGet(BusinessesKey)(count = 0)
        IncomeTaxSubscriptionConnectorStub.verifyGet(BusinessAccountingMethod)(count = 0)
        IncomeTaxSubscriptionConnectorStub.verifyGet(Property)(count = 0)
        IncomeTaxSubscriptionConnectorStub.verifyGet(OverseasProperty)(count = 0)
        IncomeTaxSubscriptionConnectorStub.verifyGet(subscriptionId)(count = 0)

        MultipleIncomeSourcesSubscriptionAPIStub.verifyPostSignUpCount(testNino)(count = 0)
        MultipleIncomeSourcesSubscriptionAPIStub.verifyPostSubscriptionCount(testMtdId)(count = 0)

        ThrottlingStub.verifyThrottle(IndividualEndOfJourneyThrottle.throttleId)()
      }
    }

    "throttling is enabled and the user is not throttled" should {
      "allow the user to sign up successfully" in {
        Given("I set the required feature switches")
        enable(ThrottlingFeature)

        And("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        ThrottlingStub.stubThrottle(IndividualEndOfJourneyThrottle.throttleId)(throttled = false)

        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
          incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
          accountingMethod = Some(testAccountingMethod)
        ))

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessName, OK, Json.toJson(testBusinessName))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

        MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
        MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
          mtdbsa = testMtdId,
          request = CreateIncomeSourcesModel(
            nino = testNino,
            soleTraderBusinesses = Some(testSoleTraderBusinesses)
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
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(confirmationURI)
        )

        val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
        verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))

        ThrottlingStub.verifyThrottle(IndividualEndOfJourneyThrottle.throttleId)()
      }
    }

    "throttling is disabled" should {
      "the subscription successfully" when {
        "the income source is only self employment" should {
          "send the correct details to the backend, call sps with the users details and redirect to the confirmation page" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              accountingMethod = Some(testAccountingMethod)
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessName, OK, Json.toJson(testBusinessName))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
              mtdbsa = testMtdId,
              request = CreateIncomeSourcesModel(
                nino = testNino,
                soleTraderBusinesses = Some(testSoleTraderBusinesses)
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
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))

            ThrottlingStub.verifyThrottle(IndividualEndOfJourneyThrottle.throttleId)(count = 0)
          }
        }

        "the income source is only uk property" should {
          "send the correct details to the backend, call sps with the users details and redirect to the confirmation page" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessName, NO_CONTENT)
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
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
              mtdbsa = testMtdId,
              request = CreateIncomeSourcesModel(
                nino = testNino,
                soleTraderBusinesses = None,
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
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))

            ThrottlingStub.verifyThrottle(IndividualEndOfJourneyThrottle.throttleId)(count = 0)
          }
        }

        "the income source is only overseas property" should {
          "send the correct details to the backend, call sps with the users details and redirect to the confirmation page" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
              accountingMethod = None
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessName, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              OverseasProperty,
              OK,
              Json.toJson(testFullOverseasPropertyModel.copy(
                accountingMethod = Some(testOverseasProperty.accountingMethod),
                startDate = Some(testOverseasProperty.tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
              mtdbsa = testMtdId,
              request = CreateIncomeSourcesModel(
                nino = testNino,
                soleTraderBusinesses = None,
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
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))

            ThrottlingStub.verifyThrottle(IndividualEndOfJourneyThrottle.throttleId)(count = 0)
          }
        }

        "the income source contains self-employments, uk property and overseas property" should {
          "send the correct details to the backend, call sps with the users details and redirect to the confirmation page" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
              accountingMethod = Some(testAccountingMethod)
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessName, OK, Json.toJson(testBusinessName))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              Property,
              OK,
              Json.toJson(testFullPropertyModel.copy(
                accountingMethod = Some(testUkProperty.accountingMethod),
                startDate = Some(testUkProperty.tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              OverseasProperty,
              OK,
              Json.toJson(testFullOverseasPropertyModel.copy(
                accountingMethod = Some(testOverseasProperty.accountingMethod),
                startDate = Some(testOverseasProperty.tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
              mtdbsa = testMtdId,
              request = CreateIncomeSourcesModel(
                nino = testNino,
                soleTraderBusinesses = Some(testSoleTraderBusinesses),
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
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))

            ThrottlingStub.verifyThrottle(IndividualEndOfJourneyThrottle.throttleId)(count = 0)
          }
        }
      }

      "the subscription failed" should {
        "return a internalServer error and will not call sps" in {
          And("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
            accountingMethod = None
          ))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessName, OK, Json.toJson(testBusinessName))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(AccountingYearModel(Current, confirmed = true)))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscriptionForTaskList(
            mtdbsa = testMtdId,
            request = CreateIncomeSourcesModel(
              nino = testNino,
              soleTraderBusinesses = Some(testSoleTraderBusinesses)
            )
          )(INTERNAL_SERVER_ERROR)

          When("POST /business/task-list is called")
          val res = IncomeTaxSubscriptionFrontend.submitTaskList()

          Then("Should return a INTERNAL SERVER ERROR status")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )

          verifyPost("/channel-preferences/confirm", count = Some(0))

          ThrottlingStub.verifyThrottle(IndividualEndOfJourneyThrottle.throttleId)(count = 0)
        }
      }
    }
  }
}
