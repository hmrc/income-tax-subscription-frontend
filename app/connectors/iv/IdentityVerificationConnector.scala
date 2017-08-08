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

package connectors.iv

import javax.inject.{Inject, Singleton}

import audit.Logging
import config.AppConfig
import connectors.RawResponseReads
import connectors.models.iv.{IVFailure, IVJourneyResult, IVJourneyResultModel}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, InternalServerException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IdentityVerificationConnector @Inject()(appConfig: AppConfig,
                                              httpGet: HttpGet,
                                              logging: Logging
                                             ) extends RawResponseReads {

  def journeyResultUrl(journeyId: String): String =
    appConfig.identityVerificationURL + IdentityVerificationConnector.journeyResultUri(journeyId)

  def getJourneyResult(journeyId: String)(implicit hc: HeaderCarrier): Future[IVJourneyResult] = {
    httpGet.GET(journeyResultUrl(journeyId)).flatMap { response =>
      response.status match {
        case OK =>
          logging.debug(s"IdentityVerificationConnector.getJourneyResult responded with body=${response.body}")
          Future.successful(IVJourneyResult(Json.fromJson[IVJourneyResultModel](response.json).get))
        case NOT_FOUND =>
          logging.debug(s"IdentityVerificationConnector.getJourneyResult responded with not found, body=${response.body}")
          Future.successful(IVFailure)
        case status =>
          logging.warn(s"IdentityVerificationConnector.getJourneyResult responded with an unexpected status=$status body=${response.body}")
          Future.failed(new InternalServerException(s"IdentityVerificationConnector.getJourneyResult responded with an unexpected status=$status body=${response.body}"))
      }
    }
  }

}


object IdentityVerificationConnector {

  def journeyResultUri(journeyId: String) = s"/mdtp/journey/journeyId/$journeyId"

}