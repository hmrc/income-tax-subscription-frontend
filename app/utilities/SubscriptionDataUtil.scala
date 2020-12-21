/*
 * Copyright 2021 HM Revenue & Customs
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

import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import models.common._
import models.common.business._
import models.{AgentSummary, IndividualSummary, SummaryModel}
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys._

//scalastyle:off
object SubscriptionDataUtil extends FeatureSwitching {

  implicit class CacheMapUtil(cacheMap: CacheMap) {

    def getIncomeSource: Option[IncomeSourceModel] = cacheMap.getEntry[IncomeSourceModel](IncomeSource)

    def getBusinessName: Option[BusinessNameModel] = cacheMap.getEntry[BusinessNameModel](BusinessName)

    def getSelectedTaxYear: Option[AccountingYearModel] = cacheMap.getEntry[AccountingYearModel](SelectedTaxYear)

    def getAccountingMethod: Option[AccountingMethodModel] = cacheMap.getEntry[AccountingMethodModel](AccountingMethod)

    def getPropertyAccountingMethod: Option[AccountingMethodPropertyModel] = cacheMap.getEntry[AccountingMethodPropertyModel](PropertyAccountingMethod)

    def getPropertyStartDate: Option[PropertyStartDateModel] = cacheMap.getEntry[PropertyStartDateModel](PropertyStartDate)

    def getOverseasPropertyStartDate: Option[OverseasPropertyStartDateModel] =
      cacheMap.getEntry[OverseasPropertyStartDateModel](OverseasPropertyStartDate)

    def getOverseasPropertyAccountingMethod: Option[OverseasAccountingMethodPropertyModel] =
      cacheMap.getEntry[OverseasAccountingMethodPropertyModel](OverseasPropertyAccountingMethod)

    def getSummary(selfEmployments: Option[Seq[SelfEmploymentData]] = None,
                   selfEmploymentsAccountingMethod: Option[AccountingMethodModel] = None,
                   isReleaseFourEnabled: Boolean = false): IndividualSummary = {
      getIncomeSource match {
        case Some(IncomeSourceModel(hasSelfEmployment, hasProperty, hasForeignProperty)) =>
          applyForeignPropertyData(
            applyPropertyData(
              applySelfEmploymentsData(selfEmployments, selfEmploymentsAccountingMethod, hasSelfEmployment).asInstanceOf[IndividualSummary],
              hasProperty,
              isReleaseFourEnabled = isReleaseFourEnabled
            ).asInstanceOf[IndividualSummary],
            hasForeignProperty,
            isReleaseFourEnabled = isReleaseFourEnabled
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
            incomeSource = getIncomeSource,
            selectedTaxYear = if (isEnabled(ReleaseFour)) getSelectedTaxYear else None
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
                                  isReleaseFourEnabled: Boolean = false): SummaryModel = {
      if (hasProperty) {
        if (isAgent) {
          summaryModel.asInstanceOf[AgentSummary].copy(
            propertyStartDate = getPropertyStartDate,
            accountingMethodProperty = getPropertyAccountingMethod,
            selectedTaxYear = if (isEnabled(ReleaseFour)) summaryModel.selectedTaxYear else None
          )
        } else {
          summaryModel.asInstanceOf[IndividualSummary].copy(
            propertyStartDate = getPropertyStartDate,
            accountingMethodProperty = getPropertyAccountingMethod,
            selectedTaxYear = if (isReleaseFourEnabled) getSelectedTaxYear else None
          )
        }
      } else summaryModel
    }

    private def applyForeignPropertyData(summaryModel: SummaryModel,
                                         hasForeignProperty: Boolean,
                                         isAgent: Boolean = false,
                                         isReleaseFourEnabled: Boolean = false): SummaryModel = {
      if (hasForeignProperty) {
        if (isAgent) {
          summaryModel.asInstanceOf[AgentSummary].copy(
            overseasPropertyStartDate = getOverseasPropertyStartDate,
            overseasAccountingMethodProperty = getOverseasPropertyAccountingMethod,
            selectedTaxYear = if (isEnabled(ReleaseFour)) summaryModel.selectedTaxYear else None
          )
        } else {
          summaryModel.asInstanceOf[IndividualSummary].copy(
            overseasPropertyStartDate = getOverseasPropertyStartDate,
            overseasAccountingMethodProperty = getOverseasPropertyAccountingMethod,
            selectedTaxYear = if (isReleaseFourEnabled) getSelectedTaxYear else None
          )
        }
      } else summaryModel
    }
  }

}
