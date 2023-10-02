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
  private val unconfirmedEditableYearNext = Some(AccountingYearModel(Next))

  val fullCompleteTaskListModel: TaskListModel = TaskListModel(
    taxYearSelection = confirmedEditableYearNext,
    selfEmployments = Seq(completeSeModel),
    selfEmploymentAccountingMethod = Some(Cash),
    ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
    overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true)),
    incomeSourcesConfirmed = Some(true)
  )

  val emptyTaskListModel: TaskListModel = TaskListModel(
    taxYearSelection = None,
    selfEmployments = Seq.empty,
    selfEmploymentAccountingMethod = None,
    ukProperty = None,
    overseasProperty = None,
    incomeSourcesConfirmed = None
  )

  "TaskListModel.ukPropertyComplete" should {
    "return true" when {
      "provided an income summary data with a confirmed uk property" in {
        fullCompleteTaskListModel
          .ukPropertyComplete shouldBe true
      }
    }
    "return false" when {
      "provided an income summary data with an unconfirmed uk property" in {
        fullCompleteTaskListModel.copy(ukProperty = fullCompleteTaskListModel.ukProperty.map(_.copy(confirmed = false)))
          .ukPropertyComplete shouldBe false
      }
      "provided an income summary without a uk property" in {
        fullCompleteTaskListModel.copy(ukProperty = None)
          .ukPropertyComplete shouldBe false
      }
    }
  }

  "TaskListModel.overseasPropertyComplete" should {
    "return true" when {
      "provided an income summary data with a confirmed uk property" in {
        fullCompleteTaskListModel
          .overseasPropertyComplete shouldBe true
      }
    }
    "return false" when {
      "provided an income summary data with an unconfirmed uk property" in {
        fullCompleteTaskListModel.copy(overseasProperty = fullCompleteTaskListModel.overseasProperty.map(_.copy(confirmed = false)))
          .overseasPropertyComplete shouldBe false
      }
      "provided an income summary without a uk property" in {
        fullCompleteTaskListModel.copy(overseasProperty = None)
          .overseasPropertyComplete shouldBe false
      }
    }
  }

  "TaskListModel.selfEmploymentsComplete" should {
    "return true" when {
      "there is a single confirmed self employment income source and accounting method" in {
        fullCompleteTaskListModel
          .selfEmploymentsComplete shouldBe true
      }
      "there are multiple confirmed self employment income sources and accounting method" in {
        fullCompleteTaskListModel.copy(selfEmployments = Seq(completeSeModel, completeSeModel))
          .selfEmploymentsComplete shouldBe true
      }
    }
    "return false" when {
      "there are no self employment income sources" in {
        fullCompleteTaskListModel.copy(selfEmployments = Seq.empty)
          .selfEmploymentsComplete shouldBe false
      }
      "there is a single unconfirmed self employment income source" in {
        fullCompleteTaskListModel.copy(selfEmployments = Seq(incompleteSeModel))
          .selfEmploymentsComplete shouldBe false
      }
      "there are a mixture of confirmed and unconfirmed self employment income sources" in {
        fullCompleteTaskListModel.copy(selfEmployments = Seq(completeSeModel, incompleteSeModel))
          .selfEmploymentsComplete shouldBe false
      }
    }
  }

  "TaskListModel.sectionsTotal" when {
    "the task list redesign flag is false" should {
      "return 3" when {
        "there are no details" in {
          emptyTaskListModel.sectionsTotal() shouldBe 3
        }
      }
      "return 3 plus the number of income sources added" when {
        "there are multiple income sources added" in {
          fullCompleteTaskListModel
            .sectionsComplete() shouldBe 5 // 1: information about you + 1: tax year selection + 3: each income source
        }
      }

    }
    "the task list redesign flag is true" should {
      "return 3" in {
        fullCompleteTaskListModel
          .sectionsTotal(true) shouldBe 3
      }
    }
  }

  "TaskListModel.sectionsComplete" when {
    "the task list redesign flag is false" should {
      "have the correct total" when {
        "there are no income sources confirmed and no tax year confirmed" in {
          emptyTaskListModel
            .sectionsComplete() shouldBe 1
        }
        "there are only unconfirmed income sources and tax year" in {
          fullCompleteTaskListModel.copy(
            taxYearSelection = unconfirmedEditableYearNext,
            selfEmployments = Seq(incompleteSeModel), selfEmploymentAccountingMethod = None,
            ukProperty = fullCompleteTaskListModel.ukProperty.map(_.copy(confirmed = false)),
            overseasProperty = fullCompleteTaskListModel.overseasProperty.map(_.copy(confirmed = false))
          ).sectionsComplete() shouldBe 1
        }
        "there are multiple self employment income sources confirmed" in {
          fullCompleteTaskListModel.copy(
            selfEmployments = Seq(completeSeModel, completeSeModel),
            ukProperty = None,
            overseasProperty = None
          ).sectionsComplete() shouldBe 4
        }
        "there are multiple confirmed income sources and tax year selection" in {
          fullCompleteTaskListModel
            .sectionsComplete() shouldBe 5
        }
      }
    }
    "the task list redesign flag is true" should {
      "have the correct total" when {
        "the income sources section is not complete and the tax year is not selected" in {
          fullCompleteTaskListModel.copy(incomeSourcesConfirmed = None, taxYearSelection = None)
            .sectionsComplete(true) shouldBe 1
        }
        "the income sources section is complete and the tax year is not selected" in {
          fullCompleteTaskListModel.copy(taxYearSelection = None)
            .sectionsComplete(true) shouldBe 2
        }
        "the income sources section is complete and the tax year is confirmed" in {
          fullCompleteTaskListModel
            .sectionsComplete(true) shouldBe 3
        }
      }
    }
  }

  "TaskListModel.taskListComplete" when {
    "the task list redesign flag is true" should {
      "return true" when {
        "a tax year is selected and the income sources are complete" in {
          fullCompleteTaskListModel
            .taskListComplete(true) shouldBe true
        }
      }
      "return false" when {
        "a tax year is not selected but income sources are complete" in {
          fullCompleteTaskListModel.copy(taxYearSelection = None)
            .taskListComplete(true) shouldBe false
        }
        "a tax year is selected but income sources are not complete" in {
          fullCompleteTaskListModel.copy(incomeSourcesConfirmed = None)
            .taskListComplete(true) shouldBe false
        }
      }
    }
    "the task list redesign flag is false" should {
      "return true" when {
        "a tax year is selected and all income sources confirmed" in {
          fullCompleteTaskListModel
            .taskListComplete() shouldBe true
        }
      }
      "return false" when {
        "a tax year is not selected, income sources are confirmed" in {
          fullCompleteTaskListModel.copy(taxYearSelection = None)
            .taskListComplete() shouldBe false
        }
        "a tax year is selected, but an income source is not confirmed" in {
          fullCompleteTaskListModel.copy(selfEmployments = Seq(incompleteSeModel))
            .taskListComplete() shouldBe false
        }
      }
    }
  }

}
