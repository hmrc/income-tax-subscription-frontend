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

package models.common.business

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import uk.gov.hmrc.crypto.Sensitive.SensitiveString

case class BusinessAddressModel(address: Address)

object BusinessAddressModel {
  implicit val format: OFormat[BusinessAddressModel] = Json.format[BusinessAddressModel]
}

case class Address(lines: Seq[String], postcode: Option[String], country: Option[Country]) {
  override def toString: String = (lines ++ postcode ++ country.map(_.name)).mkString("<br>")
}

object Address {

  implicit val format: OFormat[Address] = Json.format[Address]

  def encryptedFormat(implicit sensitiveFormat: Format[SensitiveString]): OFormat[Address] = {

    implicit val countryFormat: OFormat[Country] = Country.encryptedFormat

    val reads: Reads[Address] = (
      (__ \ "lines").read[Seq[SensitiveString]] and
        (__ \ "postcode").readNullable[SensitiveString] and
        (__ \ "country").readNullable[Country]
      )(
      (lines, postcode, country) =>
        Address.apply(lines.map(_.decryptedValue), postcode.map(_.decryptedValue), country)
    )

    val writes: OWrites[Address] = (
      (__ \ "lines").write[Seq[SensitiveString]] and
        (__ \ "postcode").writeNullable[SensitiveString] and
        (__ \ "country").writeNullable[Country]
      )(
      address =>
        (
          address.lines.map(SensitiveString.apply),
          address.postcode.map(SensitiveString.apply),
          address.country
        )
    )

    OFormat(reads, writes)
  }
}

case class Country(code: String, name: String)

object Country {

  implicit val format: OFormat[Country] = Json.format[Country]

  def encryptedFormat(implicit sensitiveFormat: Format[SensitiveString]): OFormat[Country] = {

    val reads: Reads[Country] = (
      (__ \ "code").read[SensitiveString] and
        (__ \ "name").read[SensitiveString]
      )((code, name) => Country(code.decryptedValue, name.decryptedValue))

    val writes: OWrites[Country] = (
      (__ \ "code").write[SensitiveString] and
        (__ \ "name").write[SensitiveString]
      )(country => (SensitiveString(country.code), SensitiveString(country.name)))

    OFormat(reads, writes)

  }

}
