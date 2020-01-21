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

package agent.helpers

import java.util.UUID
import java.net.URLEncoder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Generator

object IntegrationTestConstants {
  lazy val testNino: String = _root_.helpers.IntegrationTestConstants.testNino
  lazy val testUtr: String = _root_.helpers.IntegrationTestConstants.testUtr
  lazy val testMTDID = _root_.helpers.IntegrationTestConstants.testMTDID
  lazy val startDate = _root_.helpers.IntegrationTestConstants.startDate
  lazy val endDate = _root_.helpers.IntegrationTestConstants.endDate
  lazy val ggServiceName = "HMRC-MTD-IT"
  val SessionId = s"stubbed-${UUID.randomUUID}"
  val userId = "/auth/oid/1234567890"
  val dateOfBirth = _root_.helpers.IntegrationTestConstants.dateOfBirth
  lazy val testARN = new Generator().nextAtedUtr.utr //Not a valid ARN, for test purposes only
  val testUserIdEncoded = URLEncoder.encode(userId, "UTF-8")

  val baseURI = "/report-quarterly/income-and-expenses/sign-up/client"
  val indexURI = s"$baseURI/index"
  val userDetailsURI = "/user-details"
  val confirmDetailsURI = "/confirm-details"
  val clientDetailsURI = s"$baseURI/client-details"
  val clientDetailsErrorURI = s"$baseURI/error/client-details"
  val agentLockedOutURI = s"$baseURI/error/lockout"
  val alreadySubscribedURI = s"$baseURI/error/client-already-subscribed"
  val registerForSAURI = s"$baseURI/register-for-SA"
  val confirmationURI = s"$baseURI/confirmation"
  val noClientRelationshipURI = s"$baseURI/error/no-client-relationship"
  val incomeSourceURI = s"$baseURI/income"
  val businessNameURI = s"$baseURI/business/name"
  val businessAccountingMethodURI = s"$baseURI/business/accounting-method"
  val propertyAccountingMethodURI = s"$baseURI/business/accounting-method-property"
  val errorMainIncomeURI = s"$baseURI/error/main-income"
  val accountingPeriodDatesURI = s"$baseURI/business/accounting-period-dates"
  val matchTaxYearURI = s"$baseURI/business/match-to-tax-year"
  val checkYourAnswersURI = s"$baseURI/check-your-answers"
  val feedbackSubmittedURI = s"$baseURI/feedback-submitted"
  val ineligibleURI = s"$baseURI/cannot-use-service-yet"
    //TODO see if this needs to be different for agents/individuals
  val ggSignOutURI = s"/gg/sign-out"
  val signOutURI = s"/report-quarterly/income-and-expenses/sign-up/logout"


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
