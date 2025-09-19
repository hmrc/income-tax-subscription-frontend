/*
 * Copyright 2023 HM Revenue & Customs
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
  val SelectedTaxYear = "SelectedTaxYear"
  val AccountingPeriod = "AccountingPeriod"
  val Property = "Property"
  val OverseasProperty = "OverseasProperty"
  val IncomeSourceConfirmation = "IncomeSourceConfirmation"
  val SoleTraderBusinessesKey = "SoleTraderBusinesses"
  val EligibilityInterruptPassed = "EligibilityInterruptPassed"

  val subscriptionId = "subscriptionId"
  val lastUpdatedTimestamp = "lastUpdatedTimestamp"

  //Boolean flag indicating if pre pop complete, even if there was no pre pop data
  val PrePopFlag = "PrePopFlag"
}
