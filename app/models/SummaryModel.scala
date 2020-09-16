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

package models

import models.common._
import models.individual.business.{PropertyCommencementDateModel, _}
import models.individual.incomesource.IncomeSourceModel
import models.individual.subscription.IncomeSourceType
import utilities.AccountingPeriodUtil._

sealed trait SummaryModel {
  def incomeSourceIndiv: Option[IncomeSourceModel]

  //  agent
  def incomeSource: Option[IncomeSourceType]

  def matchTaxYear: Option[MatchTaxYearModel]

  def accountingPeriodDate: Option[AccountingPeriodModel]

  def businessName: Option[BusinessNameModel]

  def selectedTaxYear: Option[AccountingYearModel]

  def accountingMethod: Option[AccountingMethodModel]

  def propertyCommencementDate: Option[PropertyCommencementDateModel]

  def accountingMethodProperty: Option[AccountingMethodPropertyModel]
}


case class IndividualSummary(incomeSourceIndiv: Option[IncomeSourceModel] = None,
                             incomeSource: Option[IncomeSourceType] = None,
                             matchTaxYear: Option[MatchTaxYearModel] = None,
                             accountingPeriodDate: Option[AccountingPeriodModel] = None,
                             businessName: Option[BusinessNameModel] = None,
                             selectedTaxYear: Option[AccountingYearModel] = None,
                             accountingMethod: Option[AccountingMethodModel] = None,
                             propertyCommencementDate: Option[PropertyCommencementDateModel] = None,
                             accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                             selfEmployments: Option[Seq[SelfEmploymentData]] = None,
                             overseasPropertyCommencementDateModel: Option[OverseasPropertyCommencementDateModel] = None,
                             overseasAccountingMethodPropertyModel: Option[OverseasAccountingMethodPropertyModel] = None) extends SummaryModel {

  lazy val toBusinessSubscriptionDetailsModel: BusinessSubscriptionDetailsModel = {
    val useSelfEmployments = incomeSourceIndiv.exists(_.selfEmployment)
    val useUkProperty = incomeSourceIndiv.exists(_.ukProperty)
    val useForeignProperty = incomeSourceIndiv.exists(_.foreignProperty)

    val hasValidProperty: Boolean = if (useUkProperty) propertyCommencementDate.isDefined && accountingMethodProperty.isDefined else true

    val hasValidForeignProperty: Boolean = if (useForeignProperty) overseasPropertyCommencementDateModel.isDefined && overseasAccountingMethodPropertyModel.isDefined else true

    val hasValidSelfEmployments: Boolean = if (useSelfEmployments) selfEmployments.exists(_.exists(_.isComplete)) && accountingMethod.isDefined else true

    if (!hasValidProperty) throw new Exception("Missing data items for valid property submission")
    if (!hasValidForeignProperty) throw new Exception("Missing data items for valid foreign property submission")
    if (!hasValidSelfEmployments) throw new Exception("Missing data items for valid self employments submission")

    val accountingPeriodVal: Option[AccountingPeriodModel] =
      if (incomeSourceIndiv.exists(sources => sources.ukProperty || sources.foreignProperty)) Some(getCurrentTaxYear)
      else accountingPeriodDate

    BusinessSubscriptionDetailsModel(
      accountingPeriodVal.getOrElse(throw new Exception("Accounting period not defined for BusinessSubscriptionDetailsModel")),
      if(useSelfEmployments) selfEmployments.map(_.filter(_.isComplete)) else None,
      if(useSelfEmployments) accountingMethod.map(_.accountingMethod) else None,
      incomeSourceIndiv.getOrElse(throw new Exception("IncomeSource model not defined for BusinessSubscriptionDetailsModel")),
      if(useUkProperty) propertyCommencementDate else None,
      if(useUkProperty) accountingMethodProperty else None,
      if(useForeignProperty) overseasPropertyCommencementDateModel else None,
      if(useForeignProperty) overseasAccountingMethodPropertyModel else None
    )
  }

}



case class AgentSummary(incomeSourceIndiv: Option[IncomeSourceModel] = None,
                        incomeSource: Option[IncomeSourceType] = None,
                        matchTaxYear: Option[MatchTaxYearModel] = None,
                        accountingPeriodDate: Option[AccountingPeriodModel] = None,
                        businessName: Option[BusinessNameModel] = None,
                        selectedTaxYear: Option[AccountingYearModel] = None,
                        accountingMethod: Option[AccountingMethodModel] = None,
                        accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                        propertyCommencementDate: Option[PropertyCommencementDateModel] = None) extends SummaryModel
