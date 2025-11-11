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

import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.SPSEntityId
import connectors.stubs.{CreateIncomeSourcesAPIStub, IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub, SignUpAPIStub}
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.WiremockHelper.verifyPost
import helpers._
import helpers.servicemocks.{AuthStub, ChannelPreferencesStub, TaxEnrolmentsStub}
import models._
import models.common.BusinessAccountingPeriod
import models.common.subscription.{CreateIncomeSourcesModel, SignUpRequestModel}
import models.sps.SPSPayload
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys._

class GlobalCheckYourAnswersControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  def testSignUpModel(taxYear: AccountingYear): SignUpRequestModel = SignUpRequestModel(
    nino = testNino,
    utr = testUtr,
    taxYear = taxYear
  )

  "GET /report-quarterly/income-and-expenses/sign-up/final-check-your-answers" should {
    "return OK" when {
      "all data was received and is complete" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(AccountingPeriod, OK, Json.toJson(BusinessAccountingPeriod.SixthAprilToFifthApril.key))
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason= None))
        ))

        val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"

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
                startDateBeforeLimit = Some(true),
                startDate = Some(testUkProperty().tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              OverseasProperty,
              OK,
              Json.toJson(testFullOverseasPropertyModel.copy(
                startDateBeforeLimit = Some(true),
                startDate = Some(testOverseasProperty().tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))
            SessionDataConnectorStub.stubGetAllSessionData(Map(
              ITSASessionKeys.NINO -> JsString(testNino),
              ITSASessionKeys.UTR -> JsString(testUtr),
              ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason= None))
            ))

            SignUpAPIStub.stubSignUp(testSignUpModel(Current))(OK, Json.obj("mtdbsa" -> testMtdId))
            CreateIncomeSourcesAPIStub.stubCreateIncomeSources(
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
          "sign up indicated the customer is already signed up" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              Property,
              OK,
              Json.toJson(testFullPropertyModel.copy(
                startDateBeforeLimit = Some(true),
                startDate = Some(testUkProperty().tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              OverseasProperty,
              OK,
              Json.toJson(testFullOverseasPropertyModel.copy(
                startDateBeforeLimit = Some(true),
                startDate = Some(testOverseasProperty().tradingStartDate)
              ))
            )
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrentConfirmed))
            SessionDataConnectorStub.stubGetAllSessionData(Map(
              ITSASessionKeys.NINO -> JsString(testNino),
              ITSASessionKeys.UTR -> JsString(testUtr),
              ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason= None))
            ))

            SignUpAPIStub.stubSignUp(testSignUpModel(Current))(UNPROCESSABLE_ENTITY)

            When("POST /final-check-your-answers is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers(Map(SPSEntityId -> testEntityId))

            Then("Should redirect to the confirmation page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.confirmationURI)
            )

            verifyPost("/channel-preferences/confirm", count = Some(0))
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
              ITSASessionKeys.NINO -> JsString(testNino),
              ITSASessionKeys.UTR -> JsString(testUtr),
              ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason= None))
            ))

            SignUpAPIStub.stubSignUp(testSignUpModel(Next))(OK, Json.obj("mtdbsa" -> testMtdId))
            CreateIncomeSourcesAPIStub.stubCreateIncomeSources(
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
          "sign up indicated the customer is already signed up" in {
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
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNextConfirmed))
            SessionDataConnectorStub.stubGetAllSessionData(Map(
              ITSASessionKeys.NINO -> JsString(testNino),
              ITSASessionKeys.UTR -> JsString(testUtr),
              ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason= None))
            ))

            SignUpAPIStub.stubSignUp(testSignUpModel(Next))(UNPROCESSABLE_ENTITY)

            When("POST /final-check-your-answers is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers(Map(SPSEntityId -> testEntityId))

            Then("Should redirect to the confirmation page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.confirmationURI)
            )

            verifyPost("/channel-preferences/confirm", count = Some(0))
          }
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

          SignUpAPIStub.stubSignUp(testSignUpModel(Current))(INTERNAL_SERVER_ERROR)

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

          SignUpAPIStub.stubSignUp(testSignUpModel(Current))(OK)
          CreateIncomeSourcesAPIStub.stubCreateIncomeSources(
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

          SignUpAPIStub.stubSignUp(testSignUpModel(Current))(OK)
          CreateIncomeSourcesAPIStub.stubCreateIncomeSources(
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

          SignUpAPIStub.stubSignUp(testSignUpModel(Current))(OK)
          CreateIncomeSourcesAPIStub.stubCreateIncomeSources(
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
