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

package incometax.subscription.connectors

import javax.inject.{Inject, Singleton}

import core.connectors.RawResponseReads
import core.Constants._
import core.audit.Logging
import core.config.AppConfig
import incometax.subscription.connectors.GGAdminConnector._
import incometax.subscription.models.{KnownFactsFailure, KnownFactsRequest, KnownFactsSuccess}
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

  val addKnownFactsUrl: String = ggAdminUrl + addKnownFactsUri

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
  val addKnownFactsUri: String = s"/government-gateway-admin/service/${GovernmentGateway.ggServiceName}/known-facts"
}
