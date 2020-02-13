
package connectors.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.servicemocks.WireMockMethods
import play.api.libs.json.{JsValue, Json}
import helpers.IntegrationTestConstants.testMTDID
import models.individual.subscription.SubscriptionRequest

object SubscriptionAPIStub extends WireMockMethods {

  private def uri(nino: String): String = s"/income-tax-subscription/subscription-v2/$nino"

  def stubGetSubscriptionResponse(nino: String)(responseCode: Int, response: JsValue = Json.obj("mtditId" -> testMTDID)): StubMapping = {
    when (
      method = GET,
      uri = uri(nino)
    ) thenReturn (
      status = responseCode,
      body = response
    )
  }

  def stubPostSubscription(request: SubscriptionRequest)(responseCode: Int, response: JsValue = Json.obj("mtditId" -> testMTDID)): StubMapping = {
    when (
      method = POST,
      uri = uri(request.nino),
      body = Json.toJson(request)
    ) thenReturn (
      status = responseCode,
      body = response
    )
  }

}
