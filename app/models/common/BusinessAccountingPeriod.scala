/*
 * Copyright 2025 HM Revenue & Customs
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

package models.common

import play.api.libs.json._

sealed trait BusinessAccountingPeriod {
  def key: String
}

object BusinessAccountingPeriod {

  case object SixthAprilToFifthApril extends BusinessAccountingPeriod {
    val key: String = "sixth-april-to-fifth-april"
  }

  case object FirstAprilToThirtyFirstMarch extends BusinessAccountingPeriod {
    val key: String = "first-april-to-thirty-first-march"
  }

  case object OtherAccountingPeriod extends BusinessAccountingPeriod {
    val key: String = "other"
  }

  private val keyToPeriod: Map[String, BusinessAccountingPeriod] = Seq(
    SixthAprilToFifthApril,
    FirstAprilToThirtyFirstMarch,
    OtherAccountingPeriod)
    .map(value => value.key -> value).toMap

  implicit val reads: Reads[BusinessAccountingPeriod] = __.read[String] map keyToPeriod

  implicit val writes: Writes[BusinessAccountingPeriod] = Writes(value => JsString(value.key))


}
