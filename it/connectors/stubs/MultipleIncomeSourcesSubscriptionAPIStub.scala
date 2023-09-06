
package connectors.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationTestConstants.testMtdId
import helpers.servicemocks.WireMockMethods
import models.common.subscription.CreateIncomeSourcesModel
import play.api.libs.json.{JsValue, Json}

object MultipleIncomeSourcesSubscriptionAPIStub extends WireMockMethods {

  private def signUpUri(nino: String, taxYear: String): String = s"/income-tax-subscription/mis/sign-up/$nino/$taxYear"

  private def createIncomeSourcesUri(mtdbsa: String): String = s"/income-tax-subscription/mis/create/$mtdbsa"

  def stubPostSignUp(nino: String, taxYear: String)(responseCode: Int, response: JsValue = Json.obj("mtdbsa" -> testMtdId)): StubMapping = {
    when(
      method = POST,
      uri = signUpUri(nino, taxYear)
    ).thenReturn(
      status = responseCode,
      body = response
    )
  }

  def stubPostSubscriptionForTaskList(mtdbsa: String, request: CreateIncomeSourcesModel)
                                     (responseCode: Int,
                                      response: JsValue = Json.obj()): StubMapping = {
    when(
      method = POST,
      uri = createIncomeSourcesUri(mtdbsa),
      body = Json.toJson(request)
    ).thenReturn(
      status = responseCode,
      body = response
    )
  }
}
