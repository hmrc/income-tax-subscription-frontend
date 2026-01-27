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

import play.api.libs.json.*

sealed trait GetITSAStatus {
  def value: String
}

object GetITSAStatus {

  case object NoStatus extends GetITSAStatus {
    override val value: String = "No Status"
  }

  case object MTDMandated extends GetITSAStatus {
    override val value: String = "MTD Mandated"
  }

  case object MTDVoluntary extends GetITSAStatus {
    override val value: String = "MTD Voluntary"
  }

  case object Annual extends GetITSAStatus {
    override val value: String = "Annual"
  }

  case object DigitallyExempt extends GetITSAStatus {
    override val value: String = "Digitally Exempt"
  }

  case object Dormant extends GetITSAStatus {
    override val value: String = "Dormant"
  }

  case object MTDExempt extends GetITSAStatus {
    override val value: String = "MTD Exempt"
  }

  implicit val reads: Reads[GetITSAStatus] = __.read[String] flatMapResult {
    case NoStatus.value => JsSuccess(NoStatus)
    case MTDMandated.value => JsSuccess(MTDMandated)
    case MTDVoluntary.value => JsSuccess(MTDVoluntary)
    case Annual.value => JsSuccess(Annual)
    case DigitallyExempt.value => JsSuccess(DigitallyExempt)
    case Dormant.value => JsSuccess(Dormant)
    case MTDExempt.value => JsSuccess(MTDExempt)
    case _ => JsError("Unsupported get itsa status")
  }

  implicit val writes: Writes[GetITSAStatus] = Writes[GetITSAStatus] { status =>
    JsString(status.value)
  }

}
