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

package models

import models.SubmissionStatus.maxSeconds
import play.api.libs.json.{Json, OFormat, OWrites, Reads, __}

import java.time.LocalDateTime

sealed case class SubmissionStatus(status: Status, timestamp: Option[LocalDateTime] = None) {
  def hasExpired: Boolean = {
    timestamp match {
      case Some(value) => LocalDateTime.now().isAfter(value.plusSeconds(maxSeconds))
      case None => false
    }
  }
}

object SubmissionStatus {
  val maxSeconds: Int = 8

  implicit val format: OFormat[SubmissionStatus] = Json.format[SubmissionStatus]

  def inProgress: SubmissionStatus = SubmissionStatus(InProgress, Some(LocalDateTime.now()))
  def success: SubmissionStatus = SubmissionStatus(Success)
  def handledError: SubmissionStatus = SubmissionStatus(HandledError)
  def otherError: SubmissionStatus = SubmissionStatus(OtherError)
}

sealed trait Status

object Status {
  implicit val format: OFormat[Status] = Json.format[Status]
}

case object InProgress extends Status {
  implicit val format: OFormat[InProgress.type] = Json.format[InProgress.type]
}

case object Success extends Status {
  implicit val format: OFormat[Success.type] = Json.format[Success.type]
}

case object HandledError extends Status {
  implicit val format: OFormat[HandledError.type] = Json.format[HandledError.type]
}

case object OtherError extends Status {
  implicit val format: OFormat[OtherError.type] = Json.format[OtherError.type]
}

