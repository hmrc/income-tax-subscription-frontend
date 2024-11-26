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

package models.common

import models.AccountingMethod
import models.common.business.SelfEmploymentData

case class IncomeSources(selfEmployments: Seq[SelfEmploymentData],
                         selfEmploymentAccountingMethod: Option[AccountingMethod],
                         ukProperty: Option[PropertyModel],
                         foreignProperty: Option[OverseasPropertyModel]) {

  val hasNoIncomeSources: Boolean = selfEmployments.isEmpty && ukProperty.isEmpty && foreignProperty.isEmpty

  val isComplete: Boolean = {
    val incomeSourceExists: Boolean = selfEmployments.nonEmpty || ukProperty.nonEmpty || foreignProperty.nonEmpty
    val selfEmploymentsComplete: Boolean = selfEmployments.isEmpty || (selfEmployments.forall(_.confirmed) && selfEmploymentAccountingMethod.isDefined)
    val ukPropertyComplete: Boolean = ukProperty.forall(_.confirmed)
    val foreignPropertyComplete: Boolean = foreignProperty.forall(_.confirmed)

    incomeSourceExists && selfEmploymentsComplete && ukPropertyComplete && foreignPropertyComplete
  }

}
