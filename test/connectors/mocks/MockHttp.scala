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

import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.http.{HttpGet, HttpPost, HttpResponse}
import utils.MockTrait

import scala.concurrent.Future


trait MockHttp extends MockTrait {

  val mockHttpPost = mock[HttpPost]
  val mockHttpGet = mock[HttpGet]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpPost)
    reset(mockHttpGet)
  }

  def setupMockHttpPost[I](url: Option[String] = None, body: Option[I] = None)(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(Matchers.any[String]())(x => Matchers.eq(x))
    lazy val bodyMatcher = body.fold(Matchers.any[I]())(x => Matchers.eq(x))
    when(mockHttpPost.POST[I, HttpResponse](urlMatcher, bodyMatcher, Matchers.any()
    )(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(status, Some(response))))
  }

  def setupMockHttpPostEmpty(url: Option[String] = None)(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(Matchers.any[String]())(x => Matchers.eq(x))
    when(mockHttpPost.POSTEmpty[HttpResponse](urlMatcher)(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(status, Some(response))))
  }

  def setupMockHttpGet(url: Option[String] = None)(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(Matchers.any[String]())(x => Matchers.eq(x))

    when(mockHttpGet.GET[HttpResponse](urlMatcher)(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(status, Some(response))))
  }

  def setupMockHttpGetWithParams(url: Option[String], params: Option[Seq[(String, String)]])(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(Matchers.any[String]())(x => Matchers.eq(x))
    lazy val paramsMatcher = params.fold(Matchers.any[Seq[(String, String)]]())(x => Matchers.eq(x))
    when(mockHttpGet.GET[HttpResponse](urlMatcher, paramsMatcher)(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(status, Some(response))))
  }
}
