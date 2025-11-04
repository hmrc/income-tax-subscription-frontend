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

package auth

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsString, JsValue}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import utilities.UnitTestTrait
import utilities.individual.TestConstants.testErrorMessage

import scala.concurrent.Future

trait MockHttp extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val mockHttp: HttpClientV2 = mock[HttpClientV2]

  val errorJson = JsString(testErrorMessage)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttp)
  }

  def setupMockHttpPost[I](url: Option[String] = None, body: Option[I] = None)(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.startsWith(x))
    lazy val bodyMatcher = body.fold(ArgumentMatchers.any[I]())(x => ArgumentMatchers.eq(x))

    when(
      mockHttp
        .post(url"$urlMatcher")
        .withBody(bodyMatcher)(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        .execute[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(HttpResponse(status, json = response, Map.empty)))

  }

  def setupMockHttpPostException[I](url: Option[String] = None, body: Option[I] = None)(exception: Exception): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.startsWith(x))
    lazy val bodyMatcher = body.fold(ArgumentMatchers.any[I]())(x => ArgumentMatchers.eq(x))

    when(
      mockHttp
        .post(url"$urlMatcher")
        .withBody(bodyMatcher)(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        .execute[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.failed(exception))

  }

  def setupMockHttpPostEmpty(url: Option[String] = None)(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    when(
      mockHttp
        .post(url"$urlMatcher")
        .execute[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(HttpResponse(status, json = response, Map.empty)))
  }

  def setupMockHttpPostEmptyException(url: Option[String] = None)(exception: Exception): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    when(
      mockHttp
        .post(url"$urlMatcher")
        .execute[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.failed(exception))
  }

  def setupMockHttpGet(url: Option[String] = None)(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    when(
      mockHttp
        .get(url"$urlMatcher")
        .execute[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(HttpResponse(status, json = response, Map.empty)))
  }

  def setupMockHttpGetException(url: Option[String] = None)(exception: Exception): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    when(
      mockHttp
        .get(url"$urlMatcher")
        .execute[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.failed(exception))
  }

  def setupMockHttpGetWithParams(url: Option[String], params: Option[Seq[(String, String)]])(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    lazy val paramsMatcher = params.fold(ArgumentMatchers.any[Seq[(String, String)]]())(x => ArgumentMatchers.eq(x))
    when(
      mockHttp
        .get(url"$urlMatcher $paramsMatcher")
        .execute[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(HttpResponse(status, json = response, Map.empty)))
  }

  def setupMockHttpPut[I](url: Option[String] = None, body: Option[I] = None)(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.startsWith(x))
    lazy val bodyMatcher = body.fold(ArgumentMatchers.any[I]())(x => ArgumentMatchers.eq(x))
    when(
      mockHttp
        .put(url"$urlMatcher")
        .withBody(bodyMatcher)(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        .execute[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(HttpResponse(status, json = response, Map.empty)))
  }

}
