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

package models.common.subscription

import models.ConnectorError
import play.api.libs.json.{Json, OFormat}

sealed trait SignUpSuccessResponse

object SignUpSuccessResponse {

  case class SignUpSuccessful(mtdbsa: String) extends SignUpSuccessResponse

  case object AlreadySignedUp extends SignUpSuccessResponse

  implicit val format: OFormat[SignUpSuccessful] = Json.format[SignUpSuccessful]

}

sealed trait SignUpIncomeSourcesFailure extends ConnectorError

object SignUpSourcesFailure {

  case object BadlyFormattedSignUpIncomeSourcesResponse extends SignUpIncomeSourcesFailure

  case class SignUpIncomeSourcesFailureResponse(status: Int) extends SignUpIncomeSourcesFailure

}

