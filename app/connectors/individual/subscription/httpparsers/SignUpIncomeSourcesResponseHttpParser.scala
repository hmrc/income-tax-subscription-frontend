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

package connectors.individual.subscription.httpparsers

import models.common.subscription.SignUpSourcesFailure.{BadlyFormattedSignUpIncomeSourcesResponse, SignUpIncomeSourcesFailureResponse}
import models.common.subscription.SignUpSuccessResponse.{AlreadySignedUp, SignUpSuccessful}
import models.common.subscription.{SignUpIncomeSourcesFailure, SignUpSuccessResponse}
import play.api.http.Status.{OK, UNPROCESSABLE_ENTITY}
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object SignUpIncomeSourcesResponseHttpParser {

  type PostSignUpIncomeSourcesResponse = Either[SignUpIncomeSourcesFailure, SignUpSuccessResponse]

  implicit object PostMultipleIncomeSourcesSignUpResponseHttpReads extends HttpReads[PostSignUpIncomeSourcesResponse] {
    override def read(method: String, url: String, response: HttpResponse): PostSignUpIncomeSourcesResponse = {
      response.status match {
        case OK =>
          response.json.validate[SignUpSuccessful] match {
            case JsSuccess(successResponse, _) => Right(successResponse)
            case _ => Left(BadlyFormattedSignUpIncomeSourcesResponse)
          }
        case UNPROCESSABLE_ENTITY => Right(AlreadySignedUp)
        case status => Left(SignUpIncomeSourcesFailureResponse(status))
      }
    }
  }
}
