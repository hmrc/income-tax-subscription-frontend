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

import models.common.{AccountingMethodModel, AccountingMethodPropertyModel, AccountingYearModel, BusinessNameModel}
import models.individual.business._
import  models.individual.business.PropertyCommencementDateModel
import models.individual.incomesource.IncomeSourceModel
import models.individual.subscription.IncomeSourceType


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
                             selfEmployments: Option[Seq[SelfEmploymentData]] = None) extends SummaryModel



case class AgentSummary(incomeSourceIndiv: Option[IncomeSourceModel] = None,
                        incomeSource: Option[IncomeSourceType] = None,
                        matchTaxYear: Option[MatchTaxYearModel] = None,
                        accountingPeriodDate: Option[AccountingPeriodModel] = None,
                        businessName: Option[BusinessNameModel] = None,
                        selectedTaxYear: Option[AccountingYearModel] = None,
                        accountingMethod: Option[AccountingMethodModel] = None,
                        accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                        propertyCommencementDate: Option[PropertyCommencementDateModel] = None) extends SummaryModel
