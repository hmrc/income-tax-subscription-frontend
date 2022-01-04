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

package models


import models.common.business._
import models.common.{IncomeSourceModel, _}
import utilities.AccountingPeriodUtil._


sealed trait SummaryModel {

  def incomeSource: Option[IncomeSourceModel]

  def businessName: Option[BusinessNameModel]

  def selectedTaxYear: Option[AccountingYearModel]

  def accountingMethod: Option[AccountingMethodModel]

  def propertyStartDate: Option[PropertyStartDateModel]

  def accountingMethodProperty: Option[AccountingMethodPropertyModel]

  def overseasPropertyStartDate: Option[OverseasPropertyStartDateModel]

  def overseasAccountingMethodProperty: Option[OverseasAccountingMethodPropertyModel]
}

case class IndividualSummary(incomeSource: Option[IncomeSourceModel] = None,
                             businessName: Option[BusinessNameModel] = None,
                             selectedTaxYear: Option[AccountingYearModel] = None,
                             accountingMethod: Option[AccountingMethodModel] = None,
                             selfEmployments: Option[Seq[SelfEmploymentData]] = None,
                             propertyStartDate: Option[PropertyStartDateModel] = None,
                             accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                             overseasPropertyStartDate: Option[OverseasPropertyStartDateModel] = None,
                             overseasAccountingMethodProperty: Option[OverseasAccountingMethodPropertyModel] = None) extends SummaryModel {


  lazy val foreignPropertyComplete: Boolean = {
    overseasPropertyStartDate.isDefined && overseasAccountingMethodProperty.isDefined
  }

  def ukPropertyComplete(releaseFourEnabled: Boolean): Boolean = {
    if (releaseFourEnabled) {
      propertyStartDate.isDefined && accountingMethodProperty.isDefined
    }
    else {
      accountingMethodProperty.isDefined
    }
  }

  def selfEmploymentComplete(releaseFourEnabled: Boolean): Boolean = {
    if (releaseFourEnabled) {
      selfEmployments.exists(_.exists(_.isComplete)) && accountingMethod.isDefined
    } else {
      businessName.isDefined && accountingMethod.isDefined
    }
  }

  def toBusinessSubscriptionDetailsModel(nino: String): BusinessSubscriptionDetailsModel = {
    val useSelfEmployments = incomeSource.exists(_.selfEmployment)
    val useUkProperty = incomeSource.exists(_.ukProperty)
    val useForeignProperty = incomeSource.exists(_.foreignProperty)

    val hasValidProperty: Boolean = if (useUkProperty) propertyStartDate.isDefined && accountingMethodProperty.isDefined else true

    val hasValidForeignProperty: Boolean = if (useForeignProperty) overseasPropertyStartDate.isDefined && overseasAccountingMethodProperty.isDefined else true

    val hasValidSelfEmployments: Boolean = if (useSelfEmployments) selfEmployments.exists(_.exists(_.isComplete)) && accountingMethod.isDefined else true

    if (!hasValidProperty) throw new Exception("Missing data items for valid property submission")
    if (!hasValidForeignProperty) throw new Exception("Missing data items for valid foreign property submission")
    if (!hasValidSelfEmployments) throw new Exception("Missing data items for valid self employments submission")

    val accountingPeriodVal: Option[AccountingPeriodModel] = {
      selectedTaxYear map {
        case AccountingYearModel(Next, _) => getNextTaxYear
        case AccountingYearModel(Current, _) => getCurrentTaxYear
      }
    }

    BusinessSubscriptionDetailsModel(
      nino,
      accountingPeriodVal.getOrElse(throw new Exception("Accounting period not defined for BusinessSubscriptionDetailsModel")),
      if (useSelfEmployments) selfEmployments.map(_.filter(_.isComplete)) else None,
      if (useSelfEmployments) accountingMethod.map(_.accountingMethod) else None,
      incomeSource.getOrElse(throw new Exception("IncomeSource model not defined for BusinessSubscriptionDetailsModel")),
      if (useUkProperty) propertyStartDate else None,
      if (useUkProperty) accountingMethodProperty else None,
      if (useForeignProperty) overseasPropertyStartDate else None,
      if (useForeignProperty) overseasAccountingMethodProperty else None
    )
  }
}


case class AgentSummary(incomeSource: Option[IncomeSourceModel] = None,
                        businessName: Option[BusinessNameModel] = None,
                        selectedTaxYear: Option[AccountingYearModel] = None,
                        accountingMethod: Option[AccountingMethodModel] = None,
                        propertyStartDate: Option[PropertyStartDateModel] = None,
                        accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                        overseasPropertyStartDate: Option[OverseasPropertyStartDateModel] = None,
                        overseasAccountingMethodProperty: Option[OverseasAccountingMethodPropertyModel] = None,
                        selfEmployments: Option[Seq[SelfEmploymentData]] = None
                       ) extends SummaryModel {

  lazy val foreignPropertyComplete: Boolean = {
    overseasPropertyStartDate.isDefined && overseasAccountingMethodProperty.isDefined
  }

  def ukPropertyComplete(releaseFourEnabled: Boolean): Boolean = {
    if (releaseFourEnabled) {
      propertyStartDate.isDefined && accountingMethodProperty.isDefined
    }
    else {
      accountingMethodProperty.isDefined
    }
  }

  def selfEmploymentComplete(releaseFourEnabled: Boolean): Boolean = {
    if (releaseFourEnabled) {
      selfEmployments.exists(_.exists(_.isComplete)) && accountingMethod.isDefined
    } else {
      businessName.isDefined && accountingMethod.isDefined
      }
    }

  def toBusinessSubscriptionDetailsModel(nino: String): BusinessSubscriptionDetailsModel = {

    val useSelfEmployments = incomeSource.exists(_.selfEmployment)
    val useUkProperty = incomeSource.exists(_.ukProperty)
    val useForeignProperty = incomeSource.exists(_.foreignProperty)

    val hasValidProperty: Boolean = if (useUkProperty) propertyStartDate.isDefined && accountingMethodProperty.isDefined else true

    val hasValidForeignProperty: Boolean = if (useForeignProperty) overseasPropertyStartDate.isDefined && overseasAccountingMethodProperty.isDefined else true

    val hasValidSelfEmployments: Boolean = if (useSelfEmployments) selfEmployments.exists(_.exists(_.isComplete)) && accountingMethod.isDefined else true

    if (!hasValidProperty) throw new Exception("Missing data items for valid property submission")
    if (!hasValidForeignProperty) throw new Exception("Missing data items for valid foreign property submission")
    if (!hasValidSelfEmployments) throw new Exception("Missing data items for valid self employments submission")

    val accountingPeriodVal: Option[AccountingPeriodModel] =
      selectedTaxYear map {
        case AccountingYearModel(Next, _) => getNextTaxYear
        case AccountingYearModel(Current, _) => getCurrentTaxYear
      }

    BusinessSubscriptionDetailsModel(
      nino,
      accountingPeriodVal.getOrElse(throw new Exception("Accounting period not defined for BusinessSubscriptionDetailsModel")),
      if (useSelfEmployments) selfEmployments.map(_.filter(_.isComplete)) else None,
      if (useSelfEmployments) accountingMethod.map(_.accountingMethod) else None,
      incomeSource.getOrElse(throw new Exception("IncomeSource model not defined for BusinessSubscriptionDetailsModel")),
      if (useUkProperty) propertyStartDate else None,
      if (useUkProperty) accountingMethodProperty else None,
      if (useForeignProperty) overseasPropertyStartDate else None,
      if (useForeignProperty) overseasAccountingMethodProperty else None
    )
  }

}
