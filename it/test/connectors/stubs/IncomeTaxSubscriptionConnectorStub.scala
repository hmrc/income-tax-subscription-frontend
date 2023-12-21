/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors.stubs

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, stubFor}
import helpers.IntegrationTestModels._
import helpers.WiremockHelper
import helpers.servicemocks.WireMockMethods
import models.common.{OverseasPropertyModel, PropertyModel}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json, OFormat, Writes}
import utilities.SubscriptionDataKeys._

object IncomeTaxSubscriptionConnectorStub extends WireMockMethods {

  implicit val SubscriptionDetailsFormat: OFormat[SubscriptionData] = Json.format[SubscriptionData]

  def subscriptionUri(id: String) = s"/income-tax-subscription/subscription-data/test-reference/id/$id"

  private def subscriptionDeleteUri = s"/income-tax-subscription/subscription-data/all"

  def verifyGet(id: String)(count: Int = 1): Unit = {
    WiremockHelper.verifyGet(subscriptionUri(id), count = Some(count))
  }

  def stubGetSubscriptionDetails(id: String, responseStatus: Int, responseBody: JsValue = Json.obj()): Unit = {
    when(
      method = GET,
      uri = subscriptionUri(id)
    ).thenReturn(responseStatus, responseBody)
  }

  def stubSaveSubscriptionDetails[T](id: String, body: T)(implicit writer: Writes[T]): Unit = {
    when(method = POST, uri = subscriptionUri(id), Json.toJson(body))
      .thenReturn(Status.OK)
  }

  def stubSaveSubscriptionDetailsFailure(id: String): Unit = {
    when(method = POST, uri = subscriptionUri(id))
      .thenReturn(Status.INTERNAL_SERVER_ERROR)
  }

  def verifySaveSubscriptionDetails[T](id: String, body: T, count: Option[Int] = None)(implicit writer: Writes[T]): Unit = {
    WiremockHelper.verifyPost(subscriptionUri(id), Some(Json.toJson(body).toString()), count)
  }

  def stubSaveProperty(property: PropertyModel): Unit = {
    when(method = POST, uri = subscriptionUri(Property), body = Json.toJson(property))
      .thenReturn(Status.OK)
  }

  def stubSaveIncomeSourceConfirmation(flag: Boolean): Unit = {
    when(method = POST, uri = subscriptionUri(IncomeSourceConfirmation), body = Json.toJson(flag))
      .thenReturn(Status.OK)
  }

  def verifySaveProperty[T](property: PropertyModel, count: Option[Int] = None): Unit = {
    WiremockHelper.verifyPost(subscriptionUri(Property), Some((Json.toJson(property): JsValue).toString()), count)
  }

  def stubSaveOverseasProperty(property: OverseasPropertyModel): Unit = {
    when(method = POST, uri = subscriptionUri(OverseasProperty), body = Json.toJson(property))
      .thenReturn(Status.OK)
  }

  def verifySaveOverseasProperty[T](property: OverseasPropertyModel, count: Option[Int] = None): Unit = {
    WiremockHelper.verifyPost(subscriptionUri(OverseasProperty), Some((Json.toJson(property): JsValue).toString()), count)
  }

  def stubDeleteSubscriptionDetails(id: String): Unit = {
    when(method = DELETE, uri = subscriptionUri(id)).thenReturn(Status.OK)
  }

  def stubDeleteSubscriptionDetailsFailure(id: String): Unit = {
    when(method = DELETE, uri = subscriptionUri(id)).thenReturn(Status.INTERNAL_SERVER_ERROR)
  }

  def verifyDeleteSubscriptionDetails(id: String, count: Option[Int] = None): Unit = {
    WiremockHelper.verifyDelete(subscriptionUri(id), count)
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
    WiremockHelper.verifyPost(subscriptionUri(id), Some((body: JsValue).toString()), count)
  }

  def stubSubscriptionFailure(): Unit = {
    when(method = GET, uri = subscriptionUri(subscriptionId))
      .thenReturn(Status.INTERNAL_SERVER_ERROR)

    val mapping = POST.wireMockMapping(WireMock.urlPathMatching(subscriptionUri(subscriptionId) + "*"))
    val response = aResponse().withStatus(Status.INTERNAL_SERVER_ERROR)
    stubFor(mapping.willReturn(response))
  }

  case class SubscriptionData(id: String, data: Map[String, JsValue])

}
