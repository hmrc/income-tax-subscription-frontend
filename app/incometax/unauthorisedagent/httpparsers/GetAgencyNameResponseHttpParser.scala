/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.unauthorisedagent.httpparsers

import core.utils.HttpParser
import incometax.unauthorisedagent.models.{GetAgencyNameFailure, GetAgencyNameSuccess}
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.HttpResponse

object GetAgencyNameResponseHttpParser {
  type GetAgencyNameResponse = Either[GetAgencyNameFailure, GetAgencyNameSuccess]

  implicit val getAgencyNameResponseHttpReads = HttpParser[GetAgencyNameResponse] {
    case HttpResponse(OK, json, _, _) =>
      json.validate[GetAgencyNameSuccess].fold(
        errors => Left(GetAgencyNameFailure(errors.toString)),
        success => Right(success)
      )
    case response =>
      Left(GetAgencyNameFailure(response.body))
  }
}
