
package helpers.agent

import models.DateModel
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Generator

import java.net.URLEncoder
import java.util.UUID
import scala.util.matching.Regex

object IntegrationTestConstants {

  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  lazy val testNino: String = helpers.IntegrationTestConstants.testNino
  lazy val testFormattedNino: String = testNino match {
    case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
      s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
    case other => other
  }
  lazy val testUtr: String = helpers.IntegrationTestConstants.testUtr
  lazy val testUtrEnrolmentKey: String = s"IR-SA~UTR~$testUtr"
  lazy val testMTDID: String = helpers.IntegrationTestConstants.testMtdId
  lazy val testMTDIDEnrolmentKey: String = s"HMRC-MTD-IT~MTDITID~$testMTDID"
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
  val baseSEURI = "http://localhost:9563/report-quarterly/income-and-expenses/sign-up/self-employments/client"
  val indexURI = s"$baseURI/index"
  val userDetailsURI = "/user-details"
  val confirmDetailsURI = "/confirm-details"
  val whatYouNeedToDoURI = s"$baseURI/what-you-need-to-do"
  val clientDetailsURI = s"$baseURI/client-details"
  val clientDetailsErrorURI = s"$baseURI/error/client-details"
  val agentLockedOutURI = s"$baseURI/error/lockout"
  val alreadySubscribedURI = s"$baseURI/error/client-already-subscribed"
  val registerForSAURI = s"$baseURI/register-for-SA"
  val confirmationURI = s"$baseURI/confirmation"
  val globalCheckYourAnswersURI = s"$baseURI/final-check-your-answers"
  val noClientRelationshipURI = s"$baseURI/error/no-client-relationship"
  val incomeSourceURI = s"$baseURI/income"
  val incomeSourcesURI = "/report-quarterly/income-and-expenses/sign-up/client/eligibility/income-sources"
  val businessNameSEURI = s"$baseSEURI/details"
  val propertyAccountingMethodURI = s"$baseURI/business/accounting-method-property"
  val overseasPropertyAccountingMethod = s"$baseURI/business/overseas-property-accounting-method"
  val propertyStartDateURI = s"$baseURI/business/property-commencement-date"
  val overseasPropertyStartDateURI = s"$baseURI/business/overseas-commencement-date"
  val errorMainIncomeURI = s"$baseURI/error/main-income"
  val checkYourAnswersURI = s"$baseURI/check-your-answers"
  val ukPropertyCheckYourAnswersURI = s"$baseURI/business/uk-property-check-your-answers"
  val overseasPropertyCheckYourAnswersURI = s"$baseURI/business/overseas-property-check-your-answers"
  val feedbackSubmittedURI = s"$baseURI/feedback-submitted"
  val ggSignOutURI = s"/bas-gateway/sign-out-without-state"
  val signOutURI = s"/report-quarterly/income-and-expenses/sign-up/logout"
  val whatYearToSignUpURI = s"$baseURI/business/what-year-to-sign-up"
  val taxYearCheckYourAnswersURI = s"$baseURI/business/tax-year-check-your-answers"
  val taskListURI = s"$baseURI/business/task-list"
  val yourIncomeSourcesURI = s"$baseURI/your-income-source"
  val addAnotherClient = s"$baseURI/add-another"

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
