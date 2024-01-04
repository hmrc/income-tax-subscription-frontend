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

package controllers.individual

import common.Constants.ITSASessionKeys.SPSEntityId
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, MultipleIncomeSourcesSubscriptionAPIStub}
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.{testAccountingMethod, testAccountingYearCurrent, testAccountingYearCurrentConfirmed, testAccountingYearNextConfirmed, testBusinesses, testFullOverseasPropertyModel, testFullPropertyModel, testMTDITEnrolmentKey}
import helpers.WiremockHelper.verifyPost
import helpers._
import helpers.servicemocks.{AuthStub, ChannelPreferencesStub, TaxEnrolmentsStub}
import models.common.subscription.CreateIncomeSourcesModel
import models.sps.SPSPayload
import models.{DateModel, Next}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys._

class GlobalCheckYourAnswersControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/final-check-your-answers" should {
    "return SEE_OTHER to the task list page" when {
      "there is missing data from the users subscription" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

        When("GET /final-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getGlobalCheckYourAnswers()

        Then("Should redirect to the task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.taskListURI)
        )
      }
    }
    "return OK" when {
      "all data was received and is complete" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))

        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

        When("GET /final-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getGlobalCheckYourAnswers()

        Then("Should return OK with the global check your answers page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("individual.global-check-your-answers.heading") + serviceNameGovUk)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/final-check-your-answers" when {
    "there is missing data from the users subscription" should {
      "return SEE_OTHER to the task list page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

        When("POST /final-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers()

        Then("Should redirect to the task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.taskListURI)
        )
      }
    }
    "there is complete data" should {
      "sign up and redirect to the confirmation page" when {
        "signing up for the current tax year" when {
          "all calls were successful" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
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

            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, CREATED)

            ChannelPreferencesStub.stubChannelPreferenceConfirm()

            When("POST /final-check-your-answers is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers(Map(SPSEntityId -> testEntityId))

            Then("Should redirect to the confirmation page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.confirmationURI)
            )

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))
          }
        }
        "signing up for the next tax year" when {
          "all calls were successful" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
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

            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, CREATED)

            ChannelPreferencesStub.stubChannelPreferenceConfirm()

            When("POST /final-check-your-answers is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers(Map(SPSEntityId -> testEntityId))

            Then("Should redirect to the confirmation page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.confirmationURI)
            )

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))
          }
        }
      }
      "return INTERNAL SERVER ERROR" when {
        "sign up failed" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
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

          When("POST /final-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers()

          Then("Should show the internal service error page")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "create income sources failed" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
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

          When("POST /final-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers()

          Then("Should show the internal service error page")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "add known facts failed" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
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

          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, INTERNAL_SERVER_ERROR)

          When("POST /final-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers()

          Then("Show an internal server error")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "enrolment failed" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
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

          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
          TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, INTERNAL_SERVER_ERROR)

          When("POST /final-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers()

          Then("Show an internal server error")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }

}
