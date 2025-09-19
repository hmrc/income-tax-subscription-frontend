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

package models.common.subscription

import models.common.business._
import models.common.{AccountingPeriodModel, AccountingYearModel}
import models.{Current, DateModel, Next}
import play.api.libs.json.{Json, OFormat}
import services.GetCompleteDetailsService.CompleteDetails
import utilities.AccountingPeriodUtil
import utilities.AccountingPeriodUtil.{getCurrentTaxYear, getNextTaxYear}

case class CreateIncomeSourcesModel(nino: String,
                                    soleTraderBusinesses: Option[SoleTraderBusinesses] = None,
                                    ukProperty: Option[UkProperty] = None,
                                    overseasProperty: Option[OverseasProperty] = None) {
  require(soleTraderBusinesses.isDefined || ukProperty.isDefined || overseasProperty.isDefined, "at least one income source is required")
}

case class SoleTraderBusinesses(accountingPeriod: AccountingPeriodModel,
                                businesses: Seq[SelfEmploymentData])

object SoleTraderBusinesses {
  implicit val format: OFormat[SoleTraderBusinesses] = Json.format[SoleTraderBusinesses]
}

case class UkProperty(startDateBeforeLimit: Option[Boolean] = None,
                      accountingPeriod: AccountingPeriodModel,
                      tradingStartDate: DateModel)

object UkProperty {
  implicit val format: OFormat[UkProperty] = Json.format[UkProperty]
}

case class OverseasProperty(startDateBeforeLimit: Option[Boolean] = None,
                            accountingPeriod: AccountingPeriodModel,
                            tradingStartDate: DateModel)

object OverseasProperty {
  implicit val format: OFormat[OverseasProperty] = Json.format[OverseasProperty]
}

object CreateIncomeSourcesModel {
  implicit val format: OFormat[CreateIncomeSourcesModel] = Json.format[CreateIncomeSourcesModel]

  def createIncomeSources(nino: String, completeDetails: CompleteDetails): CreateIncomeSourcesModel = {
    val accountingPeriod: AccountingPeriodModel = {
      completeDetails.taxYear match {
        case AccountingYearModel(Next, _, _) => getNextTaxYear
        case AccountingYearModel(Current, _, _) => getCurrentTaxYear
      }
    }

    val soleTraderBusinesses: Option[SoleTraderBusinesses] = {
      completeDetails.incomeSources.soleTraderBusinesses map { businesses =>
        val selfEmployments: Seq[SelfEmploymentData] = businesses.businesses.map { business =>
          val startDate: DateModel = DateModel.dateConvert(business.startDate.getOrElse(AccountingPeriodUtil.getStartDateLimit))
          SelfEmploymentData(
            id = business.id,
            startDateBeforeLimit = if (business.startDate.isEmpty) Some(true) else Some(false),
            businessStartDate = Some(BusinessStartDate(startDate)),
            businessName = Some(BusinessNameModel(business.name)),
            businessTradeName = Some(BusinessTradeNameModel(business.trade)),
            businessAddress = Some(BusinessAddressModel(business.address)),
            confirmed = true
          )
        }
        SoleTraderBusinesses(accountingPeriod, selfEmployments)
      }
    }

    val ukProperty: Option[UkProperty] = {
      completeDetails.incomeSources.ukProperty map { property =>
        val startDate: DateModel = DateModel.dateConvert(property.startDate.getOrElse(AccountingPeriodUtil.getStartDateLimit))
        UkProperty(
          startDateBeforeLimit = if (property.startDate.isEmpty) Some(true) else Some(false),
          accountingPeriod = accountingPeriod,
          tradingStartDate = startDate
        )
      }
    }

    val foreignProperty: Option[OverseasProperty] = {
      completeDetails.incomeSources.foreignProperty map { property =>
        val startDate: DateModel = DateModel.dateConvert(property.startDate.getOrElse(AccountingPeriodUtil.getStartDateLimit))
        OverseasProperty(
          startDateBeforeLimit = if (property.startDate.isEmpty) Some(true) else Some(false),
          accountingPeriod = accountingPeriod,
          tradingStartDate = startDate
        )
      }
    }

    CreateIncomeSourcesModel(
      nino = nino,
      soleTraderBusinesses = soleTraderBusinesses,
      ukProperty = ukProperty,
      overseasProperty = foreignProperty
    )
  }
}
