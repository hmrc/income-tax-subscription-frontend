
package connectors

import connectors.individual.subscription.SubscriptionConnector
import connectors.stubs.SubscriptionAPIStub._
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{testMtdId, testNino}
import models.common.AccountingPeriodModel
import models.common.subscription._
import models.{Cash, DateModel}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

class SubscriptionConnectorISpec extends ComponentSpecBase {

  implicit val hc = HeaderCarrier()

  val TestSubscriptionConnector: SubscriptionConnector = app.injector.instanceOf[SubscriptionConnector]

  val testSubscriptionRequest = SubscriptionRequest(
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
        stubPostSubscription(testSubscriptionRequest)(OK)

        val res = TestSubscriptionConnector.subscribe(testSubscriptionRequest)

        await(res) mustBe Right(SubscriptionSuccess(testMtdId))
      }
      "business income is defined but property income isn't defined" in {
        val testSubscriptionWithBusiness = testSubscriptionRequest.copy(businessIncome = Some(testBusinessIncome))
        stubPostSubscription(testSubscriptionWithBusiness)(OK)

        val res = TestSubscriptionConnector.subscribe(testSubscriptionWithBusiness)

        await(res) mustBe Right(SubscriptionSuccess(testMtdId))
      }
      "property income is defined but business income isn't defined" in {
        val testSubscriptionWithProperty = testSubscriptionRequest.copy(propertyIncome = Some(testPropertyIncome))
        stubPostSubscription(testSubscriptionWithProperty)(OK)

        val res = TestSubscriptionConnector.subscribe(testSubscriptionWithProperty)

        await(res) mustBe Right(SubscriptionSuccess(testMtdId))
      }
    }
    "return BadlyFormattedSubscriptionResponse when the request is malformed" in {
      stubPostSubscription(testSubscriptionRequest)(OK, Json.obj("not" -> "correct"))

      val res = TestSubscriptionConnector.subscribe(testSubscriptionRequest)

      await(res) mustBe Left(BadlyFormattedSubscriptionResponse)
    }
    "return SubscriptionFailureResponse if the request fails" in {
      stubPostSubscription(testSubscriptionRequest)(INTERNAL_SERVER_ERROR)

      val res = TestSubscriptionConnector.subscribe(testSubscriptionRequest)

      await(res) mustBe Left(SubscriptionFailureResponse(INTERNAL_SERVER_ERROR))
    }
  }

}
