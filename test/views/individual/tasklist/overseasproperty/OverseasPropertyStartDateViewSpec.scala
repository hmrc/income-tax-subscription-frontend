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

import forms.individual.business.OverseasPropertyStartDateForm
import models.DateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.tasklist.overseasproperty.OverseasPropertyStartDate

import java.time.LocalDate

class OverseasPropertyStartDateViewSpec extends ViewSpec {

  object OverseasPropertyStartDateMessages {
    val title = "When did you start your foreign property business?"
    val heading: String = title
    val captionVisible = "Foreign property"
    val hintText = "This is when you started letting any foreign property."
    val para1 = "The date your business started trading can be today, in the past or up to 7 days in the future."
    val hint = "For example, 17 8 2014."
    val continue = "Continue"
    val saveAndContinue = "Save and continue"
    val backLink = "Back"
    val update = "Update"
    val maxDate = "The date your foreign property business started trading must be the same as or before 11 April 2021"
    val minDate = "The date your property business started must be on or after 11 April 2021"
  }

  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("startDate", "agent.error.overseas.property.day-month-year.empty")

  private val maxDateValidationError = FormError("startDate", "error.overseas.property.day-month-year.max-date", List("11 April 2021"))

  private val minDateValidationError = FormError("startDate", "error.overseas.property.day-month-year.min-date", List("11 April 2021"))

  val overseasPropertyStartDate: OverseasPropertyStartDate = app.injector.instanceOf[OverseasPropertyStartDate]

  class Setup(isEditMode: Boolean = false,
              form: Form[DateModel] = OverseasPropertyStartDateForm.overseasPropertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)) {

    val page: Html = overseasPropertyStartDate(
      form,
      testCall,
      isEditMode,
      testBackUrl
    )

    val document: Document = Jsoup.parse(page.body)
  }

  "overseas property start date page" must {
    "have the correct template" when {
      "there is no error" in new TemplateViewTest(
        view = overseasPropertyStartDate(
          overseasPropertyStartDateForm = OverseasPropertyStartDateForm.overseasPropertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString),
          postAction = testCall,
          isEditMode = false,
          backUrl = testBackUrl
        ),
        title = OverseasPropertyStartDateMessages.title,
        backLink = Some(testBackUrl),
      )
      "there is an error" in new TemplateViewTest(
        view = overseasPropertyStartDate(
          overseasPropertyStartDateForm = OverseasPropertyStartDateForm.overseasPropertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString).withError(testError),
          postAction = testCall,
          isEditMode = false,
          backUrl = testBackUrl
        ),
        title = OverseasPropertyStartDateMessages.title,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a heading and a caption" in new Setup {
      document.mainContent.mustHaveHeadingAndCaption(
        heading = OverseasPropertyStartDateMessages.heading,
        caption = OverseasPropertyStartDateMessages.captionVisible,
        isSection = true
      )
    }

    "have a hint text" in new Setup {
      document.selectNth("p", 3).text mustBe OverseasPropertyStartDateMessages.hintText
    }

    "have a paragraph" in new Setup {
      document.selectNth("p", 4).text mustBe OverseasPropertyStartDateMessages.para1
    }

    "have a form" in new Setup {
      document.getForm.attr("method") mustBe testCall.method
      document.getForm.attr("action") mustBe testCall.url
    }

    "have a date input" when {
      "there is no error" in new Setup {
        document.mustHaveDateInput(
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
      "there is max date error" in new Setup(
        form = OverseasPropertyStartDateForm.overseasPropertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString).withError(maxDateValidationError)
      ) {
        document.mustHaveDateInput(
          id = "startDate",
          legend = OverseasPropertyStartDateMessages.heading,
          exampleDate = OverseasPropertyStartDateMessages.hint,
          errorMessage = Some(OverseasPropertyStartDateMessages.maxDate),
          isHeading = false,
          isLegendHidden = true,
          dateInputsValues = Seq(
            DateInputFieldValues("Day", None),
            DateInputFieldValues("Month", None),
            DateInputFieldValues("Year", None)
          )
        )
      }
      "there is min date error" in new Setup(
        form = OverseasPropertyStartDateForm.overseasPropertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString).withError(minDateValidationError)
      ) {
        document.mustHaveDateInput(
          id = "startDate",
          legend = OverseasPropertyStartDateMessages.heading,
          exampleDate = OverseasPropertyStartDateMessages.hint,
          errorMessage = Some(OverseasPropertyStartDateMessages.minDate),
          isHeading = false,
          isLegendHidden = true,
          dateInputsValues = Seq(
            DateInputFieldValues("Day", None),
            DateInputFieldValues("Month", None),
            DateInputFieldValues("Year", None)
          )
        )
      }
    }

    "have a Save and Continue button when not in edit mode" in new Setup() {
      document.mainContent.selectHead("div.govuk-button-group").selectHead("button").text mustBe OverseasPropertyStartDateMessages.saveAndContinue
    }
  }

}
