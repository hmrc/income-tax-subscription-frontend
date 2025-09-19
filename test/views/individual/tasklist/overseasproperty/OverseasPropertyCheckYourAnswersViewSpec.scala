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

import models.DateModel
import models.common.OverseasPropertyModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.tasklist.overseasproperty.OverseasPropertyCheckYourAnswers

import java.time.format.DateTimeFormatter

class OverseasPropertyCheckYourAnswersViewSpec extends ViewSpec {

  "OverseasPropertyCheckYourAnswers" must {
    "have the correct template" in new TemplateViewTest(
      view = view(
        viewModel = completeProperty,
        postAction = testCall,
        isGlobalEdit = false,
        backUrl = testBackUrl
      ),
      title = OverseasPropertyCheckYourAnswers.title,
      isAgent = false,
      backLink = Some(testBackUrl)
    )

    "have a heading and caption" in {
      document().mainContent.mustHaveHeadingAndCaption(
        heading = OverseasPropertyCheckYourAnswers.heading,
        caption = OverseasPropertyCheckYourAnswers.caption,
        isSection = true
      )
    }

    "have a summary list" when {
      "all data is missing" when {
        "not in edit mode" in {
          document(viewModel = emptyOverseasProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            startDateBeforeLimitRow(None)
          ))
        }
        "in global edit mode" in {
          document(viewModel = emptyOverseasProperty, isGlobalEdit = true).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            startDateBeforeLimitRow(None, globalEditMode = true)
          ))
        }
      }
      "data is complete" when {
        "the start date before limit was answered with 'Yes'" in {
          document(viewModel = OverseasPropertyModel(startDateBeforeLimit = Some(true))).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            startDateBeforeLimitRow(Some(OverseasPropertyCheckYourAnswers.beforeStartDateLimit))
          ))
        }
        "the start date before limit was answered with 'No' and no start date was provided" in {
          document(viewModel = OverseasPropertyModel(startDateBeforeLimit = Some(false), startDate = None)).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            startDateBeforeLimitRow(None)
          ))
        }
        "the start date before limit was answered with 'No' and the stored start date is after the limit" in {
          document(viewModel = OverseasPropertyModel(startDateBeforeLimit = Some(false), startDate = Some(limitDate))).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            startDateBeforeLimitRow(Some(limitDate.toLocalDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))))
          ))
        }
        "the start date before limit was answered with 'No' but there is a stored start date before the limit" in {
          document(viewModel = OverseasPropertyModel(startDateBeforeLimit = Some(false), startDate = Some(olderThanLimitDate))).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            startDateBeforeLimitRow(Some(OverseasPropertyCheckYourAnswers.beforeStartDateLimit))
          ))
        }
      }

      "have a form" which {
        def form: Element = document().mainContent.getForm

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
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
  }

  private lazy val view = app.injector.instanceOf[OverseasPropertyCheckYourAnswers]

  object OverseasPropertyCheckYourAnswers {
    val title = "Check your answers - Foreign property"
    val heading = "Check your answers"
    val caption = "Foreign property"
    val startDateQuestion = "Start date"
    val confirmedAndContinue = "Confirm and continue"
    val saveAndComeBack = "Save and come back later"
    val change = "Change"
    val changeStartDate = "Change start date"
    val add = "Add"
    val addStartDate = "Add start date"
    val beforeStartDateLimit = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
  }

  private lazy val completeProperty = OverseasPropertyModel(
    startDateBeforeLimit = Some(false),
    startDate = Some(limitDate)
  )

  private lazy val emptyOverseasProperty = OverseasPropertyModel()

  private lazy val olderThanLimitDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit.minusDays(1))
  private lazy val limitDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

  private def page(viewModel: OverseasPropertyModel, isGlobalEdit: Boolean) = view(
    viewModel,
    postAction = testCall,
    backUrl = testBackUrl,
    isGlobalEdit = isGlobalEdit
  )

  private def document(viewModel: OverseasPropertyModel = completeProperty, isGlobalEdit: Boolean = false) = Jsoup.parse(page(viewModel, isGlobalEdit).body)

  private def simpleSummaryRow(key: String): (Option[String], String) => SummaryListRowValues = {
    case (value, href) =>
      SummaryListRowValues(
        key = key,
        value = value,
        actions = Seq(
          SummaryListActionValues(
            href = href,
            text = (if (value.isDefined) OverseasPropertyCheckYourAnswers.change else OverseasPropertyCheckYourAnswers.add) + " " + key,
            visuallyHidden = key
          )
        )
      )
  }

  private def startDateBeforeLimitRow(value: Option[String], globalEditMode: Boolean = false) = {
    simpleSummaryRow(OverseasPropertyCheckYourAnswers.startDateQuestion)(
      value,
      controllers.individual.tasklist.overseasproperty.routes.ForeignPropertyStartDateBeforeLimitController.show(editMode = true, isGlobalEdit = globalEditMode).url
    )
  }

}

