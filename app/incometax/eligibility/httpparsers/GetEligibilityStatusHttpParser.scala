/*
 * Copyright 2019 HM Revenue & Customs
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

package incometax.eligibility.httpparsers

import core.utils.HttpResult.{HttpConnectorError, HttpResult, HttpResultParser}
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.HttpResponse

object GetEligibilityStatusHttpParser {

  val key = "eligible"

  implicit object GetEligibilityStatusHttpReads extends HttpResultParser[Boolean] {
    override def read(method: String, url: String, response: HttpResponse): HttpResult[Boolean] = {

      response.status match {
        case OK => (response.json \ key).validate[Boolean] match {
          case JsSuccess(eligibilityStatus, _) => Right(eligibilityStatus)
          case error: JsError => Left(HttpConnectorError(response, Some(error)))
        }
        case _ => Left(HttpConnectorError(response))
      }
    }

  }

}