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

package connectors.httpparser

import models.common.subscription.SignUpFailureResponse.{InvalidJson, UnexpectedStatus, UnprocessableSignUp}
import models.common.subscription.{SignUpFailureResponse, SignUpSuccessful}
import play.api.Logging
import play.api.http.Status.{OK, UNPROCESSABLE_ENTITY}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object SignUpResponseHttpParser extends Logging {

  type SignUpResponse = Either[SignUpFailureResponse, SignUpSuccessful]

  implicit object SignUpResponseHttpReads extends HttpReads[SignUpResponse] {
    override def read(method: String, url: String, response: HttpResponse): SignUpResponse = {
      response.status match {
        case OK =>
          response.json.validate[SignUpSuccessful] match {
            case JsSuccess(successResponse, _) => Right(successResponse)
            case JsError(errors) =>
              logger.error(s"[SignUpResponseHttpReads] - Unexpected json returned from sign up API, Status: $OK, Errors: $errors")
              Left(InvalidJson)
          }
        case UNPROCESSABLE_ENTITY =>
          response.json.validate[UnprocessableSignUp] match {
            case JsSuccess(unprocessableResponse, _) =>
              Left(unprocessableResponse)
            case JsError(errors) =>
              logger.error(s"[SignUpResponseHttpReads] - Unexpected json returned from sign up API, Status: $UNPROCESSABLE_ENTITY, Errors: $errors")
              Left(InvalidJson)
          }
        case status =>
          logger.error(s"[SignUpResponseHttpReads] - Unexpected status returned from sign up: $status")
          Left(UnexpectedStatus(status))
      }
    }
  }

}
