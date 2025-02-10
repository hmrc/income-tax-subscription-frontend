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

package views.agent.tasklist.overseasproperty

import models.common.OverseasPropertyModel
import models.{Accruals, Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.tasklist.overseasproperty.OverseasPropertyCheckYourAnswers

import java.time.format.DateTimeFormatter

class OverseasPropertyCheckYourAnswersViewSpec extends ViewSpec {


  private val overseasPropertyCheckYourAnswersView = app.injector.instanceOf[OverseasPropertyCheckYourAnswers]

  object OverseasPropertyCheckYourAnswers {
    val title = "Check your answers - Foreign property"
    val caption = "FirstName LastName | ZZ 11 11 11 Z"
    val heading = "Check your answers"
    val startDate = "Start date"
    val addStartDate = "Add start date"
    val changeStartDate = "Change start date"
    val accountingMethod = "Accounting method"
    val confirmedAndContinue = "Confirm and continue"
    val saveAndComeBack = "Save and come back later"
    val continue = "Continue"
    val change = "Change"
    val add = "Add"
    val cash = "Cash basis accounting"
    val accruals = "Traditional accounting"
    val beforeStartDateLimit = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
  }

  private val completeForeignProperty = OverseasPropertyModel(
    startDate = Some(DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)),
    accountingMethod = Some(Cash)
  )

  private val incompleteForeignProperty = OverseasPropertyModel()
  private lazy val olderThanLimitDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit.minusDays(1))
  private lazy val limitDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

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

  private def startDateRow(value: Option[String], globalEditMode: Boolean = false) = {
    simpleSummaryRow(OverseasPropertyCheckYourAnswers.startDate)(
      value,
      controllers.agent.tasklist.overseasproperty.routes.IncomeSourcesOverseasPropertyController.show(editMode = true, isGlobalEdit = globalEditMode).url
    )
  }

  private def accountingMethodRow(value: Option[String], globalEditMode: Boolean = false) = {
    simpleSummaryRow(OverseasPropertyCheckYourAnswers.accountingMethod)(
      value,
      controllers.agent.tasklist.overseasproperty.routes.IncomeSourcesOverseasPropertyController.show(editMode = true, isGlobalEdit = globalEditMode).url
    )
  }

  private def page(viewModel: OverseasPropertyModel, isGlobalEdit: Boolean = false): HtmlFormat.Appendable = overseasPropertyCheckYourAnswersView(
    viewModel = viewModel,
    postAction = controllers.agent.tasklist.overseasproperty.routes.IncomeSourcesOverseasPropertyController.submit(isGlobalEdit = isGlobalEdit),
    isGlobalEdit = isGlobalEdit,
    backUrl = "test-back-url",
    clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")
  )

  private def document(viewModel: OverseasPropertyModel = completeForeignProperty, isGlobalEdit: Boolean = false): Document = {
    Jsoup.parse(page(viewModel, isGlobalEdit).body)
  }

  "OverseasPropertyCheckYourAnswers" must {
    "have the correct template" in new TemplateViewTest(
      view = overseasPropertyCheckYourAnswersView(
        viewModel = completeForeignProperty,
        postAction = testCall,
        isGlobalEdit = false,
        backUrl = testBackUrl,
        clientDetails = ClientDetails("", "")
      ),
      title = OverseasPropertyCheckYourAnswers.title,
      isAgent = true,
      backLink = Some(testBackUrl)
    )

    "have a heading and caption" in {
      document().mainContent.mustHaveHeadingAndCaption(
        heading = OverseasPropertyCheckYourAnswers.heading,
        caption = OverseasPropertyCheckYourAnswers.caption,
        isSection = false
      )
    }

    "display a summary of answers" when {
      "not in edit mode" when {
        "all data is complete" in {
          document(completeForeignProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            startDateRow(value = Some(limitDate.toLocalDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy")))),
            accountingMethodRow(value = Some(OverseasPropertyCheckYourAnswers.cash))
          ))
        }
        "all data is missing" in {
          document(incompleteForeignProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            startDateRow(value = None),
            accountingMethodRow(value = None)
          ))
        }
        "start date before limit field is present" which {
          "is true" in {
            document(completeForeignProperty.copy(startDateBeforeLimit = Some(true))).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateRow(value = Some(OverseasPropertyCheckYourAnswers.beforeStartDateLimit)),
              accountingMethodRow(value = Some(OverseasPropertyCheckYourAnswers.cash))
            ))
          }
          "is false" when {
            "the stored start date is not older than the limit" in {
              document(completeForeignProperty.copy(startDateBeforeLimit = Some(false))).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
                startDateRow(value = Some(limitDate.toLocalDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy")))),
                accountingMethodRow(value = Some(OverseasPropertyCheckYourAnswers.cash))
              ))
            }
            "the stored start date is older than the limit" in {
              document(completeForeignProperty.copy(
                startDateBeforeLimit = Some(false),
                startDate = Some(olderThanLimitDate)
              )).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
                startDateRow(value = Some(OverseasPropertyCheckYourAnswers.beforeStartDateLimit)),
                accountingMethodRow(value = Some(OverseasPropertyCheckYourAnswers.cash))
              ))
            }
          }
        }
        "in global edit mode" in {
          document(completeForeignProperty, isGlobalEdit = true).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            startDateRow(value = Some(limitDate.toLocalDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))), globalEditMode = true),
            accountingMethodRow(value = Some(OverseasPropertyCheckYourAnswers.cash), globalEditMode = true)
          ))
        }
      }
    }

    "have a form" which {
      def form: Element = document().mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe "POST"
        form.attr("action") mustBe controllers.agent.tasklist.overseasproperty.routes.IncomeSourcesOverseasPropertyController.submit().url
      }

      "has a confirm and continue button" in {
        form.selectNth(".govuk-button", 1).text mustBe OverseasPropertyCheckYourAnswers.confirmedAndContinue
      }

      "has a save and come back later button" in {
        val saveAndComeBackLater = form.selectNth(".govuk-button", 2)
        saveAndComeBackLater.text mustBe OverseasPropertyCheckYourAnswers.saveAndComeBack
        saveAndComeBackLater.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(location = Some("overseas-property-check-your-answers")).url
      }
    }
  }
}
