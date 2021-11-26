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

package models.common

import models.AccountingMethod
import models.common.business.SelfEmploymentData
import play.api.libs.json.{Format, Json}

case class TaskListModel(taxYearSelection: Option[AccountingYearModel],
                         selfEmployments: Seq[SelfEmploymentData],
                         selfEmploymentAccountingMethod: Option[AccountingMethod],
                         ukProperty:  Option[PropertyModel],
                         overseasProperty: Option[OverseasPropertyModel]) {

  val taxYearSelectedAndConfirmed: Boolean = taxYearSelection.exists(_.confirmed)

  val taxYearSelectedNotConfirmed: Boolean = taxYearSelection.exists(!_.confirmed)

  val ukPropertyComplete: Boolean =  ukProperty.exists(_.confirmed)

  var selfEmploymentsComplete: Boolean = selfEmployments.forall(_.confirmed) && selfEmploymentAccountingMethod.isDefined

  val overseasPropertyComplete: Boolean = overseasProperty.exists(_.startDate.isDefined) && overseasProperty.exists(_.accountingMethod.isDefined)

  val sectionsTotal: Int = Math.max(2, 1 + selfEmployments.size + ukProperty.size + overseasProperty.size)

  val sectionsComplete: Int = (if(taxYearSelectedAndConfirmed) 1 else 0) +
    selfEmployments.count { business => business.confirmed && selfEmploymentAccountingMethod.isDefined} +
    (if(ukPropertyComplete) 1 else 0) +
    (if(overseasPropertyComplete)1 else 0)

  var canAddMoreBusinesses: Boolean = selfEmployments.size < 50  || ukProperty.isEmpty || overseasProperty.isEmpty

  def taskListComplete: Boolean = sectionsComplete == sectionsTotal

}

object TaskListModel {
  implicit val format: Format[TaskListModel] = Json.format[TaskListModel]
}
