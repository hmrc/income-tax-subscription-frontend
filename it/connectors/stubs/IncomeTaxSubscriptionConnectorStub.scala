
package connectors.stubs

import helpers.servicemocks.WireMockMethods
import play.api.http.Status
import play.api.libs.json.{JsValue, Json, OFormat, Writes}
import helpers.agent.IntegrationTestConstants.{SessionId, testSubscriptionID}
import helpers.agent.IntegrationTestModels.{fullSubscriptionData, subscriptionData}
import helpers.agent.WiremockHelper
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.subscriptionId
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, stubFor}
import helpers.IntegrationTestModels.{fullIndivSubscriptionDataBothPost, fullIndivSubscriptionDataPropertyPost}

object IncomeTaxSubscriptionConnectorStub extends WireMockMethods {

  implicit val keystoreFormat: OFormat[SubscriptionData] = Json.format[SubscriptionData]

  private def subscriptionUri = s"/income-tax-subscription/self-employments/id/$subscriptionId"

  def stubGetSubscriptionDetails(responseStatus: Int, responseBody: JsValue = Json.obj()): Unit = {
    when(
      method = GET,
      uri = subscriptionUri
    ) thenReturn(responseStatus, responseBody)
  }

  def stubSaveSubscriptionDetails[T](id: String, body: T)(implicit writer: Writes[T]): Unit = {
    when(method = POST, uri = subscriptionUri)
      .thenReturn(Status.OK, CacheMap(SessionId, fullSubscriptionData + (id -> Json.toJson(body))))
  }

  def stubSaveSubscriptionDetails(id: String): Unit = {
    when(method = PUT, uri = subscriptionUri)
      .thenReturn(Status.OK, CacheMap(SessionId, fullIndivSubscriptionDataBothPost))
  }

  def stubIndivFullSubscriptionBothPost(): Unit = stubSubscriptionData(fullIndivSubscriptionDataBothPost)

  def stubIndivFullSubscriptionPropertyPost(): Unit = stubSubscriptionData(fullIndivSubscriptionDataPropertyPost)

  def stubFullSubscriptionData(): Unit = stubSubscriptionData( fullSubscriptionData)

  def stubEmptySubscriptionData(): Unit = stubSubscriptionData( subscriptionData())

  def stubSubscriptionData(data: Map[String, JsValue]): Unit = {
    val body = CacheMap(SessionId, data)

    when(method = GET, uri = subscriptionUri)
      .thenReturn(Status.OK, body)
  }

  def stubSubscriptionDelete(): Unit = {
    when(method = DELETE, uri = subscriptionUri)
      .thenReturn(Status.OK, "")
  }

  def verifySubscriptionDelete(count: Option[Int] = None): Unit = {
    WiremockHelper.verifyDelete(subscriptionUri, count)
  }

  def verifySubscriptionSave[T](id: String, body: T, count: Option[Int] = None)(implicit writer: Writes[T]): Unit = {
    import helpers.ImplicitConversions._
    WiremockHelper.verifyPut(postUri(id), Some((body: JsValue).toString()), count)
  }

  def stubSubscriptionFailure(): Unit = {
    when(method = GET, uri = subscriptionUri)
      .thenReturn(Status.INTERNAL_SERVER_ERROR)

    val mapping = POST.wireMockMapping(WireMock.urlPathMatching(subscriptionUri + "*"))
    val response = aResponse().withStatus(Status.INTERNAL_SERVER_ERROR)
    stubFor(mapping.willReturn(response))
  }

  def stubPostSubscriptionId(): Unit = {

    when(method = POST, uri = postUri(subscriptionId))
      .thenReturn(Status.OK, CacheMap(SessionId, fullSubscriptionData + ( subscriptionId -> Json.toJson(testSubscriptionID))))
  }

  def postUri(key: String) =  s"$subscriptionUri/$key"


  case class SubscriptionData(id: String, data: Map[String, JsValue])

}
