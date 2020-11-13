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


import models.common.{IncomeSourceModel, _}
import models.individual.business._
import utilities.AccountingPeriodUtil._


sealed trait SummaryModel {

  def incomeSource: Option[IncomeSourceModel]

  def businessName: Option[BusinessNameModel]

  def selectedTaxYear: Option[AccountingYearModel]

  def accountingMethod: Option[AccountingMethodModel]

  def propertyCommencementDate: Option[PropertyCommencementDateModel]

  def accountingMethodProperty: Option[AccountingMethodPropertyModel]

  def overseasPropertyCommencementDate: Option[OverseasPropertyCommencementDateModel]

  def overseasAccountingMethodProperty: Option[OverseasAccountingMethodPropertyModel]
}

//scalastyle:off
case class IndividualSummary(incomeSource: Option[IncomeSourceModel] = None,
                             businessName: Option[BusinessNameModel] = None,
                             selectedTaxYear: Option[AccountingYearModel] = None,
                             accountingMethod: Option[AccountingMethodModel] = None,
                             selfEmployments: Option[Seq[SelfEmploymentData]] = None,
                             propertyCommencementDate: Option[PropertyCommencementDateModel] = None,
                             accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                             overseasPropertyCommencementDate: Option[OverseasPropertyCommencementDateModel] = None,
                             overseasAccountingMethodProperty: Option[OverseasAccountingMethodPropertyModel] = None) extends SummaryModel {


  lazy val foreignPropertyComplete: Boolean = {
    overseasPropertyCommencementDate.isDefined && overseasAccountingMethodProperty.isDefined
  }

  def ukPropertyComplete(releaseFourEnabled: Boolean): Boolean = {
    if (releaseFourEnabled) {
      propertyCommencementDate.isDefined && accountingMethodProperty.isDefined
    }
    else {
      accountingMethodProperty.isDefined
    }
  }

  def selfEmploymentComplete(releaseFourEnabled: Boolean, ignoreSelectedTaxYear: Boolean): Boolean = {
    if (releaseFourEnabled) {
      if (ignoreSelectedTaxYear) {
        selfEmployments.exists(_.exists(_.isComplete)) && accountingMethod.isDefined
      } else {
        selectedTaxYear.isDefined && selfEmployments.exists(_.exists(_.isComplete)) && accountingMethod.isDefined
      }
    }
    else {
      if (ignoreSelectedTaxYear) {
        businessName.isDefined && accountingMethod.isDefined
      } else {
        selectedTaxYear.isDefined && businessName.isDefined && accountingMethod.isDefined
      }
    }
  }

  def toBusinessSubscriptionDetailsModel(isPropertyNextTaxYearEnabled: Boolean): BusinessSubscriptionDetailsModel = {
    val useSelfEmployments = incomeSource.exists(_.selfEmployment)
    val useUkProperty = incomeSource.exists(_.ukProperty)
    val useForeignProperty = incomeSource.exists(_.foreignProperty)

    val hasValidProperty: Boolean = if (useUkProperty) propertyCommencementDate.isDefined && accountingMethodProperty.isDefined else true

    val hasValidForeignProperty: Boolean = if (useForeignProperty) overseasPropertyCommencementDate.isDefined && overseasAccountingMethodProperty.isDefined else true

    val hasValidSelfEmployments: Boolean = if (useSelfEmployments) selfEmployments.exists(_.exists(_.isComplete)) && accountingMethod.isDefined else true

    if (!hasValidProperty) throw new Exception("Missing data items for valid property submission")
    if (!hasValidForeignProperty) throw new Exception("Missing data items for valid foreign property submission")
    if (!hasValidSelfEmployments) throw new Exception("Missing data items for valid self employments submission")

    val accountingPeriodVal: Option[AccountingPeriodModel] = {
      if (isPropertyNextTaxYearEnabled) {
        selectedTaxYear map {
          case AccountingYearModel(Next) => getNextTaxYear
          case AccountingYearModel(Current) => getCurrentTaxYear
        }
      } else {
        if (incomeSource.exists(sources => sources.ukProperty || sources.foreignProperty)) Some(getCurrentTaxYear)
        else selectedTaxYear map {
          case AccountingYearModel(Next) => getNextTaxYear
          case AccountingYearModel(Current) => getCurrentTaxYear
        }
      }
    }

    BusinessSubscriptionDetailsModel(
      accountingPeriodVal.getOrElse(throw new Exception("Accounting period not defined for BusinessSubscriptionDetailsModel")),
      if (useSelfEmployments) selfEmployments.map(_.filter(_.isComplete)) else None,
      if (useSelfEmployments) accountingMethod.map(_.accountingMethod) else None,
      incomeSource.getOrElse(throw new Exception("IncomeSource model not defined for BusinessSubscriptionDetailsModel")),
      if (useUkProperty) propertyCommencementDate else None,
      if (useUkProperty) accountingMethodProperty else None,
      if (useForeignProperty) overseasPropertyCommencementDate else None,
      if (useForeignProperty) overseasAccountingMethodProperty else None
    )
  }
}


case class AgentSummary(incomeSource: Option[IncomeSourceModel] = None,
                        businessName: Option[BusinessNameModel] = None,
                        selectedTaxYear: Option[AccountingYearModel] = None,
                        accountingMethod: Option[AccountingMethodModel] = None,
                        propertyCommencementDate: Option[PropertyCommencementDateModel] = None,
                        accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                        overseasPropertyCommencementDate: Option[OverseasPropertyCommencementDateModel] = None,
                        overseasAccountingMethodProperty: Option[OverseasAccountingMethodPropertyModel] = None,
                        selfEmployments: Option[Seq[SelfEmploymentData]] = None
                       ) extends SummaryModel {

  lazy val toBusinessSubscriptionDetailsModel: BusinessSubscriptionDetailsModel = {
    val useSelfEmployments = incomeSource.exists(_.selfEmployment)
    val useUkProperty = incomeSource.exists(_.ukProperty)
    val useForeignProperty = incomeSource.exists(_.foreignProperty)

    val hasValidProperty: Boolean = if (useUkProperty) propertyCommencementDate.isDefined && accountingMethodProperty.isDefined else true

    val hasValidForeignProperty: Boolean = if (useForeignProperty) overseasPropertyCommencementDate.isDefined && overseasAccountingMethodProperty.isDefined else true

    val hasValidSelfEmployments: Boolean = if (useSelfEmployments) selfEmployments.exists(_.exists(_.isComplete)) && accountingMethod.isDefined else true

    if (!hasValidProperty) throw new Exception("Missing data items for valid property submission")
    if (!hasValidForeignProperty) throw new Exception("Missing data items for valid foreign property submission")
    if (!hasValidSelfEmployments) throw new Exception("Missing data items for valid self employments submission")

    val accountingPeriodVal: Option[AccountingPeriodModel] =
      if (useUkProperty || useForeignProperty) Some(getCurrentTaxYear)
      else selectedTaxYear map {
        case AccountingYearModel(Next) => getNextTaxYear
        case AccountingYearModel(Current) => getCurrentTaxYear
      }

    BusinessSubscriptionDetailsModel(
      accountingPeriodVal.getOrElse(throw new Exception("Accounting period not defined for BusinessSubscriptionDetailsModel")),
      if (useSelfEmployments) selfEmployments.map(_.filter(_.isComplete)) else None,
      if (useSelfEmployments) accountingMethod.map(_.accountingMethod) else None,
      incomeSource.getOrElse(throw new Exception("IncomeSource model not defined for BusinessSubscriptionDetailsModel")),
      if (useUkProperty) propertyCommencementDate else None,
      if (useUkProperty) accountingMethodProperty else None,
      if (useForeignProperty) overseasPropertyCommencementDate else None,
      if (useForeignProperty) overseasAccountingMethodProperty else None
    )
  }

}
