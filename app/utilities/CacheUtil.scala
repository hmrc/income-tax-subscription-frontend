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

package utilities

import config.AppConfig
import models.common.{AccountingMethodModel, AccountingMethodPropertyModel, AccountingYearModel, BusinessNameModel}
import models.individual.business.{AccountingPeriodModel, MatchTaxYearModel, PropertyCommencementDateModel}
import models.individual.incomesource.IncomeSourceModel
import models.individual.subscription._
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.CacheConstants._

object CacheUtil {

  implicit class CacheMapUtil(cacheMap: CacheMap) {


    def getIncomeSourceModel: Option[IncomeSourceModel] = cacheMap.getEntry[IncomeSourceModel](IndividualIncomeSource)

    def agentGetIncomeSource: Option[IncomeSourceType] = cacheMap.getEntry[IncomeSourceType](IncomeSource)

    def getMatchTaxYear: Option[MatchTaxYearModel] = cacheMap.getEntry[MatchTaxYearModel](MatchTaxYear)

    def getEnteredAccountingPeriodDate: Option[AccountingPeriodModel] = cacheMap.getEntry[AccountingPeriodModel](AccountingPeriodDate)

    def getBusinessName: Option[BusinessNameModel] = cacheMap.getEntry[BusinessNameModel](BusinessName)

    def getSelectedTaxYear: Option[AccountingYearModel] = cacheMap.getEntry[AccountingYearModel](SelectedTaxYear)

    def getAccountingMethod: Option[AccountingMethodModel] = cacheMap.getEntry[AccountingMethodModel](AccountingMethod)

    def getPropertyAccountingMethod: Option[AccountingMethodPropertyModel] = cacheMap.getEntry[AccountingMethodPropertyModel](PropertyAccountingMethod)

    def getPropertyCommencementDate: Option[PropertyCommencementDateModel] = cacheMap.getEntry[PropertyCommencementDateModel](PropertyCommencementDate)

    def getSummary()(implicit appConfig: AppConfig): IndividualSummary =
      getIncomeSourceModel match {
        case Some(IncomeSourceModel(false, true, _)) =>
          IndividualSummary(
            incomeSourceIndiv = getIncomeSourceModel,
            propertyCommencementDate = getPropertyCommencementDate,
            accountingMethodProperty = getPropertyAccountingMethod
          )
        case Some(IncomeSourceModel(true, false, _)) =>
          IndividualSummary(
            incomeSourceIndiv = getIncomeSourceModel,
            businessName = getBusinessName,
            selectedTaxYear = getSelectedTaxYear,
            propertyCommencementDate = getPropertyCommencementDate,
            accountingMethod = getAccountingMethod
          )
        case Some(_) =>
          IndividualSummary(
            incomeSourceIndiv = getIncomeSourceModel,
            businessName = getBusinessName,
            accountingMethod = getAccountingMethod,
            propertyCommencementDate = getPropertyCommencementDate,
            accountingMethodProperty = getPropertyAccountingMethod
          )
        case _ => IndividualSummary()
      }

    def getAgentSummary()(implicit appConfig: AppConfig): AgentSummary = {
      agentGetIncomeSource match {
        case Some(Property) =>
          AgentSummary(
            incomeSource = agentGetIncomeSource,
            accountingMethodProperty = getPropertyAccountingMethod
          )
        case Some(Business) =>
          AgentSummary(
            incomeSource = agentGetIncomeSource,
            matchTaxYear = getMatchTaxYear,
            selectedTaxYear = getSelectedTaxYear,
            accountingPeriodDate = getEnteredAccountingPeriodDate,
            businessName = getBusinessName,
            accountingMethod = getAccountingMethod
          )
        case Some(Both) =>
          AgentSummary(
            incomeSource = agentGetIncomeSource,
            matchTaxYear = getMatchTaxYear,
            accountingPeriodDate = getEnteredAccountingPeriodDate,
            businessName = getBusinessName,
            accountingMethod = getAccountingMethod,
            accountingMethodProperty = getPropertyAccountingMethod
          )
        case _ => AgentSummary()
      }

    }
  }

}
