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

  private val partialTaskListWithoutIncome = customTaskListModel(
    taxYearSelection = Some(AccountingYearModel(Current, confirmed = true)),
    selfEmployments = Nil,
    ukProperty = None,
    overseasProperty = None,
    incomeSourcesConfirmed = None
  )

  private val partialTaskListWithoutYear = customTaskListModel(
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
      val contentList = document().select("h2")
      contentList.text() must include(item1)
      contentList.text() must include(item2)
      contentList.text() must include(item3)
    }

    "display the save and come back later button" in {
      document().mainContent.getElementsByClass("govuk-button--secondary").text mustBe messages.saveAndComeBackLater
    }

    "display the dynamic content correctly" when {
      "there is no user data" must {
        "display the income sources and tax year task list" in {
          document().mainContent.mustHaveTaskList(".govuk-task-list:nth-of-type(1)")(
            idPrefix = "client-details",
            items = Seq(
              TaskListItemValues(
                text = addYourIncomeSources,
                link = Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url),
                hint = Some(addYourIncomeSourcesHint),
                tagText = incomplete,
                tagColor = Some("blue")
              ),
              TaskListItemValues(
                text = selectTaxYear,
                link = Some(controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url),
                hint = Some(selectYourTaxYearHintBoth),
                tagText = incomplete,
                tagColor = Some("blue")
              )
            )
          )
        }

        "display the review and sign up task list" in {
          document().mainContent.mustHaveTaskList(".govuk-task-list:nth-of-type(2)")(
            idPrefix = "sign-up-section",
            items = Seq(
              TaskListItemValues(
                text = ReviewAndSignUpText,
                link = None,
                hint = Some(ReviewAndSignUpHint),
                tagText = cannotStart,
                tagColor = None
              )
            )
          )
        }

      }

      "there is partial user data" must {
        "display the income sources item as completed and tax year item as incomplete" in {
          document(partialTaskListWithoutYear).mainContent.mustHaveTaskList(".govuk-task-list:nth-of-type(1)")(
            idPrefix = "client-details",
            items = Seq(
              TaskListItemValues(
                text = addYourIncomeSources,
                link = Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url),
                hint = Some(addYourIncomeSourcesHint),
                tagText = complete,
                tagColor = None
              ),
              TaskListItemValues(
                text = selectTaxYear,
                link = Some(controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url),
                hint = Some(selectYourTaxYearHintBoth),
                tagText = incomplete,
                tagColor = Some("blue")
              )
            )
          )
        }

        "display the income sources item as incomplete and tax year item as completed" in {
          document(partialTaskListWithoutIncome).mainContent.mustHaveTaskList(".govuk-task-list:nth-of-type(1)")(
            idPrefix = "client-details",
            items = Seq(
              TaskListItemValues(
                text = addYourIncomeSources,
                link = Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url),
                hint = Some(addYourIncomeSourcesHint),
                tagText = incomplete,
                tagColor = Some("blue")
              ),
              TaskListItemValues(
                text = selectTaxYear,
                link = Some(controllers.individual.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show(editMode=true).url),
                hint = Some(selectYourTaxYearHintBoth),
                tagText = complete,
                tagColor = None
              )
            )
          )
        }

        "display the review and sign up task list" in {
          document(partialTaskListWithoutIncome).mainContent.mustHaveTaskList(".govuk-task-list:nth-of-type(2)")(
            idPrefix = "sign-up-section",
            items = Seq(
              TaskListItemValues(
                text = ReviewAndSignUpText,
                link = None,
                hint = Some(ReviewAndSignUpHint),
                tagText = cannotStart,
                tagColor = None
              )
            )
          )
        }
      }

      "there is full user data" must {
        "display the income sources item and tax year item as completed" in {
          document(completedTaskListComplete).mainContent.mustHaveTaskList(".govuk-task-list:nth-of-type(1)")(
            idPrefix = "client-details",
            items = Seq(
              TaskListItemValues(
                text = addYourIncomeSources,
                link = Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url),
                hint = Some(addYourIncomeSourcesHint),
                tagText = complete,
                tagColor = None
              ),
              TaskListItemValues(
                text = selectTaxYear,
                link = Some(controllers.individual.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show(editMode=true).url),
                hint = Some(selectYourTaxYearHintBoth),
                tagText = complete,
                tagColor = None
              )
            )
          )
        }

        "display the review and sign up task list" in {
          document(completedTaskListComplete).mainContent.mustHaveTaskList(".govuk-task-list:nth-of-type(2)")(
            idPrefix = "sign-up-section",
            items = Seq(
              TaskListItemValues(
                text = ReviewAndSignUpText,
                link = Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url),
                hint = None,
                tagText = incomplete,
                tagColor = Some("blue")
              )
            )
          )
        }
      }
    }
  }

  "given a forced year in the task list model" must {
    val doc = document(forcedYearTaskList)

    "display select tax year as complete status with links, hint" in {

      val individualSelectTaxYear = doc.mainContent.selectHead("ul").selectNth("li", 2)
      val individualSelectTaxYearTag = doc.mainContent.selectHead("ul").selectNth("li", 2).selectHead("#client-details-2-status")

      individualSelectTaxYear.select("a").size() mustBe 0
      individualSelectTaxYear.selectHead("div").text mustBe next(getCurrentTaxEndYear, getCurrentTaxEndYear + 1) + " " + selectYourNextTaxYearHint
      individualSelectTaxYearTag.text mustBe complete

      }
    }
}
