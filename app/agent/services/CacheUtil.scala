/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.services


import agent.models.AccountingPeriodPriorModel
import core.config.AppConfig
import core.models.YesNo
import incometax.business.models._
import incometax.subscription.models._
import play.api.libs.json.Reads
import uk.gov.hmrc.http.cache.client.CacheMap

object CacheUtil {

  implicit class CacheMapUtil(cacheMap: CacheMap) {

    import CacheConstants._

    def getIncomeSource()(implicit read: Reads[IncomeSourceType]): Option[IncomeSourceType] = cacheMap.getEntry(IncomeSource)

    def getOtherIncome()(implicit read: Reads[YesNo]): Option[YesNo] = cacheMap.getEntry(OtherIncome)

    def getAccountingPeriodPrior()(implicit read: Reads[AccountingPeriodPriorModel]): Option[AccountingPeriodPriorModel] = cacheMap.getEntry(AccountingPeriodPrior)

    def getMatchTaxYear()(implicit read: Reads[MatchTaxYearModel]): Option[MatchTaxYearModel] = cacheMap.getEntry(MatchTaxYear)

    def getSelectedTaxYear()(implicit read: Reads[AccountingYearModel]): Option[AccountingYearModel] = cacheMap.getEntry(WhatYearToSignUp)

    def getAccountingPeriodDate()(implicit read: Reads[AccountingPeriodModel]): Option[AccountingPeriodModel] = cacheMap.getEntry(AccountingPeriodDate)

    def getBusinessName()(implicit read: Reads[BusinessNameModel]): Option[BusinessNameModel] = cacheMap.getEntry(BusinessName)

    def getAccountingMethod()(implicit read: Reads[AccountingMethodModel]): Option[AccountingMethodModel] = cacheMap.getEntry(AccountingMethod)

    def getAccountingMethodProperty()(implicit read: Reads[AccountingMethodPropertyModel]): Option[AccountingMethodPropertyModel] = cacheMap.getEntry(AccountingMethodProperty)

    def getTerms()(implicit read: Reads[Boolean]): Option[Boolean] = cacheMap.getEntry(Terms)

    def getSummary()(implicit appConfig: AppConfig): AgentSummary = {
      getIncomeSource() match {
        case Some(Property) =>
          AgentSummary(
            incomeSource = getIncomeSource(),
            otherIncome = getOtherIncome(),
            accountingMethodProperty = getAccountingMethodProperty(),
            terms = getTerms()
          )
        case Some(Business) =>
          AgentSummary(
            incomeSource = getIncomeSource(),
            otherIncome = getOtherIncome(),
            accountingPeriodPrior = getAccountingPeriodPrior(),
            matchTaxYear = getMatchTaxYear(),
            selectedTaxYear = getSelectedTaxYear(),
            accountingPeriodDate = getAccountingPeriodDate(),
            businessName = getBusinessName(),
            accountingMethod = getAccountingMethod(),
            terms = getTerms()
          )
        case Some(Both) =>
          AgentSummary(
            incomeSource = getIncomeSource(),
            otherIncome = getOtherIncome(),
            accountingPeriodPrior = getAccountingPeriodPrior(),
            matchTaxYear = getMatchTaxYear(),
            accountingPeriodDate = getAccountingPeriodDate(),
            businessName = getBusinessName(),
            accountingMethod = getAccountingMethod(),
            accountingMethodProperty = getAccountingMethodProperty(),
            terms = getTerms()
          )
        case _ => AgentSummary()
      }

    }
  }

}
