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

import play.api.data.validation.ValidationError
import play.api.http.Status.NON_AUTHORITATIVE_INFORMATION
import play.api.libs.json._
import uk.gov.hmrc.auth.core.{Assistant, CredentialRole, User}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object GetUsersForGroupHttpParser {

  type GetUsersForGroupResponse = Either[GetUsersForGroupFailure, GetUsersForGroupSuccess]

  implicit object CredentialRoleReads extends Reads[CredentialRole] {
    val AdminKey = "Admin"
    val AssistantKey = "Assistant"

    override def reads(json: JsValue): JsResult[CredentialRole] =
      json.validate[String].collect(ValidationError(Seq("Invalid credential role"), Nil)){
        case AdminKey => User
        case AssistantKey => Assistant
      }
  }

  implicit object UserReads extends Reads[(String, CredentialRole)] {
    val userIdKey = "userId"
    val credentialRoleKey = "credentialRole"

    override def reads(json: JsValue): JsResult[(String, CredentialRole)] =
      for {
        userId <- (json \ userIdKey).validate[String]
        credentialRole <- (json \ credentialRoleKey).validate[CredentialRole]
      } yield (userId, credentialRole)
  }

  implicit object GetUsersForGroupsHttpReads extends HttpReads[GetUsersForGroupResponse] {
    override def read(method: String, url: String, response: HttpResponse): GetUsersForGroupResponse =
      response.status match {
        case NON_AUTHORITATIVE_INFORMATION => response.json.validate[Seq[(String, CredentialRole)]] match {
          case JsSuccess(users, _) => Right(UsersFound(users.toMap))
          case JsError(_) => Left(InvalidJson)
        }
        case status => Left(UsersGroupsSearchConnectionFailure(status))
      }
  }

  sealed trait GetUsersForGroupSuccess

  case class UsersFound(retrievedUserIds: Map[String, CredentialRole]) extends GetUsersForGroupSuccess

  sealed trait GetUsersForGroupFailure

  case object InvalidJson extends GetUsersForGroupFailure

  case class UsersGroupsSearchConnectionFailure(status: Int) extends GetUsersForGroupFailure

}
