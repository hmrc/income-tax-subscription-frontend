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
import config.featureswitch.FeatureSwitch.UseIdempotency
import connectors.stubs.CreateIncomeSourcesAPIStub.StubResponse
import connectors.stubs.SessionDataConnectorStub.{sessionDataUri, stubGetAllSessionData, stubSaveSubmissionStatus}
import connectors.stubs.{CreateIncomeSourcesAPIStub, IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub, SignUpAPIStub}
import helpers.*
import helpers.IntegrationTestConstants.*
import helpers.IntegrationTestModels.*
import helpers.WiremockHelper.verifyPost
import helpers.servicemocks.{AuthStub, ChannelPreferencesStub, TaxEnrolmentsStub}
import models.*
import models.SubmissionStatus.{handledError, otherError, success}
import models.common.BusinessAccountingPeriod
import models.common.subscription.{CreateIncomeSourcesModel, SignUpRequestModel}
import models.sps.SPSPayload
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import play.api.http.Status.*
import play.api.libs.json.{JsString, Json}
import services.individual.SignUpOrchestrationService.{ALREADY_SIGNED_UP, BUSINESS_PARTNER_CATEGORY_ORGANISATION, ID_NOT_FOUND, MULTIPLE_BUSINESS_PARTNERS_FOUND}
import utilities.SubscriptionDataKeys.*

class GlobalCheckYourAnswersControllerISpec extends ComponentSpecBase with SubmissionStatusHelper with SessionCookieCrumbler {

  override def beforeEach(): Unit = {
    super.beforeEach()
    stubSaveSubmissionStatus()(OK)
    SessionDataConnectorStub.stubGetAllSessionData(
      Map(
        ITSASessionKeys.NINO -> JsString(testNino),
        ITSASessionKeys.UTR -> JsString(testUtr),
        ITSASessionKeys.MANDATION_STATUS ->
          Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
        ITSASessionKeys.ELIGIBILITY_STATUS ->
          Json.toJson(
            EligibilityStatus(
              eligibleCurrentYear = true,
              eligibleNextYear = true,
              exemptionReason = None
            )
          )
      )
    )
  }

  override def afterEach(): Unit = {
    disable(UseIdempotency)
    super.afterEach()
  }

  def testSignUpModel(taxYear: AccountingYear, withIdempotencyKey: Boolean = false): SignUpRequestModel = SignUpRequestModel(
    nino = testNino,
    utr = testUtr,
    taxYear = taxYear,
    idempotencyKey = if (withIdempotencyKey) Some("test-uuid") else None
  )

  "GET /report-quarterly/income-and-expenses/sign-up/final-check-your-answers" should {
    "return SEE_OTHER to the login page" when {
      "the user is not logged in" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.getGlobalCheckYourAnswers()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn())
        )
      }
    }
    "return OK" when {
      "all data was received and is complete" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(AccountingPeriod, OK, Json.toJson(BusinessAccountingPeriod.SixthAprilToFifthApril.key))
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
        SessionDataConnectorStub.stubGetAllSessionData(
          Map(
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr),
            ITSASessionKeys.MANDATION_STATUS ->
              Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
            ITSASessionKeys.ELIGIBILITY_STATUS ->
              Json.toJson(
                EligibilityStatus(
                  eligibleCurrentYear = true,
                  eligibleNextYear = true,
                  exemptionReason = None
                )
              )
          )
        )

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

  "POST /report-quarterly/income-and-expenses/sign-up/final-check-your-answers" should {
    "return SEE_OTHER to the login page" when {
      "user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn())
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
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
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
              redirectURI(IndividualURI.spinnyWheelURI)
            )

            waitUntilStatusIs(success)

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))
            verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(success).toString), Some(1))
          }
          "sign up initially returns retryable 422 code 003 and then succeeds when UseIdempotency is enabled" in {
            Given("I setup the Wiremock stubs")
            enable(UseIdempotency)

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
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
            ))

            SignUpAPIStub.stubIdempotencyRetryNewKeyScenario(
              scenarioName = "global-cya-idempotency-retry-success",
              firstAttemptKey = "test-uuid",
              secondAttemptKey = "test-uuid",
              retryableCode = "003",
              successBody = Json.obj("mtdbsa" -> testMtdId)
            )

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
                overseasProperty = Some(testOverseasProperty().copy(tradingStartDate = DateModel.dateConvert(testOverseasProperty().tradingStartDate.toLocalDate), startDateBeforeLimit = Some(true))),
                idempotencyKey = Some("test-uuid")
              )
            )(NO_CONTENT)

            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, CREATED)

            ChannelPreferencesStub.stubChannelPreferenceConfirm()

            When("POST /final-check-your-answers is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers(Map(SPSEntityId -> testEntityId))

            Then("Should still redirect to the confirmation flow")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.spinnyWheelURI)
            )

            waitUntilStatusIs(success)

            SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 2, idempotencyKey = "test-uuid")
            verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(success).toString), Some(1))
          }
          "create income sources initially returns a retry status amd then succeeds" in {
            Given("I setup the Wiremock stubs")
            enable(UseIdempotency)

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
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
            ))

            SignUpAPIStub.stubSignUp(testSignUpModel(Current, true))(OK, Json.obj("mtdbsa" -> testMtdId))
            CreateIncomeSourcesAPIStub.stubCreateIncomeSources(testMtdId, CreateIncomeSourcesModel(
              nino = testNino,
              soleTraderBusinesses = Some(testSoleTraderBusinesses().copy(
                businesses = testSoleTraderBusinesses().businesses.map(business =>
                  business.copy(confirmed = true, businessStartDate = business.businessStartDate.map(date => date.copy(startDate = DateModel.dateConvert(date.startDate.toLocalDate))), startDateBeforeLimit = Some(true))
                )
              )),
              ukProperty = Some(testUkProperty().copy(tradingStartDate = DateModel.dateConvert(testUkProperty().tradingStartDate.toLocalDate), startDateBeforeLimit = Some(true))),
              overseasProperty = Some(testOverseasProperty().copy(tradingStartDate = DateModel.dateConvert(testOverseasProperty().tradingStartDate.toLocalDate), startDateBeforeLimit = Some(true))),
              idempotencyKey = Some("test-uuid")
            ))(
              responses = Seq(
                StubResponse(GATEWAY_TIMEOUT),
                StubResponse(NO_CONTENT)
              )
            )

            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, CREATED)

            ChannelPreferencesStub.stubChannelPreferenceConfirm()

            When("POST /final-check-your-answers is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers(Map(SPSEntityId -> testEntityId))

            Then("Should redirect to the confirmation page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.spinnyWheelURI)
            )

            waitUntilStatusIs(success)

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))
            verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(success).toString), Some(1))
          }
          "sign up and create income sources fail with a retry status on the first attempt and then succeed" in {
            Given("I setup the Wiremock stubs")
            enable(UseIdempotency)

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
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
            ))

            SignUpAPIStub.stubIdempotencyRetryNewKeyScenario(
              scenarioName = "global-cya-idempotency-retry-success",
              firstAttemptKey = "test-uuid",
              secondAttemptKey = "test-uuid",
              retryableCode = "003",
              successBody = Json.obj("mtdbsa" -> testMtdId)
            )

            CreateIncomeSourcesAPIStub.stubCreateIncomeSources(testMtdId, CreateIncomeSourcesModel(
              nino = testNino,
              soleTraderBusinesses = Some(testSoleTraderBusinesses().copy(
                businesses = testSoleTraderBusinesses().businesses.map(business =>
                  business.copy(confirmed = true, businessStartDate = business.businessStartDate.map(date => date.copy(startDate = DateModel.dateConvert(date.startDate.toLocalDate))), startDateBeforeLimit = Some(true))
                )
              )),
              ukProperty = Some(testUkProperty().copy(tradingStartDate = DateModel.dateConvert(testUkProperty().tradingStartDate.toLocalDate), startDateBeforeLimit = Some(true))),
              overseasProperty = Some(testOverseasProperty().copy(tradingStartDate = DateModel.dateConvert(testOverseasProperty().tradingStartDate.toLocalDate), startDateBeforeLimit = Some(true))),
              idempotencyKey = Some("test-uuid")
            ))(
              responses = Seq(
                StubResponse(GATEWAY_TIMEOUT),
                StubResponse(NO_CONTENT)
              )
            )

            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, CREATED)

            ChannelPreferencesStub.stubChannelPreferenceConfirm()

            When("POST /final-check-your-answers is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers(Map(SPSEntityId -> testEntityId))

            Then("Should still redirect to the confirmation flow")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.spinnyWheelURI)
            )

            waitUntilStatusIs(success)

            SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 2, idempotencyKey = "test-uuid")
            verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(success).toString), Some(1))
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
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
            ))

            SignUpAPIStub.stubSignUp(testSignUpModel(Current))(
              status = UNPROCESSABLE_ENTITY,
              json = Json.obj("code" -> ALREADY_SIGNED_UP, "reason" -> "Customer already signed up")
            )

            When("POST /final-check-your-answers is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers(Map(SPSEntityId -> testEntityId))

            Then("Should redirect to the confirmation page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.spinnyWheelURI)
            )

            waitUntilStatusIs(success)

            verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(success).toString), Some(1))
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
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
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
              redirectURI(IndividualURI.spinnyWheelURI)
            )

            waitUntilStatusIs(success)

            val expectedSPSBody: SPSPayload = SPSPayload(testEntityId, s"HMRC-MTD-IT~MTDITID~$testMtdId")
            verifyPost("/channel-preferences/confirm", Some(Json.toJson(expectedSPSBody).toString), Some(1))
            verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(success).toString), Some(1))
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
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
            ))

            SignUpAPIStub.stubSignUp(testSignUpModel(Next))(
              status = UNPROCESSABLE_ENTITY,
              json = Json.obj("code" -> ALREADY_SIGNED_UP, "reason" -> "Customer already signed up")
            )

            When("POST /final-check-your-answers is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers(Map(SPSEntityId -> testEntityId))

            Then("Should redirect to the confirmation page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.spinnyWheelURI)
            )

            waitUntilStatusIs(success)

            verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(success).toString), Some(1))
          }
        }
      }
      "redirect to the contact hmrc page" when {
        "signing up for the current tax year" when {
          s"a unprocessable sign up occurs with a code: $ID_NOT_FOUND" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNextConfirmed))
            SessionDataConnectorStub.stubGetAllSessionData(Map(
              ITSASessionKeys.NINO -> JsString(testNino),
              ITSASessionKeys.UTR -> JsString(testUtr),
              ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
            ))

            SignUpAPIStub.stubSignUp(testSignUpModel(Next))(
              status = UNPROCESSABLE_ENTITY,
              json = Json.obj("code" -> ID_NOT_FOUND, "reason" -> "ID not found")
            )

            When("POST /final-check-your-answers is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers(Map(SPSEntityId -> testEntityId))

            Then("Should redirect to the contact hmrc page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.spinnyWheelURI)
            )

            waitUntilStatusIs(handledError)

            verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(handledError).toString), Some(1))
          }
          s"a unprocessable sign up occurs with a code: $BUSINESS_PARTNER_CATEGORY_ORGANISATION" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNextConfirmed))
            SessionDataConnectorStub.stubGetAllSessionData(Map(
              ITSASessionKeys.NINO -> JsString(testNino),
              ITSASessionKeys.UTR -> JsString(testUtr),
              ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
            ))

            SignUpAPIStub.stubSignUp(testSignUpModel(Next))(
              status = UNPROCESSABLE_ENTITY,
              json = Json.obj("code" -> BUSINESS_PARTNER_CATEGORY_ORGANISATION, "reason" -> "ID not found")
            )

            When("POST /final-check-your-answers is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers(Map(SPSEntityId -> testEntityId))

            Then("Should redirect to the contact hmrc page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.spinnyWheelURI)
            )

            waitUntilStatusIs(handledError)

            verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(handledError).toString), Some(1))
          }
          s"a unprocessable sign up occurs with a code: $MULTIPLE_BUSINESS_PARTNERS_FOUND" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearNextConfirmed))
            SessionDataConnectorStub.stubGetAllSessionData(Map(
              ITSASessionKeys.NINO -> JsString(testNino),
              ITSASessionKeys.UTR -> JsString(testUtr),
              ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
              ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
            ))

            SignUpAPIStub.stubSignUp(testSignUpModel(Next))(
              status = UNPROCESSABLE_ENTITY,
              json = Json.obj("code" -> MULTIPLE_BUSINESS_PARTNERS_FOUND, "reason" -> "ID not found")
            )

            When("POST /final-check-your-answers is called")
            val testEntityId: String = "testEntityId"
            val res = IncomeTaxSubscriptionFrontend.submitGlobalCheckYourAnswers(Map(SPSEntityId -> testEntityId))

            Then("Should redirect to the contact hmrc page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.spinnyWheelURI)
            )

            waitUntilStatusIs(handledError)

            verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(handledError).toString), Some(1))
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
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.spinnyWheelURI)
          )

          waitUntilStatusIs(otherError)

          verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(otherError).toString), Some(1))
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
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.spinnyWheelURI)
          )

          waitUntilStatusIs(otherError)

          verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(otherError).toString), Some(1))
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
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.spinnyWheelURI)
          )

          waitUntilStatusIs(otherError)

          verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(otherError).toString), Some(1))
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
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.spinnyWheelURI)
          )

          waitUntilStatusIs(otherError)

          verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(otherError).toString), Some(1))
        }
      }
    }
  }

}
