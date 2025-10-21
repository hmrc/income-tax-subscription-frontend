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

package helpers

import models._
import models.common.AccountingPeriodModel
import models.common.business._
import models.common.subscription.{OverseasProperty, SoleTraderBusinesses, UkProperty}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Generator
import utilities.AccountingPeriodUtil

import java.net.URLEncoder
import java.util.UUID
import scala.util.matching.Regex

object IntegrationTestConstants {

  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  lazy val testNino: String = new Generator().nextNino.nino
  lazy val testFormattedNino: String = testNino match {
    case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
      s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
    case other => other
  }
  lazy val staticTestNino = "AA111111A"
  lazy val testUtr: String = new Generator().nextAtedUtr.utr
  lazy val testUtrEnrolmentKey: String = s"IR-SA~UTR~$testUtr"
  lazy val testMtdId = "XE0001234567890"
  lazy val testMtdId2 = "XE0001234567892"
  lazy val testMTDIDEnrolmentKey: String = s"HMRC-MTD-IT~MTDITID~$testMtdId"
  lazy val testSubscriptionId = "sessionId"
  lazy val startDate: DateModel = DateModel("05", "04", "2017")
  lazy val endDate: DateModel = DateModel("04", "04", "2018")
  lazy val ggServiceName = "HMRC-MTD-IT"

  val SessionId = s"stubbed-${UUID.randomUUID}"
  val userId = s"/auth/oid/1234567890"
  lazy val testARN: String = new Generator().nextAtedUtr.utr //Not a valid ARN, for test purposes only
  val testUserIdEncoded: String = URLEncoder.encode(userId, "UTF-8")

  lazy val testFirstName: String = Math.random().toString
  lazy val testLastName: String = Math.random().toString
  lazy val testFullName: String = testFirstName + " " + testLastName
  val dateOfBirth: DateModel = DateModel("01", "01", "1980")
  val testId = "12345"
  val testGroupId: String = UUID.randomUUID.toString
  val testCredId: String = UUID.randomUUID.toString
  val testCredentialId: String = UUID.randomUUID().toString
  val testCredentialId2: String = UUID.randomUUID().toString
  val testCredentialId3: String = UUID.randomUUID().toString
  private val businessStartDate = BusinessStartDate(DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit))
  private val tradingStartDate = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)
  val testBusinessName: BusinessNameModel = BusinessNameModel("test business")
  private val testBusinessTradeName = BusinessTradeNameModel("test trade")
  private val testStartDate = AccountingPeriodUtil.getCurrentTaxYear.startDate
  private val testEndDate = AccountingPeriodUtil.getCurrentTaxYear.endDate

  val testAccountingPeriod: AccountingPeriodModel = testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  def testSoleTraderBusinesses(accountingYear: AccountingYear = Current): SoleTraderBusinesses = SoleTraderBusinesses(
    if (accountingYear == Current) AccountingPeriodUtil.getCurrentTaxYear else AccountingPeriodUtil.getNextTaxYear,
    testSelfEmploymentData
  )

  def testUkProperty(accountingYear: AccountingYear = Current): UkProperty = UkProperty(
    startDateBeforeLimit = None,
    if (accountingYear == Current) AccountingPeriodUtil.getCurrentTaxYear else AccountingPeriodUtil.getNextTaxYear,
    tradingStartDate
  )

  def testOverseasProperty(accountingYear: AccountingYear = Current): OverseasProperty = OverseasProperty(
    startDateBeforeLimit = None,
    if (accountingYear == Current) AccountingPeriodUtil.getCurrentTaxYear else AccountingPeriodUtil.getNextTaxYear,
    tradingStartDate
  )

  lazy val testSelfEmploymentData: Seq[SelfEmploymentData] =
    Seq(SelfEmploymentData
    (
      id = testId,
      businessStartDate = Some(businessStartDate),
      businessName = Some(testBusinessName),
      businessTradeName = Some(testBusinessTradeName),
      businessAddress = Some(BusinessAddressModel(Address(Seq("1 long road", "lonely town", "quiet county"), Some("ZZ1 1ZZ"))))
    )
    )

  object IndividualURI {
    val baseURI = "/report-quarterly/income-and-expenses/sign-up"
    val notEligibleURI = "/cannot-use-service-yet"
    val noSaURI = s"$baseURI/register-for-SA"
    val confirmationURI = s"$baseURI/confirmation"
    val globalCheckYourAnswersURI = s"$baseURI/final-check-your-answers"
    val cannotSignUpURI = s"$baseURI/error/cannot-sign-up"
    val cannotSignUpForCurrentYearURI = s"$baseURI/error/cannot-sign-up-for-current-year"
    val whatYouNeedToDoURI = s"$baseURI/what-you-need-to-do"
    val usingSoftwareURI = s"$baseURI/using-software"
    val spsHandoffRouteURI = s"$baseURI/sps-handoff"
    val claimEnrolSpsHandoffRouteURI = s"$baseURI/claim-enrolment/sps-handoff"
    val spsHandoffURI = s"/paperless/choose/capture?returnUrl=DO8MisXKpizAWqbqizwb%2FJa9%2BNCLHHqgAm55zTvph%2FNMwk%2F2vsApxzF%2FJsaw9jIyrHFfSwQrP%2BqQcQU90FfT%2BDcR9uIsDgZ5Bi3z4iYCJe0%3D&returnLinkText=lYCIdN%2BV3wGYJ1SSm%2BPhNA%3D%3D&regime=KucfrgeglpOjHad59vo1xg%3D%3D"
    val claimEnrolSpsHandoffURI = s"/paperless/choose/capture?returnUrl=DO8MisXKpizAWqbqizwb%2FJa9%2BNCLHHqgAm55zTvph%2FNMwk%2F2vsApxzF%2FJsaw9jIyrHFfSwQrP%2BqQcQU90FfT%2BFw14Es%2Fzqc6h9U3UpZg18WfhJXb4iUz3Y5ttgFaoTjs&returnLinkText=lYCIdN%2BV3wGYJ1SSm%2BPhNA%3D%3D&regime=KucfrgeglpOjHad59vo1xg%3D%3D"
    val accountingYearURI = s"$baseURI/business/what-year-to-sign-up"
    val checkYourAnswersURI = s"$baseURI/check-your-answers"
    val ukPropertyCYAURI = s"$baseURI/business/uk-property-check-your-answers"
    val overseasPropertyCYAURI = s"$baseURI/business/overseas-property-check-your-answers"
    val saveAndRetrieveURI = s"$baseURI/business/progress-saved"
    val feedbackSubmittedURI = s"$baseURI/feedback-submitted"
    val signOutURI = s"$baseURI/logout"
    val ggSignOutURI = s"/bas-gateway/sign-out-without-state"
    val addMTDITOverviewURI = s"$baseURI/claim-enrolment/overview"
    val notSubscribedURI = s"$baseURI/claim-enrolment/not-subscribed"
    val claimEnrolmentConfirmationURI = s"$baseURI/claim-enrolment/confirmation"
    val claimEnrolmentAlreadySignedUpURI = s"$baseURI/claim-enrolment/already-signed-up"
    val claimEnrolmentResolverURI = s"$baseURI/claim-enrolment/resolve"
    val startOfJourneyThrottleURI = s"$baseURI/throttle-start"
    val endOfJourneyThrottleURI = s"$baseURI/throttle-end"
    val yourIncomeSourcesURI = s"$baseURI/details/your-income-source"
    val youCanSignUpNow = s"$baseURI/you-can-sign-up-now"
    val signingUp = s"$baseURI/eligibility/signing-up"
  }

  object AgentURI {
    val baseURI = "/report-quarterly/income-and-expenses/sign-up/client"
    val baseSEURI = "http://localhost:9563/report-quarterly/income-and-expenses/sign-up/self-employments/client"
    val indexURI = s"$baseURI/index"
    val confirmDetailsURI = "/confirm-details"
    val whatYouNeedToDoURI = s"$baseURI/what-you-need-to-do"
    val clientDetailsURI = s"$baseURI/client-details"
    val clientDetailsErrorURI = s"$baseURI/error/client-details"
    val lockedOutURI = s"$baseURI/error/lockout"
    val alreadySubscribedURI = s"$baseURI/error/client-already-subscribed"
    val registerForSAURI = s"$baseURI/register-for-SA"
    val confirmationURI = s"$baseURI/confirmation"
    val globalCheckYourAnswersURI = s"$baseURI/final-check-your-answers"
    val clientRelationshipURI = s"$baseURI/error/no-client-relationship"
    val incomeSourceURI = s"$baseURI/income"
    val incomeSourcesEligibilityURI = s"$baseURI/eligibility/income-sources"
    val businessNameSEURI = s"$baseSEURI/details"
    val propertyStartDateURI = s"$baseURI/business/property-commencement-date"
    val overseasPropertyStartDateURI = s"$baseURI/business/overseas-commencement-date"
    val errorMainIncomeURI = s"$baseURI/error/main-income"
    val checkYourAnswersURI = s"$baseURI/check-your-answers"
    val ukPropertyCheckYourAnswersURI = s"$baseURI/business/uk-property-check-your-answers"
    val overseasPropertyCheckYourAnswersURI = s"$baseURI/business/overseas-property-check-your-answers"
    val feedbackSubmittedURI = s"$baseURI/feedback-submitted"
    val signOutURI = s"/report-quarterly/income-and-expenses/sign-up/logout"
    val whatYearToSignUpURI = s"$baseURI/business/what-year-to-sign-up"
    val yourIncomeSourcesURI = s"$baseURI/your-income-source"
    val addAnotherClient = s"$baseURI/add-another"
    val youCanSignUpNow = s"$baseURI/you-can-sign-up-now"
    val signingUp = "/report-quarterly/income-and-expenses/sign-up/eligibility/client/signing-up"
  }

  def basGatewaySignIn(continueTo: String): String = {
    val updatedContinue: String = continueTo.replace("/", "%2F")
    s"http://localhost:9553/bas-gateway/sign-in?continue_url=%2Freport-quarterly%2Fincome-and-expenses%2Fsign-up$updatedContinue&origin=income-tax-subscription-frontend"
  }

  val wrongAffinityURI = s"${IndividualURI.baseURI}/error/affinity-group"
  val ivURI = s"${IndividualURI.baseURI}/iv"
  val userLockedOutURI = s"${IndividualURI.baseURI}/error/lockout"
  val userDetailsErrorURI = s"${IndividualURI.baseURI}/error/user-details"

  object Auth {

    def authResponseJson(uri: String, userDetailsLink: String, gatewayId: String, idsLink: String): JsValue = Json.parse(
      s"""
         |{
         |  "uri":"$uri",
         |  "userDetailsLink":"$userDetailsLink",
         |  "credentials" : {
         |    "gatewayId":"$gatewayId"
         |  },
         |  "ids":"$idsLink"
         |}
     """.stripMargin
    )

    def idsResponseJson(internalId: String, externalId: String): JsValue = Json.parse(
      s"""{
           "internalId":"$internalId",
           "externalId":"$externalId"
        }""")
  }


}
