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

import models.common.business.SelfEmploymentData

case class TaskListModel(taxYearSelection: Option[AccountingYearModel],
                         selfEmployments: Seq[SelfEmploymentData],
                         ukProperty: Option[PropertyModel],
                         overseasProperty: Option[OverseasPropertyModel],
                         incomeSourcesConfirmed: Option[Boolean]) {

  val hasAnyBusiness: Boolean = selfEmployments.nonEmpty || ukProperty.nonEmpty || overseasProperty.nonEmpty

  val taxYearSelectedAndConfirmed: Boolean = taxYearSelection.exists(taxYear => !taxYear.editable || taxYear.confirmed)

  val taxYearSelectedNotConfirmed: Boolean = taxYearSelection.exists(taxYear => taxYear.editable && !taxYear.confirmed)

  val permitTaxYearChange: Boolean = taxYearSelection.forall(taxYear => taxYear.editable)

  val incomeSourcesComplete: Boolean = incomeSourcesConfirmed.contains(true)

  def sectionsTotal: Int = {
    3
  }

  def sectionsComplete: Int = {
    (if (taxYearSelectedAndConfirmed) 1 else 0) +
      (if (incomeSourcesComplete) 1 else 0) + 1
  }

  def taskListComplete: Boolean = {
    sectionsComplete == sectionsTotal
  }

}
