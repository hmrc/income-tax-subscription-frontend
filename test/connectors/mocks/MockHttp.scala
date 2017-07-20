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

package connectors.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.http.{HttpGet, HttpPost, HttpPut, HttpResponse}
import utils.MockTrait

import scala.concurrent.Future


trait MockHttp extends MockTrait {

  val mockHttpGet = mock[HttpGet]
  val mockHttpPost = mock[HttpPost]
  val mockHttpPut = mock[HttpPut]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpPost)
    reset(mockHttpGet)
    reset(mockHttpPut)
  }

  def setupMockHttpPost[I](url: Option[String] = None, body: Option[I] = None)(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.startsWith(x))
    lazy val bodyMatcher = body.fold(ArgumentMatchers.any[I]())(x => ArgumentMatchers.eq(x))
    when(mockHttpPost.POST[I, HttpResponse](urlMatcher, bodyMatcher, ArgumentMatchers.any()
    )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(status, Some(response))))
  }

  def setupMockHttpPostException[I](url: Option[String] = None, body: Option[I] = None)(exception: Exception): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.startsWith(x))
    lazy val bodyMatcher = body.fold(ArgumentMatchers.any[I]())(x => ArgumentMatchers.eq(x))
    when(mockHttpPost.POST[I, HttpResponse](urlMatcher, bodyMatcher, ArgumentMatchers.any()
    )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(exception))
  }

  def setupMockHttpPostEmpty(url: Option[String] = None)(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    when(mockHttpPost.POSTEmpty[HttpResponse](urlMatcher)(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(status, Some(response))))
  }

  def setupMockHttpPostEmptyException(url: Option[String] = None)(exception: Exception): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    when(mockHttpPost.POSTEmpty[HttpResponse](urlMatcher)(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(exception))
  }

  def setupMockHttpGet(url: Option[String] = None)(status: Int, response: Option[JsValue]): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    when(mockHttpGet.GET[HttpResponse](urlMatcher)(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(status, response)))
  }

  def setupMockHttpGetException(url: Option[String] = None)(exception: Exception): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    when(mockHttpGet.GET[HttpResponse](urlMatcher)(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(exception))
  }

  def setupMockHttpGetWithParams(url: Option[String], params: Option[Seq[(String, String)]])(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    lazy val paramsMatcher = params.fold(ArgumentMatchers.any[Seq[(String, String)]]())(x => ArgumentMatchers.eq(x))
    when(mockHttpGet.GET[HttpResponse](urlMatcher, paramsMatcher)(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(status, Some(response))))
  }

  def setupMockHttpPut[I](url: Option[String] = None, body: Option[I] = None)(status: Int, response: Option[JsValue]): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.startsWith(x))
    lazy val bodyMatcher = body.fold(ArgumentMatchers.any[I]())(x => ArgumentMatchers.eq(x))
    when(mockHttpPut.PUT[I, HttpResponse](urlMatcher, bodyMatcher
    )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(status, response)))
  }

}
