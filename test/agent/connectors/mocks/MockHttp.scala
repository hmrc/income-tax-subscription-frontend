/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.connectors.mocks

import agent.utils.TestConstants.testErrorMessage
import core.utils.MockTrait
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{JsString, JsValue}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}


trait MockHttp extends MockTrait {

  val mockHttp = mock[HttpClient]

  val errorJson = JsString(testErrorMessage)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttp)
  }

  def setupMockHttpPost[I](url: Option[String] = None, body: Option[I] = None)(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.startsWith(x))
    lazy val bodyMatcher = body.fold(ArgumentMatchers.any[I]())(x => ArgumentMatchers.eq(x))
    when(mockHttp.POST[I, HttpResponse](urlMatcher, bodyMatcher, ArgumentMatchers.any()
    )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])).thenReturn(Future.successful(HttpResponse(status, Some(response))))
  }

  def setupMockHttpPostException[I, E <: Throwable](url: Option[String] = None, body: Option[I] = None)(exception: E): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.startsWith(x))
    lazy val bodyMatcher = body.fold(ArgumentMatchers.any[I]())(x => ArgumentMatchers.eq(x))
    when(mockHttp.POST[I, HttpResponse](urlMatcher, bodyMatcher, ArgumentMatchers.any()
    )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])).thenReturn(Future.failed(exception))
  }

  def verifyHttpPost[I](url: Option[String] = None, body: Option[I] = None)(count: Int): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    lazy val bodyMatcher = body.fold(ArgumentMatchers.any[I]())(x => ArgumentMatchers.eq(x))
    verify(mockHttp, times(count)).POST[I, HttpResponse](urlMatcher, bodyMatcher, ArgumentMatchers.any()
    )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])
  }

  def setupMockHttpPostEmpty(url: Option[String] = None)(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    when(mockHttp.POSTEmpty[HttpResponse](urlMatcher)(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])).thenReturn(Future.successful(HttpResponse(status, Some(response)))
    )
  }

  def verifyMockHttpPostEmpty(url: Option[String] = None)(count: Int): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    verify(mockHttp, times(count)).POSTEmpty[HttpResponse](urlMatcher)(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])
  }

  def setupMockHttpGet(url: Option[String] = None)(status: Int, response: Option[JsValue]): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    when(mockHttp.GET[HttpResponse](urlMatcher)(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])).thenReturn(Future.successful(HttpResponse(status, response)))
  }

  def verifyMockHttpGet(url: Option[String] = None)(count: Int): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    verify(mockHttp, times(count)).GET[HttpResponse](urlMatcher)(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])
  }

  def setupMockHttpGetWithParams(url: Option[String], params: Option[Seq[(String, String)]])(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.eq(x))
    lazy val paramsMatcher = params.fold(ArgumentMatchers.any[Seq[(String, String)]]())(x => ArgumentMatchers.eq(x))
    when(mockHttp.GET[HttpResponse](
      urlMatcher, paramsMatcher)(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])
    ).thenReturn(Future.successful(HttpResponse(status, Some(response))))
  }

  def setupMockHttpPut[I](url: Option[String] = None, body: Option[I] = None)(status: Int, response: Option[JsValue]): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => ArgumentMatchers.startsWith(x))
    lazy val bodyMatcher = body.fold(ArgumentMatchers.any[I]())(x => ArgumentMatchers.eq(x))
    when(mockHttp.PUT[I, HttpResponse](urlMatcher, bodyMatcher
    )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])).thenReturn(Future.successful(HttpResponse(status, response)))
  }

}
