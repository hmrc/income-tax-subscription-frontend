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

import play.api.libs.json._
import uk.gov.hmrc.crypto.Encrypter

case class SelfEmploymentData(id: String,
                              businessStartDate: Option[BusinessStartDate] = None,
                              businessName: Option[BusinessNameModel] = None,
                              businessTradeName: Option[BusinessTradeNameModel] = None,
                              businessAddress: Option[BusinessAddressModel] = None,
                              confirmed: Boolean = false) {

  val isComplete: Boolean = businessStartDate.isDefined && businessName.isDefined && businessTradeName.isDefined && businessAddress.isDefined

  def encrypt(encrypter: Encrypter): SelfEmploymentData = this.copy(
    businessName = this.businessName.map(_.encrypt(encrypter)),
    businessAddress = this.businessAddress.map(_.encrypt(encrypter))
  )

}

object SelfEmploymentData {

  implicit val format: Format[SelfEmploymentData] = Json.format[SelfEmploymentData]

}
