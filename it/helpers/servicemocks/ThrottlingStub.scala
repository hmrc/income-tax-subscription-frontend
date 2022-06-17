
package helpers.servicemocks

import helpers.WiremockHelper
import play.api.http.Status._

object ThrottlingStub extends WireMockMethods {

  private def throttleURI(throttleId: String): String = {
    s"/income-tax-subscription/throttled?throttleId=$throttleId"
  }

  def stubThrottle(throttleId: String)(throttled: Boolean): Unit = {
    val status: Int = if (throttled) SERVICE_UNAVAILABLE else OK
    when(
      method = POST,
      uri = throttleURI(throttleId).replace("?", "\\?") // uses regex behind the scenes, \ to escape ? char in regex
    ).thenReturn(status = status)
  }

  def verifyThrottle(throttleId: String)(count: Int = 1): Unit = {
    WiremockHelper.verifyPost(uri = throttleURI(throttleId), count = Some(count))
  }

}
