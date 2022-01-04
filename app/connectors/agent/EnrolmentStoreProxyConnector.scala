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

package connectors.agent

import config.AppConfig
import connectors.agent.EnrolmentStoreProxyConnector.principalQueryKey
import connectors.agent.httpparsers.AllocateEnrolmentResponseHttpParser.AllocateEnrolmentResponse
import connectors.agent.httpparsers.AssignEnrolmentToUserHttpParser.AssignEnrolmentToUserResponse
import connectors.agent.httpparsers.EnrolmentStoreProxyHttpParser.EnrolmentStoreProxyResponse
import connectors.agent.httpparsers.QueryUsersHttpParser.QueryUsersResponse
import connectors.agent.httpparsers.UpsertEnrolmentResponseHttpParser.UpsertEnrolmentResponse
import models.common.subscription.EnrolmentKey

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentStoreProxyConnector @Inject()(http: HttpClient,
                                             appConfig: AppConfig)(implicit ec: ExecutionContext) {

  def getAllocatedEnrolments(enrolmentKey: EnrolmentKey)(implicit hc: HeaderCarrier): Future[EnrolmentStoreProxyResponse] = {
    http.GET[EnrolmentStoreProxyResponse](
      url = appConfig.getAllocatedEnrolmentUrl(enrolmentKey),
      queryParams = Seq(principalQueryKey)
    )
  }

  def getUserIds(utr: String)(implicit hc: HeaderCarrier): Future[QueryUsersResponse] = {
    http.GET[QueryUsersResponse](
      url = appConfig.queryUsersUrl(utr)
    )
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
    http.PUT[JsObject, UpsertEnrolmentResponse](
      url = appConfig.upsertEnrolmentEnrolmentStoreUrl(enrolmentKey),
      body = requestBody
    )

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
    http.POST[JsObject, AllocateEnrolmentResponse](
      url = appConfig.allocateEnrolmentEnrolmentStoreUrl(groupId, enrolmentKey),
      body = requestBody
    )
  }


  def assignEnrolment(credentialId: String,
                      mtdid: String
                     )(implicit hc: HeaderCarrier): Future[AssignEnrolmentToUserResponse] = {
    val enrolmentKey = s"HMRC-MTD-IT~MTDITID~$mtdid"

    http.POSTEmpty[AssignEnrolmentToUserResponse](
      url = appConfig.assignEnrolmentUrl(credentialId, enrolmentKey)
    )
  }
}

object EnrolmentStoreProxyConnector {
  val principalQueryKey: (String, String) = "type" -> "principal"

}
