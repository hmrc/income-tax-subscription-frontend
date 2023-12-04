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

package views.agent

import agent.assets.MessageLookup.Base.saveAndComeBackLater
import assets.MessageLookup.Summary.SelectedTaxYear
import assets.MessageLookup.TaskList._
import assets.MessageLookup.TaxYearCheckYourAnswers.next
import config.featureswitch.FeatureSwitch.EnableTaskListRedesign
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
import views.html.agent.AgentTaskList

import scala.util.Random

class AgentTaskListViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EnableTaskListRedesign)
  }

  val selectorForFirstBusiness = "ol > li:nth-of-type(2) > ul:nth-of-type(1)"
  val selectorForFirstParaOfBusiness = "ol > li:nth-of-type(2)"

  val taskListView: AgentTaskList = app.injector.instanceOf[AgentTaskList]

  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  lazy val postAction: Call = controllers.agent.routes.TaskListController.submit()

  private def customTaskListModel(taxYearSelection: Option[AccountingYearModel] = None,
                                  selfEmployments: Seq[SelfEmploymentData] = Nil,
                                  selfEmploymentAccountingMethod: Option[AccountingMethod] = None,
                                  ukProperty: Option[PropertyModel] = None,
                                  overseasProperty: Option[OverseasPropertyModel] = None,
                                  incomeSourcesConfirmed: Option[Boolean] = None): TaskListModel =
    TaskListModel(taxYearSelection,
      selfEmployments,
      selfEmploymentAccountingMethod,
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
    selfEmploymentAccountingMethod = Some(Cash),
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
    )(request, implicitly, appConfig)
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
          "display the application is incomplete" in {
            doc.getElementById("taskListStatus").text mustBe subHeadingIncomplete
          }

          "display the number of sections complete out of the total" in {
            doc.mainContent.getElementById("taskListCompletedSummary").text mustBe contentSummary(1, 3)
          }

          "in the client information section: have a client name, nino and utr" in {
            doc.mainContent.getElementsByClass("govuk-summary-list__key").text contains s"Client: $clientName | $clientNino |$clientUtr"
          }

          "in the select tax year section: display the select tax year link with status incomplete" when {
            "the user has not selected any tax year to sign up" in {
              val selectTaxYearSection = doc.mainContent.selectNth("ul", 1)
              val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
              selectTaxYearLink.text mustBe selectTaxYear
              selectTaxYearSection.selectNth("span", 2).text mustBe notStarted
              selectTaxYearSection.selectHead("strong").attr("class") mustBe "govuk-tag govuk-tag--grey"
              selectTaxYearLink.attr("href") mustBe controllers.agent.routes.WhatYearToSignUpController.show().url
            }
          }

          "display the add a business link" in {
            val businessLink = doc.mainContent.getElementById("add_business")
            businessLink.text() mustBe agentAddBusiness
            businessLink.classNames() must contain("govuk-link")
            businessLink.attr("href") mustBe controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
          }

          "display the sign up incomplete text" in {
            val incompleteText = doc.mainContent.selectNth("p", 2)
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

      "display the application is incomplete" in {
        doc.getElementById("taskListStatus").text mustBe subHeadingIncomplete
      }

      "display the number of sections complete out of the total" in {
        doc.mainContent.getElementById("taskListCompletedSummary").text mustBe
          contentSummary(numberComplete = 1, numberTotal = 6)
      }

      "in the client information section: have a client name, nino and utr" in {
        doc.mainContent.getElementsByClass("govuk-summary-list__key").text contains s"Client: $clientName | $clientNino |$clientUtr"
      }

      "in the select tax year section: display the select tax year link with status in progress" when {
        "the user has selected the tax year but not confirmed the answer in tax year CYA page" in {
          val selectTaxYearSection = doc.mainContent.selectNth("ul", 2)
          val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
          selectTaxYearLink.text mustBe selectTaxYear
          selectTaxYearSection.selectNth("span", 2).text mustBe inProgress
          selectTaxYearSection.selectHead("strong").attr("class") mustBe "govuk-tag govuk-tag--grey"
          selectTaxYearLink.attr("href") mustBe controllers.agent.routes.WhatYearToSignUpController.show().url
        }
      }

      "display an incomplete self employment with just the business name" in {
        val selfEmploymentSection = doc.mainContent.selectHead(selectorForFirstBusiness).selectNth("li", 1)
        val selfEmploymentLink = selfEmploymentSection.selectNth("span", 1).selectHead("a")
        selfEmploymentLink.text mustBe "Name1"
        selfEmploymentLink.attr("href") mustBe s"""${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id1&isEditMode=true"""
        selfEmploymentSection.selectNth("span", 2).text mustBe incomplete
        selfEmploymentSection.selectHead("strong").attr("class") mustBe "govuk-tag govuk-tag--grey"
      }

      "display an incomplete self employment" which {
        val selfEmploymentSection = doc.mainContent.selectHead(selectorForFirstBusiness).selectNth("li", 2)

        "contains a change link with a business name and a trade name" in {
          val selfEmploymentLink = selfEmploymentSection.selectNth("span", 1).selectHead("a")
          selfEmploymentLink.text mustBe "Name2 TradeName"
          selfEmploymentLink.attr("href") mustBe s"""${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id2&isEditMode=true"""
          selfEmploymentSection.selectNth("span", 2).text mustBe incomplete
          selfEmploymentSection.selectHead("strong").attr("class") mustBe "govuk-tag govuk-tag--grey"
        }

        "contains a remove link" in {
          val selfEmploymentRemoveLink = selfEmploymentSection.selectNth("span", 3).selectHead("a")
          selfEmploymentRemoveLink.selectHead("span[aria-hidden='true']").text mustBe "Remove"
          selfEmploymentRemoveLink.selectHead("span.govuk-visually-hidden").text mustBe "Remove Name2 TradeName"
          selfEmploymentRemoveLink.attr("href") mustBe controllers.agent.business.routes.RemoveBusinessController.show("id2").url
        }
      }

      "display an incomplete UK property business" which {
        val ukPropertyIncomeSection = doc.mainContent.selectHead(selectorForFirstBusiness).selectNth("li", 3)

        "contains a change link" in {
          val ukPropertyIncomeLink = ukPropertyIncomeSection.selectNth("span", 1).selectHead("a")
          ukPropertyIncomeLink.text() mustBe ukPropertyBusiness
          ukPropertyIncomeLink.attr("href") mustBe controllers.agent.business.routes.PropertyCheckYourAnswersController.show(editMode = true).url
          ukPropertyIncomeSection.selectNth("span", 2).text mustBe incomplete
          ukPropertyIncomeSection.selectHead("strong").attr("class") mustBe "govuk-tag govuk-tag--grey"
        }

        "contains a remove link" in {
          val ukPropertyRemoveLink = ukPropertyIncomeSection.selectNth("span", 3).selectHead("a")
          ukPropertyRemoveLink.selectHead("span[aria-hidden='true']").text mustBe "Remove"
          ukPropertyRemoveLink.selectHead("span.govuk-visually-hidden").text mustBe "Remove UK property business"
          ukPropertyRemoveLink.attr("href") mustBe controllers.agent.business.routes.RemoveUkPropertyController.show.url
        }
      }

      "display an incomplete overseas property business" which {
        val overseasPropertySection =
          doc
            .mainContent
            .selectHead(selectorForFirstBusiness)
            .selectNth("li", 4)

        "contains a change link" in {
          val overseasPropertyLink = overseasPropertySection.selectNth("span", 1).selectHead("a")
          overseasPropertyLink.text mustBe overseasPropertyBusiness
          overseasPropertyLink.attr("href") mustBe controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
          overseasPropertySection.selectNth("span", 2).text mustBe incomplete
          overseasPropertySection.selectHead("strong").attr("class") mustBe "govuk-tag govuk-tag--grey"
        }

        "contains a remove link" in {
          val overseasPropertyRemoveLink = overseasPropertySection.selectNth("span", 3).selectHead("a")
          overseasPropertyRemoveLink.selectHead("span[aria-hidden='true']").text mustBe "Remove"
          overseasPropertyRemoveLink.selectHead("span.govuk-visually-hidden").text mustBe "Remove overseas property business"
          overseasPropertyRemoveLink.attr("href") mustBe controllers.agent.business.routes.RemoveOverseasPropertyController.show.url
        }
      }

      "display the add your income sources link and tag" when {
        "the task list redesign feature switch is enabled" in {
          enable(EnableTaskListRedesign)

          val incomeSourcesSection = doc.mainContent.selectHead(".app-task-list").selectNth("li", 2)
          val incomeSourcesRow = incomeSourcesSection.selectHead("ul").selectHead("li")

          val incomeSourcesLink = incomeSourcesRow.selectNth("span", 1).selectHead("a")
          incomeSourcesLink.text mustBe addYourClientsIncomeSource
          incomeSourcesLink.attr("href") mustBe controllers.agent.routes.YourIncomeSourceToSignUpController.show.url

          val incomeSourcesTag = incomeSourcesRow.selectNth("span", 2)
          incomeSourcesTag.selectHead("strong").text mustBe incomplete

          incomeSourcesLink.attr("aria-describedby") mustBe incomeSourcesTag.id

          doc.mainContent.selectOptionally("#add_business") mustBe None
        }
      }


      "display the add a business link" in {
        val businessLink = doc
          .mainContent
          .getElementById("add_business")

        businessLink.text mustBe agentAddBusiness
        businessLink.attr("href") mustBe controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
      }

      "display the sign up incomplete text" in {
        val incompleteText = doc.mainContent.selectNth("p", 2)
        incompleteText.text mustBe agentSignUpIncompleteText
      }

      "do not display the sign up button" in {
        doc.mainContent.selectOptionally("button") mustBe None
      }
    }

    "given complete task list model" must {
      def doc = document(completeTaskList)

      "display the application is complete" in {
        doc.getElementById("taskListStatus").text mustBe subHeadingComplete
      }

      "display the number of sections complete out of the total" in {
        doc.mainContent.getElementById("taskListCompletedSummary").text mustBe contentSummary(5, 5)
      }

      "in the client information section: have a client name, nino and utr" in {
        doc.mainContent.getElementsByClass("govuk-summary-list__key").text contains s"Client: $clientName | $clientNino |$clientUtr"
      }

      "display a complete tax year with an edit link to the Tax Year CYA" when {
        "the user has selected the tax year and confirmed the answer in tax year CYA page" in {
          val selectTaxYearSection = doc.mainContent.selectNth("ul", 2)
          val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
          selectTaxYearLink.text mustBe SelectedTaxYear.next(accountingPeriodService.currentTaxYear, accountingPeriodService.currentTaxYear + 1)
          selectTaxYearSection.selectNth("span", 2).text mustBe complete
          selectTaxYearSection.selectHead("strong").attr("class") mustBe "govuk-tag"
          selectTaxYearLink.attr("href") mustBe controllers.agent.routes.TaxYearCheckYourAnswersController.show(editMode = true).url
        }
      }

      "display a complete self employment" in {
        val selfEmploymentSection = doc.mainContent.selectHead(selectorForFirstBusiness).selectNth("li", 1)
        val selfEmploymentLink = selfEmploymentSection.selectNth("span", 1).selectHead("a")
        selfEmploymentLink.text mustBe "Name1 TradeName"
        selfEmploymentLink.attr("href") mustBe s"""${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id1&isEditMode=true"""
        selfEmploymentSection.selectNth("span", 2).text mustBe complete
        selfEmploymentSection.selectHead("strong").attr("class") mustBe "govuk-tag"
      }

      "display a complete uk property income" in {
        val ukPropertyIncomeSection = doc.mainContent.selectHead(selectorForFirstBusiness).selectNth("li", 2)
        val ukPropertyIncomeLink = ukPropertyIncomeSection.selectNth("span", 1).selectHead("a")
        ukPropertyIncomeLink.text() mustBe ukPropertyBusiness
        ukPropertyIncomeLink.attr("href") mustBe controllers.agent.business.routes.PropertyCheckYourAnswersController.show(editMode = true).url
        ukPropertyIncomeSection.selectNth("span", 2).text mustBe complete
        ukPropertyIncomeSection.selectHead("strong").attr("class") mustBe "govuk-tag"
      }

      "display a complete overseas property income" in {
        val overseasPropertySection = doc.mainContent.selectHead(selectorForFirstBusiness).selectNth("li", 3)
        val overseasPropertyLink = overseasPropertySection.selectNth("span", 1).selectHead("a")
        overseasPropertyLink.text mustBe overseasPropertyBusiness
        overseasPropertyLink.attr("href") mustBe controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
        overseasPropertySection.selectNth("span", 2).text mustBe complete
        overseasPropertySection.selectHead("strong").attr("class") mustBe "govuk-tag"
      }

      "display the view your income sources link and tag" when {
        "the task list redesign feature switch is enabled" in {
          enable(EnableTaskListRedesign)

          val incomeSourcesSection = doc.mainContent.selectHead(".app-task-list").selectNth("li", 2)
          val incomeSourcesRow = incomeSourcesSection.selectHead("ul").selectHead("li")

          val incomeSourcesLink = incomeSourcesRow.selectNth("span", 1).selectHead("a")
          incomeSourcesLink.text mustBe viewYourClientsIncomeSources
          incomeSourcesLink.attr("href") mustBe controllers.agent.routes.YourIncomeSourceToSignUpController.show.url

          val incomeSourcesTag = incomeSourcesRow.selectNth("span", 2)
          incomeSourcesTag.selectHead("strong").text mustBe complete

          incomeSourcesLink.attr("aria-describedby") mustBe incomeSourcesTag.id

          doc.mainContent.selectOptionally("#add_business") mustBe None
        }
      }

      "display the add a business link" in {
        val businessLink = doc.mainContent.getElementById("add_business")
        businessLink.text mustBe agentAddBusiness
        businessLink.attr("href") mustBe controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
      }

      "display the text to let them know they can now sign up" in {
        doc.mainContent.selectNth("p", 2).text mustBe agentSignUpReadyText
      }

      "display the sign up button" in {
        doc.mainContent.selectHead("button").text mustBe submitContinue
      }

    }
  }

  "given a forced year in the task list model" must {
    val doc = document(forcedYearTaskList)
    "display the select tax year with status complete and no link" when {
      "the user has not selected any tax year to sign up" in {
        val selectTaxYearSection = doc.mainContent.selectNth("ul", 1)
        val selectTaxYearText = selectTaxYearSection.selectNth("span", 1)
        selectTaxYearText.text mustBe next(getCurrentTaxEndYear, getCurrentTaxEndYear + 1)
        selectTaxYearSection.selectNth("span", 2).text mustBe complete
        selectTaxYearSection.selectHead("strong").attr("class") mustBe "govuk-tag"
        selectTaxYearText.select("a").size() mustBe 0
      }
    }
  }
}
