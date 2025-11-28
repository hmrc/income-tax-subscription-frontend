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

package connectors.agent

import config.AppConfig
import models.sps.AgentSPSPayload
import play.api.Logging
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentSPSConnector @Inject()(appConfig: AppConfig,
                                  http: HttpClientV2)
                                 (implicit ec: ExecutionContext) extends Logging {

  def postSpsConfirm(arn: String, nino: String, sautr: String, itsaId: String)(implicit hc: HeaderCarrier): Future[Unit] = {

    val enrolmentIRSA = s"IR-SA~UTR~$sautr"
    val enrolmentMTDITID = s"HMRC-MTD-IT~MTDITID~$itsaId"

    http
      .post(url"${appConfig.channelPreferencesUrl + s"/channel-preferences/enrolment"}")
      .withBody(Json.toJson(AgentSPSPayload(arn, nino, enrolmentIRSA, enrolmentMTDITID)))
      .execute[HttpResponse]
      .map(_ => ()).recover { _ =>
        logger.warn("[AgentSPSConnector][postSpsConfirm] - Failure when confirming sps preference")
        ()
      }
  }

}




