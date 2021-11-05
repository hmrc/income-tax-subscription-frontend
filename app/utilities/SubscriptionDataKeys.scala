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

object SubscriptionDataKeys {
  // n.b. IncomeSource is still used on the agent flow
  val IncomeSource = "IncomeSource"
  val BusiStartDate = "BusinessStartDate"
  val PropertyIncome = "PropertyIncome"
  val SoleTrader = "SoleTrader"
  val BusinessName = "BusinessName"
  val BusiTradeName = "BusinessTradeName"
  val BusinessPhoneNumber = "BusinessPhoneNumber"
  val BusinessAddress = "BusinessAddress"
  val SelectedTaxYear = "SelectedTaxYear"
  val AccountingMethod = "AccountingMethod"
  val PropertyAccountingMethod = "PropertyAccountingMethod"
  val OverseasPropertyAccountingMethod = "OverseasPropertyAccountingMethod"
  val MtditId = "MtditId"
  val PaperlessPreferenceToken = "PaperlessPreferenceKey"
  val PropertyStartDate = "PropertyStartDate"
  val SelfEmployments = "SelfEmployments"
  val BusinessAccountingMethod = "BusinessAccountingMethod"
  val OverseasPropertyStartDate = "OverseasPropertyStartDate"
  val Property = "Property"

  val subscriptionId = "subscriptionId"
  val lastUpdatedTimestamp = "lastUpdatedTimestamp"

  val BusinessesKey = "Businesses"
}
