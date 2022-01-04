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

import config.featureswitch.FeatureSwitching
import models.common._
import models.common.business._
import models.common.subscription.{CreateIncomeSourcesModel, OverseasProperty, SoleTraderBusinesses, UkProperty}
import models.{AccountingMethod => _, _}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.AccountingPeriodUtil.{getCurrentTaxYear, getNextTaxYear}
import utilities.SubscriptionDataKeys._

//scalastyle:off
object SubscriptionDataUtil extends FeatureSwitching {

  implicit class CacheMapUtil(cacheMap: CacheMap) {

    def getIncomeSource: Option[IncomeSourceModel] = cacheMap.getEntry[IncomeSourceModel](IncomeSource)

    def getBusinessName: Option[BusinessNameModel] = cacheMap.getEntry[BusinessNameModel](BusinessName)

    def getSelectedTaxYear: Option[AccountingYearModel] = cacheMap.getEntry[AccountingYearModel](SelectedTaxYear)

    def getAccountingMethod: Option[AccountingMethodModel] = cacheMap.getEntry[AccountingMethodModel](AccountingMethod)

    def getTaskListModel(selfEmployments: Option[Seq[SelfEmploymentData]] = None,
                         selfEmploymentAccountingMethod: Option[AccountingMethodModel] = None,
                         property: Option[PropertyModel],
                         overseasProperty: Option[OverseasPropertyModel]
                        ): TaskListModel = {
      TaskListModel(
        taxYearSelection = getSelectedTaxYear,
        selfEmployments = selfEmployments match {
          case Some(businesses) => businesses
          case None => Seq.empty[SelfEmploymentData]
        },
        ukProperty = property,
        overseasProperty = overseasProperty,
        selfEmploymentAccountingMethod = selfEmploymentAccountingMethod.map(_.accountingMethod)
      )
    }


    def createIncomeSources(nino: String,
                            selfEmployments: Option[Seq[SelfEmploymentData]] = None,
                            selfEmploymentsAccountingMethod: Option[AccountingMethodModel] = None,
                            property: Option[PropertyModel] = None,
                            overseasProperty: Option[OverseasPropertyModel] = None
                           ): CreateIncomeSourcesModel = {

      val accountingPeriod: AccountingPeriodModel = {
        getSelectedTaxYear.getOrElse(
          throw new InternalServerException("[SubscriptionDataUtil][createIncomeSource] - Could not create the create income sources model due to missing selected tax year")
        ) match {
          case AccountingYearModel(_, false) => throw new InternalServerException("[SubscriptionDataUtil][createIncomeSources] - Could not create the create income sources model as the user has not confirmed their selected tax year")
          case AccountingYearModel(Next, _) => getNextTaxYear
          case AccountingYearModel(Current, _) => getCurrentTaxYear
        }
      }

      val soleTraderBusinesses: Option[SoleTraderBusinesses] = {

        (selfEmployments, selfEmploymentsAccountingMethod) match {
          case (Some(selfEmployments), Some(accountingMethod)) if selfEmployments.forall(_.isComplete) =>
            Some(SoleTraderBusinesses(
              accountingPeriod = accountingPeriod,
              accountingMethod = accountingMethod.accountingMethod,
              businesses = selfEmployments
            ))
          case (Some(_), Some(_)) =>
            throw new InternalServerException("[SubscriptionDataUtil][createIncomeSource] - not all self employment businesses are complete")
          case (Some(_), None) =>
            throw new InternalServerException("[SubscriptionDataUtil][createIncomeSource] - self employment businesses found without any accounting method")
          case (None, Some(_)) =>
            throw new InternalServerException("[SubscriptionDataUtil][createIncomeSource] - self employment accounting method found without any self employments")
          case (None, None) => None
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
        selfEmployments = soleTraderBusinesses,
        ukProperty = ukProperty,
        overseasProperty = overseas
      )
    }

    def getSummary(selfEmployments: Option[Seq[SelfEmploymentData]] = None,
                   selfEmploymentsAccountingMethod: Option[AccountingMethodModel] = None,
                   property: Option[PropertyModel] = None,
                   overseasProperty: Option[OverseasPropertyModel] = None,
                   isReleaseFourEnabled: Boolean = false): IndividualSummary = {
      getIncomeSource match {
        case Some(IncomeSourceModel(hasSelfEmployment, hasProperty, hasForeignProperty)) =>
          applyForeignPropertyData(
            applyPropertyData(
              applySelfEmploymentsData(selfEmployments, selfEmploymentsAccountingMethod, hasSelfEmployment, isReleaseFourEnabled).asInstanceOf[IndividualSummary],
              property,
              hasProperty,
              isReleaseFourEnabled = isReleaseFourEnabled
            ).asInstanceOf[IndividualSummary],
            overseasProperty,
            hasForeignProperty,
            isReleaseFourEnabled = isReleaseFourEnabled
          ).asInstanceOf[IndividualSummary]
        case _ => IndividualSummary()
      }
    }

    def getAgentSummary(selfEmployments: Option[Seq[SelfEmploymentData]] = None,
                        selfEmploymentsAccountingMethod: Option[AccountingMethodModel] = None,
                        property: Option[PropertyModel] = None,
                        overseasProperty: Option[OverseasPropertyModel] = None,
                        isReleaseFourEnabled: Boolean = false): AgentSummary = {
      getIncomeSource match {
        case Some(IncomeSourceModel(hasSelfEmployment, hasProperty, hasForeignProperty)) =>
          applyForeignPropertyData(
            applyPropertyData(
              applySelfEmploymentsData(selfEmployments, selfEmploymentsAccountingMethod, hasSelfEmployment, isReleaseFourEnabled, isAgent = true).asInstanceOf[AgentSummary],
              property,
              hasProperty,
              isAgent = true,
              isReleaseFourEnabled = isReleaseFourEnabled
            ).asInstanceOf[AgentSummary],
            overseasProperty,
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
                                         isReleaseFourEnabled: Boolean,
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
            selectedTaxYear = if (isReleaseFourEnabled) getSelectedTaxYear else None
          )
        } else {
          IndividualSummary(
            incomeSource = getIncomeSource
          )
        }
      }
    }

    private def applyPropertyData(summaryModel: SummaryModel,
                                  property: Option[PropertyModel],
                                  hasProperty: Boolean,
                                  isAgent: Boolean = false,
                                  isReleaseFourEnabled: Boolean = false): SummaryModel = {
      if (hasProperty) {
        if (isAgent) {
          summaryModel.asInstanceOf[AgentSummary].copy(
            propertyStartDate = property.flatMap(_.startDate.map(PropertyStartDateModel.apply)),
            accountingMethodProperty = property.flatMap(_.accountingMethod.map(AccountingMethodPropertyModel.apply)),
            selectedTaxYear = if (isReleaseFourEnabled) summaryModel.selectedTaxYear else None
          )
        } else {
          summaryModel.asInstanceOf[IndividualSummary].copy(
            propertyStartDate = property.flatMap(_.startDate.map(PropertyStartDateModel.apply)),
            accountingMethodProperty = property.flatMap(_.accountingMethod.map(AccountingMethodPropertyModel.apply)),
            selectedTaxYear = if (isReleaseFourEnabled) getSelectedTaxYear else None
          )
        }
      } else summaryModel
    }

    private def applyForeignPropertyData(summaryModel: SummaryModel,
                                         overseasProperty: Option[OverseasPropertyModel],
                                         hasForeignProperty: Boolean,
                                         isAgent: Boolean = false,
                                         isReleaseFourEnabled: Boolean = false): SummaryModel = {
      if (hasForeignProperty) {
        if (isAgent) {
          summaryModel.asInstanceOf[AgentSummary].copy(
            overseasPropertyStartDate = overseasProperty.flatMap(_.startDate.map(OverseasPropertyStartDateModel.apply)),
            overseasAccountingMethodProperty = overseasProperty.flatMap(_.accountingMethod.map(OverseasAccountingMethodPropertyModel.apply)),
            selectedTaxYear = if (isReleaseFourEnabled) summaryModel.selectedTaxYear else None
          )
        } else {
          summaryModel.asInstanceOf[IndividualSummary].copy(
            overseasPropertyStartDate = overseasProperty.flatMap(_.startDate.map(OverseasPropertyStartDateModel.apply)),
            overseasAccountingMethodProperty = overseasProperty.flatMap(_.accountingMethod.map(OverseasAccountingMethodPropertyModel.apply)),
            selectedTaxYear = if (isReleaseFourEnabled) getSelectedTaxYear else None
          )
        }
      } else summaryModel
    }
  }

}
