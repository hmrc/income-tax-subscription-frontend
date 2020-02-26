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

package connectors.usermatching.httpparsers

import models.usermatching.{CitizenDetailsFailureResponse, CitizenDetailsSuccess}
import play.api.Logger
import play.api.http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object CitizenDetailsResponseHttpParser {
  type GetCitizenDetailsResponse = Either[CitizenDetailsFailureResponse, Option[CitizenDetailsSuccess]]

  implicit object GetCitizenDetailsHttpReads extends HttpReads[GetCitizenDetailsResponse] {
    override def read(method: String, url: String, response: HttpResponse): GetCitizenDetailsResponse = {
      response.status match {
        case OK =>
          Logger.debug(s"[CitizenDetailsResponseHttpParser][GetCitizenDetailsHttpReads] successful, returned $OK")
          Right(Some(response.json.as[CitizenDetailsSuccess]))
        case NOT_FOUND =>
          Logger.debug(s"[CitizenDetailsResponseHttpParser][GetCitizenDetailsHttpReads] successful, returned $NOT_FOUND")
          Right(None)
        case status =>
          Logger.warn(s"[CitizenDetailsResponseHttpParser][GetCitizenDetailsHttpReads] failure, status: $status")
          Left(CitizenDetailsFailureResponse(status))
      }
    }
  }

}
