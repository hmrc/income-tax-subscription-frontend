
package connectors

import connectors.individual.subscription.MultipleIncomeSourcesSubscriptionConnector
import connectors.stubs.MultipleIncomeSourcesSubscriptionAPIStub._
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{testMtdId, testNino}
import models.common._
import models.common.business._
import models.common.subscription.{BadlyFormattedSignUpIncomeSourcesResponse, CreateIncomeSourcesFailureResponse, CreateIncomeSourcesSuccess, SignUpIncomeSourcesFailureResponse, SignUpIncomeSourcesSuccess}
import models.{Accruals, Cash, DateModel}
import org.scalatest.Matchers
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

class MultipleIncomeSourcesSubscriptionConnectorISpec extends ComponentSpecBase with Matchers {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val TestMisSubscriptionConnector: MultipleIncomeSourcesSubscriptionConnector = app.injector.instanceOf[MultipleIncomeSourcesSubscriptionConnector]

  "MultipleIncomeSourcesSubscription signup" should {
    "return SignUpIncomeSourcesSuccess when valid response is returned" in {
      stubPostSignUp(testNino)(OK)

      val res = TestMisSubscriptionConnector.signUp(testNino)

      await(res) shouldBe Right(SignUpIncomeSourcesSuccess(testMtdId))
    }
    "return BadlyFormattedSignUpIncomeSourcesResponse when the response is malformed" in {
      stubPostSignUp(testNino)(OK, Json.obj("not" -> "correct"))

      val res = TestMisSubscriptionConnector.signUp(testNino)

      await(res) shouldBe Left(BadlyFormattedSignUpIncomeSourcesResponse)
    }
    "return SignUpIncomeSourcesFailureResponse if the request fails" in {
      stubPostSignUp(testNino)(INTERNAL_SERVER_ERROR)

      val res = TestMisSubscriptionConnector.signUp(testNino)

      await(res) shouldBe Left(SignUpIncomeSourcesFailureResponse(INTERNAL_SERVER_ERROR))
    }
  }

  "MultipleIncomeSourcesSubscription createIncomeSources" should {
    val businessDetailsModel = BusinessSubscriptionDetailsModel(
      nino = testNino,
      accountingPeriod = AccountingPeriodModel(DateModel("6", "4", "2018"), DateModel("5", "4", "2019")),
      selfEmploymentsData = Some(Seq(SelfEmploymentData(
        id = "id1",
        businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "2017"))),
        businessName = Some(BusinessNameModel("ABC Limited")),
        businessTradeName = Some(BusinessTradeNameModel("Plumbing")),
        businessAddress = Some(BusinessAddressModel("12345", Address(Seq("line1", "line2", "line3", "line4"), "TF3 4NT")))
      ))),
      accountingMethod = Some(Cash),
      incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
      propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("6", "7", "2018"))),
      propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
      overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("6", "8", "2018"))),
      overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
    )

    "return CreateIncomeSourcesSuccess when valid response is returned" in {

      stubPostSubscription(testMtdId, businessDetailsModel)(NO_CONTENT)

      val res = TestMisSubscriptionConnector.createIncomeSources(testMtdId, businessDetailsModel)

      await(res) shouldBe Right(CreateIncomeSourcesSuccess())
    }

    "return CreateIncomeSourcesFailureResponse if the request fails" in {
      stubPostSubscription(testMtdId, businessDetailsModel)(INTERNAL_SERVER_ERROR)

      val res = TestMisSubscriptionConnector.createIncomeSources(testMtdId, businessDetailsModel)

      await(res) shouldBe Left(CreateIncomeSourcesFailureResponse(INTERNAL_SERVER_ERROR))
    }
  }
}
