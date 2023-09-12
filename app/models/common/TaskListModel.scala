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

case class TaskListModel(taxYearSelection: Option[AccountingYearModel],
                         selfEmployments: Seq[SelfEmploymentData],
                         selfEmploymentAccountingMethod: Option[AccountingMethod],
                         ukProperty: Option[PropertyModel],
                         overseasProperty: Option[OverseasPropertyModel],
                         incomeSourcesConfirmed: Option[Boolean]) {

  val hasAnyBusiness: Boolean = selfEmployments.nonEmpty || ukProperty.nonEmpty || overseasProperty.nonEmpty

  val taxYearSelectedAndConfirmed: Boolean = taxYearSelection.exists(taxYear => !taxYear.editable || taxYear.confirmed)

  val taxYearSelectedNotConfirmed: Boolean = taxYearSelection.exists(taxYear => taxYear.editable && !taxYear.confirmed)

  val permitTaxYearChange: Boolean = taxYearSelection.exists(taxYear => taxYear.editable)

  val ukPropertyComplete: Boolean = ukProperty.exists(_.confirmed)

  val selfEmploymentsComplete: Boolean = selfEmployments.nonEmpty && selfEmployments.forall(_.confirmed) && selfEmploymentAccountingMethod.isDefined

  val overseasPropertyComplete: Boolean = overseasProperty.exists(_.confirmed)

  val incomeSourcesComplete: Boolean = incomeSourcesConfirmed.contains(true)

  def sectionsTotal(taskListRedesignEnabled: Boolean = false): Int = {
    if (taskListRedesignEnabled) {
      3 // always this many sections in new design, no longer counting individual businesses
    } else {
      Math.max(3, 2 + selfEmployments.size + ukProperty.size + overseasProperty.size)
    }
  }

  def sectionsComplete(taskListRedesignEnabled: Boolean = false): Int = {
    if (taskListRedesignEnabled) {
      (if (taxYearSelectedAndConfirmed) 1 else 0) +
        (if (incomeSourcesComplete) 1 else 0) + 1
    } else {
      (if (taxYearSelectedAndConfirmed) 1 else 0) +
        selfEmployments.count { business => business.confirmed && selfEmploymentAccountingMethod.isDefined } +
        (if (ukPropertyComplete) 1 else 0) +
        (if (overseasPropertyComplete) 1 else 0) + 1
    }
  }

  def canAddMoreBusinesses(maxSelfEmployments: Int): Boolean =
    selfEmployments.size < maxSelfEmployments ||
      ukProperty.isEmpty ||
      overseasProperty.isEmpty

  def taskListComplete(taskListRedesignEnabled: Boolean = false): Boolean = {
    sectionsComplete(taskListRedesignEnabled) == sectionsTotal(taskListRedesignEnabled)
  }

}
