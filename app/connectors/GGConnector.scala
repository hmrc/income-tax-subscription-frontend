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

package connectors

import javax.inject.Inject

import _root_.utils.JsonUtils._
import audit.Logging
import config.AppConfig
import connectors.GGConnector._
import connectors.models.gg.{EnrolFailure, EnrolRequest, EnrolResponse, EnrolSuccess}
import play.api.http.Status.OK
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class GGConnector @Inject()(httpPost: HttpPost,
                            applicationConfig: AppConfig,
                            logging: Logging
                           ) extends RawResponseReads {

  lazy val governmentGatewayURL = applicationConfig.ggURL

  val enrolUrl: String = governmentGatewayURL + enrolUri

  def enrol(enrolmentRequest: EnrolRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[EnrolFailure, EnrolSuccess.type]] = {

    lazy val requestDetails: Map[String, String] = Map("enrolRequest" -> (enrolmentRequest: JsValue).toString)
    logging.debug(s"Request:\n$requestDetails")

    httpPost.POST[EnrolRequest, HttpResponse](enrolUrl, enrolmentRequest) map { response =>
      response.status match {
        case OK =>
          logging.info(s"GG enrol responded with OK")
          Right(EnrolSuccess)
        case status =>
          logging.warn(s"GG enrol responded with an error, status=$status body=${response.body}")
          Left(EnrolFailure(response.body))
      }
    }

  }
}

object GGConnector {
  val enrolUri = "/enrol"
}