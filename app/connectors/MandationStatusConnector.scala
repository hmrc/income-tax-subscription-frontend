/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.httpparser.PostMandationStatusParser.{PostMandationStatusResponse, mandationStatusResponseHttpReads}
import models.status.MandationStatusRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MandationStatusConnector @Inject()(appConfig: AppConfig, httpClient: HttpClient)
                                        (implicit ec: ExecutionContext) {

  def getMandationStatus(nino: String, utr: String)
                        (implicit hc: HeaderCarrier): Future[PostMandationStatusResponse] = {
    val requestBody = MandationStatusRequest(nino, utr)

    httpClient.POST[MandationStatusRequest, PostMandationStatusResponse](
      url = appConfig.mandationStatusUrl,
      body = requestBody
    )
  }

}
