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

import forms.individual.business.ForeignPropertyStartDateForm
import models.DateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.{Form, FormError}
import play.api.test.FakeRequest
import play.twirl.api.Html
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.tasklist.overseasproperty.ForeignPropertyStartDate

class ForeignPropertyStartDateViewSpec extends ViewSpec {

  "PropertyStartDate" must {
    "use the correct template" when {
      "there is no error" in new TemplateViewTest(
        view = page(
          startDateForm = baseForm
        ),
        title = OverseasPropertyStartDateMessages.title,
        isAgent = false,
        backLink = Some(testBackUrl)
      )
      "there is an error" in new TemplateViewTest(
        view = page(
          startDateForm = baseForm.withError(testError)
        ),
        title = OverseasPropertyStartDateMessages.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a heading with caption" in {
      document().mainContent.mustHaveHeadingAndCaption(
        heading = OverseasPropertyStartDateMessages.heading,
        caption = OverseasPropertyStartDateMessages.captionVisible,
        isSection = true
      )
    }

    "have a paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe OverseasPropertyStartDateMessages.paraOne
    }

    "have a form" which {
      def form(maybeError: Option[FormError] = None): Element = document(
        maybeError.fold(baseForm)(baseForm.withError)
      ).mainContent.getForm

      "has the correct attributes" in {
        form().attr("method") mustBe testCall.method
        form().attr("action") mustBe testCall.url
      }

      "has a date input" when {
        "there is no error" in {
          form().mustHaveDateInput(
            id = "startDate",
            legend = OverseasPropertyStartDateMessages.heading,
            exampleDate = OverseasPropertyStartDateMessages.hint,
            isHeading = false,
            isLegendHidden = true,
            dateInputsValues = Seq(
              DateInputFieldValues("Day", None),
              DateInputFieldValues("Month", None),
              DateInputFieldValues("Year", None)
            )
          )
        }
        "there is an error" which {
          "is for a min start date error" in {
            val dateValidationError = FormError("startDate", "error.individual.foreign-property.day-month-year.min-date", List("11 April 2021"))
            form(maybeError = Some(dateValidationError)).mustHaveDateInput(
              id = "startDate",
              legend = OverseasPropertyStartDateMessages.heading,
              exampleDate = OverseasPropertyStartDateMessages.hint,
              isHeading = false,
              isLegendHidden = true,
              errorMessage = Some(OverseasPropertyStartDateMessages.minDate),
              dateInputsValues = Seq(
                DateInputFieldValues("Day", None),
                DateInputFieldValues("Month", None),
                DateInputFieldValues("Year", None)
              )
            )
          }
          "is for a max start date error" in {
            val dateValidationError = FormError("startDate", "error.individual.foreign-property.day-month-year.max-date", List("18 April 2021"))
            form(maybeError = Some(dateValidationError)).mustHaveDateInput(
              id = "startDate",
              legend = OverseasPropertyStartDateMessages.heading,
              exampleDate = OverseasPropertyStartDateMessages.hint,
              isHeading = false,
              isLegendHidden = true,
              errorMessage = Some(OverseasPropertyStartDateMessages.maxDate),
              dateInputsValues = Seq(
                DateInputFieldValues("Day", None),
                DateInputFieldValues("Month", None),
                DateInputFieldValues("Year", None)
              )
            )
          }
        }
      }

      "has a button group" which {
        def buttonGroup: Element = form().selectHead(".govuk-button-group")

        "has a save and continue button" in {
          buttonGroup.selectHead(".govuk-button").text mustBe OverseasPropertyStartDateMessages.saveAndContinue
        }
        "has a save and come back later button" in {
          val saveAndComeBackLater: Element = buttonGroup.selectHead(".govuk-button--secondary")

          saveAndComeBackLater.text mustBe OverseasPropertyStartDateMessages.saveAndComeBackLater
          saveAndComeBackLater.attr("href") mustBe
            controllers.individual.tasklist.routes.ProgressSavedController.show(location = Some("foreign-property-start-date")).url
        }
      }
    }
  }

  lazy val testError: FormError = FormError("startDate", "error.individual.foreign-property.empty")
  lazy val foreignPropertyStartDate: ForeignPropertyStartDate = app.injector.instanceOf[ForeignPropertyStartDate]

  private def page(startDateForm: Form[DateModel]): Html = {
    foreignPropertyStartDate(
      startDateForm,
      testCall,
      testBackUrl
    )(FakeRequest(), implicitly)
  }

  private lazy val baseForm: Form[DateModel] = ForeignPropertyStartDateForm.startDateForm(_.toString)

  private def document(propertyStartDateForm: Form[DateModel] = baseForm): Document = {
    Jsoup.parse(page(propertyStartDateForm).body)
  }

  object OverseasPropertyStartDateMessages {
    val title = "Start date for income from foreign property"
    val heading: String = "Start date"
    val captionVisible = "Your foreign property"
    val paraOne = "We need to know the exact start date."
    val hint = s"For example, 27 9 ${AccountingPeriodUtil.getStartDateLimit.getYear}."
    val saveAndContinue = "Save and continue"
    val saveAndComeBackLater = "Save and come back later"
    val maxDate = "The date must be before 18 April 2021"
    val minDate = "The date must be on or after 11 April 2021"
  }

}
