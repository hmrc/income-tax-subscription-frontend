/*
 * Copyright 2020 HM Revenue & Customs
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

package usermatching.connectors

import javax.inject.{Inject, Singleton}

import core.audit.Logging
import core.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import usermatching.httpparsers.CitizenDetailsResponseHttpParser._
import usermatching.models.CitizenDetailsFailureResponse

import scala.concurrent.Future

@Singleton
class CitizenDetailsConnector @Inject()(appConfig: AppConfig,
                                        http: HttpClient,
                                        logging: Logging) {

  def lookupUtrUrl(nino: String): String = appConfig.citizenDetailsURL + CitizenDetailsConnector.lookupUtrUri(nino)

  def lookupNinoUrl(utr: String): String = appConfig.citizenDetailsURL + CitizenDetailsConnector.lookupNinoUri(utr)

  def lookupUtr(nino: String)(implicit hc: HeaderCarrier): Future[GetCitizenDetailsResponse] =
    http.GET[GetCitizenDetailsResponse](lookupUtrUrl(nino)).map {
      case r@Right(Some(success)) =>
        logging.debug("CitizenDetailsConnector.lookupUtr successful, returned OK")
        r
      case r@Right(None) =>
        logging.debug("CitizenDetailsConnector.lookupUtr successful, returned Not Found")
        r
      case l@Left(CitizenDetailsFailureResponse(status)) =>
        logging.warn("CitizenDetailsConnector.lookupUtr failure, status=" + status)
        l
    }

  def lookupNino(utr: String)(implicit hc: HeaderCarrier): Future[GetCitizenDetailsResponse] =
    http.GET[GetCitizenDetailsResponse](lookupNinoUrl(utr)).map {
      case r@Right(Some(success)) =>
        logging.debug("CitizenDetailsConnector.lookupNino successful, returned OK")
        r
      case r@Right(None) =>
        logging.debug("CitizenDetailsConnector.lookupNino successful, returned Not Found")
        r
      case l@Left(CitizenDetailsFailureResponse(status)) =>
        logging.warn("CitizenDetailsConnector.lookupNino failure, status=" + status)
        l
    }


}

object CitizenDetailsConnector {
  def lookupUtrUri(nino: String): String = s"/citizen-details/nino/$nino"

  def lookupNinoUri(utr: String): String = s"/citizen-details/sautr/$utr"
}
