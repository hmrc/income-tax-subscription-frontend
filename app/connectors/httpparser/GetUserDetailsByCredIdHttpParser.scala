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

package connectors.httpparser

import models.individual.ObfuscatedIdentifier
import play.api.http.Status.NON_AUTHORITATIVE_INFORMATION
import play.api.libs.json.*
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object GetUserDetailsByCredIdHttpParser {

  type GetUserDetailsByCredIdResponse = Either[GetUserDetailsByCredIdFailure, ObfuscatedIdentifier]

  implicit object GetUsersForGroupsHttpReads extends HttpReads[GetUserDetailsByCredIdResponse] {
    override def read(method: String, url: String, response: HttpResponse): GetUserDetailsByCredIdResponse =
      response.status match {
        case NON_AUTHORITATIVE_INFORMATION => response.json.validate[ObfuscatedIdentifier] match {
          case JsSuccess(userDetails, _) => Right(userDetails)
          case JsError(_) => Left(InvalidJson)
        }
        case status => Left(UnexpectedStatus(status))
      }
  }

  sealed trait GetUserDetailsByCredIdFailure

  case object InvalidJson extends GetUserDetailsByCredIdFailure

  case class UnexpectedStatus(status: Int) extends GetUserDetailsByCredIdFailure

}
