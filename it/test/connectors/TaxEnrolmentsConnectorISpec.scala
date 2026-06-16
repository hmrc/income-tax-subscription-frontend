/*
 * Copyright 2026 HM Revenue & Customs
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

package connectors

import common.Constants
import common.Constants.GovernmentGateway.{NINO, ggFriendlyName}
import connectors.individual.TaxEnrolmentsConnector
import connectors.individual.httpparsers.AllocateEnrolmentResponseHttpParser.{EnrolFailure, EnrolSuccess}
import connectors.individual.httpparsers.UpsertEnrolmentResponseHttpParser.{KnownFactsFailure, KnownFactsSuccess}
import helpers.ComponentSpecBase
import helpers.servicemocks.TaxEnrolmentsStub
import models.common.subscription.{EmacEnrolmentRequest, EnrolmentKey, EnrolmentVerifiers}
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class TaxEnrolmentsConnectorISpec extends ComponentSpecBase {

  private lazy val connector: TaxEnrolmentsConnector = app.injector.instanceOf[TaxEnrolmentsConnector]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val groupId = "test-group-id"
  private val mtditid = "test-mtditid"
  private val nino = "AA123456A"
  private val credentialId = "test-cred-id"
  private val enrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, Constants.mtdItsaEnrolmentIdentifierKey -> mtditid)
  private val enrolmentVerifiers = EnrolmentVerifiers(NINO -> nino)
  private val enrolmentRequest = EmacEnrolmentRequest(credentialId, nino)
  private val upsertFailureBody = Json.obj("reason" -> "upsert failed")
  private val allocateFailureBody = Json.obj("reason" -> "allocate failed")

  "TaxEnrolmentsConnector.upsertEnrolment" should {

    "return success when tax-enrolments responds with NO_CONTENT" in {
      TaxEnrolmentsStub.stubUpsertEnrolmentResult(
        enrolmentKey = enrolmentKey.asString,
        status = NO_CONTENT,
        requestBody = Json.obj(
          "verifiers" -> Json.arr(
            Json.obj(
              "key" -> NINO,
              "value" -> nino
            )
          )
        ),
        responseBody = Json.obj()
      )

      connector.upsertEnrolment(enrolmentKey, enrolmentVerifiers).futureValue mustBe Right(KnownFactsSuccess)
    }

    "return failure when tax-enrolments responds with an error" in {
      TaxEnrolmentsStub.stubUpsertEnrolmentResult(
        enrolmentKey = enrolmentKey.asString,
        status = INTERNAL_SERVER_ERROR,
        requestBody = Json.obj(
          "verifiers" -> Json.arr(
            Json.obj(
              "key" -> NINO,
              "value" -> nino
            )
          )
        ),
        responseBody = upsertFailureBody
      )

      connector.upsertEnrolment(enrolmentKey, enrolmentVerifiers).futureValue mustBe Left(KnownFactsFailure(upsertFailureBody.toString))
    }
  }

  "TaxEnrolmentsConnector.allocateEnrolment" should {

    "send the full payload and return success" in {

      TaxEnrolmentsStub.stubAllocateEnrolmentResult(
        groupId = groupId,
        enrolmentKey = enrolmentKey.asString,
        status = CREATED,
        requestBody = Json.obj(
          "userId" -> credentialId,
          "friendlyName" -> ggFriendlyName,
          "type" -> "principal",
          "verifiers" -> Json.arr(
            Json.obj(
              "key" -> NINO,
              "value" -> nino
            )
          )
        )
      )

      connector.allocateEnrolment(groupId, enrolmentKey, enrolmentRequest).futureValue mustBe Right(EnrolSuccess)
    }

    "return failure when tax-enrolments responds with an error" in {
      TaxEnrolmentsStub.stubAllocateEnrolmentResult(
        groupId = groupId,
        enrolmentKey = enrolmentKey.asString,
        status = INTERNAL_SERVER_ERROR,
        requestBody = Json.obj(
          "userId" -> credentialId,
          "friendlyName" -> ggFriendlyName,
          "type" -> "principal",
          "verifiers" -> Json.arr(
            Json.obj(
              "key" -> NINO,
              "value" -> nino
            )
          )
        ),
        responseBody = allocateFailureBody
      )

      connector.allocateEnrolment(groupId, enrolmentKey, enrolmentRequest).futureValue mustBe Left(EnrolFailure(allocateFailureBody.toString))
    }
  }

  "TaxEnrolmentsConnector.adminAllocateEnrolment" should {

    "send the minimal payload and return success" in {
      TaxEnrolmentsStub.stubAllocateEnrolmentResult(
        groupId = groupId,
        enrolmentKey = enrolmentKey.asString,
        status = CREATED,
        requestBody = Json.obj(
          "userId" -> credentialId,
          "type" -> "principal",
          "action" -> "enrolAndActivate"
        )
      )

      connector.adminAllocateEnrolment(groupId, enrolmentKey, credentialId).futureValue mustBe Right(EnrolSuccess)
    }

    "return failure when tax-enrolments responds with an error" in {
      TaxEnrolmentsStub.stubAllocateEnrolmentResult(
        groupId = groupId,
        enrolmentKey = enrolmentKey.asString,
        status = INTERNAL_SERVER_ERROR,
        requestBody = Json.obj(
          "userId" -> credentialId,
          "type" -> "principal",
          "action" -> "enrolAndActivate"
        ),
        responseBody = allocateFailureBody
      )

      connector.adminAllocateEnrolment(groupId, enrolmentKey, credentialId).futureValue mustBe Left(EnrolFailure(allocateFailureBody.toString))
    }
  }
}





