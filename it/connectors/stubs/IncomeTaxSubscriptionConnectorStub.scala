
package connectors.stubs

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, stubFor}
import helpers.IntegrationTestConstants.testMtdId
import helpers.IntegrationTestModels._
import helpers.agent.IntegrationTestConstants.SessionId
import helpers.agent.IntegrationTestModels.{fullSubscriptionData, subscriptionData}
import helpers.agent.WiremockHelper
import helpers.servicemocks.WireMockMethods
import models.common.{OverseasPropertyModel, PropertyModel}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json, OFormat, Writes}
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.{BusinessesKey, OverseasProperty, Property, subscriptionId}

object IncomeTaxSubscriptionConnectorStub extends WireMockMethods {

  implicit val SubscriptionDetailsFormat: OFormat[SubscriptionData] = Json.format[SubscriptionData]

  private def subscriptionUri(id: String) = s"/income-tax-subscription/subscription-data/test-reference/id/$id"

  private def subscriptionDeleteUri = s"/income-tax-subscription/subscription-data/all"

  def stubGetSubscriptionDetails(id: String, responseStatus: Int, responseBody: JsValue = Json.obj()): Unit = {
    when(
      method = GET,
      uri = subscriptionUri(id)
    ) thenReturn(responseStatus, responseBody)
  }

  def stubSaveSubscriptionDetails[T](id: String, body: T)(implicit writer: Writes[T]): Unit = {
    when(method = POST, uri = postUri(subscriptionId))
      .thenReturn(Status.OK, CacheMap(SessionId, fullSubscriptionData + (id -> Json.toJson(body))))
  }

  def verifySaveSubscriptionDetails[T](id: String, body: T, count: Option[Int] = None)(implicit writer: Writes[T]): Unit = {
    WiremockHelper.verifyPost(postUri(id), Some((Json.toJson(body)).toString()), count)
  }

  def stubSaveSubscriptionDetails(id: String): Unit = {
    when(method = POST, uri = postUri(subscriptionId))
      .thenReturn(Status.OK, CacheMap(SessionId, fullSubscriptionDataBothPost))
  }

  def stubSaveProperty(property: PropertyModel): Unit = {
    when(method = POST, uri = postUri(Property), body = Json.toJson(property))
      .thenReturn(Status.OK)
  }

  def verifySaveProperty[T](property: PropertyModel, count: Option[Int] = None): Unit = {
    WiremockHelper.verifyPost(postUri(Property), Some((Json.toJson(property): JsValue).toString()), count)
  }

  def stubSaveOverseasProperty(property: OverseasPropertyModel): Unit = {
    when(method = POST, uri = postUri(OverseasProperty), body = Json.toJson(property))
      .thenReturn(Status.OK)
  }

  def verifySaveOverseasProperty[T](property: OverseasPropertyModel, count: Option[Int] = None): Unit = {
    WiremockHelper.verifyPost(postUri(OverseasProperty), Some((Json.toJson(property): JsValue).toString()), count)
  }

  def stubDeleteSubscriptionDetails(id: String): Unit = {
    when(method = DELETE, uri = postUri(id)).thenReturn(Status.OK)
  }

  def verifyDeleteSubscriptionDetails(id: String, count: Option[Int] = None): Unit = {
    WiremockHelper.verifyDelete(postUri(id), count)
  }

  def stubFullSubscriptionGet(): Unit = stubSubscriptionData(fullSubscriptionDataAllPost)

  def stubFullSubscriptionBothPost(): Unit = stubSubscriptionData(fullSubscriptionDataBothPost)

  def stubFullSubscriptionPropertyPost(): Unit = stubSubscriptionData(fullSubscriptionDataPropertyPost)

  def stubFullSubscriptionData(): Unit = stubSubscriptionData(fullSubscriptionData)

  def stubEmptySubscriptionData(): Unit = stubSubscriptionData(subscriptionData())

  def stubSubscriptionData(data: Map[String, JsValue]): Unit = {
    val body = CacheMap(SessionId, data)

    when(method = GET, uri = subscriptionUri(subscriptionId))
      .thenReturn(Status.OK, body)
  }

  def stubBusinessesData(): Unit = {
    val body = testSummaryDataSelfEmploymentData

    when(method = GET, uri = subscriptionUri(BusinessesKey))
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

  def stubPostSubscriptionId(subscriptionDataType: Map[String, JsValue] = fullSubscriptionData): Unit = {
    val id = "MtditId"
    when(method = POST, uri = subscriptionUri(subscriptionId))
      .thenReturn(Status.OK, CacheMap(SessionId, subscriptionDataType + (id -> Json.toJson(testMtdId))))
  }


  def postUri(key: String) = s"${subscriptionUri(key)}"


  case class SubscriptionData(id: String, data: Map[String, JsValue])

}
