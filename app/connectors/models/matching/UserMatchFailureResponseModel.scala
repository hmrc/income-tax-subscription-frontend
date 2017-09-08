/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors.models.matching

import play.api.libs.json.{JsError, Json}
import uk.gov.hmrc.play.http.HttpResponse

// the response from authenticator/match with message to indicate why matching failed
case class UserMatchFailureResponseModel(errors: String)

object UserMatchUnexpectedError extends UserMatchFailureResponseModel("Internal error: unexpected result from matching")

object UserMatchFailureResponseModel {
  implicit val format = Json.format[UserMatchFailureResponseModel]

  def apply(response: HttpResponse): UserMatchFailureResponseModel =
    UserMatchFailureResponseModel(s"status: ${response.status} body: ${response.body}")

  def apply(jsError: JsError): UserMatchFailureResponseModel = UserMatchFailureResponseModel(jsError.errors.toString)
}
