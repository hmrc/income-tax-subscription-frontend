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

package views.individual.tasklist

import messagelookup.individual.MessageLookup.Summary.SelectedTaxYear
import messagelookup.individual.MessageLookup.Summary.SelectedTaxYear.next
import messagelookup.individual.MessageLookup.TaskList._
import messagelookup.individual.MessageLookup.{TaskList => messages}
import models._
import models.common.business._
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel, TaskListModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.twirl.api.Html
import services.AccountingPeriodService
import utilities.AccountingPeriodUtil.getCurrentTaxEndYear
import utilities.ViewSpec
import utilities.individual.TestConstants.testUtr
import views.html.individual.tasklist.TaskList

class TaskListViewSpec extends ViewSpec {

  val selectorForUserInformation = "ol > li:nth-of-type(1)"
  val selectorForFirstBusiness = "ol > li:nth-of-type(2) > ul:nth-of-type(1)"
  val selectorForFirstParaOfBusiness = "ol > li:nth-of-type(2)"

  val taskListView: TaskList = app.injector.instanceOf[TaskList]

  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  lazy val postAction: Call = controllers.individual.tasklist.routes.TaskListController.submit()

  def customTaskListModel(taxYearSelection: Option[AccountingYearModel] = None,
                          selfEmployments: Seq[SelfEmploymentData] = Nil,
                          ukProperty: Option[PropertyModel] = None,
                          overseasProperty: Option[OverseasPropertyModel] = None,
                          incomeSourcesConfirmed: Option[Boolean] = None): TaskListModel = {
    TaskListModel(taxYearSelection,
      selfEmployments,
      ukProperty,
      overseasProperty,
      incomeSourcesConfirmed
    )
  }

  private val forcedYearTaskList = customTaskListModel(
    taxYearSelection = Some(AccountingYearModel(Next, editable = false))
  )

  private val partialTaskListComplete = customTaskListModel(
    taxYearSelection = Some(AccountingYearModel(Current)),
    selfEmployments = Seq(
      SelfEmploymentData("id1", businessName = Some(BusinessNameModel("Name1"))),
      SelfEmploymentData("id2", businessName = Some(BusinessNameModel("Name2")), businessTradeName = Some(BusinessTradeNameModel("TradeName")))
    ),
    ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")))),
    overseasProperty = Some(OverseasPropertyModel(startDate = Some(DateModel("1", "2", "3"))))
  )

  private val completedTaskListComplete = TaskListModel(
    taxYearSelection = Some(AccountingYearModel(Next, confirmed = true)),
    selfEmployments = Seq(SelfEmploymentData(
      id = "id1",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "2", "1980"))),
      businessName = Some(BusinessNameModel("Name1")),
      businessTradeName = Some(BusinessTradeNameModel("TradeName")),
      businessAddress = Some(BusinessAddressModel(Address(Seq("line1"), Some("Postcode")))),
      confirmed = true
    )),
    ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
    overseasProperty = Some(OverseasPropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("1", "2", "3")), confirmed = true)),
    incomeSourcesConfirmed = Some(true)
  )


  def page(taskList: TaskListModel = customTaskListModel(), maybeIndividualUserFullName: Option[String], utrNumber: String): Html = taskListView(
    postAction = postAction,
    viewModel = taskList,
    accountingPeriodService = accountingPeriodService,
    individualUserNino = "individualUserNino",
    maybeIndividualUserFullName = maybeIndividualUserFullName,
    utrNumber = utrNumber
  )(request, implicitly)


  def document(
                taskList: TaskListModel = customTaskListModel(),
                maybeIndividualUserFullName: Option[String] = Some("individualUserFullName"),
                utrNumber: String = testUtr
              ): Document = Jsoup.parse(page(taskList, maybeIndividualUserFullName, utrNumber).body)

  "task list view" must {

    "have a title" in {
      document().title mustBe messages.title
    }

    "have a heading" in {
      document().select("h1").text mustBe messages.heading
    }

    "have a contents list" in {
      val contentList = document().select("ol").select("h2")
      contentList.text must include(item1)
      contentList.text must include(item2)
      contentList.text must include(item3)
    }

    "display the save and come back later button" in {
      document().mainContent.getElementsByClass("govuk-button--secondary").text mustBe messages.saveAndComeBackLater
    }

    "display the dynamic content correctly" when {
      "there is no user data" must {
        "in the information section: display heading" in {
          document().mainContent.selectHead(selectorForUserInformation).selectHead("h2").text mustBe item1
        }


        "in the information section: display a user name" in {
          document(maybeIndividualUserFullName = None).mainContent.selectHead(selectorForUserInformation).selectHead(".govuk-summary-list").text contains userInfoPartialContent
        }

        "in the information section: display a user name, user nino and user utr number" in {
          document().mainContent.selectHead(selectorForUserInformation).selectHead(".govuk-summary-list").text contains userInfoContent
        }

        "in the business section: display the information para" in {
          val infoPara = document().mainContent.selectHead(selectorForFirstParaOfBusiness).selectHead("p")
          infoPara.text mustBe item2Para
        }

        "in the select tax year section: display the select tax year link with status incomplete" when {
          "the user has not selected any tax year to sign up" in {
            val selectTaxYearSection = document().mainContent.selectNth("ul.app-task-list__items", 2)
            val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
            selectTaxYearLink.text mustBe selectTaxYear
            selectTaxYearSection.selectNth("span", 2).text mustBe notStarted
            selectTaxYearSection.selectHead("strong").attr("class") mustBe "govuk-tag govuk-tag--grey"
            selectTaxYearLink.attr("href") mustBe controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
          }
        }

        "display the add your income sources link and tag" in {
          val incomeSourcesSection = document().mainContent.selectHead(".app-task-list").selectNth("li", 2)
          val incomeSourcesRow = incomeSourcesSection.selectHead("ul").selectHead("li")

          val incomeSourcesLink = incomeSourcesRow.selectNth("span", 1).selectHead("a")
          incomeSourcesLink.text mustBe addYourIncomeSources
          incomeSourcesLink.attr("href") mustBe controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url

          val incomeSourcesTag = incomeSourcesRow.selectNth("span", 2)
          incomeSourcesTag.selectHead("strong").text mustBe notStarted

          incomeSourcesLink.attr("aria-describedby") mustBe incomeSourcesTag.id

          document().mainContent.selectOptionally("#add_business") mustBe None
        }

        "display the sign up incomplete text" in {
          val incompleteText = document().mainContent.selectNth("p", 3)
          incompleteText.text mustBe signUpIncompleteText
        }

        "do not display the sign up button" in {
          document().mainContent.selectOptionally("button") mustBe None
        }

      }

      "there is partial user data" must {
        "in the user information section: display heading, name, nino and utr" in {
          val userInfo = document().mainContent.selectHead(selectorForUserInformation)
          userInfo.selectHead("h2").text mustBe item1
          userInfo.selectHead(".govuk-summary-list").text contains userInfoContent
        }

        "in the select tax year section: display the select tax year link with status in progress" when {
          "the user has selected the tax year but not confirmed the answer in tax year CYA page" in {
            val selectTaxYearSection = document(partialTaskListComplete).mainContent
              .selectHead("ol > li:nth-of-type(3)")
              .selectHead("ul.app-task-list__items")
            val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
            selectTaxYearLink.text mustBe selectTaxYear
            selectTaxYearSection.selectNth("span", 2).text mustBe inProgress
            selectTaxYearSection.selectHead("strong").attr("class") mustBe "govuk-tag govuk-tag--grey"
            selectTaxYearLink.attr("href") mustBe controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
          }
        }

        "display the add your income sources link and tag" in {
          val incomeSourcesSection = document(partialTaskListComplete).mainContent.selectHead(".app-task-list").selectNth("li", 2)
          val incomeSourcesRow = incomeSourcesSection.selectHead("ul").selectHead("li")

          val incomeSourcesLink = incomeSourcesRow.selectNth("span", 1).selectHead("a")
          incomeSourcesLink.text mustBe addYourIncomeSources
          incomeSourcesLink.attr("href") mustBe controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url

          val incomeSourcesTag = incomeSourcesRow.selectNth("span", 2)
          incomeSourcesTag.selectHead("strong").text mustBe incomplete

          incomeSourcesLink.attr("aria-describedby") mustBe incomeSourcesTag.id

          document(partialTaskListComplete).mainContent.selectOptionally("#add_business") mustBe None
        }

        "display the sign up incomplete text" in {
          val incompleteText = document(partialTaskListComplete).mainContent.selectNth("p", 3)
          incompleteText.text mustBe signUpIncompleteText
        }

        "do not display the sign up button" in {
          document(partialTaskListComplete).mainContent.selectOptionally("button") mustBe None
        }
      }

      "there is full user data" must {
        "in the user information section: display heading, name, nino and utr" in {
          val userInfo = document().mainContent.selectHead(selectorForUserInformation)
          userInfo.selectHead("h2").text mustBe item1
          userInfo.selectHead(".govuk-summary-list").text contains userInfoContent
        }

        "display a complete tax year with an edit link to the Tax Year CYA" when {
          "the user has selected the tax year and confirmed the answer in tax year CYA page" in {
            val selectTaxYearSection = document(completedTaskListComplete).mainContent
              .selectHead("ol > li:nth-of-type(3)")
              .selectHead("ul.app-task-list__items")
            val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
            selectTaxYearLink.text mustBe SelectedTaxYear.next(accountingPeriodService.currentTaxYear, accountingPeriodService.currentTaxYear + 1)
            selectTaxYearSection.selectNth("span", 2).text mustBe complete
            selectTaxYearSection.selectHead("strong").attr("class") mustBe "govuk-tag"
            selectTaxYearLink.attr("href") mustBe controllers.individual.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show(editMode = true).url
          }
        }

        "display the view your income sources link and tag" in {
          val incomeSourcesSection = document(completedTaskListComplete).mainContent.selectHead(".app-task-list").selectNth("li", 2)
          val incomeSourcesRow = incomeSourcesSection.selectHead("ul").selectHead("li")

          val incomeSourcesLink = incomeSourcesRow.selectNth("span", 1).selectHead("a")
          incomeSourcesLink.text mustBe viewYourIncomeSources
          incomeSourcesLink.attr("href") mustBe controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url

          val incomeSourcesTag = incomeSourcesRow.selectNth("span", 2)
          incomeSourcesTag.selectHead("strong").text mustBe complete

          incomeSourcesLink.attr("aria-describedby") mustBe incomeSourcesTag.id

          document(completedTaskListComplete).mainContent.selectOptionally("#add_business") mustBe None
        }

        "display the sign up ready text" in {
          val incompleteText = document(completedTaskListComplete).mainContent.selectNth("p", 3)
          incompleteText.text mustBe signUpReadyText
        }

        "display the sign up button" in {
          document(completedTaskListComplete).mainContent.selectHead("button").text mustBe checkAndContinue
        }

      }
    }
  }

  "given a forced year in the task list model" must {
    val doc = document(forcedYearTaskList)
    "display the select tax year with status complete and no link" when {
      "the user has not selected any tax year to sign up" in {
        val selectTaxYearSection = doc.mainContent.selectHead(".app-task-list").selectHead("li:nth-of-type(3)")
        selectTaxYearSection.selectHead("h2").text mustBe item3

        selectTaxYearSection.selectHead("p").text mustBe item3Para

        val selectTaxYearContent = selectTaxYearSection.selectHead("ul").selectHead("li")
        selectTaxYearContent.selectNth("span", 1).text mustBe next(getCurrentTaxEndYear, getCurrentTaxEndYear + 1)
        selectTaxYearContent.selectOptionally("a") mustBe None

        val tag = selectTaxYearContent.selectNth("span", 2).selectHead("strong")
        tag.text mustBe complete
        tag.attr("class") mustBe "govuk-tag"
      }
    }
  }
}
