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

import config.featureswitch.FeatureSwitching
import models.common._
import models.individual.business._
import models.{AgentSummary, IndividualSummary, SummaryModel}
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys._

object SubscriptionDataUtil extends FeatureSwitching {

  implicit class CacheMapUtil(cacheMap: CacheMap) {

    def getIncomeSource: Option[IncomeSourceModel] = cacheMap.getEntry[IncomeSourceModel](IncomeSource)

    def getBusinessName: Option[BusinessNameModel] = cacheMap.getEntry[BusinessNameModel](BusinessName)

    def getSelectedTaxYear: Option[AccountingYearModel] = cacheMap.getEntry[AccountingYearModel](SelectedTaxYear)

    def getAccountingMethod: Option[AccountingMethodModel] = cacheMap.getEntry[AccountingMethodModel](AccountingMethod)

    def getPropertyAccountingMethod: Option[AccountingMethodPropertyModel] = cacheMap.getEntry[AccountingMethodPropertyModel](PropertyAccountingMethod)

    def getPropertyCommencementDate: Option[PropertyCommencementDateModel] = cacheMap.getEntry[PropertyCommencementDateModel](PropertyCommencementDate)

    def getOverseasPropertyCommencementDate: Option[OverseasPropertyCommencementDateModel] =
      cacheMap.getEntry[OverseasPropertyCommencementDateModel](OverseasPropertyCommencementDate)

    def getOverseasPropertyAccountingMethod: Option[OverseasAccountingMethodPropertyModel] =
      cacheMap.getEntry[OverseasAccountingMethodPropertyModel](OverseasPropertyAccountingMethod)

    def getSummary(selfEmployments: Option[Seq[SelfEmploymentData]] = None,
                   selfEmploymentsAccountingMethod: Option[AccountingMethodModel] = None,
                   isReleaseFourEnabled: Boolean = false,
                   isPropertyNextTaxYearEnabled: Boolean = false): IndividualSummary = {
      getIncomeSource match {
        case Some(IncomeSourceModel(hasSelfEmployment, hasProperty, hasForeignProperty)) =>
          applyForeignPropertyData(
            applyPropertyData(
              applySelfEmploymentsData(selfEmployments, selfEmploymentsAccountingMethod, hasSelfEmployment).asInstanceOf[IndividualSummary],
              hasProperty,
              isReleaseFourEnabled = isReleaseFourEnabled,
              isPropertyNextTaxYearEnabled = isPropertyNextTaxYearEnabled
            ).asInstanceOf[IndividualSummary],
            hasForeignProperty,
            isReleaseFourEnabled = isReleaseFourEnabled,
            isPropertyNextTaxYearEnabled = isPropertyNextTaxYearEnabled
          ).asInstanceOf[IndividualSummary]
        case _ => IndividualSummary()
      }
    }

    def getAgentSummary(selfEmployments: Option[Seq[SelfEmploymentData]] = None,
                        selfEmploymentsAccountingMethod: Option[AccountingMethodModel] = None,
                        isReleaseFourEnabled: Boolean = false): AgentSummary = {
      getIncomeSource match {
        case Some(IncomeSourceModel(hasSelfEmployment, hasProperty, hasForeignProperty)) =>
          applyForeignPropertyData(
            applyPropertyData(
              applySelfEmploymentsData(selfEmployments, selfEmploymentsAccountingMethod, hasSelfEmployment, isAgent = true).asInstanceOf[AgentSummary],
              hasProperty,
              isAgent = true,
              isReleaseFourEnabled = isReleaseFourEnabled
            ).asInstanceOf[AgentSummary],
            hasForeignProperty,
            isAgent = true,
            isReleaseFourEnabled = isReleaseFourEnabled
          ).asInstanceOf[AgentSummary]
        case _ => AgentSummary()
      }
    }

    private def applySelfEmploymentsData(selfEmployments: Option[Seq[SelfEmploymentData]],
                                         selfEmploymentsAccountingMethod: Option[AccountingMethodModel],
                                         hasSelfEmployments: Boolean,
                                         isAgent: Boolean = false): SummaryModel = {
      if (hasSelfEmployments) {
        if (selfEmploymentsAccountingMethod.isDefined) {
          if (isAgent) {
            AgentSummary(
              selectedTaxYear = getSelectedTaxYear,
              businessName = getBusinessName,
              incomeSource = getIncomeSource,
              selfEmployments = selfEmployments,
              accountingMethod = selfEmploymentsAccountingMethod
            )
          } else {
            IndividualSummary(
              incomeSource = getIncomeSource,
              businessName = getBusinessName,
              selectedTaxYear = getSelectedTaxYear,
              accountingMethod = selfEmploymentsAccountingMethod,
              selfEmployments = selfEmployments
            )
          }
        } else {
          if (isAgent) {
            AgentSummary(
              selectedTaxYear = getSelectedTaxYear,
              businessName = getBusinessName,
              incomeSource = getIncomeSource,
              selfEmployments = selfEmployments,
              accountingMethod = getAccountingMethod
            )
          } else {
            IndividualSummary(
              incomeSource = getIncomeSource,
              businessName = getBusinessName,
              selectedTaxYear = getSelectedTaxYear,
              accountingMethod = getAccountingMethod,
              selfEmployments = selfEmployments
            )
          }
        }
      } else {
        if (isAgent) {
          AgentSummary(
            incomeSource = getIncomeSource
          )
        } else {
          IndividualSummary(
            incomeSource = getIncomeSource
          )
        }
      }
    }

    private def applyPropertyData(summaryModel: SummaryModel,
                                  hasProperty: Boolean,
                                  isAgent: Boolean = false,
                                  isReleaseFourEnabled: Boolean = false,
                                  isPropertyNextTaxYearEnabled: Boolean = false
                                 ): SummaryModel = {
      if (hasProperty) {
        if (isAgent) {
          summaryModel.asInstanceOf[AgentSummary].copy(
            propertyCommencementDate = getPropertyCommencementDate,
            accountingMethodProperty = getPropertyAccountingMethod,
            selectedTaxYear = if (isReleaseFourEnabled) getSelectedTaxYear else None
          )
        } else {
          summaryModel.asInstanceOf[IndividualSummary].copy(
            propertyCommencementDate = getPropertyCommencementDate,
            accountingMethodProperty = getPropertyAccountingMethod,
            selectedTaxYear = if (isReleaseFourEnabled && isPropertyNextTaxYearEnabled) getSelectedTaxYear else None

          )
        }
      } else summaryModel
    }

    private def applyForeignPropertyData(summaryModel: SummaryModel,
                                         hasForeignProperty: Boolean,
                                         isAgent: Boolean = false,
                                         isReleaseFourEnabled: Boolean = false,
                                         isPropertyNextTaxYearEnabled: Boolean = false
                                        ): SummaryModel = {
      if (hasForeignProperty) {
        if (isAgent) {
          summaryModel.asInstanceOf[AgentSummary].copy(
            overseasPropertyCommencementDate = getOverseasPropertyCommencementDate,
            overseasAccountingMethodProperty = getOverseasPropertyAccountingMethod,
            selectedTaxYear = if (isReleaseFourEnabled) getSelectedTaxYear else None
          )
        } else {
          summaryModel.asInstanceOf[IndividualSummary].copy(
            overseasPropertyCommencementDate = getOverseasPropertyCommencementDate,
            overseasAccountingMethodProperty = getOverseasPropertyAccountingMethod,
            selectedTaxYear = if (isReleaseFourEnabled && isPropertyNextTaxYearEnabled) getSelectedTaxYear else None

          )
        }
      } else summaryModel
    }
  }

}
