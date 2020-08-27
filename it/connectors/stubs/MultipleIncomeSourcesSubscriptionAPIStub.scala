
package connectors.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationTestConstants.testMtdId
import helpers.servicemocks.WireMockMethods
import models.individual.subscription.SubscriptionRequest
import play.api.libs.json.{JsValue, Json}

object MultipleIncomeSourcesSubscriptionAPIStub extends WireMockMethods {

  private def signUpUri(nino: String): String = s"/income-tax-subscription/subscription/mis/sign-up/$nino"

  def stubPostSignUp(nino: String)(responseCode: Int, response: JsValue = Json.obj("mtdbsa" -> testMtdId)): StubMapping = {
    when (
      method = POST,
      uri = signUpUri(nino)
    ) thenReturn (
      status = responseCode,
      body = response
    )
  }

}
