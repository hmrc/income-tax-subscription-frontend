
package connectors.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationTestConstants.testMtdId
import helpers.WiremockHelper
import helpers.servicemocks.WireMockMethods
import models.common.subscription.CreateIncomeSourcesModel
import play.api.libs.json.{JsValue, Json}

object MultipleIncomeSourcesSubscriptionAPIStub extends WireMockMethods {

  private def signUpUri(nino: String): String = s"/income-tax-subscription/mis/sign-up/$nino"
  private def createIncomeSourcesUri(mtdbsa: String): String = s"/income-tax-subscription/mis/create/$mtdbsa"

  def verifyPostSignUpCount(nino: String)(count: Int = 1): Unit = {
    WiremockHelper.verifyPost(signUpUri(nino), count = Some(count))
  }

  def verifyPostSubscriptionCount(mtdbsa: String)(count: Int = 1): Unit = {
    WiremockHelper.verifyPost(createIncomeSourcesUri(mtdbsa), count = Some(count))
  }

  def stubPostSignUp(nino: String)(responseCode: Int, response: JsValue = Json.obj("mtdbsa" -> testMtdId)): StubMapping = {
    when (
      method = POST,
      uri = signUpUri(nino)
    ) thenReturn (
      status = responseCode,
      body = response
    )
  }

  def stubPostSubscriptionForTaskList(mtdbsa: String, request: CreateIncomeSourcesModel)
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
