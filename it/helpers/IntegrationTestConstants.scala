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

import java.net.URLEncoder
import java.util.UUID

import core.models.DateModel
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Generator

object IntegrationTestConstants {
  lazy val testNino: String = new Generator().nextNino.nino
  lazy val staticTestNino = "AA111111A"
  lazy val testUtr: String = new Generator().nextAtedUtr.utr
  lazy val testMTDID = "XE0001234567890"
  lazy val startDate = DateModel("05", "04", "2017")
  lazy val endDate = DateModel("04", "04", "2018")
  lazy val ggServiceName = "HMRC-MTD-IT"

  val SessionId = s"stubbed-${UUID.randomUUID}"
  val userId = s"/auth/oid/1234567890"
  val testUserIdEncoded = URLEncoder.encode(userId, "UTF-8")
  val testFirstName = "Test"
  val testLastName = "Name"
  val dateOfBirth = DateModel("01", "01", "1980")
  val testPaperlessPreferenceToken = s"${UUID.randomUUID()}"
  val testId = "12345"
  val testGroupId = UUID.randomUUID.toString
  val testCredId = UUID.randomUUID.toString
  val testArn = UUID.randomUUID.toString
  val testAgencyName = UUID.randomUUID.toString


  val testUrl = "/test/url/"

  val baseURI = "/report-quarterly/income-and-expenses/sign-up"
  val indexURI = s"$baseURI/index"
  val notEligibleURI = "/cannot-use-service-yet"
  val noSaURI = s"$baseURI/register-for-SA"
  val userDetailsURI = "/user-details"
  val confirmDetailsURI = "/confirm-details"
  val confirmationURI = s"$baseURI/confirmation"
  val incomeSourceURI = s"$baseURI/income"
  val rentUkPropertyURI = s"$baseURI/rent-uk-property"
  val areYouSelfEmployedURI = s"$baseURI/are-you-self-employed"
  val cannotSignUpURI = s"$baseURI/error/cannot-sign-up"
  val otherIncomeURI = s"$baseURI/income-other"
  val cannotReportYetURI = s"$baseURI/error/cannot-report-yet"
  val businessNameURI = s"$baseURI/business/name"
  val businessAddressURI = s"$baseURI/business/address"
  val businessAddressInitURI = s"$baseURI/business/address/init"
  val businessStartDateURI = s"$baseURI/business/start-date"
  val businessAccountingMethodURI = s"$baseURI/business/accounting-method"
  val errorMainIncomeURI = s"$baseURI/error/main-income"
  val errorOtherIncomeURI = s"$baseURI/other-income-in-final-report"
  val preferencesURI = s"$baseURI/preferences"
  val choosePaperlessURI = s"/paperless/choose?returnUrl"
  val errorPreferencesURI = s"$baseURI/paperless-error"
  val matchTaxYearURI = s"$baseURI/business/match-to-tax-year"
  val accountingYearURI = s"$baseURI/business/what-year-to-sign-up"
  val accountingPeriodDatesURI = s"$baseURI/business/accounting-period-dates"
  val termsURI = s"$baseURI/terms"
  val checkYourAnswersURI = s"$baseURI/check-your-answers"
  val feedbackSubmittedURI = s"$baseURI/feedback-submitted"
  val ggSignInURI = "/gg/sign-in"
  val signOutURI = s"$baseURI/logout"
  val ggSignOutURI = s"/gg/sign-out"
  val claimSubscriptionURI = s"$baseURI/claim-subscription"
  val wrongAffinityURI = s"$baseURI/error/affinity-group"
  val ivURI = s"$baseURI/iv"
  val userLockedOutURI = s"$baseURI/error/lockout"
  val userDetailsErrorURI = s"$baseURI/error/user-details"
  val authoriseAgentUri = s"$baseURI/authorise-agent"
  val agentNotAuthorisedUri = s"$baseURI/agent-not-authorised"
  val subscribeUnauthorisedUri = s"$baseURI/subscribe-unauthorised"

  object Auth {
    def idsResponseJson(internalId: String, externalId: String): JsValue = Json.parse(
      s"""{
           "internalId":"$internalId",
           "externalId":"$externalId"
        }""")
  }

}
