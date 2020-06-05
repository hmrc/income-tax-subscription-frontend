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
import models.individual.business.address.Address
import models.individual.business.{AccountingPeriodModel, BusinessPhoneNumberModel, BusinessStartDateModel, MatchTaxYearModel}
import models.individual.incomesource.{AreYouSelfEmployedModel, IncomeSourceModel, RentUkPropertyModel}
import models.individual.subscription._
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.CacheConstants._

object CacheUtil {

  implicit class CacheMapUtil(cacheMap: CacheMap) {

    def getRentUkProperty: Option[RentUkPropertyModel] = cacheMap.getEntry[RentUkPropertyModel](RentUkProperty)

    def getAreYouSelfEmployed: Option[AreYouSelfEmployedModel] = cacheMap.getEntry[AreYouSelfEmployedModel](AreYouSelfEmployed)

    def getIncomeSourceType: Option[IncomeSourceType] =
      getRentUkProperty.flatMap(rentUkProperty => IncomeSourceType.from(rentUkProperty, getAreYouSelfEmployed))

    def getIncomeSourceModel: Option[IncomeSourceModel] = cacheMap.getEntry[IncomeSourceModel](IndividualIncomeSource)

    def agentGetIncomeSource: Option[IncomeSourceType] = cacheMap.getEntry[IncomeSourceType](IncomeSource)

    def getMatchTaxYear: Option[MatchTaxYearModel] = cacheMap.getEntry[MatchTaxYearModel](MatchTaxYear)

    def getEnteredAccountingPeriodDate: Option[AccountingPeriodModel] = cacheMap.getEntry[AccountingPeriodModel](AccountingPeriodDate)

    def getBusinessName: Option[BusinessNameModel] = cacheMap.getEntry[BusinessNameModel](BusinessName)

    def getBusinessPhoneNumber: Option[BusinessPhoneNumberModel] = cacheMap.getEntry[BusinessPhoneNumberModel](BusinessPhoneNumber)

    def getBusinessAddress: Option[Address] = cacheMap.getEntry[Address](BusinessAddress)

    def getBusinessStartDate: Option[BusinessStartDateModel] = cacheMap.getEntry[BusinessStartDateModel](BusinessStartDate)

    def getSelectedTaxYear: Option[AccountingYearModel] = cacheMap.getEntry[AccountingYearModel](SelectedTaxYear)

    def getAccountingMethod: Option[AccountingMethodModel] = cacheMap.getEntry[AccountingMethodModel](AccountingMethod)

    def getPropertyAccountingMethod: Option[AccountingMethodPropertyModel] = cacheMap.getEntry[AccountingMethodPropertyModel](PropertyAccountingMethod)

    def getSummary()(implicit appConfig: AppConfig): IndividualSummary =
      getIncomeSourceModel match {
        case Some(IncomeSourceModel(false, true)) =>
          IndividualSummary(
            incomeSourceIndiv = getIncomeSourceModel,
            accountingMethodProperty = getPropertyAccountingMethod
          )
        case Some(IncomeSourceModel(true, false)) =>
          IndividualSummary(
            incomeSourceIndiv = getIncomeSourceModel,
            businessName = getBusinessName,
            businessPhoneNumber = getBusinessPhoneNumber,
            businessAddress = getBusinessAddress,
            businessStartDate = getBusinessStartDate,
            selectedTaxYear = getSelectedTaxYear,
            accountingMethod = getAccountingMethod
          )
        case Some(_) =>
          IndividualSummary(
            incomeSourceIndiv = getIncomeSourceModel,
            businessName = getBusinessName,
            businessPhoneNumber = getBusinessPhoneNumber,
            businessAddress = getBusinessAddress,
            businessStartDate = getBusinessStartDate,
            accountingMethod = getAccountingMethod,
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
