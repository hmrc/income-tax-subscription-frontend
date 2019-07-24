
package helpers.servicemocks

import play.api.http.Status._
import play.api.libs.json.Json

object EligibilityStub extends WireMockMethods {

  def stubEligibilityResponse(sautr: String)(response: Boolean): Unit =
    when (
      method = GET,
      uri = s"/income-tax-subscription-eligibility/eligibility/$sautr"
    ) thenReturn (
      status = OK,
      body = Json.obj("eligible" -> response)
    )

}
