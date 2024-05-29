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

import models.common.business._
import models.{Cash, DateModel, Next}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class TaskListModelSpec extends AnyWordSpecLike with Matchers with OptionValues {

  val date: DateModel = DateModel("1", "2", "1980")

  val completeSeModel: SelfEmploymentData = SelfEmploymentData(
    id = "",
    businessStartDate = Some(BusinessStartDate(date)),
    businessName = Some(BusinessNameModel("Fake Name")),
    businessTradeName = Some(BusinessTradeNameModel("Trade")),
    businessAddress = Some(BusinessAddressModel(Address(Seq("line1"), Some("Postcode")))),
    confirmed = true
  )

  val incompleteSeModel: SelfEmploymentData = SelfEmploymentData(
    id = "",
    businessStartDate = Some(BusinessStartDate(date)),
    businessName = None,
    businessTradeName = Some(BusinessTradeNameModel("Trade")),
    businessAddress = Some(BusinessAddressModel(Address(Seq("line1"), Some("Postcode"))))
  )

  private val confirmedEditableYearNext = Some(AccountingYearModel(Next, confirmed = true))

  val fullCompleteTaskListModel: TaskListModel = TaskListModel(
    taxYearSelection = confirmedEditableYearNext,
    selfEmployments = Seq(completeSeModel),
    ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
    overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true)),
    incomeSourcesConfirmed = Some(true)
  )

  val emptyTaskListModel: TaskListModel = TaskListModel(
    taxYearSelection = None,
    selfEmployments = Seq.empty,
    ukProperty = None,
    overseasProperty = None,
    incomeSourcesConfirmed = None
  )

  "TaskListModel.sectionsTotal" should {
    "return 3" in {
      fullCompleteTaskListModel.sectionsTotal shouldBe 3
    }
  }

  "TaskListModel.sectionsComplete" when {
    "have the correct total" when {
      "the income sources section is not complete and the tax year is not selected" in {
        fullCompleteTaskListModel.copy(incomeSourcesConfirmed = None, taxYearSelection = None)
          .sectionsComplete shouldBe 1
      }
      "the income sources section is complete and the tax year is not selected" in {
        fullCompleteTaskListModel.copy(taxYearSelection = None)
          .sectionsComplete shouldBe 2
      }
      "the income sources section is complete and the tax year is confirmed" in {
        fullCompleteTaskListModel
          .sectionsComplete shouldBe 3
      }
    }
  }

  "TaskListModel.taskListComplete" when {
    "return true" when {
      "a tax year is selected and the income sources are complete" in {
        fullCompleteTaskListModel
          .taskListComplete shouldBe true
      }
    }
    "return false" when {
      "a tax year is not selected but income sources are complete" in {
        fullCompleteTaskListModel.copy(taxYearSelection = None)
          .taskListComplete shouldBe false
      }
      "a tax year is selected but income sources are not complete" in {
        fullCompleteTaskListModel.copy(incomeSourcesConfirmed = None)
          .taskListComplete shouldBe false
      }
    }
  }

}
