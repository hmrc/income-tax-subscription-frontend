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

import play.api.libs.json.*

sealed trait Channel

case object CustomerLed extends Channel

case object HmrcLedUnconfirmed extends Channel

case object HmrcLedConfirmed extends Channel

object Channel {
  private val CUSTOMERLED = JsString("1")
  private val HMRCLEDUNCONFIRMED = JsString("2")
  private val HMRCLEDCONFIRMED = JsString("3")

  private val reads: Reads[Channel] = Reads[Channel] {
    case CUSTOMERLED => JsSuccess(CustomerLed)
    case HMRCLEDUNCONFIRMED => JsSuccess(HmrcLedUnconfirmed)
    case HMRCLEDCONFIRMED => JsSuccess(HmrcLedConfirmed)
    case _ => JsError("Invalid channel")
  }

  private val writes: Writes[Channel] = Writes[Channel] {
    case CustomerLed => CUSTOMERLED
    case HmrcLedUnconfirmed => HMRCLEDUNCONFIRMED
    case HmrcLedConfirmed => HMRCLEDCONFIRMED
  }

  implicit val format: Format[Channel] = Format[Channel](reads, writes)
}