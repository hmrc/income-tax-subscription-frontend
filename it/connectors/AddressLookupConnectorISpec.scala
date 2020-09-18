
package connectors

import connectors.httpparser.addresslookup.{GetAddressLookupDetailsHttpParser, PostAddressLookupHttpParser}
import connectors.httpparser.addresslookup.PostAddressLookupHttpParser.PostAddressLookupSuccessResponse
import connectors.stubs.AddressLookupConnectorStub._
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.testAddressLookupConfig
import models.individual.business.{Address, BusinessAddressModel}
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import play.api.i18n.Lang
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier


class AddressLookupConnectorISpec extends ComponentSpecBase {

  lazy val connector: AddressLookupConnector = app.injector.instanceOf[AddressLookupConnector]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val lang: Lang = Lang("en")

  val businessAddressModel = BusinessAddressModel(auditRef = "1",
    Address(lines = Seq("line1", "line2", "line3"), postcode = "TF3 4NT"))

  val successJson = Json.obj("auditRef"-> "1",
    "address" -> Json.obj("lines" -> Seq("line1", "line2", "line3"), "postcode" -> "TF3 4NT"))

  "GetAddressLookupDetails" should {
    "Return TestModel" in {

      stubGetAddressLookupDetails("1")(OK, successJson)

      val res = connector.getAddressDetails("1")

      await(res) mustBe Right(Some(businessAddressModel))
    }

    "Return InvalidJson" in {
      stubGetAddressLookupDetails("2")(OK, Json.obj())

      val res = connector.getAddressDetails("2")

      await(res) mustBe Left(GetAddressLookupDetailsHttpParser.InvalidJson)
    }

    "Return None" in {
      stubGetAddressLookupDetails("3")(NOT_FOUND, Json.obj())

      val res = connector.getAddressDetails("3")

      await(res) mustBe Right(None)

    }
    "Return UnexpectedStatusFailure" in {
      stubGetAddressLookupDetails("4")(INTERNAL_SERVER_ERROR, Json.obj())

      val res = connector.getAddressDetails("4")

      await(res) mustBe Left(GetAddressLookupDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))

    }
  }

  "Initialise AddressLookup journey" should {
    "Return PostSelfEmploymentsSuccessResponse" in {
      stubInitializeAddressLookup(Json.parse(testAddressLookupConfig("testUrl")))("testLocation", ACCEPTED)

      val res = connector.initialiseAddressLookup("testUrl")

      await(res) mustBe Right(PostAddressLookupSuccessResponse(Some("testLocation")))
    }

    "Return UnexpectedStatusFailure(status)" in {
      stubInitializeAddressLookup(Json.parse(testAddressLookupConfig("test")))("testLocation", INTERNAL_SERVER_ERROR)

      val res = connector.initialiseAddressLookup("test")

      await(res) mustBe Left(PostAddressLookupHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
    }
  }
}
