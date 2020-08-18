
package connectors.stubs

import helpers.servicemocks.WireMockMethods
import play.api.http.Status
import play.api.libs.json.{JsValue, Json, OFormat, Writes}
import helpers.agent.IntegrationTestConstants.{SessionId, testSubscriptionID}
import helpers.IntegrationTestConstants.testMtdId
import helpers.agent.IntegrationTestModels.{fullSubscriptionData, subscriptionData}
import helpers.agent.WiremockHelper
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.{BusinessesKey, subscriptionId}
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, stubFor}
import helpers.IntegrationTestModels.{fullIndivSubscriptionDataBothPost, fullIndivSubscriptionDataPropertyPost}

object IncomeTaxSubscriptionConnectorStub extends WireMockMethods {

  implicit val kSubscriptionDetailsFormat: OFormat[SubscriptionData] = Json.format[SubscriptionData]

  private def subscriptionUri(id: String) = s"/income-tax-subscription/self-employments/id/$id"
  private def subscriptionDeleteUri = s"/income-tax-subscription/subscription-data/all"

  def stubGetSubscriptionDetails(id: String, responseStatus: Int, responseBody: JsValue = Json.obj()): Unit = {
    when(
      method = GET,
      uri = subscriptionUri(id)
    ) thenReturn(responseStatus, responseBody)
  }

  def stubSaveSubscriptionDetails[T](id: String, body: T)(implicit writer: Writes[T]): Unit = {
    when(method = POST, uri = postUri(id))
      .thenReturn(Status.OK, CacheMap(SessionId, fullSubscriptionData + (id -> Json.toJson(body))))
  }

  def stubSaveSubscriptionDetails(id: String): Unit = {
    when(method = POST, uri = postUri(id))
      .thenReturn(Status.OK, CacheMap(SessionId, fullIndivSubscriptionDataBothPost))
  }

  def stubIndivFullSubscriptionBothPost(): Unit = stubSubscriptionData(fullIndivSubscriptionDataBothPost)

  def stubIndivFullSubscriptionPropertyPost(): Unit = stubSubscriptionData(fullIndivSubscriptionDataPropertyPost)

  def stubFullSubscriptionData(): Unit = stubSubscriptionData(fullSubscriptionData)

  def stubEmptySubscriptionData(): Unit = stubSubscriptionData(subscriptionData())

  def stubSubscriptionData(data: Map[String, JsValue]): Unit = {
    val body = CacheMap(SessionId, data)

    when(method = GET, uri = subscriptionUri(subscriptionId))
      .thenReturn(Status.OK, body)
  }

  def stubSubscriptionDeleteAll(): Unit = {
    when(method = DELETE, uri = subscriptionDeleteUri)
      .thenReturn(Status.OK, "")
  }

  def verifySubscriptionDelete(count: Option[Int] = None): Unit = {
    WiremockHelper.verifyDelete(subscriptionDeleteUri, count)
  }

  def verifySubscriptionSave[T](id: String, body: T, count: Option[Int] = None)(implicit writer: Writes[T]): Unit = {
    import helpers.ImplicitConversions._
    WiremockHelper.verifyPost(subscriptionUri(subscriptionId), Some((body: JsValue).toString()), count)
  }

  def stubSubscriptionFailure(): Unit = {
    when(method = GET, uri = subscriptionUri(subscriptionId))
      .thenReturn(Status.INTERNAL_SERVER_ERROR)

    val mapping = POST.wireMockMapping(WireMock.urlPathMatching(subscriptionUri(subscriptionId) + "*"))
    val response = aResponse().withStatus(Status.INTERNAL_SERVER_ERROR)
    stubFor(mapping.willReturn(response))
  }

  def stubPostSubscriptionId(): Unit = {
    val id = "MtditId"
    when(method = POST, uri =subscriptionUri(subscriptionId))
      .thenReturn(Status.OK, CacheMap(SessionId, fullSubscriptionData + ( id -> Json.toJson(testMtdId))))
  }

  def postUri(key: String) =  s"${subscriptionUri(subscriptionId)}/$key"


  case class SubscriptionData(id: String, data: Map[String, JsValue])

}
