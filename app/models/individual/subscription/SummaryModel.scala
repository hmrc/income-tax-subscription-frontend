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

package models.individual.subscription

import models.common.{AccountingMethodModel, AccountingMethodPropertyModel, AccountingYearModel, BusinessNameModel}
import models.individual.business._
import models.individual.business.address.Address
import models.individual.incomesource.IncomeSourceModel


sealed trait SummaryModel {
  def incomeSourceIndiv: Option[IncomeSourceModel]

  //  agent
  def incomeSource: Option[IncomeSourceType]

  def matchTaxYear: Option[MatchTaxYearModel]

  def accountingPeriodDate: Option[AccountingPeriodModel]

  def businessName: Option[BusinessNameModel]

  def businessPhoneNumber: Option[BusinessPhoneNumberModel]

  def businessAddress: Option[Address]

  def businessStartDate: Option[BusinessStartDateModel]

  def selectedTaxYear: Option[AccountingYearModel]

  def accountingMethod: Option[AccountingMethodModel]

  def accountingMethodProperty: Option[AccountingMethodPropertyModel]

}


case class IndividualSummary(incomeSourceIndiv: Option[IncomeSourceModel] = None,
                             incomeSource: Option[IncomeSourceType] = None,
                             matchTaxYear: Option[MatchTaxYearModel] = None,
                             accountingPeriodDate: Option[AccountingPeriodModel] = None,
                             businessName: Option[BusinessNameModel] = None,
                             businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                             businessAddress: Option[Address] = None,
                             businessStartDate: Option[BusinessStartDateModel] = None,
                             selectedTaxYear: Option[AccountingYearModel] = None,
                             accountingMethod: Option[AccountingMethodModel] = None,
                             accountingMethodProperty: Option[AccountingMethodPropertyModel] = None) extends SummaryModel


case class AgentSummary(incomeSourceIndiv: Option[IncomeSourceModel] = None,
                        incomeSource: Option[IncomeSourceType] = None,
                        matchTaxYear: Option[MatchTaxYearModel] = None,
                        accountingPeriodDate: Option[AccountingPeriodModel] = None,
                        businessName: Option[BusinessNameModel] = None,
                        businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                        businessAddress: Option[Address] = None,
                        businessStartDate: Option[BusinessStartDateModel] = None,
                        selectedTaxYear: Option[AccountingYearModel] = None,
                        accountingMethod: Option[AccountingMethodModel] = None,
                        accountingMethodProperty: Option[AccountingMethodPropertyModel] = None) extends SummaryModel
