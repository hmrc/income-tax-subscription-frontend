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

import models.common.{AccountingYearModel, TaskListModel}
import models.{Current, Next}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import services.AccountingPeriodService
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.tasklist.TaskList

class TaskListViewSpec extends ViewSpec {

  val taskListView: TaskList = app.injector.instanceOf[TaskList]
  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  val clientName: String = "FirstName LastName"
  val clientNino: String = "ZZ111111Z"
  val clientUtr: String = "1234567890"

  val accountingYearModel: AccountingYearModel = AccountingYearModel(Current, confirmed = true)

  val completeTaskListModel: TaskListModel = TaskListModel(
    taxYearSelection = Some(accountingYearModel),
    incomeSourcesConfirmed = Some(true)
  )

  val emptyTaskListModel: TaskListModel = TaskListModel(
    taxYearSelection = None,
    incomeSourcesConfirmed = None
  )

  def page(taskListModel: TaskListModel): HtmlFormat.Appendable = taskListView(
    postAction = testCall,
    viewModel = taskListModel,
    clientName = clientName,
    clientNino = clientNino,
    clientUtr = clientUtr
  )

  def document(taskListModel: TaskListModel = completeTaskListModel): Document = Jsoup.parse(page(taskListModel).body)

  "TaskList" must {
    "use the correct template" in new TemplateViewTest(
      view = page(completeTaskListModel),
      title = Messages.heading,
      isAgent = true,
      backLink = None,
      hasSignOutLink = true
    )

    "have a page heading" in {
      document().mainContent.getH1Element.text mustBe Messages.heading
    }

    "have a subheading for the client's information" in {
      document().mainContent.selectNth("h2", 1).text mustBe Messages.ClientDetails.heading
    }

    "have a summary list of the client's details" in {
      document().mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
        SummaryListRowValues(
          key = Messages.ClientDetails.name,
          value = Some(clientName),
          actions = Seq.empty
        ),
        SummaryListRowValues(
          key = Messages.ClientDetails.nino,
          value = Some(clientNino),
          actions = Seq.empty
        ),
        SummaryListRowValues(
          key = Messages.ClientDetails.utr,
          value = Some(clientUtr),
          actions = Seq.empty
        )
      ))
    }

    "have a subheading for what we need to sign up your client" in {
      document().mainContent.selectNth("h2", 2).text mustBe Messages.WhatWeNeed.heading
    }

    "have a task list of the income source and tax year sections" when {
      "tax year and income sources are complete" in {
        document().mainContent.mustHaveTaskList(".govuk-task-list")(
          idPrefix = "client-details",
          items = Seq(
            TaskListItemValues(
              text = Messages.ClientDetails.IncomeSources.link,
              link = Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url),
              hint = Some(Messages.ClientDetails.IncomeSources.hint),
              tagText = Messages.Tags.completed,
              tagColor = None
            ),
            TaskListItemValues(
              text = Messages.ClientDetails.TaxYear.link,
              link = Some(controllers.agent.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show(editMode = true).url),
              hint = Some(Messages.ClientDetails.TaxYear.hint),
              tagText = Messages.Tags.completed,
              tagColor = None
            )
          )
        )
      }
      "tax year is locked to current year and income sources are complete" in {
        document(
          completeTaskListModel.copy(taxYearSelection = Some(accountingYearModel.copy(editable = false)))
        ).mainContent.mustHaveTaskList(".govuk-task-list")(
          idPrefix = "client-details",
          items = Seq(
            TaskListItemValues(
              text = Messages.ClientDetails.IncomeSources.link,
              link = Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url),
              hint = Some(Messages.ClientDetails.IncomeSources.hint),
              tagText = Messages.Tags.completed,
              tagColor = None
            ),
            TaskListItemValues(
              text = Messages.ClientDetails.TaxYear.currentTaxYear,
              link = None,
              hint = None,
              tagText = Messages.Tags.completed,
              tagColor = None
            )
          )
        )
      }
      "tax year is locked to next year and income sources are complete" in {
        document(
          completeTaskListModel.copy(taxYearSelection = Some(accountingYearModel.copy(accountingYear = Next, editable = false)))
        ).mainContent.mustHaveTaskList(".govuk-task-list")(
          idPrefix = "client-details",
          items = Seq(
            TaskListItemValues(
              text = Messages.ClientDetails.IncomeSources.link,
              link = Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url),
              hint = Some(Messages.ClientDetails.IncomeSources.hint),
              tagText = Messages.Tags.completed,
              tagColor = None
            ),
            TaskListItemValues(
              text = Messages.ClientDetails.TaxYear.nextTaxYear,
              link = None,
              hint = Some(Messages.ClientDetails.TaxYear.hintNextYearOnly),
              tagText = Messages.Tags.completed,
              tagColor = None
            )
          )
        )
      }
      "tax year is not confirmed and income sources are not confirmed" in {
        document(
          completeTaskListModel.copy(taxYearSelection = Some(accountingYearModel.copy(confirmed = false)), incomeSourcesConfirmed = Some(false))
        ).mainContent.mustHaveTaskList(".govuk-task-list")(
          idPrefix = "client-details",
          items = Seq(
            TaskListItemValues(
              text = Messages.ClientDetails.IncomeSources.link,
              link = Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url),
              hint = Some(Messages.ClientDetails.IncomeSources.hint),
              tagText = Messages.Tags.incomplete,
              tagColor = Some("blue")
            ),
            TaskListItemValues(
              text = Messages.ClientDetails.TaxYear.link,
              link = Some(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url),
              hint = Some(Messages.ClientDetails.TaxYear.hint),
              tagText = Messages.Tags.incomplete,
              tagColor = Some("blue")
            )
          )
        )
      }
      "tax year is empty and income sources are empty" in {
        document(emptyTaskListModel).mainContent.mustHaveTaskList(".govuk-task-list")(
          idPrefix = "client-details",
          items = Seq(
            TaskListItemValues(
              text = Messages.ClientDetails.IncomeSources.link,
              link = Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url),
              hint = Some(Messages.ClientDetails.IncomeSources.hint),
              tagText = Messages.Tags.incomplete,
              tagColor = Some("blue")
            ),
            TaskListItemValues(
              text = Messages.ClientDetails.TaxYear.link,
              link = Some(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url),
              hint = Some(Messages.ClientDetails.TaxYear.hint),
              tagText = Messages.Tags.incomplete,
              tagColor = Some("blue")
            )
          )
        )
      }
    }

    "have a subheading for review and sign up" in {
      document().mainContent.selectNth("h2", 3).text mustBe Messages.ReviewAndSignUp.heading
    }

    "have a task list of the sign up task" when {
      "sign up is not available as income sources is not complete" in {
        document(
          completeTaskListModel.copy(incomeSourcesConfirmed = None)
        ).mainContent.mustHaveTaskList(".govuk-task-list:nth-of-type(2)")(
          idPrefix = "sign-up-section",
          items = Seq(
            TaskListItemValues(
              text = Messages.ReviewAndSignUp.link,
              link = None,
              hint = Some(Messages.ReviewAndSignUp.hint),
              tagText = Messages.Tags.cannotStartYet,
              tagColor = None
            )
          )
        )
      }
      "sign up is not available as tax year is not complete" in {
        document(
          completeTaskListModel.copy(taxYearSelection = None)
        ).mainContent.mustHaveTaskList(".govuk-task-list:nth-of-type(2)")(
          idPrefix = "sign-up-section",
          items = Seq(
            TaskListItemValues(
              text = Messages.ReviewAndSignUp.link,
              link = None,
              hint = Some(Messages.ReviewAndSignUp.hint),
              tagText = Messages.Tags.cannotStartYet,
              tagColor = None
            )
          )
        )
      }
      "sign up is available" in {
        document(completeTaskListModel).mainContent.mustHaveTaskList(".govuk-task-list:nth-of-type(2)")(
          idPrefix = "sign-up-section",
          items = Seq(
            TaskListItemValues(
              text = Messages.ReviewAndSignUp.link,
              link = Some(controllers.agent.routes.GlobalCheckYourAnswersController.show.url),
              hint = None,
              tagText = Messages.Tags.incomplete,
              tagColor = Some("blue")
            )
          )
        )
      }
    }

    "have a save and come back later button" in {
      val saveAndComeBackLater = document().mainContent.selectHead(".govuk-button")

      saveAndComeBackLater.text mustBe Messages.saveAndComeBackLater
      saveAndComeBackLater.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(location = Some("task-list")).url
    }

  }

  object Messages {
    val heading: String = "Sign up your client for Making Tax Digital for Income Tax"

    object Tags {
      val incomplete: String = "Incomplete"
      val completed: String = "Completed"
      val cannotStartYet: String = "Cannot start yet"
    }

    object ClientDetails {
      val heading: String = "Your client"
      val name: String = "Name"
      val nino: String = "National Insurance number"
      val utr: String = "Unique Taxpayer Reference (UTR)"

      object IncomeSources {
        val link: String = "Your client’s income sources"
        val hint: String = "Include all of your client’s sole trader or property income sources"
      }

      object TaxYear {
        val link: String = "Select tax year"
        private val currentTaxYearEndYear: Int = AccountingPeriodUtil.getCurrentTaxEndYear
        val currentTaxYear = s"Current tax year (6 April ${currentTaxYearEndYear - 1} to 5 April $currentTaxYearEndYear)"
        val nextTaxYear = s"Next tax year (6 April $currentTaxYearEndYear to 5 April ${currentTaxYearEndYear + 1})"
        val hint: String = "You can sign up your client during the current tax year or from next tax year"
        val hintNextYearOnly: String = "You can only sign up your client from next tax year"
      }
    }

    object WhatWeNeed {
      val heading: String = "What we need to sign up your client"
    }

    object ReviewAndSignUp {
      val heading: String = "Review and sign up"
      val link: String = "Sign up your client"
      val hint: String = "Complete your client’s income details and starting tax year before you sign up your client"
    }

    val saveAndComeBackLater: String = "Save and come back later"
  }

}
