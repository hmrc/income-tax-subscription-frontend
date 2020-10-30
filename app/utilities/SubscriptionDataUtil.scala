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
import models.common._
import models.individual.business._
import models.{AgentSummary, IndividualSummary}
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys._

object SubscriptionDataUtil {

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
                   selfEmploymentsAccountingMethod: Option[AccountingMethodModel] = None
                  )(implicit appConfig: AppConfig): IndividualSummary = {
      getIncomeSource match {
        case Some(IncomeSourceModel(hasSelfEmployment, hasProperty, hasForeignProperty)) =>
          applyForeignPropertyData(
            applyPropertyData(
              applySelfEmploymentsData(selfEmployments, selfEmploymentsAccountingMethod, hasSelfEmployment),
              hasProperty
            ),
            hasForeignProperty
          )
        case _ => IndividualSummary()
      }
    }

    def getAgentSummary: AgentSummary = {
      getIncomeSource match {
        case Some(incomeSourceModel) =>
          applyAgentOverseasPropertyData(
            agentSummary = applyAgentUKPropertyData(
              agentSummary = applyAgentBusinessData(
                hasBusiness = incomeSourceModel.selfEmployment
              ),
              hasUKProperty = incomeSourceModel.ukProperty
            ),
            hasOverseasProperty = incomeSourceModel.foreignProperty
          )
        case None => AgentSummary()
      }
    }

    private def applyAgentBusinessData(hasBusiness: Boolean): AgentSummary = {
      if (hasBusiness) {
        AgentSummary(
          incomeSource = getIncomeSource,
          businessName = getBusinessName,
          selectedTaxYear = getSelectedTaxYear,
          accountingMethod = getAccountingMethod
        )
      } else {
        AgentSummary(
          incomeSource = getIncomeSource
        )
      }
    }

    private def applyAgentUKPropertyData(agentSummary: AgentSummary, hasUKProperty: Boolean): AgentSummary = {
      if (hasUKProperty) {
        agentSummary.copy(
          propertyCommencementDate = getPropertyCommencementDate,
          accountingMethodProperty = getPropertyAccountingMethod,
          selectedTaxYear = None
        )
      } else {
        agentSummary
      }
    }

    private def applyAgentOverseasPropertyData(agentSummary: AgentSummary, hasOverseasProperty: Boolean): AgentSummary = {
      if (hasOverseasProperty) {
        agentSummary.copy(
          overseasPropertyCommencementDate = getOverseasPropertyCommencementDate,
          overseasAccountingMethodProperty = getOverseasPropertyAccountingMethod,
          selectedTaxYear = None
        )
      } else {
        agentSummary
      }
    }

    private def applySelfEmploymentsData(selfEmployments: Option[Seq[SelfEmploymentData]],
                                         selfEmploymentsAccountingMethod: Option[AccountingMethodModel],
                                         hasSelfEmployments: Boolean) = {
      if (hasSelfEmployments) {
        if (selfEmploymentsAccountingMethod.isDefined) {
          IndividualSummary(
            incomeSource = getIncomeSource,
            businessName = getBusinessName,
            selectedTaxYear = getSelectedTaxYear,
            accountingMethod = selfEmploymentsAccountingMethod,
            selfEmployments = selfEmployments
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
      } else IndividualSummary(
        incomeSource = getIncomeSource
      )
    }

    private def applyPropertyData(individualSummary: IndividualSummary,
                                  hasProperty: Boolean) = {
      if (hasProperty) {
        individualSummary.copy(
          propertyCommencementDate = getPropertyCommencementDate,
          accountingMethodProperty = getPropertyAccountingMethod,
          selectedTaxYear = None
        )
      } else individualSummary
    }

    private def applyForeignPropertyData(individualSummary: IndividualSummary,
                                         hasForeignProperty: Boolean) = {
      if (hasForeignProperty) {
        individualSummary.copy(
          overseasPropertyCommencementDate = getOverseasPropertyCommencementDate,
          overseasAccountingMethodProperty = getOverseasPropertyAccountingMethod,
          selectedTaxYear = None
        )
      } else individualSummary
    }
  }

}
