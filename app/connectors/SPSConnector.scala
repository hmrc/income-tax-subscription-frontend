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

package connectors

import config.AppConfig
import models.sps.SPSPayload
import play.api.Logging
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SPSConnector @Inject()(appConfig: AppConfig,
                             http: HttpClient)
                            (implicit ec: ExecutionContext) extends Logging {

  def postSpsConfirm(entityId: String, itsaId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.POST[JsValue, HttpResponse](
      appConfig.channelPreferencesUrl + s"/channel-preferences/confirm",
      Json.toJson(SPSPayload(entityId, itsaId))
    ).map(_ => ()).recover { _ =>
      logger.warn("[SPSConnector][postSpsConfirm] - Failure when confirming sps preference")
      ()
    }
  }

}
