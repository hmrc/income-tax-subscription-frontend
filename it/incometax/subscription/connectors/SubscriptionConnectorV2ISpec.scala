
package incometax.subscription.connectors

import core.models.{Cash, DateModel}
import play.api.test.Helpers._
import helpers.ComponentSpecBase
import incometax.subscription.stubs.SubscriptionAPIV2Stub._
import org.scalatest.Matchers
import helpers.IntegrationTestConstants.{testMTDID, testNino}
import incometax.business.models.AccountingPeriodModel
import incometax.subscription.models._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class SubscriptionConnectorV2ISpec extends ComponentSpecBase with Matchers {

  implicit val hc = HeaderCarrier()

  val TestSubscriptionConnector = app.injector.instanceOf[SubscriptionConnectorV2]

  val testSubscriptionRequestV2 = SubscriptionRequestV2(
    nino = testNino,
    arn = None,
    businessIncome = None,
    propertyIncome = None
  )

  val testStartDate = DateModel("6", "4", "2017")
  val testEndDate = DateModel("5", "4", "2018")

  val testBusinessIncome = BusinessIncomeModel(
    tradingName = Some("Test trading name"),
    accountingPeriod = AccountingPeriodModel(testStartDate, testEndDate),
    accountingMethod = Cash
  )

  val testPropertyIncome = PropertyIncomeModel(
    accountingMethod = Some(Cash)
  )

  "subscription" should {
    "return SubscriptionSuccess if successful" when {
      "neither business or property income sections are defined" in {
        stubSubscription(testSubscriptionRequestV2)(OK)

        val res = TestSubscriptionConnector.subscribe(testSubscriptionRequestV2)

        await(res) shouldBe Right(SubscriptionSuccess(testMTDID))
      }
      "business income is defined but property income isn't defined" in {
        val testSubscriptionWithBusiness = testSubscriptionRequestV2.copy(businessIncome = Some(testBusinessIncome))
        stubSubscription(testSubscriptionWithBusiness)(OK)

        val res = TestSubscriptionConnector.subscribe(testSubscriptionWithBusiness)

        await(res) shouldBe Right(SubscriptionSuccess(testMTDID))
      }
      "property income is defined but business income isn't defined" in {
        val testSubscriptionWithProperty = testSubscriptionRequestV2.copy(propertyIncome = Some(testPropertyIncome))
        stubSubscription(testSubscriptionWithProperty)(OK)

        val res = TestSubscriptionConnector.subscribe(testSubscriptionWithProperty)

        await(res) shouldBe Right(SubscriptionSuccess(testMTDID))
      }
    }
    "return BadlyFormattedSubscriptionResponse when the request is malformed" in {
      stubSubscription(testSubscriptionRequestV2)(OK, Json.obj("not" -> "correct"))

      val res = TestSubscriptionConnector.subscribe(testSubscriptionRequestV2)

      await(res) shouldBe Left(BadlyFormattedSubscriptionResponse)
    }
    "return SubscriptionFailureResponse if the request fails" in {
      stubSubscription(testSubscriptionRequestV2)(INTERNAL_SERVER_ERROR)

      val res = TestSubscriptionConnector.subscribe(testSubscriptionRequestV2)

      await(res) shouldBe Left(SubscriptionFailureResponse(INTERNAL_SERVER_ERROR))
    }
  }

}
