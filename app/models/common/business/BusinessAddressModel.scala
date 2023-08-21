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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText}

case class Address(lines: Seq[String], postcode: Option[String]) {
  override def toString: String = s"${lines.mkString(", ")}${postcode.map(t => s", $t").getOrElse("")}"
}

case class BusinessAddressModel(auditRef: String,
                                address: Address) {

  def encrypt(encrypter: Encrypter): BusinessAddressModel = this.copy(
    address = Address(
      lines = this.address.lines.map(line => encrypter.encrypt(PlainText(line)).value),
      postcode = this.address.postcode.map(postcode => encrypter.encrypt(PlainText(postcode)).value)
    )
  )

  def decrypt(decrypter: Decrypter): BusinessAddressModel = this.copy(
    address = Address(
      lines = this.address.lines.map(line => decrypter.decrypt(Crypted(line)).value),
      postcode = this.address.postcode.map(postcode => decrypter.decrypt(Crypted(postcode)).value)
    )
  )

}

object Address {
  implicit val format: OFormat[Address] = Json.format[Address]
}

object BusinessAddressModel {
  implicit val format: OFormat[BusinessAddressModel] = Json.format[BusinessAddressModel]
}
