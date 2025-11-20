/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors

import config.AppConfig
import connectors.httpparser.DeleteSessionDataHttpParser.DeleteSessionDataResponse
import connectors.httpparser.GetSessionDataHttpParser.GetSessionDataResponse
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataResponse
import play.api.libs.json._
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionDataConnector @Inject()(appConfig: AppConfig,
                                     http: HttpClientV2)
                                    (implicit ec: ExecutionContext) {

  private def sessionDataUrl(id: String): String = {
    appConfig.microServiceUrl + s"/income-tax-subscription/session-data/id/$id"
  }

  private def sessionIdDataUrl(): String = {
    appConfig.microServiceUrl + s"/income-tax-subscription/session-data/id"
  }

  private def allSessionDataUrl(): String = {
    s"${appConfig.microServiceUrl}/income-tax-subscription/session-data/all"
  }

  def getSessionData[T](id: String)(implicit hc: HeaderCarrier, reads: Reads[T]): Future[GetSessionDataResponse[T]] = {
    http.get(url"${sessionDataUrl(id)}").execute[GetSessionDataResponse[T]]
  }

  def getAllSessionData()(implicit hc: HeaderCarrier, reads: Reads[Map[String, JsValue]]): Future[GetSessionDataResponse[Map[String, JsValue]]] = {
    http.get(url"${allSessionDataUrl()}").execute[GetSessionDataResponse[Map[String, JsValue]]]
  }

  def saveSessionData[T](id: String, data: T)(implicit hc: HeaderCarrier, writes: Writes[T]): Future[SaveSessionDataResponse] = {
    http.post(url"${sessionDataUrl(id)}").withBody(Json.toJson(data)).execute[SaveSessionDataResponse]
  }

  def deleteSessionData(id: String)(implicit hc: HeaderCarrier): Future[DeleteSessionDataResponse] = {
    http.delete(url"${sessionDataUrl(id)}").execute[DeleteSessionDataResponse]
  }

  def deleteAllSessionData(implicit hc: HeaderCarrier): Future[DeleteSessionDataResponse] = {
    http.delete(url"${sessionIdDataUrl()}").execute[DeleteSessionDataResponse]
  }

}
