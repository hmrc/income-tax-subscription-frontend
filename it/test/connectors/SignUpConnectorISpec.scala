/*
 * Copyright 2025 HM Revenue & Customs
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

import connectors.stubs.SignUpAPIStub
import helpers.ComponentSpecBase
import models.common.subscription.{SignUpFailureResponse, SignUpRequestModel, SignUpSuccessResponse}
import models.{Current, Next}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, UNPROCESSABLE_ENTITY}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier

class SignUpConnectorISpec extends ComponentSpecBase {

  "signUp" when {
    "an OK status response is received" should {
      "return a sign up success response" when {
        "signing up for the current year" in {
          SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current))(
            status = OK,
            json = Json.obj("mtdbsa" -> mtdbsa)
          )

          val result = connector.signUp(nino, utr, Current)

          await(result) mustBe Right(SignUpSuccessResponse.SignUpSuccessful(mtdbsa))
        }
        "signing up for the next year" in {
          SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Next))(
            status = OK,
            json = Json.obj("mtdbsa" -> mtdbsa)
          )

          val result = connector.signUp(nino, utr, Next)

          await(result) mustBe Right(SignUpSuccessResponse.SignUpSuccessful(mtdbsa))
        }
      }
      "return an invalid json failure response" when {
        "the response body could not be parsed" in {
          SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current))(
            status = OK
          )

          val result = connector.signUp(nino, utr, Current)

          await(result) mustBe Left(SignUpFailureResponse.InvalidJson)
        }
      }
    }
    "an UNPROCESSABLE_ENTITY status response is received" should {
      "return an already signed up response" in {
        SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current))(
          status = UNPROCESSABLE_ENTITY
        )

        val result = connector.signUp(nino, utr, Current)

        await(result) mustBe Right(SignUpSuccessResponse.AlreadySignedUp)
      }
    }
    "an unhandled status is received" should {
      "return an unexpected status failure response" in {
        SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current))(
          status = INTERNAL_SERVER_ERROR
        )

        val result = connector.signUp(nino, utr, Current)

        await(result) mustBe Left(SignUpFailureResponse.UnexpectedStatus(INTERNAL_SERVER_ERROR))
      }
    }
  }

  lazy val connector: SignUpConnector = app.injector.instanceOf[SignUpConnector]
  lazy val nino: String = "test-nino"
  lazy val utr: String = "test-utr"
  lazy val mtdbsa: String = "test-mtdbsa"

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

}
