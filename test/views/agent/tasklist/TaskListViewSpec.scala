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
import controllers.agent.tasklist.taxyear.routes

import scala.util.Random

class TaskListViewSpec extends ViewSpec {

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

  private val partialTaskListIncomeOnly = customTaskListModel(
    taxYearSelection = None,
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

  private val partialTaskListTaxYearOnly = customTaskListModel(
    taxYearSelection = Some(AccountingYearModel(Current, confirmed = true)),
    selfEmployments = Nil,
    ukProperty = None,
    overseasProperty = None
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

  "Task List view" when {

    "given empty task list model" must {
      def doc = document(emptyTaskList)

      "have a title" in {
        doc.title mustBe agentTitle
      }

      "have a contents list" in {
        val contentList = doc.select("h2")
        contentList.text() must include(agentItem1)
        contentList.text() must include(agentItem2)
        contentList.text() must include(agentItem3)
      }


      "display the dynamic content correctly" when {
        "there is no user data" must {
          "in the client information section: have a client name, nino and utr" in {
            doc.mainContent.getElementsByClass("govuk-summary-list__key").text contains s"Client: $clientName | $clientNino |$clientUtr"
          }

          "display income sources and select tax year items with links, hints and incomplete status" in {

            val clientIncomeSources = doc.mainContent.selectHead("ul").selectHead("li")
            val clientIncomeSourcesTag = doc.mainContent.selectHead("ul").selectHead("li").selectHead("#client-details-1-status")

            clientIncomeSources.selectHead("a").text mustBe addYourClientsIncomeSource
            clientIncomeSources.selectHead("a").attr("href") mustBe controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
            clientIncomeSourcesTag.text mustBe incomplete
            clientIncomeSources.select("#client-details-1-hint").text mustBe addYourClientsIncomeSourceHint

            val clientSelectTaxYear = doc.mainContent.selectHead("ul").selectNth("li", 2)
            val clientSelectTaxYearTag = doc.mainContent.selectHead("ul").selectNth("li", 2).selectHead("#client-details-2-status")

            clientSelectTaxYear.selectHead("a").text mustBe selectTaxYear
            clientSelectTaxYear.selectHead("a").attr("href") mustBe controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
            clientSelectTaxYearTag.text mustBe incomplete
            clientSelectTaxYear.select("#client-details-2-hint").text mustBe selectTaxYearHintBoth

          }

          "display Review and sign up with disabled link and status" in {

            val signUpYourClient = doc.mainContent.selectNth("ul", 2).selectHead("li").selectHead("div")
            val signUpYourClientTag = doc.mainContent.selectNth("ul", 2).selectHead("li").select("#sign-up-section-1-status")

            signUpYourClient.text mustBe signUpClientLinkText
            signUpYourClientTag.text mustBe cannotStart

          }

          "display the save and come back later button" in {
            doc.mainContent.getElementsByClass("govuk-button--secondary").text mustBe saveAndComeBackLater
          }

        }

      }
    }


    "given partial Task List model with only income sources completed" must {
      def doc = document(partialTaskListIncomeOnly)

      "have a title" in {
        doc.title mustBe agentTitle
      }

      "have a contents list" in {
        val contentList = doc.select("h2")
        contentList.text() must include(agentItem1)
        contentList.text() must include(agentItem2)
        contentList.text() must include(agentItem3)
      }

      "in the client information section: have a client name, nino and utr" in {
        doc.mainContent.getElementsByClass("govuk-summary-list__key").text contains s"Client: $clientName | $clientNino |$clientUtr"
      }

      "display income sources item with complete status and select tax year item with incomplete status both with links and hints" in {

        val clientIncomeSources = doc.mainContent.selectHead("ul").selectHead("li")
        val clientIncomeSourcesTag = doc.mainContent.selectHead("ul").selectHead("li").selectHead("#client-details-1-status")

        clientIncomeSources.selectHead("a").text mustBe addYourClientsIncomeSource
        clientIncomeSources.selectHead("a").attr("href") mustBe controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
        clientIncomeSourcesTag.text mustBe complete
        clientIncomeSources.select("#client-details-1-hint").text mustBe addYourClientsIncomeSourceHint

        val clientSelectTaxYear = doc.mainContent.selectHead("ul").selectNth("li", 2)
        val clientSelectTaxYearTag = doc.mainContent.selectHead("ul").selectNth("li", 2).selectHead("#client-details-2-status")

        clientSelectTaxYear.selectHead("a").text mustBe selectTaxYear
        clientSelectTaxYear.selectHead("a").attr("href") mustBe controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
        clientSelectTaxYearTag.text mustBe incomplete
        clientSelectTaxYear.select("#client-details-2-hint").text mustBe selectTaxYearHintBoth

      }

      "display Review and sign up with disabled link and status" in {

        val signUpYourClient = doc.mainContent.selectNth("ul", 2).selectHead("li").selectHead("div")
        val signUpYourClientTag = doc.mainContent.selectNth("ul", 2).selectHead("li").select("#sign-up-section-1-status")

        signUpYourClient.text mustBe signUpClientLinkText
        signUpYourClientTag.text mustBe cannotStart

      }

      "display the save and come back later button" in {
        doc.mainContent.getElementsByClass("govuk-button--secondary").text mustBe saveAndComeBackLater
      }

    }

    "given partial Task List model with only tax year completed" must {
      def doc = document(partialTaskListTaxYearOnly)

      "have a title" in {
        doc.title mustBe agentTitle
      }

      "have a contents list" in {
        val contentList = doc.select("h2")
        contentList.text() must include(agentItem1)
        contentList.text() must include(agentItem2)
        contentList.text() must include(agentItem3)
      }

      "in the client information section: have a client name, nino and utr" in {
        doc.mainContent.getElementsByClass("govuk-summary-list__key").text contains s"Client: $clientName | $clientNino |$clientUtr"
      }

      "display income sources item with incomplete status and select tax year item with complete status both with links and hints" in {

        val clientIncomeSources = doc.mainContent.selectHead("ul").selectHead("li")
        val clientIncomeSourcesTag = doc.mainContent.selectHead("ul").selectHead("li").selectHead("#client-details-1-status")

        clientIncomeSources.selectHead("a").text mustBe addYourClientsIncomeSource
        clientIncomeSources.selectHead("a").attr("href") mustBe controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
        clientIncomeSourcesTag.text mustBe incomplete
        clientIncomeSources.select("#client-details-1-hint").text mustBe addYourClientsIncomeSourceHint

        val clientSelectTaxYear = doc.mainContent.selectHead("ul").selectNth("li", 2)
        val clientSelectTaxYearTag = doc.mainContent.selectHead("ul").selectNth("li", 2).selectHead("#client-details-2-status")

        clientSelectTaxYear.selectHead("a").text mustBe selectTaxYear
        clientSelectTaxYear.selectHead("a").attr("href") mustBe routes.TaxYearCheckYourAnswersController.show(editMode = true).url
        clientSelectTaxYearTag.text mustBe complete
        clientSelectTaxYear.select("#client-details-2-hint").text mustBe selectTaxYearHintBoth

      }

      "display Review and sign up with disabled link and status" in {

        val signUpYourClient = doc.mainContent.selectNth("ul", 2).selectHead("li").selectHead("div")
        val signUpYourClientTag = doc.mainContent.selectNth("ul", 2).selectHead("li").select("#sign-up-section-1-status")

        signUpYourClient.text mustBe signUpClientLinkText
        signUpYourClientTag.text mustBe cannotStart

      }

      "display the save and come back later button" in {
        doc.mainContent.getElementsByClass("govuk-button--secondary").text mustBe saveAndComeBackLater
      }

    }


    "given complete task list model" must {
      def doc = document(completeTaskList)

      "have a title" in {
        doc.title mustBe agentTitle
      }

      "have a contents list" in {
        val contentList = doc.select("h2")
        contentList.text() must include(agentItem1)
        contentList.text() must include(agentItem2)
        contentList.text() must include(agentItem3)
      }

      "in the client information section: have a client name, nino and utr" in {
        doc.mainContent.getElementsByClass("govuk-summary-list__key").text contains s"Client: $clientName | $clientNino |$clientUtr"
      }

      "display income sources and select tax year as complete status with links, hint" in {

        val clientIncomeSources = doc.mainContent.selectHead("ul").selectHead("li")
        val clientIncomeSourcesTag = doc.mainContent.selectHead("ul").selectHead("li").selectHead("#client-details-1-status")

        clientIncomeSources.selectHead("a").text mustBe addYourClientsIncomeSource
        clientIncomeSources.selectHead("a").attr("href") mustBe controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
        clientIncomeSourcesTag.text mustBe complete
        clientIncomeSources.select("#client-details-1-hint").text mustBe addYourClientsIncomeSourceHint

        val clientSelectTaxYear = doc.mainContent.selectHead("ul").selectNth("li", 2)
        val clientSelectTaxYearTag = doc.mainContent.selectHead("ul").selectNth("li", 2).selectHead("#client-details-2-status")

        clientSelectTaxYear.selectHead("a").text mustBe selectTaxYear
        clientSelectTaxYear.selectHead("a").attr("href") mustBe routes.TaxYearCheckYourAnswersController.show(editMode = true).url
        clientSelectTaxYearTag.text mustBe complete
        clientSelectTaxYear.select("#client-details-2-hint").text mustBe selectTaxYearHintBoth

      }

      "display Review and sign up with active link and status" in {

        val signUpYourClient = doc.mainContent.selectNth("ul", 2).selectHead("li").selectHead("div")
        val signUpYourClientTag = doc.mainContent.selectNth("ul", 2).selectHead("li").select("#sign-up-section-1-status")

        signUpYourClient.selectHead("a").text mustBe signUpClientLinkText
        signUpYourClient.selectHead("a").attr("href") mustBe controllers.agent.routes.GlobalCheckYourAnswersController.show.url
        signUpYourClientTag.text mustBe incomplete

      }

      "display the save and come back later button" in {
        doc.mainContent.getElementsByClass("govuk-button--secondary").text mustBe saveAndComeBackLater
      }

    }

    "given a forced year in the task list model" must {
      val doc = document(forcedYearTaskList)

      "display select tax year as complete status with links, hint" in {

        val clientSelectTaxYear = doc.mainContent.selectHead("ul").selectNth("li", 2)
        val clientSelectTaxYearTag = doc.mainContent.selectHead("ul").selectNth("li", 2).selectHead("#client-details-2-status")

        clientSelectTaxYear.select("a").size() mustBe 0
        clientSelectTaxYear.selectHead("div").text mustBe next(getCurrentTaxEndYear, getCurrentTaxEndYear + 1) + " " + selectNextTaxYearHint
        clientSelectTaxYearTag.text mustBe complete

      }
    }
  }
}
