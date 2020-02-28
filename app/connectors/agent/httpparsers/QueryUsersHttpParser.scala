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

package connectors.agent.httpparsers

import play.api.http.Status._
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object QueryUsersHttpParser {

  type QueryUsersResponse = Either[QueryUsersFailure, QueryUsersSuccess]

  val principalUserIdKey = "principalUserIds"

  implicit object QueryUsersHttpReads extends HttpReads[QueryUsersResponse] {
    override def read(method: String, url: String, response: HttpResponse): QueryUsersResponse =
      response.status match {
        case OK => (response.json \ principalUserIdKey).validate[Set[String]] match {
          case JsSuccess(userIds, _) => Right(UsersFound(userIds))
          case _ => Left(InvalidJson)
        }
        case NO_CONTENT => Right(NoUsersFound)
        case status => Left(EnrolmentStoreProxyConnectionFailure(status))
      }
  }

  sealed trait QueryUsersSuccess

  case class UsersFound(retrievedUserIds: Set[String]) extends QueryUsersSuccess

  case object NoUsersFound extends QueryUsersSuccess

  sealed trait QueryUsersFailure

  case object InvalidJson extends QueryUsersFailure

  case class EnrolmentStoreProxyConnectionFailure(status: Int) extends QueryUsersFailure

}