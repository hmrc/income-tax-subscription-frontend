/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatest.{Matchers, OptionValues, WordSpecLike}

class TaskListModelSpec extends WordSpecLike with Matchers with OptionValues {

  val date: DateModel = DateModel("1", "2", "1980")

  val completeSeModel: SelfEmploymentData = SelfEmploymentData(
    id = "",
    businessStartDate = Some(BusinessStartDate(date)),
    businessName = Some(BusinessNameModel("Fake Name")),
    businessTradeName = Some(BusinessTradeNameModel("Trade")),
    businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line1"), "Postcode"))),
    confirmed = true
  )

  val incompleteSeModel: SelfEmploymentData = SelfEmploymentData(
    id = "",
    businessStartDate = Some(BusinessStartDate(date)),
    businessName = None,
    businessTradeName = Some(BusinessTradeNameModel("Trade")),
    businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line1"), "Postcode")))
  )

  "Task List " should {

    "provided income summary data with a uk property, ukPropertyComplete returns true" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = Seq(completeSeModel),
        selfEmploymentAccountingMethod = None,
        ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
        overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
      )

      summary.ukPropertyComplete shouldBe true
    }

    "provided income summary data without a uk property, ukPropertyComplete returns false" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = Seq(completeSeModel),
        selfEmploymentAccountingMethod = None,
        ukProperty = Some(PropertyModel(None, Some(DateModel("1", "2", "1980")))),
        overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
      )

      summary.ukPropertyComplete shouldBe false
    }

    "provided income summary data with a completed self-employment, selfEmploymentsComplete returns true" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = Seq(completeSeModel),
        selfEmploymentAccountingMethod = Some(Cash),
        ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
        overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
      )

      summary.selfEmploymentsComplete shouldBe true
    }

    "provided income summary data with an incomplete self-employment, selfEmploymentsComplete returns false" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = Seq(incompleteSeModel),
        selfEmploymentAccountingMethod = Some(Cash),
        ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
        overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
      )

      summary.selfEmploymentsComplete shouldBe false
    }

    "provided income summary data with an incomplete self-employment accounting method, selfEmploymentsComplete returns false" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = Seq(completeSeModel),
        selfEmploymentAccountingMethod = None,
        ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
        overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
      )

      summary.selfEmploymentsComplete shouldBe false
    }


    "provided income summary data with a foreign property, overseasPropertyComplete returns true" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = Seq(completeSeModel),
        selfEmploymentAccountingMethod = None,
        ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
        overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
      )

      summary.overseasPropertyComplete shouldBe true
    }

    "provided income summary data without a foreign property, overseasPropertyComplete returns false" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = Seq(completeSeModel),
        selfEmploymentAccountingMethod = None,
        ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
        overseasProperty = Some(OverseasPropertyModel(Some(Cash)))
      )

      summary.overseasPropertyComplete shouldBe false
    }

    "provided income summary data with a foreign property, ukProperty and a complete self employment, sectionsTotal returns 4" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = Seq(completeSeModel),
        selfEmploymentAccountingMethod = Some(Cash),
        ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
        overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
      )

      summary.sectionsTotal shouldBe 4
    }

    "provided income summary data with a foreign property, ukProperty and an incomplete self employment, sectionsCompleted returns 3" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = Seq(incompleteSeModel),
        selfEmploymentAccountingMethod = None,
        ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
        overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
      )

      summary.sectionsComplete shouldBe 3
    }

    "provided income summary data with a foreign property, ukProperty and a complete self employment, taskListComplete returns true" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = Seq(completeSeModel),
        selfEmploymentAccountingMethod = Some(Cash),
        ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
        overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
      )

      summary.taskListComplete shouldBe true
    }

    "taskListComplete returns false" when {
      "income summary data with an unfinished foreign property, a complete ukProperty and a complete self employment" in {
        val summary = TaskListModel(
          taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
          selfEmployments = Seq(completeSeModel),
          selfEmploymentAccountingMethod = Some(Cash),
          ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
          overseasProperty = Some(OverseasPropertyModel(None, Some(date), confirmed = false))
        )

        summary.taskListComplete shouldBe false
      }
    }

    "taskListComplete returns false" when {
      "income summary data with a complete foreign property, an unfinished ukProperty and a complete self employment" in {
        val summary = TaskListModel(
          taxYearSelection = Some(AccountingYearModel((Next))),
          selfEmployments = Seq(completeSeModel),
          selfEmploymentAccountingMethod = Some(Cash),
          ukProperty = Some(PropertyModel(None, Some(DateModel("1", "2", "1980")))),
          overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
        )

        summary.taskListComplete shouldBe false
      }
    }

    "taskListComplete returns false" when {
      "income summary data with a complete foreign property, a complete ukProperty and an incomplete self employment" in {

        val summary = TaskListModel(
          taxYearSelection = Some(AccountingYearModel((Next), true)),
          selfEmployments = Seq(incompleteSeModel),
          selfEmploymentAccountingMethod = None,
          ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), true)),
          overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
        )

        summary.taskListComplete shouldBe false
      }
    }

    "canAddMoreBusinesses returns true" when {
      "provided income summary data with a foreign property, ukProperty and with less than 50 complete self employment summaries, " in {
        val summary = TaskListModel(
          taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
          selfEmployments = (1 to 30).toSeq.map { _ =>
            SelfEmploymentData(
              id = "",
              businessStartDate = Some(BusinessStartDate(date)),
              businessName = Some(BusinessNameModel("Fake Name")),
              businessTradeName = Some(BusinessTradeNameModel("Trade")),
              businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line1"), "Postcode")))
            )
          },
          selfEmploymentAccountingMethod = Some(Cash),
          ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
          overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
        )

        summary.canAddMoreBusinesses shouldBe true
      }
    }

    "provided income summary data with a foreign property and with 50 complete self employment summaries, canAddMoreBusinesses returns true" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = (1 to 50).toSeq.map { _ =>
          SelfEmploymentData(
            id = "",
            businessStartDate = Some(BusinessStartDate(date)),
            businessName = Some(BusinessNameModel("Fake Name")),
            businessTradeName = Some(BusinessTradeNameModel("Trade")),
            businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line1"), "Postcode")))
          )
        },
        selfEmploymentAccountingMethod = Some(Cash),
        ukProperty = None,
        overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
      )

      summary.canAddMoreBusinesses shouldBe true

    }

    "provided income summary data with a ukProperty and with 50 complete self employment summaries, canAddMoreBusinesses returns true" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = (1 to 50).toSeq.map { _ =>
          SelfEmploymentData(
            id = "",
            businessStartDate = Some(BusinessStartDate(date)),
            businessName = Some(BusinessNameModel("Fake Name")),
            businessTradeName = Some(BusinessTradeNameModel("Trade")),
            businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line1"), "Postcode")))
          )
        },
        selfEmploymentAccountingMethod = Some(Cash),

        ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")))),
        overseasProperty = None
      )

      summary.canAddMoreBusinesses shouldBe true

    }

    "provided income summary data with a foreign property, ukProperty and with 50 complete self employment summaries, canAddMoreBusinesses returns false" in {

      val summary = TaskListModel(
        taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
        selfEmployments = (1 to 50).toSeq.map { _ =>
          SelfEmploymentData(
            id = "",
            businessStartDate = Some(BusinessStartDate(date)),
            businessName = Some(BusinessNameModel("Fake Name")),
            businessTradeName = Some(BusinessTradeNameModel("Trade")),
            businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line1"), "Postcode")))
          )
        },
        selfEmploymentAccountingMethod = Some(Cash),
        ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
        overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
      )

      summary.canAddMoreBusinesses shouldBe false

    }

    "provided income summary data with a foreign property, ukProperty and a complete self employment," +
      "sectionsTotal returns 4, sectionsComplete returns 3, canAddMoreBusinesses returns true" +
      "taskListComplete returns true, and taxYearSelectedAndConfirmed return true" when {
      "tax year selected and has confirmed on tax year CYA page" in {


        val summary = TaskListModel(
          taxYearSelection = Some(AccountingYearModel((Next), confirmed = true)),
          selfEmployments = Seq(completeSeModel),
          selfEmploymentAccountingMethod = Some(Cash),
          ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
          overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
        )

        summary.sectionsTotal shouldBe 4
        summary.sectionsComplete shouldBe 4
        summary.canAddMoreBusinesses shouldBe true
        summary.taskListComplete shouldBe true
        summary.taxYearSelectedAndConfirmed shouldBe true

      }
    }

    "provided income summary data with a foreign property, ukProperty and a complete self employment," +
      "sectionsTotal returns 4, sectionsComplete returns 3, canAddMoreBusinesses returns true" +
      "taskListComplete returns false, and taxYearSelectedNotConfirmed return true" when {
      "tax year selected but has not confirmed on tax year CYA page" in {


        val summary = TaskListModel(
          taxYearSelection = Some(AccountingYearModel((Next))),
          selfEmployments = Seq(completeSeModel),
          selfEmploymentAccountingMethod = Some(Cash),
          ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
          overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
        )

        summary.sectionsTotal shouldBe 4
        summary.sectionsComplete shouldBe 3
        summary.canAddMoreBusinesses shouldBe true
        summary.taskListComplete shouldBe false
        summary.taxYearSelectedNotConfirmed shouldBe true

      }
    }

    "provided income summary data with a foreign property, ukProperty and a complete self employment," +
      "sectionsTotal returns 4, sectionsComplete returns 3, canAddMoreBusinesses returns true" +
      "taskListComplete returns false, and taxYearSelectedAndConfirmed and taxYearSelectedAndConfirmed both return false" when {
      "tax year has not been selected" in {


        val summary = TaskListModel(
          taxYearSelection = None,
          selfEmployments = Seq(completeSeModel),
          selfEmploymentAccountingMethod = Some(Cash),

          ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
          overseasProperty = Some(OverseasPropertyModel(Some(Cash), Some(date), confirmed = true))
        )

        summary.sectionsTotal shouldBe 4
        summary.sectionsComplete shouldBe 3
        summary.canAddMoreBusinesses shouldBe true
        summary.taskListComplete shouldBe false
        summary.taxYearSelectedNotConfirmed shouldBe false
        summary.taxYearSelectedAndConfirmed shouldBe false

      }
    }

  }

}
