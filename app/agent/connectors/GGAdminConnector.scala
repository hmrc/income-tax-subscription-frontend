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

package agent.connectors

import javax.inject.{Inject, Singleton}

import agent.audit.Logging
import agent.common.Constants
import core.config.AppConfig
import agent.connectors.GGAdminConnector._
import agent.connectors.models.gg.{KnownFactsFailure, KnownFactsRequest, KnownFactsSuccess}
import core.connectors.RawResponseReads
import play.api.http.Status.OK
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, HttpResponse}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future


@Singleton
class GGAdminConnector @Inject()(applicationConfig: AppConfig,
                                 httpPost: HttpPost,
                                 logging: Logging
                                ) extends RawResponseReads {

  private lazy val ggAdminUrl: String = applicationConfig.ggAdminURL

  lazy val addKnownFactsUrl: String = ggAdminUrl + addKnownFactsUri

  def addKnownFacts(knownFacts: KnownFactsRequest)(implicit hc: HeaderCarrier): Future[Either[KnownFactsFailure, KnownFactsSuccess.type]] = {
    lazy val requestDetails: Map[String, String] = Map("knownFacts" -> toJson(knownFacts).toString)
    logging.debug(s"Request:\n$requestDetails")

    httpPost.POST[KnownFactsRequest, HttpResponse](addKnownFactsUrl, knownFacts) map {
      case HttpResponse(OK, _, _, body) =>
        logging.debug("addKnownFacts responded with OK")
        Right(KnownFactsSuccess)
      case HttpResponse(status, _, _, body) =>
        logging.warn(s"addKnownFacts responded with an error: $status: $body")
        Left(KnownFactsFailure(body))
    }
  }

}

object GGAdminConnector {
  val addKnownFactsUri: String = s"/government-gateway-admin/service/${Constants.mtdItsaEnrolmentName}/known-facts"
}

