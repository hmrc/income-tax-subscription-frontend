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

import models.common.{AccountingPeriodModel, AccountingYearModel, OverseasPropertyModel, PropertyModel}
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import models.{AccountingMethod, Current, DateModel, Next}
import play.api.libs.json.Json
import uk.gov.hmrc.http.InternalServerException
import utilities.AccountingPeriodUtil.{getCurrentTaxYear, getNextTaxYear}

case class CreateIncomeSourcesModel(
                                     nino: String,
                                     soleTraderBusinesses: Option[SoleTraderBusinesses] = None,
                                     ukProperty: Option[UkProperty] = None,
                                     overseasProperty: Option[OverseasProperty] = None
                                   ){
  require(soleTraderBusinesses.isDefined || ukProperty.isDefined || overseasProperty.isDefined, "at least one income source is required")
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

  def createIncomeSources(nino: String,
                          selfEmployments: Seq[SelfEmploymentData] = Seq.empty,
                          selfEmploymentsAccountingMethod: Option[AccountingMethodModel] = None,
                          property: Option[PropertyModel] = None,
                          overseasProperty: Option[OverseasPropertyModel] = None,
                          accountingYear: Option[AccountingYearModel] = None
                         ): CreateIncomeSourcesModel = {

    val accountingPeriod: AccountingPeriodModel = {
      accountingYear match {
        case None =>
          throw new InternalServerException("[SubscriptionDataUtil][createIncomeSource] - Could not create the create income sources model due to missing selected tax year")
        case Some(AccountingYearModel(_, false, _)) =>
          throw new InternalServerException("[SubscriptionDataUtil][createIncomeSources] - Could not create the create income sources model as the user has not confirmed their selected tax year")
        case Some(AccountingYearModel(Next, _, _)) => getNextTaxYear
        case Some(AccountingYearModel(Current, _, _)) => getCurrentTaxYear
      }
    }

    val soleTraderBusinesses: Option[SoleTraderBusinesses] = {

      (selfEmployments, selfEmploymentsAccountingMethod) match {
        case (Seq(), None) => None
        case (Seq(), Some(_)) =>
          throw new InternalServerException("[SubscriptionDataUtil][createIncomeSource] - self employment accounting method found without any self employments")
        case (_, None) =>
          throw new InternalServerException("[SubscriptionDataUtil][createIncomeSource] - self employment businesses found without any accounting method")
        case (selfEmployments, _) if selfEmployments.exists(!_.isComplete) =>
          throw new InternalServerException("[SubscriptionDataUtil][createIncomeSource] - not all self employment businesses are complete")
        case (selfEmployments, Some(accountingMethod)) =>
          Some(SoleTraderBusinesses(
            accountingPeriod = accountingPeriod,
            accountingMethod = accountingMethod.accountingMethod,
            businesses = selfEmployments
          ))
      }

    }

    val ukProperty: Option[UkProperty] = {

      (property.flatMap(_.startDate), property.flatMap(_.accountingMethod)) match {
        case (Some(startDate), Some(accountingMethod)) =>
          Some(UkProperty(
            accountingPeriod = accountingPeriod,
            tradingStartDate = startDate,
            accountingMethod = accountingMethod
          ))

        case (Some(_), None) =>
          throw new InternalServerException("[SubscriptionDataUtil][createIncomeSource] - uk property accounting method missing")

        case (None, Some(_)) =>
          throw new InternalServerException("[SubscriptionDataUtil][createIncomeSource] - uk property start date missing")

        case (None, None) => None

      }

    }

    val overseas: Option[OverseasProperty] = {
      (overseasProperty.flatMap(_.startDate), overseasProperty.flatMap(_.accountingMethod)) match {
        case (Some(startDate), Some(accountingMethod)) =>
          Some(OverseasProperty(
            accountingPeriod = accountingPeriod,
            tradingStartDate = startDate,
            accountingMethod = accountingMethod
          ))

        case (Some(_), None) =>
          throw new InternalServerException("[SubscriptionDataUtil][createIncomeSource] - oversea property accounting method missing")

        case (None, Some(_)) =>
          throw new InternalServerException("[SubscriptionDataUtil][createIncomeSource] - oversea property start date missing")

        case (None, None) => None

      }
    }

    CreateIncomeSourcesModel(
      nino = nino,
      soleTraderBusinesses = soleTraderBusinesses,
      ukProperty = ukProperty,
      overseasProperty = overseas
    )
  }
}
