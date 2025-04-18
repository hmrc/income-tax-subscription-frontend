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

import models.common.{EncryptingAddress, SoleTraderBusiness}
import play.api.libs.json._

case class SelfEmploymentData(id: String,
                              startDateBeforeLimit: Option[Boolean] = None,
                              businessStartDate: Option[BusinessStartDate] = None,
                              businessName: Option[BusinessNameModel] = None,
                              businessTradeName: Option[BusinessTradeNameModel] = None,
                              businessAddress: Option[BusinessAddressModel] = None,
                              confirmed: Boolean = false) {

  val isComplete: Boolean = {
    startDateBeforeLimit match {
      case Some(true) => businessName.isDefined && businessTradeName.isDefined && businessAddress.isDefined
      case _ => businessStartDate.isDefined && businessName.isDefined && businessTradeName.isDefined && businessAddress.isDefined
    }
  }

  def toSoleTraderBusiness: SoleTraderBusiness = SoleTraderBusiness(
    id = id,
    confirmed = confirmed,
    startDateBeforeLimit = startDateBeforeLimit,
    startDate = businessStartDate.map(_.startDate),
    name = businessName.map(_.businessName),
    trade = businessTradeName.map(_.businessTradeName),
    address = businessAddress.map(_.address).map(address => EncryptingAddress(address.lines, address.postcode))
  )

}

object SelfEmploymentData {

  implicit val format: Format[SelfEmploymentData] = Json.format[SelfEmploymentData]

}
