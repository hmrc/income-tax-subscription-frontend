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

package models.status

import models.status.MandationStatus.Mandated
import play.api.libs.json._

sealed trait MandationStatus {
  def value: String
  val isMandated: Boolean = this == Mandated
}

object MandationStatus {

  private val MANDATED: String = "MTD Mandated"
  private val VOLUNTARY: String = "MTD Voluntary"

  case object Mandated extends MandationStatus {
    override val value: String = MANDATED
  }

  case object Voluntary extends MandationStatus {
    override def value: String = VOLUNTARY
  }

  implicit val reads: Reads[MandationStatus] = Reads[MandationStatus] {
    case JsString(MANDATED) => JsSuccess(Mandated)
    case JsString(VOLUNTARY) => JsSuccess(Voluntary)
    case _ => JsError("error.mandation-status.invalid")
  }

  implicit val writes: Writes[MandationStatus] = Writes[MandationStatus] { status =>
    JsString(status.value)
  }

}

