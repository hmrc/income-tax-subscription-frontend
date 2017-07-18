/*
 * Copyright 2017 HM Revenue & Customs
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

import java.util.UUID

import models.DateModel
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Generator

object IntegrationTestConstants {
  lazy val testNino: String = new Generator().nextNino.nino
  lazy val testMTDID = "XE0001234567890"
  lazy val startDate = DateModel("05", "04", "2017")
  lazy val endDate = DateModel("04", "04", "2018")
  lazy val ggServiceName = "HMRC-MTD-IT"
  val SessionId = s"stubbed-${UUID.randomUUID}"
  val userId = "/auth/oid/1234567890"
  val dateOfBirth = DateModel("01", "01", "1980")

  val baseURI = "/report-quarterly/income-and-expenses/sign-up"
  val userDetailsURI = "/user-details"
  val confirmDetailsURI = "/confirm-details"
  val confirmationURI = s"$baseURI/confirmation"
  val otherIncomeURI = s"$baseURI/income-other"
  val businessNameURI = s"$baseURI/business/name"
  val errorMainIncomeURI = s"$baseURI/error/main-income"
  val errorOtherIncomeURI = s"$baseURI/error/other-income"
  val accountingPeriodPriorURI = s"$baseURI/business/accounting-period-prior"
  val registerNextAccountingPeriodURI = s"$baseURI/business/register-next-accounting-period"
  val accountingPeriodDatesURI = s"$baseURI/business/accounting-period-dates"
  val termsURI = s"$baseURI/terms"
  val checkYourAnswersURI = s"$baseURI/check-your-answers"
  val signInURI = s"$baseURI/sign-in"

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
