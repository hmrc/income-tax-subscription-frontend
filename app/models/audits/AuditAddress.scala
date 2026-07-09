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

package models.audits

import models.common.business.{Address, Country}
import play.api.libs.json.{Json, OFormat}

case class AuditAddress(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  townOrCity: String,
  postcode: String,
  country: Country,
  uprn: Option[String]
)

object AuditAddress {
  implicit val format: OFormat[AuditAddress] = Json.format[AuditAddress]

  private def error(field: String) =
    new IllegalArgumentException(s"[AuditAddress] Missing data: $field")

  def apply(address: Address): AuditAddress = {
    val lines = if (address.lines.nonEmpty) address.lines.tail.dropRight(1) else Seq()
    AuditAddress(
      addressLine1 = address.lines.headOption.getOrElse(throw error("addressLine1")),
      addressLine2 = lines.headOption,
      addressLine3 = lines.lift(1),
      townOrCity = address.lines.lastOption.getOrElse(throw error("townOrCity")),
      postcode = address.postcode.getOrElse(throw error("postcode")),
      country = address.country.getOrElse(throw error("country")),
      uprn = address.uprn
    )
  }
}
