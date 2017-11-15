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

package agent.connectors.httpparsers

import play.api.http.HttpVerbs
import play.api.http.Status._
import play.api.libs.json.{JsSuccess, JsValue}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import usermatching.models._

object LockoutStatusHttpParser {
  type LockoutStatusResponse = Either[LockoutStatusFailure, LockoutStatus]

  implicit object LockoutStatusHttpReads extends HttpReads[LockoutStatusResponse] {
    override def read(method: String, url: String, response: HttpResponse): LockoutStatusResponse =
      method match {
        case HttpVerbs.GET =>
          response.status match {
            case NOT_FOUND => Right(NotLockedOut)
            case OK => parse(response.json)
            case status => Left(LockoutStatusFailureResponse(status))
          }
        case HttpVerbs.POST =>
          response.status match {
            case CREATED => parse(response.json)
            case status => Left(LockoutStatusFailureResponse(status))
          }
      }
  }

  private def parse(body: JsValue): LockoutStatusResponse = {
    body.validate[LockedOut] match {
      case JsSuccess(l, _) => Right(l)
      case _ => Left(BadlyFormattedLockedStatusResponse)
    }
  }

}
