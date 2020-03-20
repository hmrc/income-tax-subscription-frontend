/*
 * Copyright 2020 HM Revenue & Customs
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

package models.usermatching

import java.time.OffsetDateTime

import models.ConnectorError
import play.api.libs.json.{Json, OFormat}

sealed trait LockoutStatus

case object NotLockedOut extends LockoutStatus

case class LockedOut(arn: String, expiryTimestamp: OffsetDateTime) extends LockoutStatus


sealed trait LockoutStatusFailure extends ConnectorError

case class LockoutStatusFailureResponse(status: Int) extends LockoutStatusFailure

case object BadlyFormattedLockedStatusResponse extends LockoutStatusFailure

object LockedOut {
  implicit val format: OFormat[LockedOut] = Json.format[LockedOut]
}
