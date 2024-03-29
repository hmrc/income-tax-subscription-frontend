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

package connectors.individual.eligibility.httpparsers

import models.EligibilityStatus
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.HttpResponse
import utilities.HttpResult.{HttpConnectorError, HttpResult, HttpResultParser}

object GetEligibilityStatusHttpParser {

  implicit object GetEligibilityStatusHttpReads extends HttpResultParser[EligibilityStatus] {
    override def read(method: String, url: String, response: HttpResponse): HttpResult[EligibilityStatus] = response.status match {
      case OK => response.json.validate[EligibilityStatus] match {
        case error: JsError => Left(HttpConnectorError(response, Some(error)))
        case JsSuccess(result, _) => Right(result)
      }
      case _ => Left(HttpConnectorError(response))
    }
  }
}
