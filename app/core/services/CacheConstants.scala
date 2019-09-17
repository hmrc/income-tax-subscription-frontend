/*
 * Copyright 2019 HM Revenue & Customs
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

package core.services

object CacheConstants {
  // n.b. IncomeSource is still used on the agent flow
  val IncomeSource = "IncomeSource"
  val RentUkProperty = "RentUkProperty"
  val AreYouSelfEmployed = "AreYouSelfEmployed"
  val PropertyIncome = "PropertyIncome"
  val SoleTrader = "SoleTrader"
  val MatchTaxYear = "MatchTaxYear"
  val BusinessName = "BusinessName"
  val BusinessPhoneNumber = "BusinessPhoneNumber"
  val BusinessAddress = "BusinessAddress"
  val BusinessStartDate = "BusinessStartDate"
  val AccountingPeriodDate = "AccountingPeriodDate"
  val AccountingMethod = "AccountingMethod"
  val Terms = "Terms"
  val OtherIncome = "OtherIncome"
  val MtditId = "MtditId"
  val PaperlessPreferenceToken = "PaperlessPreferenceKey"
}
