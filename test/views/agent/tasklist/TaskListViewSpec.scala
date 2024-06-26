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

package views.agent.tasklist

import messagelookup.individual.MessageLookup.Summary.SelectedTaxYear
import messagelookup.individual.MessageLookup.Summary.SelectedTaxYear.next
import messagelookup.individual.MessageLookup.TaskList._
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
import views.html.agent.tasklist.TaskList

import scala.util.Random

class TaskListViewSpec extends ViewSpec {

  val selectorForFirstBusiness = "ol > li:nth-of-type(2) > ul:nth-of-type(1)"
  val selectorForFirstParaOfBusiness = "ol > li:nth-of-type(2)"

  val taskListView: TaskList = app.injector.instanceOf[TaskList]

  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  lazy val postAction: Call = controllers.agent.tasklist.routes.TaskListController.submit()

  private def customTaskListModel(taxYearSelection: Option[AccountingYearModel] = None,
                                  selfEmployments: Seq[SelfEmploymentData] = Nil,
                                  ukProperty: Option[PropertyModel] = None,
                                  overseasProperty: Option[OverseasPropertyModel] = None,
                                  incomeSourcesConfirmed: Option[Boolean] = None): TaskListModel =
    TaskListModel(taxYearSelection,
      selfEmployments,
      ukProperty,
      overseasProperty,
      incomeSourcesConfirmed
    )

  private val emptyTaskList = customTaskListModel()

  private val partialTaskList = customTaskListModel(
    taxYearSelection = Some(AccountingYearModel(Current)),
    selfEmployments = Seq(
      SelfEmploymentData("id1", businessName = Some(BusinessNameModel("Name1"))),
      SelfEmploymentData("id2", businessName = Some(BusinessNameModel("Name2")), businessTradeName = Some(BusinessTradeNameModel("TradeName")))
    ),
    ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")))),
    overseasProperty = Some(OverseasPropertyModel(startDate = Some(DateModel("1", "2", "3"))))
  )

  private val completeTaskList = TaskListModel(
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

  private val forcedYearTaskList = customTaskListModel(
    taxYearSelection = Some(AccountingYearModel(Next, editable = false))
  )

  private val nameLengthCharacters = 10
  private val clientName = Random.alphanumeric.take(nameLengthCharacters).mkString
  private val clientNino = Random.alphanumeric.take(nameLengthCharacters).mkString
  private val clientUtr = Random.alphanumeric.take(nameLengthCharacters).mkString

  def page(taskList: TaskListModel = customTaskListModel()): Html = {
    taskListView(
      postAction = postAction,
      viewModel = taskList,
      clientName = clientName,
      clientNino = clientNino,
      clientUtr = clientUtr
    )(request, implicitly)
  }


  def document(taskList: TaskListModel): Document = Jsoup.parse(page(taskList).body)

  "business task list view" when {

    "given empty task list model" must {
      def doc = document(emptyTaskList)

      "have a title" in {
        doc.title mustBe agentTitle
      }

      "have a contents list" in {
        val contentList = doc.select("ol").select("h2")
        contentList.text() must include(agentItem1)
        contentList.text() must include(agentItem2)
        contentList.text() must include(agentItem3)
      }

      "display the save and come back later button" in {
        doc.mainContent.getElementsByClass("govuk-button--secondary").text mustBe saveAndComeBackLater
      }

      "display the dynamic content correctly" when {
        "there is no user data" must {
          "in the client information section: have a client name, nino and utr" in {
            doc.mainContent.getElementsByClass("govuk-summary-list__key").text contains s"Client: $clientName | $clientNino |$clientUtr"
          }

          "display the add your income sources link and tag" in {

            val incomeSourcesSection = doc.mainContent.selectHead(".app-task-list").selectNth("li", 2)
            val incomeSourcesRow = incomeSourcesSection.selectHead("ul").selectHead("li")

            val incomeSourcesLink = incomeSourcesRow.selectNth("span", 1).selectHead("a")
            incomeSourcesLink.text mustBe addYourClientsIncomeSource
            incomeSourcesLink.attr("href") mustBe controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url

            val incomeSourcesTag = incomeSourcesRow.selectNth("span", 2)
            incomeSourcesTag.selectHead("strong").text mustBe notStarted

            incomeSourcesLink.attr("aria-describedby") mustBe incomeSourcesTag.id

            doc.mainContent.selectOptionally("#add_business") mustBe None
          }

          "in the select tax year section: display the select tax year link with status incomplete" when {
            "the user has not selected any tax year to sign up" in {
              val selectTaxYearSection = doc.mainContent.selectHead(".app-task-list").selectHead("li:nth-of-type(3)")
              val selectTaxYearRow = selectTaxYearSection.selectHead("ul").selectHead("li")
              val selectTaxYearLink = selectTaxYearRow.selectNth("span", 1).selectHead("a")
              selectTaxYearLink.text mustBe selectTaxYear
              selectTaxYearLink.attr("href") mustBe controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
              selectTaxYearRow.selectNth("span", 2).text mustBe notStarted
              selectTaxYearRow.selectHead("strong").attr("class") mustBe "govuk-tag govuk-tag--grey"
            }
          }

          "display the sign up incomplete text" in {
            val incompleteText = doc.mainContent.selectHead("p")
            incompleteText.text mustBe agentSignUpIncompleteText
          }

          "do not display the sign up button" in {
            doc.mainContent.selectOptionally("button") mustBe None
          }
        }

      }
    }
    "given partial task list model" must {
      def doc = document(partialTaskList)

      "in the client information section: have a client name, nino and utr" in {
        doc.mainContent.getElementsByClass("govuk-summary-list__key").text contains s"Client: $clientName | $clientNino |$clientUtr"
      }

      "display the add your income sources link and tag" in {

        val incomeSourcesSection = doc.mainContent.selectHead(".app-task-list").selectNth("li", 2)
        val incomeSourcesRow = incomeSourcesSection.selectHead("ul").selectHead("li")

        val incomeSourcesLink = incomeSourcesRow.selectNth("span", 1).selectHead("a")
        incomeSourcesLink.text mustBe addYourClientsIncomeSource
        incomeSourcesLink.attr("href") mustBe controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url

        val incomeSourcesTag = incomeSourcesRow.selectNth("span", 2)
        incomeSourcesTag.selectHead("strong").text mustBe incomplete

        incomeSourcesLink.attr("aria-describedby") mustBe incomeSourcesTag.id

        doc.mainContent.selectOptionally("#add_business") mustBe None
      }

      "in the select tax year section: display the select tax year link with status in progress" when {
        "the user has selected the tax year but not confirmed the answer in tax year CYA page" in {
          val selectTaxYearSection = doc.mainContent.selectHead(".app-task-list").selectHead("li:nth-of-type(3)")
          val selectTaxYearRow = selectTaxYearSection.selectHead("ul").selectHead("li")
          val selectTaxYearLink = selectTaxYearRow.selectNth("span", 1).selectHead("a")
          selectTaxYearLink.text mustBe selectTaxYear
          selectTaxYearLink.attr("href") mustBe controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
          selectTaxYearRow.selectNth("span", 2).text mustBe inProgress
          selectTaxYearRow.selectHead("strong").attr("class") mustBe "govuk-tag govuk-tag--grey"
        }
      }

      "display the sign up incomplete text" in {
        val incompleteText = doc.mainContent.selectHead("p")
        incompleteText.text mustBe agentSignUpIncompleteText
      }

      "do not display the sign up button" in {
        doc.mainContent.selectOptionally("button") mustBe None
      }
    }


    "given complete task list model" must {
      def doc = document(completeTaskList)

      "in the client information section: have a client name, nino and utr" in {
        doc.mainContent.getElementsByClass("govuk-summary-list__key").text contains s"Client: $clientName | $clientNino |$clientUtr"
      }

      "display the view your income sources link and tag" in {

        val incomeSourcesSection = doc.mainContent.selectHead(".app-task-list").selectNth("li", 2)
        val incomeSourcesRow = incomeSourcesSection.selectHead("ul").selectHead("li")

        val incomeSourcesLink = incomeSourcesRow.selectNth("span", 1).selectHead("a")
        incomeSourcesLink.text mustBe viewYourClientsIncomeSources
        incomeSourcesLink.attr("href") mustBe controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url

        val incomeSourcesTag = incomeSourcesRow.selectNth("span", 2)
        incomeSourcesTag.selectHead("strong").text mustBe complete

        incomeSourcesLink.attr("aria-describedby") mustBe incomeSourcesTag.id

        doc.mainContent.selectOptionally("#add_business") mustBe None
      }

      "display a complete tax year with an edit link to the Tax Year CYA" when {
        "the user has selected the tax year and confirmed the answer in tax year CYA page" in {
          val selectTaxYearSection = doc.mainContent.selectHead(".app-task-list").selectHead("li:nth-of-type(3)")
          val selectTaxYearRow = selectTaxYearSection.selectHead("ul").selectHead("li")
          val selectTaxYearLink = selectTaxYearRow.selectNth("span", 1).selectHead("a")
          selectTaxYearLink.text mustBe SelectedTaxYear.next(accountingPeriodService.currentTaxYear, accountingPeriodService.currentTaxYear + 1)
          selectTaxYearLink.attr("href") mustBe controllers.agent.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show(editMode = true).url
          selectTaxYearRow.selectNth("span", 2).text mustBe complete
          selectTaxYearRow.selectHead("strong").attr("class") mustBe "govuk-tag"
        }
      }

      "display the text to let them know they can now sign up" in {
        doc.mainContent.selectHead("p").text mustBe agentSignUpReadyText
      }

      "display the sign up button" in {
        doc.mainContent.selectHead("button").text mustBe submitContinue
      }

    }

    "given a forced year in the task list model" must {
      val doc = document(forcedYearTaskList)
      "display the select tax year with status complete and no link" when {
        "the user has not selected any tax year to sign up" in {
          val selectTaxYearSection = doc.mainContent.selectNth("ul", 2)
          val selectTaxYearText = selectTaxYearSection.selectNth("span", 1)
          selectTaxYearText.text mustBe next(getCurrentTaxEndYear, getCurrentTaxEndYear + 1)
          selectTaxYearSection.selectNth("span", 2).text mustBe complete
          selectTaxYearSection.selectHead("strong").attr("class") mustBe "govuk-tag"
          selectTaxYearText.select("a").size() mustBe 0
        }
      }
    }
  }
}
