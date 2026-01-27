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

import models.ErrorModel
import models.status.GetITSAStatusModel
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object GetITSAStatusParser {
  type GetITSAStatusResponse = Either[ErrorModel, GetITSAStatusModel]

  implicit val getITSAStatusResponseHttpReads: HttpReads[GetITSAStatusResponse] =
    (_: String, _: String, response: HttpResponse) => {
      response.status match {
        case OK => response.json.validate[GetITSAStatusModel] match {
          case JsSuccess(value, _) => Right(value)
          case JsError(errors) =>
            Left(ErrorModel(OK, s"Invalid Json for getITSAStatusResponseHttpReads: $errors"))
        }
        case status => Left(ErrorModel(status, response.body))
      }
    }
}
