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
import connectors.httpparser.GetSessionDataHttpParser.GetSessionDataResponse
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionDataConnector @Inject()(appConfig: AppConfig,
                                     http: HttpClient)
                                    (implicit ec: ExecutionContext) {

  private def sessionDataUrl(id: String): String = {
    appConfig.microServiceUrl + s"/income-tax-subscription/session-data/id/$id"
  }

  def getSessionData[T](id: String)(implicit hc: HeaderCarrier, reads: Reads[T]): Future[GetSessionDataResponse[T]] = {
    http.GET[GetSessionDataResponse[T]](sessionDataUrl(id))
  }

}
