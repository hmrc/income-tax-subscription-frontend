
package connectors.stubs

import helpers.servicemocks.WireMockMethods
import play.api.libs.json.{JsValue, Json}

object AddressLookupConnectorStub extends WireMockMethods {

  private def addressLookupInitializeUrl = s"/api/v2/init"
  private def getAddressDetailsUrl(id: String) = s"/api/v2/confirmed\\?id=$id"

  def stubGetAddressLookupDetails(id: String)(responseStatus: Int, responseBody: JsValue = Json.obj()): Unit = {
    when(
      method = GET,
      uri = getAddressDetailsUrl(id)
    ) thenReturn(responseStatus, responseBody)
  }

  def stubInitializeAddressLookup(body: JsValue = Json.obj())(locationHeader: String, responseStatus: Int, responseBody: JsValue = Json.obj()): Unit = {
    when (
      method = POST,
      uri = addressLookupInitializeUrl,
      body = body
    ) thenReturn (responseStatus, Map("Location" -> locationHeader), responseBody)
  }
}
