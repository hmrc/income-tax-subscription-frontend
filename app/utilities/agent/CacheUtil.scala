/*
 * Copyright 2020 HM Revenue & Customs
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

package utilities.agent

import config.AppConfig
import models.agent
import models.individual.business._
import models.individual.subscription._
import play.api.libs.json.Reads
import uk.gov.hmrc.http.cache.client.CacheMap

object CacheUtil {

  implicit class CacheMapUtil(cacheMap: CacheMap) {

    import CacheConstants._

    def getIncomeSource()(implicit read: Reads[IncomeSourceType]): Option[IncomeSourceType] = cacheMap.getEntry(IncomeSource)

    def getMatchTaxYear()(implicit read: Reads[MatchTaxYearModel]): Option[MatchTaxYearModel] = cacheMap.getEntry(MatchTaxYear)

    def getSelectedTaxYear()(implicit read: Reads[AccountingYearModel]): Option[AccountingYearModel] = cacheMap.getEntry(WhatYearToSignUp)

    def getAccountingPeriodDate()(implicit read: Reads[AccountingPeriodModel]): Option[AccountingPeriodModel] = cacheMap.getEntry(AccountingPeriodDate)

    def getBusinessName()(implicit read: Reads[BusinessNameModel]): Option[BusinessNameModel] = cacheMap.getEntry(BusinessName)

    def getAccountingMethod()(implicit read: Reads[AccountingMethodModel]): Option[AccountingMethodModel] = cacheMap.getEntry(AccountingMethod)

    def agentGetAccountingMethod()(implicit read: Reads[agent.AccountingMethodModel]): Option[agent.AccountingMethodModel] = cacheMap.getEntry(AccountingMethod)

    def getAccountingMethodProperty()(implicit read: Reads[AccountingMethodPropertyModel]):
                                                                      Option[AccountingMethodPropertyModel] = cacheMap.getEntry(AccountingMethodProperty)

    def getSummary()(implicit appConfig: AppConfig): AgentSummary = {
      getIncomeSource() match {
        case Some(Property) =>
          AgentSummary(
            incomeSource = getIncomeSource(),
            accountingMethodProperty = getAccountingMethodProperty()
          )
        case Some(Business) =>
          AgentSummary(
            incomeSource = getIncomeSource(),
            matchTaxYear = getMatchTaxYear(),
            selectedTaxYear = getSelectedTaxYear(),
            accountingPeriodDate = getAccountingPeriodDate(),
            businessName = getBusinessName(),
            accountingMethod = getAccountingMethod()
          )
        case Some(Both) =>
          AgentSummary(
            incomeSource = getIncomeSource(),
            matchTaxYear = getMatchTaxYear(),
            accountingPeriodDate = getAccountingPeriodDate(),
            businessName = getBusinessName(),
            accountingMethod = getAccountingMethod(),
            accountingMethodProperty = getAccountingMethodProperty()
          )
        case _ => AgentSummary()
      }

    }
  }

}
