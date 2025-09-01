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

package views.individual.tasklist.ukproperty

import config.featureswitch.FeatureSwitch.RemoveAccountingMethod
import config.featureswitch.FeatureSwitching
import models.common.{OverseasPropertyModel, PropertyModel}
import models.{Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.twirl.api.HtmlFormat
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.tasklist.ukproperty.PropertyCheckYourAnswers

import java.time.format.DateTimeFormatter

class PropertyCheckYourAnswersViewSpec extends ViewSpec with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(RemoveAccountingMethod)
  }

  "PropertyCheckYourAnswers" must {
    "have the correct template" in new TemplateViewTest(
      view = propertyCheckYourAnswersView(
        viewModel = completeProperty,
        postAction = testCall,
        isGlobalEdit = false,
        backUrl = testBackUrl
      ),
      title = PropertyCheckYourAnswers.title,
      isAgent = false,
      backLink = Some(testBackUrl)
    )

    "have a heading and caption" in {
      document().mainContent.mustHaveHeadingAndCaption(
        heading = PropertyCheckYourAnswers.heading,
        caption = PropertyCheckYourAnswers.caption,
        isSection = true
      )
    }

    "display a summary of answers" when {
      "remove accounting method feature switch disabled" when {
        "all data is missing" when {
          "not in edit mode" in {
            document(viewModel = emptyProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateBeforeLimitRow(None),
              accountingMethodRow(None)
            ))
          }
          "in global edit mode" in {
            document(viewModel = emptyProperty, isGlobalEdit = true).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateBeforeLimitRow(None, globalEditMode = true),
              accountingMethodRow(None, globalEditMode = true)
            ))
          }
        }
        "data is complete" when {
          "the start date before limit was answered with 'Yes'" in {
            document(viewModel = completeProperty.copy(startDateBeforeLimit = Some(true))).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateBeforeLimitRow(Some(PropertyCheckYourAnswers.beforeStartDateLimit)),
              accountingMethodRow(Some(PropertyCheckYourAnswers.cash))
            ))
          }
          "the start date before limit was answered with 'No' and no start date was provided" in {
            document(
              viewModel = completeProperty.copy(startDateBeforeLimit = Some(false), startDate = None)
            ).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateBeforeLimitRow(None),
              accountingMethodRow(Some(PropertyCheckYourAnswers.cash))
            ))
          }
          "the start date before limit was answered with 'No' and the stored start date is after the limit" in {
            document(
              viewModel = completeProperty.copy(startDateBeforeLimit = Some(false), startDate = Some(limitDate))
            ).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateBeforeLimitRow(Some(limitDate.toLocalDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy")))),
              accountingMethodRow(Some(PropertyCheckYourAnswers.cash))
            ))
          }
          "the start date before limit was answered with 'No' but there is a stored start date before the limit" in {
            document(
              viewModel = completeProperty.copy(startDateBeforeLimit = Some(false), startDate = Some(olderThanLimitDate))
            ).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateBeforeLimitRow(Some(PropertyCheckYourAnswers.beforeStartDateLimit)),
              accountingMethodRow(Some(PropertyCheckYourAnswers.cash))
            ))
          }
        }
      }

      "remove accounting method feature switch enabled" when {
        "all data is missing" when {
          "not in edit mode" in {
            enable(RemoveAccountingMethod)
            document(viewModel = emptyProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateBeforeLimitRow(None)
            ))
          }
          "in global edit mode" in {
            enable(RemoveAccountingMethod)
            document(viewModel = emptyProperty, isGlobalEdit = true).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateBeforeLimitRow(None, globalEditMode = true)
            ))
          }
        }
        "data is complete" when {
          "the start date before limit was answered with 'Yes'" in {
            enable(RemoveAccountingMethod)
            document(viewModel = PropertyModel(startDateBeforeLimit = Some(true)))
              .mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateBeforeLimitRow(Some(PropertyCheckYourAnswers.beforeStartDateLimit))
            ))
          }
          "the start date before limit was answered with 'No' and no start date was provided" in {
            enable(RemoveAccountingMethod)
            document(
              viewModel = PropertyModel(startDateBeforeLimit = Some(false), startDate = None)
            ).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateBeforeLimitRow(None)
            ))
          }
          "the start date before limit was answered with 'No' and the stored start date is after the limit" in {
            enable(RemoveAccountingMethod)
            document(
              viewModel = PropertyModel(startDateBeforeLimit = Some(false), startDate = Some(limitDate))
            ).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateBeforeLimitRow(Some(limitDate.toLocalDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))))
            ))
          }
          "the start date before limit was answered with 'No' but there is a stored start date before the limit" in {
            enable(RemoveAccountingMethod)
            document(
              viewModel = PropertyModel(startDateBeforeLimit = Some(false), startDate = Some(olderThanLimitDate))
            ).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateBeforeLimitRow(Some(PropertyCheckYourAnswers.beforeStartDateLimit))
            ))
          }
        }
      }
    }

    "have a form" which {
      def form: Element = document().mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a confirm and continue button" in {
        form.selectNth(".govuk-button", 1).text mustBe PropertyCheckYourAnswers.confirmedAndContinue
      }

      "has a save and come back later button" in {
        val saveAndComeBackLater = form.selectNth(".govuk-button", 2)
        saveAndComeBackLater.text mustBe PropertyCheckYourAnswers.saveAndComeBack
        saveAndComeBackLater.attr("href") mustBe controllers.individual.tasklist.routes.ProgressSavedController.show(location = Some("uk-property-check-your-answers")).url
      }
    }
  }

  private lazy val propertyCheckYourAnswersView = app.injector.instanceOf[PropertyCheckYourAnswers]
  private lazy val completeProperty = PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(limitDate)
  )
  private lazy val emptyProperty = PropertyModel()
  private lazy val olderThanLimitDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit.minusDays(1))
  private lazy val limitDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

  object PropertyCheckYourAnswers {
    val title = "Check your answers - UK property"
    val heading = "Check your answers"
    val caption = "UK property"
    val startDateQuestion = "Start date"
    val accountMethodQuestion = "Accounting method"
    val confirmedAndContinue = "Confirm and continue"
    val saveAndComeBack = "Save and come back later"
    val change = "Change"
    val add = "Add"
    val beforeStartDateLimit: String = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
    val cash: String = "Cash basis accounting"
  }

  private def page(viewModel: PropertyModel, isGlobalEdit: Boolean): HtmlFormat.Appendable = {
    propertyCheckYourAnswersView(
      viewModel,
      postAction = testCall,
      backUrl = testBackUrl,
      isGlobalEdit = isGlobalEdit
    )
  }

  private def document(viewModel: PropertyModel = completeProperty, isGlobalEdit: Boolean = false) = {
    Jsoup.parse(page(viewModel, isGlobalEdit).body)
  }

  private def simpleSummaryRow(key: String): (Option[String], String) => SummaryListRowValues = {
    case (value, href) =>
      SummaryListRowValues(
        key = key,
        value = value,
        actions = Seq(
          SummaryListActionValues(
            href = href,
            text = (if (value.isDefined) PropertyCheckYourAnswers.change else PropertyCheckYourAnswers.add) + " " + key,
            visuallyHidden = key
          )
        )
      )
  }

  private def startDateBeforeLimitRow(value: Option[String], globalEditMode: Boolean = false) = {
    simpleSummaryRow(PropertyCheckYourAnswers.startDateQuestion)(
      value,
      controllers.individual.tasklist.ukproperty.routes.PropertyStartDateBeforeLimitController.show(editMode = true, isGlobalEdit = globalEditMode).url
    )
  }

  private def accountingMethodRow(value: Option[String], globalEditMode: Boolean = false) = {
    simpleSummaryRow(PropertyCheckYourAnswers.accountMethodQuestion)(
      value,
      controllers.individual.tasklist.ukproperty.routes.PropertyAccountingMethodController.show(editMode = true, isGlobalEdit = globalEditMode).url
    )
  }

}

