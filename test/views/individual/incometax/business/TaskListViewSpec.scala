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

package views.individual.incometax.business

import assets.MessageLookup.Summary.SelectedTaxYear
import assets.MessageLookup.TaskList._
import models._
import models.common.{AccountingYearModel, TaskListModel}
import models.common.business._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.twirl.api.Html
import services.AccountingPeriodService
import utilities.ViewSpec
import views.html.individual.incometax.business.TaskList

class TaskListViewSpec extends ViewSpec {

  val taskListView: TaskList = app.injector.instanceOf[TaskList]

  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  lazy val postAction: Call = controllers.individual.business.routes.TaskListController.submit()

  def customTaskListModel(taxYearSelection: Option[AccountingYearModel] = None,
                          selfEmployments: Seq[SelfEmploymentData] = Nil,
                          selfEmploymentAccountingMethod: Option[AccountingMethod] = None,
                          ukPropertyStart: Option[DateModel] = None,
                          ukPropertyAccountingMethod: Option[AccountingMethod] = None,
                          overseasPropertyStart: Option[DateModel] = None,
                          overseasPropertyAccountingMethod: Option[AccountingMethod] = None): TaskListModel = {
    TaskListModel(taxYearSelection,
      selfEmployments,
      selfEmploymentAccountingMethod,
      ukPropertyStart,
      ukPropertyAccountingMethod,
      overseasPropertyStart,
      overseasPropertyAccountingMethod
    )

  }

  val partialTaskListComplete = customTaskListModel(
    taxYearSelection = Some(AccountingYearModel(Current, false)),
    selfEmployments = Seq(
      SelfEmploymentData("id1", businessName = Some(BusinessNameModel("Name1"))),
      SelfEmploymentData("id2", businessName = Some(BusinessNameModel("Name2")), businessTradeName = Some(BusinessTradeNameModel("TradeName")))
    ),
    ukPropertyStart = Some(DateModel("1", "2", "3")),
    overseasPropertyStart = Some(DateModel("1", "2", "3"))
  )
  val completedTaskListComplete = TaskListModel(
    taxYearSelection = Some(AccountingYearModel(Next, true)),
    selfEmployments = Seq(SelfEmploymentData(
      id = "id1",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "2", "1980"))),
      businessName = Some(BusinessNameModel("Name1")),
      businessTradeName = Some(BusinessTradeNameModel("TradeName")),
      businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line1"), "Postcode")))
    )),
    selfEmploymentAccountingMethod = Some(Cash),
    ukPropertyStart = Some(DateModel("1", "2", "1980")),
    ukPropertyAccountingMethod = Some(Cash),
    overseasPropertyStart = Some(DateModel("1", "2", "1980")),
    overseasPropertyAccountingMethod = Some(Cash)
  )


  def page(taskList: TaskListModel = customTaskListModel()): Html = taskListView(
    postAction = postAction,
    viewModel = taskList,
    accountingPeriodService = accountingPeriodService
  )(request, implicitly, appConfig)


  def document(taskList: TaskListModel = customTaskListModel()): Document = Jsoup.parse(page(taskList).body)

  "business task list view" must {
    "have a title" in {
      document().title mustBe title
    }

    "have a heading" in {
      document().select("h1").text mustBe heading
    }

    "have a contents list" in {
      val contentList = document().select("ol").select("h2")
      contentList.text() must include(item1)
      contentList.text() must include(item2)
      contentList.text() must include(item3)
    }

    "display the dynamic content correctly" when {

      "there is no user data" must {
        "display the application is incomplete" in {
          document().selectNth("h2", 1).text mustBe subHeadingIncomplete
        }

        "display the number of sections complete out of the total" in {
          document().mainContent.selectNth("p", 1).text mustBe contentSummary(0, 2)
        }


        "in the select tax year section: display the select tax year link with status incomplete" when {
          "the user has not selected any tax year to sign up" in {
            val selectTaxYearSection = document().mainContent.selectNth("ul", 1)
            val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
            selectTaxYearLink.text mustBe selectTaxYear
            selectTaxYearSection.selectNth("span", 2).text mustBe notStarted
            selectTaxYearLink.attr("href") mustBe controllers.individual.business.routes.WhatYearToSignUpController.show().url
          }
        }

        "display the add a business link" in {
          val businessLink = document().mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectHead("a")
          businessLink.text mustBe addBusiness
          businessLink.attr("href") mustBe controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show().url
        }

        "display the sign up incomplete text" in {
          val incompleteText = document().mainContent.selectHead("ol > li:nth-of-type(3) > ul").selectHead("span")
          incompleteText.text mustBe signUpIncompleteText
        }

        "do not display the sign up button" in {
          document().mainContent.selectOptionally("button") mustBe None
        }
      }

      "there is partial user data" must {

        "display the application is incomplete" in {
          document(partialTaskListComplete).selectNth("h2", 1).text mustBe subHeadingIncomplete
        }

        "display the number of sections complete out of the total" in {
          document(partialTaskListComplete).mainContent.selectNth("p", 1).text mustBe contentSummary(0, 5)
        }


        "in the select tax year section: display the select tax year link with status in progress" when {
          "the user has selected the tax year but not confirmed the answer in tax year CYA page" in {
            val selectTaxYearSection = document(partialTaskListComplete).mainContent.selectNth("ul", 1)
            val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")

            selectTaxYearLink.text mustBe selectTaxYear
            selectTaxYearSection.selectNth("span", 2).text mustBe inProgress
            selectTaxYearLink.attr("href") mustBe controllers.individual.business.routes.WhatYearToSignUpController.show().url
          }
        }

        "display an incomplete self employment with just the business name" in {
          val selfEmploymentSection = document(partialTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 1)
          val selfEmploymentLink = selfEmploymentSection.selectNth("span", 1).selectHead("a")
          selfEmploymentLink.text mustBe "Name1"
          selfEmploymentLink.attr("href") mustBe s"""${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id1&isEditMode=true"""
          selfEmploymentSection.selectNth("span", 2).text mustBe incomplete

        }

        "display an incomplete self employment with a business name and trade" in {
          val selfEmploymentSection = document(partialTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 2)
          val selfEmploymentLink = selfEmploymentSection.selectNth("span", 1).selectHead("a")
          selfEmploymentLink.text mustBe "Name2 TradeName"
          selfEmploymentLink.attr("href") mustBe s"""${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id2&isEditMode=true"""
          selfEmploymentSection.selectNth("span", 2).text mustBe incomplete

        }

        "display an incomplete uk property income" in {
          val ukPropertyIncomeSection = document(partialTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 3)
          val ukPropertyIncomeLink = ukPropertyIncomeSection.selectNth("span", 1).selectHead("a")
          ukPropertyIncomeLink.text() mustBe ukPropertyBusiness
          ukPropertyIncomeLink.attr("href") mustBe controllers.individual.business.routes.PropertyStartDateController.show().url
          ukPropertyIncomeSection.selectNth("span", 2).text mustBe incomplete
        }
        "display an incomplete overseas property income" in {
          val overseasPropertySection = document(partialTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 4)
          val overseasPropertyLink = overseasPropertySection.selectNth("span", 1).selectHead("a")
          overseasPropertyLink.text mustBe overseasPropertyBusiness
          overseasPropertyLink.attr("href") mustBe controllers.individual.business.routes.OverseasPropertyStartDateController.show().url
          overseasPropertySection.selectNth("span", 2).text mustBe incomplete
        }
        "display the add a business link" in {
          val businessLink = document(partialTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 5).selectHead("a")
          businessLink.text mustBe addBusiness
          businessLink.attr("href") mustBe controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show().url
        }
        "display the sign up incomplete text" in {
          val incompleteText = document(partialTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(3) > ul").selectHead("span")
          incompleteText.text mustBe signUpIncompleteText
        }
        "do not display the sign up button" in {
          document(partialTaskListComplete).mainContent.selectOptionally("button") mustBe None
        }
      }


      "there is full user data" must {
        "display the application is complete" in {
          document(completedTaskListComplete).selectNth("h2", 1).text mustBe subHeadingComplete
        }
        "display the number of sections complete out of the total" in {
          document(completedTaskListComplete).mainContent.selectNth("p", 1).text mustBe contentSummary(4, 4)
        }

        "in the select tax year section: display a link with next tax year as the selected option and it will redirect to tax year CYA page along with status complete" when {
          "the user has selected the tax year and confirmed the answer in tax year CYA page" in {
            val selectTaxYearSection = document(completedTaskListComplete).mainContent.selectNth("ul", 1)
            val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
            selectTaxYearLink.text mustBe SelectedTaxYear.next(accountingPeriodService.currentTaxYear, accountingPeriodService.currentTaxYear + 1)
            selectTaxYearSection.selectNth("span", 2).text mustBe complete
            selectTaxYearLink.attr("href") mustBe controllers.individual.business.routes.TaxYearCheckYourAnswersController.show().url
          }
        }
        "display a complete self employment" in {
          val selfEmploymentSection = document(completedTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 1)
          val selfEmploymentLink = selfEmploymentSection.selectNth("span", 1).selectHead("a")
          selfEmploymentLink.text mustBe "Name1 TradeName"
          selfEmploymentLink.attr("href") mustBe s"""${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id1&isEditMode=true"""
          selfEmploymentSection.selectNth("span", 2).text mustBe complete
        }
        "display a complete uk property income" in {
          val ukPropertyIncomeSection = document(completedTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 2)
          val ukPropertyIncomeLink = ukPropertyIncomeSection.selectNth("span", 1).selectHead("a")
          ukPropertyIncomeLink.text() mustBe ukPropertyBusiness
          ukPropertyIncomeLink.attr("href") mustBe controllers.individual.business.routes.PropertyStartDateController.show().url
          ukPropertyIncomeSection.selectNth("span", 2).text mustBe complete
        }
        "display a complete overseas property income" in {
          val overseasPropertySection = document(completedTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 3)
          val overseasPropertyLink = overseasPropertySection.selectNth("span", 1).selectHead("a")
          overseasPropertyLink.text mustBe overseasPropertyBusiness
          overseasPropertyLink.attr("href") mustBe controllers.individual.business.routes.OverseasPropertyStartDateController.show().url
          overseasPropertySection.selectNth("span", 2).text mustBe complete
        }
        "display the add a business link" in {
          val businessLink = document(completedTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 4).selectHead("a")
          businessLink.text mustBe addBusiness
          businessLink.attr("href") mustBe controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show().url
        }
        "display the sign up button" in {
          document(completedTaskListComplete).mainContent.selectHead("button").text mustBe continue
        }
        "do not display the sign up incomplete text" in {
          document(completedTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(3) > ul").selectOptionally("span") mustBe None

        }
      }
    }


  }
}
