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

package helpers.servicemocks

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsValue, Json}
import helpers.IntegrationTestConstants._
import connectors.agent.httpparsers.EnrolmentStoreProxyHttpParser.principalGroupIdKey
import connectors.agent.httpparsers.QueryUsersHttpParser.principalUserIdKey

object EnrolmentStoreProxyStub extends WireMockMethods {

  val enrolmentStoreProxyUri = "/enrolment-store-proxy/enrolment-store"

  def jsonResponseBody(idKey: String, idValues: String*): JsValue =
    Json.obj(
      idKey -> idValues
    )

  def stubGetUserIds(utr: String)(status: Int, body: JsValue = Json.obj()): StubMapping = {
    when(method = GET, uri = s"$enrolmentStoreProxyUri/enrolments/IR-SA~UTR~$utr/users")
      .thenReturn(status = status, body = body)
  }

  def stubGetAllocatedLegacyITEnrolmentStatus(utr: String)(status: Int): StubMapping = {
    when(method = GET, uri = s"$enrolmentStoreProxyUri/enrolments/IR-SA~UTR~$utr/groups\\?type=principal")
      .thenReturn(status = status, body = jsonResponseBody(principalGroupIdKey, testGroupId))
  }

  private def upsertEnrolmentUrl(enrolmentKey: String) =
    s"$enrolmentStoreProxyUri/enrolments/$enrolmentKey"

  private def allocateEnrolmentUrl(groupId: String, enrolmentKey: String) =
    s"$enrolmentStoreProxyUri/groups/$groupId/enrolments/$enrolmentKey"

  private def assignEnrolmentUrl(userId: String, enrolmentKey: String) =
    s"$enrolmentStoreProxyUri/users/$userId/enrolments/$enrolmentKey"

  def stubAllocateEnrolmentWithoutKnownFacts(mtdid: String, groupId: String, credentialId: String)(status: Int): Unit = {
    val allocateEnrolmentJsonBody = Json.obj(
      "userId" -> credentialId,
      "type" -> "principal",
      "action" -> "enrolAndActivate"
    )

    val enrolmentKey = s"HMRC-MTD-IT~MTDITID~$mtdid"

    when(
      method = POST,
      uri = allocateEnrolmentUrl(
        groupId = groupId,
        enrolmentKey = enrolmentKey
      ),
      body = allocateEnrolmentJsonBody
    ) thenReturn status
  }

  def verifyAllocateEnrolmentWithoutKnownFacts(mtdid: String, groupId: String, credentialId: String): Unit = {
    val allocateEnrolmentJsonBody = Json.obj(
      "userId" -> credentialId,
      "type" -> "principal",
      "action" -> "enrolAndActivate"
    )

    val enrolmentKey = s"HMRC-MTD-IT~MTDITID~$mtdid"

    verify(
      method = POST,
      uri = allocateEnrolmentUrl(
        groupId = groupId,
        enrolmentKey = enrolmentKey
      ),
      body = allocateEnrolmentJsonBody)
  }

  def stubAssignEnrolment(mtdid: String, userId: String)(status: Int): Unit = {
    val enrolmentKey = s"HMRC-MTD-IT~MTDITID~$mtdid"

    when(
      method = POST,
      uri = assignEnrolmentUrl(
        userId = userId,
        enrolmentKey = enrolmentKey
      )
    ) thenReturn status
  }

  def verifyAssignEnrolment(mtdid: String, userId: String): Unit = {
    val enrolmentKey = s"HMRC-MTD-IT~MTDITID~$mtdid"

    verify(
      method = POST,
      uri = assignEnrolmentUrl(
        userId = userId,
        enrolmentKey = enrolmentKey
      )
    )
  }

  def stubUpsertEnrolment(mtdid: String, nino: String)(status: Int): Unit = {
    val allocateEnrolmentJsonBody = Json.obj(
      "verifiers" -> Json.arr(
        Json.obj(
          "key" -> "NINO",
          "value" -> nino
        )
      )
    )

    val enrolmentKey = s"HMRC-MTD-IT~MTDITID~$mtdid"

    when(
      method = PUT,
      uri = upsertEnrolmentUrl(
        enrolmentKey = enrolmentKey
      ),
      body = allocateEnrolmentJsonBody
    ) thenReturn status
  }

  def verifyUpsertEnrolment(mtdid: String, nino: String): Unit = {
    val allocateEnrolmentJsonBody = Json.obj(
      "verifiers" -> Json.arr(
        Json.obj(
          "key" -> "NINO",
          "value" -> nino
        )
      )
    )

    val enrolmentKey = s"HMRC-MTD-IT~MTDITID~$mtdid"

    verify(
      method = PUT,
      uri = upsertEnrolmentUrl(
        enrolmentKey = enrolmentKey
      ),
      body = allocateEnrolmentJsonBody
    )

  }

}

