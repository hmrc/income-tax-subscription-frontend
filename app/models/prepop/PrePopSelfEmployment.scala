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

package models.prepop

import models.common.business._
import models.{AccountingMethod, DateModel}
import play.api.libs.json.{Json, Reads}
import utilities.AccountingPeriodUtil

case class PrePopSelfEmployment(name: Option[String],
                                trade: Option[String],
                                address: Option[Address],
                                startDate: Option[DateModel],
                                accountingMethod: AccountingMethod) {

  def toSelfEmploymentData(id: String): SelfEmploymentData = {

    val startDateBeforeLimit: Option[Boolean] = startDate.map(_.toLocalDate.isBefore(AccountingPeriodUtil.getStartDateLimit))

    SelfEmploymentData(
      id = id,
      startDateBeforeLimit = startDateBeforeLimit,
      businessStartDate = if (startDateBeforeLimit.contains(true)) None else startDate.map(BusinessStartDate.apply),
      businessName = name.map(BusinessNameModel.apply),
      businessTradeName = trade.map(BusinessTradeNameModel.apply),
      businessAddress = address.map(BusinessAddressModel.apply)
    )
  }

}

object PrePopSelfEmployment {
  implicit val reads: Reads[PrePopSelfEmployment] = Json.reads[PrePopSelfEmployment]
}