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

package connectors.usermatching.httpparsers

import connectors.agent.httpparsers.GetUsersForGroupHttpParser.CredentialRoleReads._
import connectors.agent.httpparsers.GetUsersForGroupHttpParser.GetUsersForGroupsHttpReads.read
import connectors.agent.httpparsers.GetUsersForGroupHttpParser.UserReads._
import connectors.agent.httpparsers.GetUsersForGroupHttpParser._
import org.scalatest.EitherValues
import play.api.libs.json._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{Assistant, CredentialRole, User}
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

class GetUsersForGroupsHttpParserSpec extends UnitTestTrait with EitherValues{
  val testMethod = "GET"
  val testUrl = "/"

  val testUsers: Map[String, CredentialRole] = Map(testCredentialId -> User, testCredentialId2 -> Assistant)

  "GetUsersForGroupHttpReads#read" when {
    s"the http status is $NON_AUTHORITATIVE_INFORMATION" when {
      "the json is valid" should {
        s"return ${UsersFound(testUsers)}" in {
          val testResponse = HttpResponse(
            status = NON_AUTHORITATIVE_INFORMATION,
            json = Json.arr(
              Json.obj(
                userIdKey -> testCredentialId,
                credentialRoleKey -> AdminKey
              ),
              Json.obj(
                userIdKey -> testCredentialId2,
                credentialRoleKey -> AssistantKey
              )
            ),
            Map.empty
          )

          read(testMethod, testUrl, testResponse) mustBe Right(UsersFound(testUsers))
        }
      }
      "the json is not valid" should {
        "return Invalid Json" in{
          val json = Json.obj()

          val testResponse = HttpResponse(
            status = NON_AUTHORITATIVE_INFORMATION,
            json = json,
            headers = Map.empty
          )

          read(testMethod, testUrl, testResponse) mustBe Left(InvalidJson)

        }
      }
    }


    "the http status is anything else" should {
      s"return ${UsersGroupsSearchConnectionFailure(INTERNAL_SERVER_ERROR)}" in {
        val testResponse = HttpResponse(INTERNAL_SERVER_ERROR, "")

        read(testMethod, testUrl, testResponse) mustBe Left(UsersGroupsSearchConnectionFailure(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
