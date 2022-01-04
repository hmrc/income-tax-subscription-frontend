/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors.agent.httpparsers

import play.api.http.Status._
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object EnrolmentStoreProxyHttpParser {
  type EnrolmentStoreProxyResponse = Either[EnrolmentStoreFailure, EnrolmentStoreProxySuccess]

  implicit object EnrolmentStoreProxyHttpReads extends HttpReads[EnrolmentStoreProxyResponse] {
    override def read(method: String, url: String, response: HttpResponse): EnrolmentStoreProxyResponse =
      response.status match {
        case OK =>
          (response.json \ principalGroupIdKey \ 0).validate[String] match {
            case JsSuccess(groupId, _) => Right(EnrolmentAlreadyAllocated(groupId))
            case _ => Left(InvalidJsonResponse)
          }
        case NO_CONTENT => Right(EnrolmentNotAllocated)
        case status => Left(EnrolmentStoreProxyFailure(status))
      }
  }

  val principalGroupIdKey = "principalGroupIds"

  sealed trait EnrolmentStoreProxySuccess

  case class EnrolmentAlreadyAllocated(groupID: String) extends EnrolmentStoreProxySuccess

  case object EnrolmentNotAllocated extends EnrolmentStoreProxySuccess

  sealed trait EnrolmentStoreFailure

  case object InvalidJsonResponse extends EnrolmentStoreFailure

  case class EnrolmentStoreProxyFailure(status: Int) extends EnrolmentStoreFailure

}
