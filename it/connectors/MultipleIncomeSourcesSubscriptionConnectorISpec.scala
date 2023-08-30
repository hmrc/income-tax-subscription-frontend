
package connectors

import connectors.individual.subscription.MultipleIncomeSourcesSubscriptionConnector
import connectors.stubs.MultipleIncomeSourcesSubscriptionAPIStub._
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{testMtdId, testNino}
import models.common.subscription._
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

class MultipleIncomeSourcesSubscriptionConnectorISpec extends ComponentSpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val TestMisSubscriptionConnector: MultipleIncomeSourcesSubscriptionConnector = app.injector.instanceOf[MultipleIncomeSourcesSubscriptionConnector]

  val testTaxYear: String = "2023-24"

  "MultipleIncomeSourcesSubscription signup" should {
    "return SignUpIncomeSourcesSuccess when valid response is returned" in {
      stubPostSignUp(testNino, testTaxYear)(OK)

      val res = TestMisSubscriptionConnector.signUp(testNino, testTaxYear)

      await(res) mustBe Right(SignUpIncomeSourcesSuccess(testMtdId))
    }
    "return BadlyFormattedSignUpIncomeSourcesResponse when the response is malformed" in {
      stubPostSignUp(testNino, testTaxYear)(OK, Json.obj("not" -> "correct"))

      val res = TestMisSubscriptionConnector.signUp(testNino, testTaxYear)

      await(res) mustBe Left(BadlyFormattedSignUpIncomeSourcesResponse)
    }
    "return SignUpIncomeSourcesFailureResponse if the request fails" in {
      stubPostSignUp(testNino, testTaxYear)(INTERNAL_SERVER_ERROR)

      val res = TestMisSubscriptionConnector.signUp(testNino, testTaxYear)

      await(res) mustBe Left(SignUpIncomeSourcesFailureResponse(INTERNAL_SERVER_ERROR))
    }
  }
}
