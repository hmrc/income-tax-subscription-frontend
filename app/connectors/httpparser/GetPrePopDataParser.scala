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
import models.prepop.PrePopData
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object GetPrePopDataParser {

  type GetPrePopDataResponse = Either[ErrorModel, PrePopData]

  implicit val getPrePopDataResponseHttpReads: HttpReads[GetPrePopDataResponse] =
    (_: String, _: String, response: HttpResponse) => {
      response.status match {
        case OK => response.json.validate[PrePopData] match {
          case JsSuccess(value, _) => Right(value)
          case JsError(_) =>
            Left(ErrorModel(OK, s"Unable to parse json received into a PrePopData model"))
        }
        case status => Left(ErrorModel(status, response.body))
      }
    }
}
