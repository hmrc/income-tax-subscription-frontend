
package connectors.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationTestConstants.testMtdId
import helpers.servicemocks.WireMockMethods
import models.common.business.BusinessSubscriptionDetailsModel
import play.api.libs.json.{JsValue, Json}

object MultipleIncomeSourcesSubscriptionAPIStub extends WireMockMethods {

  private def signUpUri(nino: String): String = s"/income-tax-subscription/mis/sign-up/$nino"
  private def createIncomeSourcesUri(mtdbsa: String): String = s"/income-tax-subscription/mis/create/$mtdbsa"

  def stubPostSignUp(nino: String)(responseCode: Int, response: JsValue = Json.obj("mtdbsa" -> testMtdId)): StubMapping = {
    when (
      method = POST,
      uri = signUpUri(nino)
    ) thenReturn (
      status = responseCode,
      body = response
    )
  }

  def stubPostSubscription(mtdbsa: String, request: BusinessSubscriptionDetailsModel)
                          (responseCode: Int,
                           response: JsValue = Json.obj()): StubMapping = {
    when (
      method = POST,
      uri = createIncomeSourcesUri(mtdbsa),
      body = Json.toJson(request)
    ) thenReturn (
      status = responseCode,
      body = response
    )
  }
}
