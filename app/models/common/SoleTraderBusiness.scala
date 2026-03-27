/*
 * Copyright 2024 HM Revenue & Customs
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

import models.DateModel
import models.common.business.*
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import uk.gov.hmrc.crypto.Sensitive.SensitiveString

case class SoleTraderBusiness(id: String,
                              confirmed: Boolean = false,
                              startDateBeforeLimit: Option[Boolean] = None,
                              startDate: Option[DateModel] = None,
                              name: Option[String] = None,
                              trade: Option[String] = None,
                              address: Option[Address] = None) {

  def toSelfEmploymentData: SelfEmploymentData = SelfEmploymentData(
    id = id,
    startDateBeforeLimit = startDateBeforeLimit,
    businessStartDate = startDate.map(BusinessStartDate.apply),
    businessName = name.map(BusinessNameModel.apply),
    businessTradeName = trade.map(BusinessTradeNameModel.apply),
    businessAddress = address.map(BusinessAddressModel.apply),
    confirmed = confirmed
  )

}

object SoleTraderBusiness {

  def encryptedFormat(implicit sensitiveFormat: Format[SensitiveString]): OFormat[SoleTraderBusiness] = {

    implicit val addressFormat: OFormat[Address] = Address.encryptedFormat

    val reads: Reads[SoleTraderBusiness] = (
      (__ \ "id").read[String] and
        (__ \ "confirmed").read[Boolean] and
        (__ \ "startDateBeforeLimit").readNullable[Boolean] and
        (__ \ "startDate").readNullable[DateModel] and
        (__ \ "name").readNullable[SensitiveString] and
        (__ \ "trade").readNullable[String] and
        (__ \ "address").readNullable[Address]
      )(
      (id, confirmed, startDateBeforeLimit, startDate, name, trade, address) =>
        SoleTraderBusiness.apply(id, confirmed, startDateBeforeLimit, startDate, name.map(_.decryptedValue), trade, address)
    )

    val writes: OWrites[SoleTraderBusiness] = (
      (__ \ "id").write[String] and
        (__ \ "confirmed").write[Boolean] and
        (__ \ "startDateBeforeLimit").writeNullable[Boolean] and
        (__ \ "startDate").writeNullable[DateModel] and
        (__ \ "name").writeNullable[SensitiveString] and
        (__ \ "trade").writeNullable[String] and
        (__ \ "address").writeNullable[Address]
      )(
      soleTraderBusiness =>
        (
          soleTraderBusiness.id,
          soleTraderBusiness.confirmed,
          soleTraderBusiness.startDateBeforeLimit,
          soleTraderBusiness.startDate,
          soleTraderBusiness.name.map(SensitiveString.apply),
          soleTraderBusiness.trade,
          soleTraderBusiness.address
        )
    )

    OFormat(reads, writes)
  }

}

