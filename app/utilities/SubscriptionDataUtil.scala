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

package utilities

import models.common._
import models.common.business._
import models.common.subscription.{CreateIncomeSourcesModel, OverseasProperty, SoleTraderBusinesses, UkProperty}
import models.{AccountingMethod => _, _}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.AccountingPeriodUtil.{getCurrentTaxYear, getNextTaxYear}
import utilities.SubscriptionDataKeys._

//scalastyle:off
object SubscriptionDataUtil {

  implicit class CacheMapUtil(cacheMap: CacheMap) {

    def getBusinessName: Option[BusinessNameModel] = cacheMap.getEntry[BusinessNameModel](BusinessName)

    def getSelectedTaxYear: Option[AccountingYearModel] = cacheMap.getEntry[AccountingYearModel](SelectedTaxYear)

    def getAccountingMethod: Option[AccountingMethodModel] = cacheMap.getEntry[AccountingMethodModel](AccountingMethod)

    def getTaskListModel(selfEmployments: Seq[SelfEmploymentData] = Seq.empty,
                         selfEmploymentAccountingMethod: Option[AccountingMethodModel] = None,
                         property: Option[PropertyModel],
                         overseasProperty: Option[OverseasPropertyModel],
                         accountingYear: Option[AccountingYearModel]
                        ): TaskListModel = {
      TaskListModel(
        taxYearSelection = accountingYear,
        selfEmployments,
        ukProperty = property,
        overseasProperty = overseasProperty,
        selfEmploymentAccountingMethod = selfEmploymentAccountingMethod.map(_.accountingMethod)
      )
    }


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
          case Some(AccountingYearModel(_, false)) =>
            throw new InternalServerException("[SubscriptionDataUtil][createIncomeSources] - Could not create the create income sources model as the user has not confirmed their selected tax year")
          case Some(AccountingYearModel(Next, _)) => getNextTaxYear
          case Some(AccountingYearModel(Current, _)) => getCurrentTaxYear
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
}
