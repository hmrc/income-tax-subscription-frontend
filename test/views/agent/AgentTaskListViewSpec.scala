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

package views.agent

import assets.MessageLookup.Summary.SelectedTaxYear
import assets.MessageLookup.TaskList._
import models._
import models.common.business._
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel, TaskListModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.twirl.api.Html
import services.AccountingPeriodService
import utilities.ViewSpec
import views.html.agent.AgentTaskList

class AgentTaskListViewSpec extends ViewSpec {

  val taskListView: AgentTaskList = app.injector.instanceOf[AgentTaskList]

  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  lazy val postAction: Call = controllers.agent.routes.TaskListController.submit()

  def customTaskListModel(taxYearSelection: Option[AccountingYearModel] = None,
                          selfEmployments: Seq[SelfEmploymentData] = Nil,
                          selfEmploymentAccountingMethod: Option[AccountingMethod] = None,
                          ukProperty: Option[PropertyModel] = None,
                          overseasProperty: Option[OverseasPropertyModel] = None): TaskListModel = {
    TaskListModel(taxYearSelection,
      selfEmployments,
      selfEmploymentAccountingMethod,
      ukProperty,
      overseasProperty
    )

  }

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
      businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line1"), Some("Postcode")))),
      confirmed = true
    )),
    selfEmploymentAccountingMethod = Some(Cash),
    ukProperty = Some(PropertyModel(Some(Cash), Some(DateModel("1", "2", "1980")), confirmed = true)),
    overseasProperty = Some(OverseasPropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("1", "2", "3")), confirmed = true))
  )


  def page(taskList: TaskListModel = customTaskListModel()): Html = taskListView(
    postAction = postAction,
    viewModel = taskList,
    clientName = "clientName",
    clientNino = "clientNino"
  )(request, implicitly, appConfig)


  def document(taskList: TaskListModel = customTaskListModel()): Document = Jsoup.parse(page(taskList).body)

  "business task list view" must {
    "have a title" in {
      document().title mustBe agentTitle
    }

    "have a heading" in {
      document().select("h1").text mustBe agentHeading
    }

    "have a client name and client nino" in {
      document().mainContent.getElementById("userNameNino").text mustBe "clientName"+" "+"|"+" "+"clientNino"
    }

    "have a paragraph for accounting period confirm" in {
      document().mainContent.getElementById("accountingPeriodConfirm").text mustBe "Accounting period confirmed: 6 April to 5 April"
    }

    "have a contents list" in {
      val contentList = document().select("ol").select("h2")
      contentList.text() must include(item1)
      contentList.text() must include(agentItem2)
      contentList.text() must include(item3)
    }

    "display the dynamic content correctly" when {
      "there is no user data" must {
        "display the application is incomplete" in {
          document().getElementById("taskListStatus").text mustBe subHeadingIncomplete
        }

        "display the number of sections complete out of the total" in {
          document().mainContent.getElementById("taskListCompletedSummary").text mustBe contentSummary(0, 2)
        }

        "in the select tax year section: display the select tax year link with status incomplete" when {
          "the user has not selected any tax year to sign up" in {
            val selectTaxYearSection = document().mainContent.selectNth("ul", 1)
            val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
            selectTaxYearLink.text mustBe selectTaxYear
            selectTaxYearSection.selectNth("span", 2).text mustBe notStarted
            selectTaxYearLink.attr("href") mustBe controllers.agent.routes.WhatYearToSignUpController.show().url
          }
        }

        "display the add a business link" in {
          val businessLink = document().mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectHead("a")
          businessLink.text mustBe addBusiness
          businessLink.attr("href") mustBe controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
        }

        "display the sign up incomplete text" in {
          val incompleteText = document().mainContent.selectHead("ol > li:nth-of-type(3) > ul").selectHead("span")
          incompleteText.text mustBe agentSignUpIncompleteText
        }

        "do not display the sign up button" in {
          document().mainContent.selectOptionally("button") mustBe None
        }
      }

      "there is partial user data" must {
        "display the application is incomplete" in {
          document(partialTaskListComplete).getElementById("taskListStatus").text mustBe subHeadingIncomplete
        }

        "display the number of sections complete out of the total" in {
          document(partialTaskListComplete).mainContent.getElementById("taskListCompletedSummary").text mustBe
            contentSummary(numberComplete = 0, numberTotal = 5)
        }

        "in the select tax year section: display the select tax year link with status in progress" when {
          "the user has selected the tax year but not confirmed the answer in tax year CYA page" in {
            val selectTaxYearSection = document(partialTaskListComplete).mainContent.selectNth("ul", 1)
            val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
            selectTaxYearLink.text mustBe selectTaxYear
            selectTaxYearSection.selectNth("span", 2).text mustBe inProgress
            selectTaxYearLink.attr("href") mustBe controllers.agent.routes.WhatYearToSignUpController.show().url
          }
        }

        "display an incomplete self employment with just the business name" in {
          val selfEmploymentSection = document(partialTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 1)
          val selfEmploymentLink = selfEmploymentSection.selectNth("span", 1).selectHead("a")
          selfEmploymentLink.text mustBe "Name1"
          selfEmploymentLink.attr("href") mustBe s"""${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id1&isEditMode=true"""
          selfEmploymentSection.selectNth("span", 2).text mustBe incomplete
        }

        "display an incomplete self employment" which {
          val selfEmploymentSection = document(partialTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 2)

          "contains a change link with a business name and a trade name" in {
            val selfEmploymentLink = selfEmploymentSection.selectNth("span", 1).selectHead("a")
            selfEmploymentLink.text mustBe "Name2 TradeName"
            selfEmploymentLink.attr("href") mustBe s"""${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id2&isEditMode=true"""
            selfEmploymentSection.selectNth("span", 2).text mustBe incomplete
          }

          "contains a remove link" in {
            val selfEmploymentRemoveLink = selfEmploymentSection.selectNth("span", 3).selectHead("a")
            selfEmploymentRemoveLink.selectHead("span[aria-hidden='true']").text mustBe "Remove"
            selfEmploymentRemoveLink.selectHead("span.govuk-visually-hidden").text mustBe "Remove Name2 TradeName"
            selfEmploymentRemoveLink.attr("href") mustBe controllers.agent.business.routes.RemoveBusinessController.show("id2").url
          }
        }

        "display an incomplete UK property business" which {
          val ukPropertyIncomeSection = document(partialTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 3)

          "contains a change link" in {
            val ukPropertyIncomeLink = ukPropertyIncomeSection.selectNth("span", 1).selectHead("a")
            ukPropertyIncomeLink.text() mustBe ukPropertyBusiness
            ukPropertyIncomeLink.attr("href") mustBe controllers.agent.business.routes.PropertyCheckYourAnswersController.show(editMode = true).url
            ukPropertyIncomeSection.selectNth("span", 2).text mustBe incomplete
          }

          "contains a remove link" in {
            val ukPropertyRemoveLink = ukPropertyIncomeSection.selectNth("span", 3).selectHead("a")
            ukPropertyRemoveLink.selectHead("span[aria-hidden='true']").text mustBe "Remove"
            ukPropertyRemoveLink.selectHead("span.govuk-visually-hidden").text mustBe "Remove UK property business"
            ukPropertyRemoveLink.attr("href") mustBe controllers.agent.business.routes.ClientRemoveUkPropertyController.show.url
          }
        }

        "display an incomplete overseas property business" which {
          val overseasPropertySection =
            document(partialTaskListComplete)
              .mainContent
              .selectHead("ol > li:nth-of-type(2) > ul")
              .selectNth("li", 4)

          "contains a change link" in {
            val overseasPropertyLink = overseasPropertySection.selectNth("span", 1).selectHead("a")
            overseasPropertyLink.text mustBe overseasPropertyBusiness
            overseasPropertyLink.attr("href") mustBe controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
            overseasPropertySection.selectNth("span", 2).text mustBe incomplete
          }

          "contains a remove link" in {
            val overseasPropertyRemoveLink = overseasPropertySection.selectNth("span", 3).selectHead("a")
            overseasPropertyRemoveLink.selectHead("span[aria-hidden='true']").text mustBe "Remove"
            overseasPropertyRemoveLink.selectHead("span.govuk-visually-hidden").text mustBe "Remove overseas property business"
            overseasPropertyRemoveLink.attr("href") mustBe controllers.agent.business.routes.RemoveClientOverseasPropertyController.show.url
          }
        }

        "display the add a business link" in {
          val businessLink = document(partialTaskListComplete)
            .mainContent
            .selectHead("ol > li:nth-of-type(2) > ul")
            .selectNth("li", 5)
            .selectHead("a")
          businessLink.text mustBe addBusiness
          businessLink.attr("href") mustBe controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
        }

        "display the sign up incomplete text" in {
          val incompleteText = document(partialTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(3) > ul").selectHead("span")
          incompleteText.text mustBe agentSignUpIncompleteText
        }

        "do not display the sign up button" in {
          document(partialTaskListComplete).mainContent.selectOptionally("button") mustBe None
        }
      }

      "there is full user data" must {
        "display the application is complete" in {
          document(completedTaskListComplete).getElementById("taskListStatus").text mustBe subHeadingComplete
        }

        "display the number of sections complete out of the total" in {
          document(completedTaskListComplete).mainContent.getElementById("taskListCompletedSummary").text mustBe contentSummary(4, 4)
        }

        "display a complete tax year with an edit link to the Tax Year CYA" when {
          "the user has selected the tax year and confirmed the answer in tax year CYA page" in {
            val selectTaxYearSection = document(completedTaskListComplete).mainContent.selectNth("ul", 1)
            val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
            selectTaxYearLink.text mustBe SelectedTaxYear.next(accountingPeriodService.currentTaxYear, accountingPeriodService.currentTaxYear + 1)
            selectTaxYearSection.selectNth("span", 2).text mustBe complete
            selectTaxYearLink.attr("href") mustBe controllers.agent.routes.TaxYearCheckYourAnswersController.show(editMode = true).url
          }
        }

        "display a complete self employment" in {
          val selfEmploymentSection = document(completedTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 1)
          val selfEmploymentLink = selfEmploymentSection.selectNth("span", 1).selectHead("a")
          selfEmploymentLink.text mustBe "Name1 TradeName"
          selfEmploymentLink.attr("href") mustBe s"""${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id1&isEditMode=true"""
          selfEmploymentSection.selectNth("span", 2).text mustBe complete
        }

        "display a complete uk property income" in {
          val ukPropertyIncomeSection = document(completedTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 2)
          val ukPropertyIncomeLink = ukPropertyIncomeSection.selectNth("span", 1).selectHead("a")
          ukPropertyIncomeLink.text() mustBe ukPropertyBusiness
          ukPropertyIncomeLink.attr("href") mustBe controllers.agent.business.routes.PropertyCheckYourAnswersController.show(editMode = true).url
          ukPropertyIncomeSection.selectNth("span", 2).text mustBe complete
        }

        "display a complete overseas property income" in {
          val overseasPropertySection = document(completedTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 3)
          val overseasPropertyLink = overseasPropertySection.selectNth("span", 1).selectHead("a")
          overseasPropertyLink.text mustBe overseasPropertyBusiness
          overseasPropertyLink.attr("href") mustBe controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
          overseasPropertySection.selectNth("span", 2).text mustBe complete
        }

        "display the add a business link" in {
          val businessLink = document(completedTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(2) > ul").selectNth("li", 4).selectHead("a")
          businessLink.text mustBe addBusiness
          businessLink.attr("href") mustBe controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
        }

        "display the sign up button" in {
          document(completedTaskListComplete).mainContent.selectHead("button").text mustBe submitContinue
        }

        "do not display the sign up incomplete text" in {
          document(completedTaskListComplete).mainContent.selectHead("ol > li:nth-of-type(3) > ul").selectOptionally("span") mustBe None
        }
      }
    }
  }
}
