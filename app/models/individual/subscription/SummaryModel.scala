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

import models.individual.business._
import models.individual.business.address.Address
import models.individual.incomesource.{AreYouSelfEmployedModel, RentUkPropertyModel}


sealed trait SummaryModel {
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


case class IndividualSummary(rentUkProperty: Option[RentUkPropertyModel] = None,
                             areYouSelfEmployed: Option[AreYouSelfEmployedModel] = None,
                             matchTaxYear: Option[MatchTaxYearModel] = None,
                             accountingPeriodDate: Option[AccountingPeriodModel] = None,
                             businessName: Option[BusinessNameModel] = None,
                             businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                             businessAddress: Option[Address] = None,
                             businessStartDate: Option[BusinessStartDateModel] = None,
                             selectedTaxYear: Option[AccountingYearModel] = None,
                             accountingMethod: Option[AccountingMethodModel] = None,
                             accountingMethodProperty: Option[AccountingMethodPropertyModel] = None) extends SummaryModel {

  def incomeSource: Option[IncomeSourceType] =
    rentUkProperty.flatMap(rentUkProperty => IncomeSourceType.from(rentUkProperty, areYouSelfEmployed))

}


case class AgentSummary(incomeSource: Option[IncomeSourceType] = None,
                        matchTaxYear: Option[MatchTaxYearModel] = None,
                        accountingPeriodDate: Option[AccountingPeriodModel] = None,
                        businessName: Option[BusinessNameModel] = None,
                        businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                        businessAddress: Option[Address] = None,
                        businessStartDate: Option[BusinessStartDateModel] = None,
                        selectedTaxYear: Option[AccountingYearModel] = None,
                        accountingMethod: Option[AccountingMethodModel] = None,
                        accountingMethodProperty: Option[AccountingMethodPropertyModel] = None) extends SummaryModel