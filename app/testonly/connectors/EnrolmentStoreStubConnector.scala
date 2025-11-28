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

package testonly.connectors

import play.api.libs.json.{JsObject, Json}
import testonly.TestOnlyAppConfig
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreStubConnector @Inject()(appConfig: TestOnlyAppConfig,
                                            http: HttpClientV2)
                                           (implicit ec: ExecutionContext) {

  lazy val enrolmentStoreUrl: String = appConfig.enrolmentStoreStubUrl + "/enrolment-store-stub/data"

  def updateEnrolments(credId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.post(url"${enrolmentStoreUrl}").withBody(Json.toJson(updateEnrolmentsRequest(credId))).execute[HttpResponse]

  private def updateEnrolmentsRequest(credId: String): JsObject =
    Json.obj(
      "groupId" -> UUID.randomUUID().toString,
      "affinityGroup" -> "Organisation",
      "users" -> Json.arr(
        Json.obj(
          "credId" -> credId,
          "name" -> "Default User",
          "email" -> "test@test.com",
          "credentialRole" -> "Admin",
          "description" -> "User Description"
        )
      ),
      "enrolments" -> Json.arr(
        Json.obj(
          "serviceName" -> "HMRC-MTD-IT",
          "identifiers" -> Json.arr(
            Json.obj(
              "key" -> "MTDITID",
              "value" -> "XAIT000000000005"
            )
          ),
          "enrolmentFriendlyName" -> "MTD IT",
          "assignedUserCreds" -> Json.arr(credId),
          "state" -> "Activated",
          "enrolmentType" -> "principal",
          "assignedToAll" -> false
        )
      )
    )
}
