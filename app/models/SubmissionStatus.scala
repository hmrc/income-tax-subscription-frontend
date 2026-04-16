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

import models.No.NO
import models.SubmissionStatus.maxSeconds
import models.Yes.YES
import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Json, OFormat, OWrites, Reads, Writes, __}

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
  private val IN_PROGRESS = "P"
  private val SUCCESS = "S"
  private val HANDLED_ERROR = "H"
  private val OTHER_ERROR = "O"
  
  private val reads: Reads[Status] = Reads[Status] {
    case JsString(IN_PROGRESS) => JsSuccess(InProgress)
    case JsString(SUCCESS) => JsSuccess(Success)
    case JsString(HANDLED_ERROR) => JsSuccess(HandledError)
    case JsString(OTHER_ERROR) => JsSuccess(OtherError)
    case _ => JsError("Invalid status")
  }

  private val writes: Writes[Status] = {
    case InProgress => JsString(IN_PROGRESS)
    case Success => JsString(SUCCESS)
    case HandledError => JsString(HANDLED_ERROR)
    case OtherError => JsString(OTHER_ERROR)
  }

  implicit val format: Format[Status] = Format[Status](reads, writes)
}

case object InProgress extends Status

case object Success extends Status

case object HandledError extends Status

case object OtherError extends Status
