/*
 * Copyright 2019 HM Revenue & Customs
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

package usermatching.httpparsers

import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import usermatching.models.{UserMatchFailureResponseModel, UserMatchSuccessResponseModel, UserMatchUnexpectedError}

object MatchUserHttpParser {
  type MatchUserResponse = Either[UserMatchFailureResponseModel, Option[UserMatchSuccessResponseModel]]

  implicit object MatchUserHttpReads extends HttpReads[MatchUserResponse] {
    override def read(method: String, url: String, response: HttpResponse): MatchUserResponse =
      response.status match {
        case OK => response.json.validate[UserMatchSuccessResponseModel] match {
          case JsSuccess(userDetails, _) =>
            Right(Some(userDetails))
          case error @ JsError(_) =>
            Left(UserMatchFailureResponseModel(error))
        }
        case UNAUTHORIZED =>
          response.json.validate[UserMatchFailureResponseModel] match {
            case JsSuccess(UserMatchUnexpectedError, _) =>
              Left(UserMatchUnexpectedError)
            case JsSuccess(_, _) =>
              Right(None)
            case error @ JsError(_) =>
              Left(UserMatchFailureResponseModel(error))
          }
        case _ =>
          Left(UserMatchFailureResponseModel(response))
      }
  }
}
