
package connectors.stubs

import helpers.servicemocks.WireMockMethods
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json

object PreferencesFrontendConnectorStub extends WireMockMethods {

  def activateUri = s"/paperless/activate\\?returnUrl=(.*)&returnLinkText=(.*)"

  def stubGetOptedInStatus(response: Option[Boolean]): Unit = {
    response match {
      case Some(value) =>
        when(
          method = PUT,
          uri = activateUri,
          body = Json.obj()
        ).thenReturn(OK, Json.obj("optedIn" -> value))
      case None =>
        when(
          method = PUT,
          uri = activateUri,
          body = Json.obj()
        ).thenReturn(NOT_FOUND)
    }
  }

}
