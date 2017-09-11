/*
 * Copyright 2017 HM Revenue & Customs
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

package helpers.servicemocks

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, stubFor}
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.WiremockHelper
import play.api.http.Status
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http.cache.client.CacheMap

object KeystoreStub extends WireMockMethods {
  val keystoreUri = s"/keystore/income-tax-subscription-frontend/$SessionId"
  implicit val keystoreFormat = Json.format[KeystoreData]
  val emptyKeystoreData = KeystoreData(SessionId, Map.empty)

  def stubPutMtditId(): Unit = {
    val id = "MtditId"

    when(method = PUT, uri = putUri(id))
      .thenReturn(Status.OK, CacheMap(SessionId, fullKeystoreData + (id -> Json.toJson(testMTDID))))
  }

  def putUri(key: String) = s"$keystoreUri/data/$key"

  def stubFullKeystore(): Unit = stubKeystoreData(fullKeystoreData)

  def stubEmptyKeystore(): Unit = stubKeystoreData(keystoreData())

  def stubKeystoreData(data: Map[String, JsValue]): Unit = {
    val body = CacheMap(SessionId, data)

    when(method = GET, uri = keystoreUri)
      .thenReturn(Status.OK, body)
  }

  def stubKeystoreFailure() : Unit = {
    when(method = GET, uri = keystoreUri)
      .thenReturn(Status.INTERNAL_SERVER_ERROR)

    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(s"$keystoreUri/data/.*"))
    val response = aResponse().withStatus(Status.INTERNAL_SERVER_ERROR)
    stubFor(mapping.willReturn(response))
  }

  def stubKeystoreSave[T](id: String, body: T)(implicit writer: Writes[T]): Unit = {
    when(method = PUT, uri = putUri(id))
      .thenReturn(Status.OK, CacheMap(SessionId, fullKeystoreData + (id -> Json.toJson(body))))
  }

  def verifyKeyStoreDelete(count: Option[Int] = None): Unit = {
    WiremockHelper.verifyDelete(keystoreUri, count)
  }

  def verifyKeyStoreSave[T](id: String, body: T, count: Option[Int] = None)(implicit writer: Writes[T]): Unit = {
    import helpers.ImplicitConversions._
    WiremockHelper.verifyPut(putUri(id), Some((body: JsValue).toString()), count)
  }

  def stubKeystoreDelete(): Unit = {
    when(method = DELETE, uri = keystoreUri)
      .thenReturn(Status.OK, "")
  }

//  def stubKeystoreDelete(): Unit = {
//    when(method = DELETE, uri = keystoreUri)
//      .thenReturn(Status.OK, "")
//  }
//
//  def verifyKeyStoreDelete(count: Option[Int] = None): Unit = {
//    WiremockHelper.verifyDelete(keystoreUri, count)
//  }
//
//  def verifyKeyStoreSave[T](id: String, body: T, count: Option[Int] = None)(implicit writer: Writes[T]): Unit = {
//    import helpers.ImplicitConversions._
//    WiremockHelper.verifyPut(putUri(id), Some((body: JsValue).toString()), count)
//  }

  case class KeystoreData(id: String, data: Map[String, JsValue])

}

