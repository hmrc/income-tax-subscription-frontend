
package connectors

import connectors.individual.subscription.MultipleIncomeSourcesSubscriptionConnector
import connectors.stubs.MultipleIncomeSourcesSubscriptionAPIStub._
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{testMtdId, testNino}
import models.individual.subscription.{BadlyFormattedSignUpResponse, MultipleIncomeSourcesSignUpFailure}
import models.individual.subscription.{MultipleIncomeSourcesSignUpFailureResponse, MultipleIncomeSourcesSignUpSuccess}
import org.scalatest.Matchers
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

class MultipleIncomeSourcesSubscriptionConnectorISpec extends ComponentSpecBase with Matchers {

  implicit val hc = HeaderCarrier()

  val TestMisSubscriptionConnector: MultipleIncomeSourcesSubscriptionConnector = app.injector.instanceOf[MultipleIncomeSourcesSubscriptionConnector]

  "MultipleIncomeSourcesSubscription" should {
    "return MultipleIncomeSourcesSignUpSuccess when valid response is returned" in {
        stubPostSignUp(testNino)(OK)

        val res = TestMisSubscriptionConnector.signUp(testNino)

        await(res) shouldBe Right(MultipleIncomeSourcesSignUpSuccess(testMtdId))
    }
    "return BadlyFormattedSubscriptionResponse when the response is malformed" in {
      stubPostSignUp(testNino)(OK, Json.obj("not" -> "correct"))

      val res = TestMisSubscriptionConnector.signUp(testNino)

      await(res) shouldBe Left(BadlyFormattedSignUpResponse)
    }
    "return SubscriptionFailureResponse if the request fails" in {
      stubPostSignUp(testNino)(INTERNAL_SERVER_ERROR)

      val res = TestMisSubscriptionConnector.signUp(testNino)

      await(res) shouldBe Left(MultipleIncomeSourcesSignUpFailureResponse(INTERNAL_SERVER_ERROR))
    }
  }

}
