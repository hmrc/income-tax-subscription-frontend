
package helpers.agent

import java.net.URLEncoder
import java.util.UUID

import models.DateModel
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Generator

object IntegrationTestConstants {
  lazy val testNino: String = helpers.IntegrationTestConstants.testNino
  lazy val testUtr: String = helpers.IntegrationTestConstants.testUtr
  lazy val testMTDID: String = helpers.IntegrationTestConstants.testMtdId
  lazy val testSubscriptionID: String = helpers.IntegrationTestConstants.testSubscriptionId
  lazy val startDate: DateModel = helpers.IntegrationTestConstants.startDate
  lazy val endDate: DateModel = helpers.IntegrationTestConstants.endDate

  lazy val ggServiceName = "HMRC-MTD-IT"
  val SessionId = s"stubbed-${UUID.randomUUID}"
  val userId = "/auth/oid/1234567890"
  val dateOfBirth: DateModel = helpers.IntegrationTestConstants.dateOfBirth
  lazy val testARN: String = new Generator().nextAtedUtr.utr //Not a valid ARN, for test purposes only
  val testUserIdEncoded: String = URLEncoder.encode(userId, "UTF-8")

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
  val foreignPropertyAccountingMethodURI = s"$baseURI/business/accounting-method-property"
  val propertyCommencementDateURI = s"$baseURI/business/property-commencement-date"
  val foreignPropertyCommencementDateURI = s"$baseURI/business/overseas-commencement-date"
  val errorMainIncomeURI = s"$baseURI/error/main-income"
  val checkYourAnswersURI = s"$baseURI/check-your-answers"
  val feedbackSubmittedURI = s"$baseURI/feedback-submitted"
  val ggSignOutURI = s"/gg/sign-out"
  val signOutURI = s"/report-quarterly/income-and-expenses/sign-up/logout"
  val whatYearToSignUpURI = s"$baseURI/business/what-year-to-sign-up"

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
