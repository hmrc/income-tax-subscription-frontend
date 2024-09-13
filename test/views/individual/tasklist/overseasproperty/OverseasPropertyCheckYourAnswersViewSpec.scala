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

package views.individual.tasklist.overseasproperty

import models.common.OverseasPropertyModel
import models.{Accruals, Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import utilities.ViewSpec
import views.html.individual.tasklist.overseasproperty.OverseasPropertyCheckYourAnswers

class OverseasPropertyCheckYourAnswersViewSpec extends ViewSpec {

  private val view = app.injector.instanceOf[OverseasPropertyCheckYourAnswers]

  object OverseasPropertyCheckYourAnswers {
    val title = "Check your answers - Foreign property"
    val heading = "Check your answers"
    val caption = "Foreign property"
    val startDateQuestion = "Start date"
    val accountMethodQuestion = "Accounting method"
    val confirmedAndContinue = "Confirm and continue"
    val saveAndComeBack = "Save and come back later"
    val change = "Change"
    val changeStartDate = "Change start date"
    val changeAccountingMethod = "Change accounting method"
    val add = "Add"
    val addStartDate = "Add start date"
    val addAccountingMethod = "Add accounting method"
    val cash = "Cash basis accounting"
    val accruals = "Traditional accounting"
  }

  private val completeCashProperty = OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("8", "11", "2021")),
  )

  private val completeAccrualsProperty = OverseasPropertyModel(
    accountingMethod = Some(Accruals),
    startDate = Some(DateModel("8", "11", "2021")),
  )

  private val incompleteProperty = OverseasPropertyModel()

  "OverseasPropertyCheckYourAnswers" must {
    "use the correct template" in new TemplateViewTest(
      view = view(
        completeCashProperty,
        controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.submit(),
        testBackUrl
      ),
      title = OverseasPropertyCheckYourAnswers.title,
      isAgent = false,
      backLink = Some(testBackUrl),
      hasSignOutLink = true
    )

    "have a heading and caption" in {
      document(completeCashProperty).mainContent.mustHaveHeadingAndCaption(
        heading = OverseasPropertyCheckYourAnswers.heading,
        caption = OverseasPropertyCheckYourAnswers.caption,
        isSection = true
      )
    }

    "have a summary of the answers" when {
      "start date and accounting method cash are defined" in {
        document(completeCashProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = OverseasPropertyCheckYourAnswers.startDateQuestion,
            value = Some("8 November 2021"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show(editMode = true).url,
                text = s"${OverseasPropertyCheckYourAnswers.change} ${OverseasPropertyCheckYourAnswers.startDateQuestion}",
                visuallyHidden = OverseasPropertyCheckYourAnswers.startDateQuestion
              )
            )
          ),
          SummaryListRowValues(
            key = OverseasPropertyCheckYourAnswers.accountMethodQuestion,
            value = Some("Cash basis accounting"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyAccountingMethodController.show(editMode = true).url,
                text = s"${OverseasPropertyCheckYourAnswers.change} ${OverseasPropertyCheckYourAnswers.accountMethodQuestion}",
                visuallyHidden = OverseasPropertyCheckYourAnswers.accountMethodQuestion
              )
            )
          )
        ))
      }
      "start date and accounting method accruals are defined" in {
        document(completeAccrualsProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = OverseasPropertyCheckYourAnswers.startDateQuestion,
            value = Some("8 November 2021"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show(editMode = true).url,
                text = s"${OverseasPropertyCheckYourAnswers.change} ${OverseasPropertyCheckYourAnswers.startDateQuestion}",
                visuallyHidden = OverseasPropertyCheckYourAnswers.startDateQuestion
              )
            )
          ),
          SummaryListRowValues(
            key = OverseasPropertyCheckYourAnswers.accountMethodQuestion,
            value = Some("Traditional accounting"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyAccountingMethodController.show(editMode = true).url,
                text = s"${OverseasPropertyCheckYourAnswers.change} ${OverseasPropertyCheckYourAnswers.accountMethodQuestion}",
                visuallyHidden = OverseasPropertyCheckYourAnswers.accountMethodQuestion
              )
            )
          )
        ))
      }
      "all answers are missing" in {
        document(incompleteProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = OverseasPropertyCheckYourAnswers.startDateQuestion,
            value = None,
            actions = Seq(
              SummaryListActionValues(
                href = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show(editMode = true).url,
                text = s"${OverseasPropertyCheckYourAnswers.add} ${OverseasPropertyCheckYourAnswers.startDateQuestion}",
                visuallyHidden = OverseasPropertyCheckYourAnswers.startDateQuestion
              )
            )
          ),
          SummaryListRowValues(
            key = OverseasPropertyCheckYourAnswers.accountMethodQuestion,
            value = None,
            actions = Seq(
              SummaryListActionValues(
                href = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyAccountingMethodController.show(editMode = true).url,
                text = s"${OverseasPropertyCheckYourAnswers.add} ${OverseasPropertyCheckYourAnswers.accountMethodQuestion}",
                visuallyHidden = OverseasPropertyCheckYourAnswers.accountMethodQuestion
              )
            )
          )
        ))
      }
    }

    "have a form" which {
      def form: Element = document(completeCashProperty).mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe "POST"
        form.attr("action") mustBe controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.submit().url
      }
      "has a confirm and continue button" in {
        form.selectNth(".govuk-button", 1).text mustBe OverseasPropertyCheckYourAnswers.confirmedAndContinue
      }
      "has a save and come back later button" in {
        val saveAndComeBackLater = form.selectNth(".govuk-button", 2)
        saveAndComeBackLater.text mustBe OverseasPropertyCheckYourAnswers.saveAndComeBack
        saveAndComeBackLater.attr("href") mustBe controllers.individual.tasklist.routes.ProgressSavedController.show(location = Some("overseas-property-check-your-answers")).url
      }
    }
  }

  private def page(viewModel: OverseasPropertyModel) = view(
    viewModel,
    postAction = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.submit(),
    backUrl = "test-back-url"
  )

  private def document(viewModel: OverseasPropertyModel) = Jsoup.parse(page(viewModel).body)
}
