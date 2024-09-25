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

import messagelookup.individual.MessageLookup.TaskList._
import messagelookup.individual.MessageLookup.{TaskList => messages}
import models._
import models.common.{AccountingYearModel, TaskListModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import services.AccountingPeriodService
import utilities.ViewSpec
import utilities.individual.TestConstants.testUtr
import views.html.individual.tasklist.TaskList

class TaskListViewSpec extends ViewSpec {

  val taskListView: TaskList = app.injector.instanceOf[TaskList]

  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  def customTaskListModel(taxYearSelection: Option[AccountingYearModel] = None,
                          incomeSourcesConfirmed: Option[Boolean] = None): TaskListModel = {
    TaskListModel(
      taxYearSelection,
      incomeSourcesConfirmed
    )
  }

  private val forcedYearTaskList = customTaskListModel(
    taxYearSelection = Some(AccountingYearModel(Next, editable = false))
  )

  private val partialTaskListWithoutIncome = customTaskListModel(
    taxYearSelection = Some(AccountingYearModel(Current, confirmed = true)),
    incomeSourcesConfirmed = None
  )

  private val partialTaskListWithoutYear = customTaskListModel(
    taxYearSelection = None,
    incomeSourcesConfirmed = Some(true)
  )

  private val completedTaskListComplete = TaskListModel(
    taxYearSelection = Some(AccountingYearModel(Next, confirmed = true)),
    incomeSourcesConfirmed = Some(true)
  )

  def page(taskList: TaskListModel = customTaskListModel(), maybeIndividualUserFullName: Option[String], utrNumber: String): Html = taskListView(
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

    "display a section for the users information" which {
      "contains a full name" when {
        "a full name is available" in {
          document().mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            SummaryListRowValues(
              key = UserInformation.name,
              value = Some("individualUserFullName"),
              actions = Seq.empty
            ),
            SummaryListRowValues(
              key = UserInformation.nino,
              value = Some("individualUserNino"),
              actions = Seq.empty
            ),
            SummaryListRowValues(
              key = UserInformation.utr,
              value = Some(testUtr),
              actions = Seq.empty
            )
          ))
        }
      }
      "contains no name" when {
        "no name is available" in {
          document(maybeIndividualUserFullName = None).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            SummaryListRowValues(
              key = UserInformation.nino,
              value = Some("individualUserNino"),
              actions = Seq.empty
            ),
            SummaryListRowValues(
              key = UserInformation.utr,
              value = Some(testUtr),
              actions = Seq.empty
            )
          ))
        }
      }

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
                link = Some(controllers.individual.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show(editMode = true).url),
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
                link = Some(controllers.individual.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show(editMode = true).url),
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
                hint = Some(ReviewAndSignUpHint),
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
    "display the first task list part without a tax year link" in {
      document(forcedYearTaskList).mainContent.mustHaveTaskList(".govuk-task-list:nth-of-type(1)")(
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
            link = None,
            hint = Some(selectYourNextTaxYearHint),
            tagText = complete,
            tagColor = None
          )
        )
      )
    }
  }
}
