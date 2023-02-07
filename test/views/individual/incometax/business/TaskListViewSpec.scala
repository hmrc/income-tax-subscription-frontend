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

package views.individual.incometax.business

import assets.MessageLookup.Summary.SelectedTaxYear
import assets.MessageLookup.TaskList._
import assets.MessageLookup.TaxYearCheckYourAnswers.next
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
import views.html.individual.incometax.business.TaskList

class TaskListViewSpec extends ViewSpec {

  val selectorForFirstBusiness = "ol > li:nth-of-type(2) > ul:nth-of-type(2)"
  val selectorForFirstParaOfBusiness = "ol > li:nth-of-type(2) > ul:nth-of-type(1)"
  val selectorForFirstParaOfSignup = "ol > li:nth-of-type(3) > ul"

  val taskListView: TaskList = app.injector.instanceOf[TaskList]

  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  lazy val postAction: Call = controllers.individual.business.routes.TaskListController.submit()

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
    taxYearSelection = Some(AccountingYearModel(Next, confirmed = true, editable = true)),
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


  def page(taskList: TaskListModel = customTaskListModel(), maybeIndividualUserFullName: Option[String]): Html = taskListView(
    postAction = postAction,
    viewModel = taskList,
    accountingPeriodService = accountingPeriodService,
    individualUserNino = "individualUserNino",
    maybeIndividualUserFullName = maybeIndividualUserFullName
  )(request, implicitly, appConfig)


  def document(
                taskList: TaskListModel = customTaskListModel(),
                maybeIndividualUserFullName: Option[String] = Some("individualUserFullName")
              ): Document = Jsoup.parse(page(taskList, maybeIndividualUserFullName).body)

  "business task list view" must {
    "have a title" in {
      document().title mustBe title
    }

    "have a heading" in {
      document().select("h1").text mustBe heading
    }

    "have user information" which {
      "includes a heading" in {
        document().mainContent.selectHead(".app-task-list").selectHead("h2").text mustBe userInfoHeading
      }

      "includes a user nino" in {
        document(maybeIndividualUserFullName = None).mainContent.selectHead(".app-task-list").selectHead("p").text mustBe userInfoPartialContent
      }

      "includes a user name and user nino" in {
        document().mainContent.selectHead(".app-task-list").selectHead("p").text mustBe userInfoContent
      }
    }

    "have a contents list" in {
      val contentList = document().select("ol").select("h2")
      contentList.text() must include(item1)
      contentList.text() must include(item2)
      contentList.text() must include(item3)
    }

    "display the save and come back later button" in {
      document().mainContent.getElementsByClass("govuk-button--secondary").text mustBe saveAndComeBackLater
    }

    "display the dynamic content correctly" when {
      "there is no user data" must {
        "display the application is incomplete" in {
          document().getElementById("taskListStatus").text mustBe subHeadingIncomplete
        }


        "display the number of sections complete out of the total" in {
          document().mainContent.getElementById("taskListCompletedSummary").text mustBe contentSummary(0, 2)
        }

        "in the business section: display the information para" should {
          "display the sign up incomplete text" in {
            val infoPara = document().mainContent.selectHead(selectorForFirstParaOfBusiness).selectHead("p")
            infoPara.text mustBe item2Para
          }
        }

        "in the select tax year section: display the select tax year link with status incomplete" when {
          "the user has not selected any tax year to sign up" in {
            val selectTaxYearSection = document().mainContent.selectHead("ul.app-task-list__items")
            val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
            selectTaxYearLink.text mustBe selectTaxYear
            selectTaxYearSection.selectNth("span", 2).text mustBe notStarted
            selectTaxYearLink.attr("href") mustBe controllers.individual.business.routes.WhatYearToSignUpController.show().url
          }
        }

        "display the add a business link" in {
          val businessLink = document().mainContent.getElementById("add_business")
          businessLink.text mustBe addBusiness
          businessLink.classNames() must contain ("govuk-link")
          businessLink.attr("href") mustBe controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show().url
        }

        "display the sign up incomplete text" in {
          val incompleteText = document().mainContent.selectHead(selectorForFirstParaOfSignup).selectHead("p")
          incompleteText.text mustBe signUpIncompleteText
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
            val selectTaxYearSection = document(partialTaskListComplete).mainContent.selectHead("ul.app-task-list__items")
            val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
            selectTaxYearLink.text mustBe selectTaxYear
            selectTaxYearSection.selectNth("span", 2).text mustBe inProgress
            selectTaxYearLink.attr("href") mustBe controllers.individual.business.routes.WhatYearToSignUpController.show().url
          }
        }

        "display an incomplete self employment with just the business name" in {
          val selfEmploymentSection = document(partialTaskListComplete).mainContent.selectHead(selectorForFirstBusiness).selectNth("li", 1)
          val selfEmploymentLink = selfEmploymentSection.selectNth("span", 1).selectHead("a")
          selfEmploymentLink.text mustBe "Name1"
          selfEmploymentLink.attr("href") mustBe s"""${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id1&isEditMode=true"""
          selfEmploymentSection.selectNth("span", 2).text mustBe incomplete

        }

        "display an incomplete self employment" which {
          val selfEmploymentSection = document(partialTaskListComplete).mainContent.selectHead(selectorForFirstBusiness).selectNth("li", 2)

          "contains a change link with a business name and a trade name" in {
            val selfEmploymentLink = selfEmploymentSection.selectNth("span", 1).selectHead("a")
            selfEmploymentLink.text mustBe "Name2 TradeName"
            selfEmploymentLink.attr("href") mustBe s"""${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id2&isEditMode=true"""
            selfEmploymentSection.selectNth("span", 2).text mustBe incomplete
          }

          "contains a remove link" in {
            val selfEmploymentRemoveLink = selfEmploymentSection.selectNth("span", 3).selectHead("a")
            selfEmploymentRemoveLink.selectHead("span[aria-hidden='true']").text mustBe "Remove"
            selfEmploymentRemoveLink.selectHead("span.govuk-visually-hidden").text mustBe "Remove Name2 TradeName"
            selfEmploymentRemoveLink.attr("href") mustBe controllers.individual.business.routes.RemoveBusinessController.show("id2").url
          }
        }

        "display an incomplete uk property income" which {
          val ukPropertyIncomeSection = document(partialTaskListComplete).mainContent.selectHead(selectorForFirstBusiness).selectNth("li", 3)

          "contains a change link" in {
            val ukPropertyIncomeLink = ukPropertyIncomeSection.selectNth("span", 1).selectHead("a")
            ukPropertyIncomeLink.text() mustBe ukPropertyBusiness
            ukPropertyIncomeLink.attr("href") mustBe controllers.individual.business.routes.PropertyCheckYourAnswersController.show(editMode=true).url
            ukPropertyIncomeSection.selectNth("span", 2).text mustBe incomplete
          }

          "contains a remove link" in {
            val ukPropertyRemoveLink = ukPropertyIncomeSection.selectNth("span", 3).selectHead("a")
            ukPropertyRemoveLink.selectHead("span[aria-hidden='true']").text mustBe "Remove"
            ukPropertyRemoveLink.selectHead("span.govuk-visually-hidden").text mustBe "Remove UK property business"
            ukPropertyRemoveLink.attr("href") mustBe controllers.individual.business.routes.RemoveUkPropertyController.show.url
          }
        }

        "display an incomplete overseas property income with remove-link" which {
          val overseasPropertySection = document(partialTaskListComplete)
            .mainContent.
            selectHead(selectorForFirstBusiness).
            selectNth("li", 4)

          "contains a change link" in {
            val overseasPropertyLink = overseasPropertySection.selectNth("span", 1).selectHead("a")
            overseasPropertyLink.text mustBe overseasPropertyBusiness
            overseasPropertyLink.attr("href") mustBe controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.show(editMode=true).url
            overseasPropertySection.selectNth("span", 2).text mustBe incomplete
          }

          "contains a remove link" in {
            val overseasPropertyRemoveLink = overseasPropertySection.selectNth("span", 3).selectHead("a")
            overseasPropertyRemoveLink.selectHead("span[aria-hidden='true']").text mustBe "Remove"
            overseasPropertyRemoveLink.selectHead("span.govuk-visually-hidden").text mustBe "Remove overseas property business"
            overseasPropertyRemoveLink.attr("href") mustBe controllers.individual.business.routes.RemoveOverseasPropertyController.show.url
          }
        }

        "display the add a business link" in {
          val businessLink = document(partialTaskListComplete)
            .mainContent
            .getElementById("add_business")
          businessLink.text mustBe addBusiness
          businessLink.attr("href") mustBe controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show().url
        }

        "display the sign up incomplete text" in {
          val incompleteText = document(partialTaskListComplete).mainContent.selectHead(selectorForFirstParaOfSignup).selectHead("p")
          incompleteText.text mustBe signUpIncompleteText
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
            val selectTaxYearSection = document(completedTaskListComplete).mainContent.selectHead("ul.app-task-list__items")
            val selectTaxYearLink = selectTaxYearSection.selectNth("span", 1).selectHead("a")
            selectTaxYearLink.text mustBe SelectedTaxYear.next(accountingPeriodService.currentTaxYear, accountingPeriodService.currentTaxYear + 1)
            selectTaxYearSection.selectNth("span", 2).text mustBe complete
            selectTaxYearLink.attr("href") mustBe controllers.individual.business.routes.TaxYearCheckYourAnswersController.show(editMode = true).url
          }
        }

        "display a complete self employment" in {
          val selfEmploymentSection = document(completedTaskListComplete).mainContent.selectHead(selectorForFirstBusiness).selectNth("li", 1)
          val selfEmploymentLink = selfEmploymentSection.selectNth("span", 1).selectHead("a")
          selfEmploymentLink.text mustBe "Name1 TradeName"
          selfEmploymentLink.attr("href") mustBe s"""${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id1&isEditMode=true"""
          selfEmploymentSection.selectNth("span", 2).text mustBe complete
        }

        "display a complete uk property income" in {
          val ukPropertyIncomeSection = document(completedTaskListComplete).mainContent.selectHead(selectorForFirstBusiness).selectNth("li", 2)
          val ukPropertyIncomeLink = ukPropertyIncomeSection.selectNth("span", 1).selectHead("a")
          ukPropertyIncomeLink.text() mustBe ukPropertyBusiness
          ukPropertyIncomeLink.attr("href") mustBe controllers.individual.business.routes.PropertyCheckYourAnswersController.show(editMode=true).url
          ukPropertyIncomeSection.selectNth("span", 2).text mustBe complete
        }

        "display a complete overseas property income" in {
          val overseasPropertySection = document(completedTaskListComplete).mainContent.selectHead(selectorForFirstBusiness).selectNth("li", 3)
          val overseasPropertyLink = overseasPropertySection.selectNth("span", 1).selectHead("a")
          overseasPropertyLink.text mustBe overseasPropertyBusiness
          overseasPropertyLink.attr("href") mustBe controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.show(editMode=true).url
          overseasPropertySection.selectNth("span", 2).text mustBe complete
        }

        "display the add a business link" in {
          val businessLink = document(completedTaskListComplete).mainContent.getElementById("add_business")
          businessLink.text mustBe addBusiness
          businessLink.attr("href") mustBe controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show().url
        }

        "display the sign up button" in {
          document(completedTaskListComplete).mainContent.selectHead("button").text mustBe continue
        }

        "do not display the sign up incomplete text" in {
          document(completedTaskListComplete).mainContent.selectHead(selectorForFirstParaOfSignup).selectOptionally("span") mustBe None
        }
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
        selectTaxYearText.select("a").size() mustBe 0
      }
    }
  }
}
