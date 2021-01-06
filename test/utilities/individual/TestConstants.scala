/*
 * Copyright 2021 HM Revenue & Customs
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

package utilities.individual

import java.net.URLEncoder
import java.time.OffsetDateTime
import java.util.UUID

import models.common.{AccountingYearModel, IncomeSourceModel}
import models.common.business._
import models.common.subscription.{BusinessIncomeModel, CreateIncomeSourcesFailureResponse, CreateIncomeSourcesSuccess, EmacEnrolmentRequest, EnrolFailure, EnrolRequest, EnrolSuccess, EnrolmentKey, KnownFactsFailure, KnownFactsRequest, KnownFactsSuccess, PropertyIncomeModel, RefreshProfileFailure, RefreshProfileSuccess, SignUpIncomeSourcesFailureResponse, SignUpIncomeSourcesSuccess, SubscriptionFailureResponse, SubscriptionRequest, SubscriptionSuccess, TypeValuePair}
import models.individual.subscription._
import models.usermatching.{LockedOut, UserMatchFailureResponseModel, UserMatchSuccessResponseModel}
import models.{Cash, Current, DateModel, IndividualSummary}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.domain.Generator
import utilities.AccountingPeriodUtil
import utilities.TestModels.testBusinessName
import utilities.individual.Constants.GovernmentGateway.{MTDITID, NINO, ggFriendlyName, ggPortalId}
import utilities.individual.Constants.mtdItsaEnrolmentName

object TestConstants {
  /*
  * this nino is a constant, if you need a fresh one use TestModels.newNino
  */
  lazy val testNino: String = new Generator().nextNino.nino
  lazy val testUtr: String = UUID.randomUUID().toString
  //Not an actual UTRTestAuthenticatorConnector
  lazy val testArn: String = UUID.randomUUID().toString
  lazy val testToken: String = s"${UUID.randomUUID()}"
  lazy val testMTDID = "XE0001234567890"
  lazy val testMTDID2 = "XE0001234567892"
  lazy val testSubscriptionId = "sessionId"
  lazy val startDate = DateModel("05", "04", "2017")
  lazy val endDate = DateModel("04", "04", "2018")
  lazy val ggServiceName = "HMRC-MTD-IT"
  lazy val testLockoutResponse = LockedOut(testNino, OffsetDateTime.now())
  lazy val testAgencyName = UUID.randomUUID().toString

  val testUserId = "/auth/oid/1234567"
  val escapedUserId = URLEncoder.encode(testUserId, "UTF-8")

  val testFirstName = "Test"
  val testLastName = "Name"

  val testPhoneNumber = "000 000 0000"

  val testUrl = "/test/url/"

  lazy val knownFactsRequest = KnownFactsRequest(
    List(
      TypeValuePair(MTDITID, testMTDID),
      TypeValuePair(NINO, testNino)
    )
  )

  val testEnrolRequest = EnrolRequest(
    portalId = ggPortalId,
    serviceName = ggServiceName,
    friendlyName = ggFriendlyName,
    knownFacts = List(testMTDID, testNino)
  )

  val testSubmissionRequest = SubscriptionRequest(
    nino = TestConstants.testNino,
    arn = None,
    businessIncome = Some(BusinessIncomeModel(Some(testBusinessName.businessName), AccountingPeriodUtil.getCurrentTaxYear, Cash)),
    propertyIncome = Some(PropertyIncomeModel(Some(Cash)))
  )

  val testCredentialId: String = UUID.randomUUID().toString
  val testCredentialId2: String = UUID.randomUUID().toString

  val testErrorMessage = "This is an error"
  val testException = new Exception

  val testRefreshProfileSuccess = Right(RefreshProfileSuccess)

  val testRefreshProfileFailure = Left(RefreshProfileFailure)

  val testEnrolSuccess = Right(EnrolSuccess)

  val testEnrolFailure = Left(EnrolFailure(testErrorMessage))

  val testSubscriptionSuccess = Right(SubscriptionSuccess(testMTDID))

  val testSubscriptionFailure = Left(SubscriptionFailureResponse(INTERNAL_SERVER_ERROR))

  val testKnownFactsSuccess = Right(KnownFactsSuccess)

  val testKnownFactsFailure = Left(KnownFactsFailure(testErrorMessage))

  val testMatchSuccess = Right(UserMatchSuccessResponseModel)

  val testMatchFailure = Left(UserMatchFailureResponseModel)

  val testGroupId = UUID.randomUUID().toString

  val testCredId = UUID.randomUUID().toString

  val testEnrolmentKey = EnrolmentKey(mtdItsaEnrolmentName, MTDITID -> testMTDID)

  val testEnrolmentRequest = EmacEnrolmentRequest(testCredId, testNino)

  val testSignUpIncomeSourcesSuccess = Right(SignUpIncomeSourcesSuccess(testMTDID))

  val testSignUpIncomeSourcesFailure = Left(SignUpIncomeSourcesFailureResponse(INTERNAL_SERVER_ERROR))

  val testCreateIncomeSourcesSuccess = Right(CreateIncomeSourcesSuccess())

  val testCreateIncomeSourcesFailure = Left(CreateIncomeSourcesFailureResponse(INTERNAL_SERVER_ERROR))

  val testIndividualSummary: IndividualSummary = IndividualSummary(
    incomeSource = Some(IncomeSourceModel(true, false, false)),
    selfEmployments = Some(Seq(SelfEmploymentData("1", Some(BusinessStartDate(startDate)), Some(testBusinessName),
      Some(BusinessTradeNameModel("plumbing")), Some(BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), "TF2 1PF")))))),
    accountingMethod = Some(AccountingMethodModel(Cash)),
    selectedTaxYear = Some(AccountingYearModel(Current))
  )

}
