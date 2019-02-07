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

package agent.models.enums

import core.models.{No, Yes}

sealed trait AccountingPeriodViewType

case object CurrentAccountingPeriodView extends AccountingPeriodViewType

case object NextAccountingPeriodView extends AccountingPeriodViewType


object AccountingPeriodViewType {

  import agent.models.AccountingPeriodPriorModel

  implicit def conv(accountingPeriodPrior: AccountingPeriodPriorModel): AccountingPeriodViewType =
    accountingPeriodPrior.currentPeriodIsPrior match {
      case Yes =>
        NextAccountingPeriodView
      case No =>
        CurrentAccountingPeriodView
    }

}