/*
 * Copyright 2022 HM Revenue & Customs
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

package models.common.subscription

import models.common.AccountingPeriodModel
import models.common.business.SelfEmploymentData
import models.{AccountingMethod, DateModel}
import play.api.libs.json.Json

case class CreateIncomeSourcesModel(
                                     nino: String,
                                     selfEmployments: Option[SoleTraderBusinesses] = None,
                                     ukProperty: Option[UkProperty] = None,
                                     overseasProperty: Option[OverseasProperty] = None
                                   ){
  require(selfEmployments.isDefined || ukProperty.isDefined || overseasProperty.isDefined, "at least one income source is required")
}

case class SoleTraderBusinesses(
                                 accountingPeriod: AccountingPeriodModel,
                                 accountingMethod: AccountingMethod,
                                 businesses: Seq[SelfEmploymentData]
                               )

object SoleTraderBusinesses {
  implicit val format = Json.format[SoleTraderBusinesses]
}

case class UkProperty(
                       accountingPeriod: AccountingPeriodModel,
                       tradingStartDate: DateModel,
                       accountingMethod: AccountingMethod
                     )

object UkProperty {
  implicit val format = Json.format[UkProperty]
}

case class OverseasProperty(
                             accountingPeriod: AccountingPeriodModel,
                             tradingStartDate: DateModel,
                             accountingMethod: AccountingMethod
                           )

object OverseasProperty {
  implicit val format = Json.format[OverseasProperty]
}

object CreateIncomeSourcesModel {
  implicit val format = Json.format[CreateIncomeSourcesModel]
}
