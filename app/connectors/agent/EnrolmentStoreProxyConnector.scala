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

package connectors.agent

import config.AppConfig
import connectors.agent.httpparsers.AllocateEnrolmentResponseHttpParser.AllocateEnrolmentResponse
import connectors.agent.httpparsers.AssignEnrolmentToUserHttpParser.AssignEnrolmentToUserResponse
import connectors.agent.httpparsers.EnrolmentStoreProxyHttpParser.EnrolmentStoreProxyResponse
import connectors.agent.httpparsers.QueryUsersHttpParser.QueryUsersResponse
import connectors.agent.httpparsers.UpsertEnrolmentResponseHttpParser.UpsertEnrolmentResponse
import models.common.subscription.EnrolmentKey
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentStoreProxyConnector @Inject()(http: HttpClientV2,
                                             appConfig: AppConfig)(implicit ec: ExecutionContext) {

  def getAllocatedEnrolments(enrolmentKey: EnrolmentKey)(implicit hc: HeaderCarrier): Future[EnrolmentStoreProxyResponse] = {
    val base = appConfig.getAllocatedEnrolmentUrl(enrolmentKey)
    http
      .get(url"$base?type=principal")
      .execute[EnrolmentStoreProxyResponse]
  }

  def getUserIds(utr: String)(implicit hc: HeaderCarrier): Future[QueryUsersResponse] = {
    http.get(url"${appConfig.queryUsersUrl(utr)}").execute[QueryUsersResponse]
  }

  def upsertEnrolment(mtditid: String,
                      nino: String)
                     (implicit hc: HeaderCarrier): Future[UpsertEnrolmentResponse] = {
    val enrolmentKey = s"HMRC-MTD-IT~MTDITID~$mtditid"

    val requestBody = Json.obj(
      "verifiers" -> Json.arr(
        Json.obj(
          "key" -> "NINO",
          "value" -> nino
        )
      )
    )
    http
      .put(url"${appConfig.upsertEnrolmentEnrolmentStoreUrl(enrolmentKey)}")
      .withBody(Json.toJson(requestBody))
      .execute[UpsertEnrolmentResponse]
  }

  def allocateEnrolmentWithoutKnownFacts(groupId: String,
                                         credentialId: String,
                                         mtditid: String
                                        )(implicit hc: HeaderCarrier): Future[AllocateEnrolmentResponse] = {
    val enrolmentKey = s"HMRC-MTD-IT~MTDITID~$mtditid"

    val requestBody = Json.obj(
      "userId" -> credentialId,
      "type" -> "principal",
      "action" -> "enrolAndActivate"
    )
    http
      .post(url"${appConfig.allocateEnrolmentEnrolmentStoreUrl(groupId, enrolmentKey)}")
      .withBody(Json.toJson(requestBody))
      .execute[AllocateEnrolmentResponse]
  }


  def assignEnrolment(credentialId: String,
                      mtdid: String
                     )(implicit hc: HeaderCarrier): Future[AssignEnrolmentToUserResponse] = {
    val enrolmentKey = s"HMRC-MTD-IT~MTDITID~$mtdid"

    http
      .post(url"${appConfig.assignEnrolmentUrl(credentialId, enrolmentKey)}")
      .execute[AssignEnrolmentToUserResponse]
  }
}

object EnrolmentStoreProxyConnector {
  val principalQueryKey: (String, String) = "type" -> "principal"

}
