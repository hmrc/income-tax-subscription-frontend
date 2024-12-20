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

import forms.individual.business.PropertyStartDateForm
import models.DateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.test.FakeRequest
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.tasklist.ukproperty.PropertyStartDate

import java.time.LocalDate

class UkPropertyStartDateViewSpec extends ViewSpec {

  object PropertyStartDateMessages {
    val captionVisible = "UK property"
    val heading: String = "When did you start your UK property business?"
    val para1 = "This is when you started letting any UK property."
    val para2 = "The date your business started trading can be today, in the past or up to 7 days in the future."
    val hint = "For example, 17 8 2014."
    val continue = "Continue"
    val saveAndContinue = "Save and continue"
    val backLink = "Back"
    val update = "Update"
    val maxDate = "The date your UK property business started trading must be the same as or before 11 April 2021"
    val minDate = "The date your property business started must be on or after 11 April 2021"
  }

  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("startDate", "error.property.day-month-year.empty")

  val propertyStartDate: PropertyStartDate = app.injector.instanceOf[PropertyStartDate]

  "UK property business start" must {

    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = propertyStartDate(
          PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString),
          testCall,
          testBackUrl
        ),
        title = PropertyStartDateMessages.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
      )

      "there is an error" in new TemplateViewTest(
        view = propertyStartDate(
          PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString).withError(testError),
          testCall,
          testBackUrl
        ),
        title = PropertyStartDateMessages.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a heading with caption" in {
      document().mainContent.mustHaveHeadingAndCaption(
        heading = PropertyStartDateMessages.heading,
        caption = PropertyStartDateMessages.captionVisible,
        isSection = true
      )
    }

    "have a paragraph One" in {
      document().selectNth("p", 3).text mustBe PropertyStartDateMessages.para1
    }

    "have a paragraph Two" in {
      document().selectNth("p", 4).text mustBe PropertyStartDateMessages.para2
    }

    "have a form" in {
      document().getForm.attr("method") mustBe testCall.method
      document().getForm.attr("action") mustBe testCall.url
    }

    "have a save & continue button when save & retrieve feature is enabled" in {
      document().mainContent.selectHead("div.govuk-button-group").selectHead("button").text mustBe PropertyStartDateMessages.saveAndContinue
    }

    "must display max date error on page" in {
      val dateValidationError = FormError("startDate", "error.property.day-month-year.max-date", List("11 April 2021"))
      val formWithError = PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString).withError(dateValidationError)
      document(propertyStartDateForm = formWithError).mustHaveDateInput(
        id = "startDate",
        legend = PropertyStartDateMessages.heading,
        exampleDate = PropertyStartDateMessages.hint,
        errorMessage = Some(PropertyStartDateMessages.maxDate),
        isHeading = false,
        isLegendHidden = true,
        dateInputsValues = Seq(
          DateInputFieldValues("Day", None),
          DateInputFieldValues("Month", None),
          DateInputFieldValues("Year", None)
        )
      )
    }

    "must display min date error on page" in {
      val dateValidationError = FormError("startDate", "error.property.day-month-year.min-date", List("11 April 2021"))
      val formWithError = PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString).withError(dateValidationError)
      document(propertyStartDateForm = formWithError).mustHaveDateInput(
        id = "startDate",
        legend = PropertyStartDateMessages.heading,
        exampleDate = PropertyStartDateMessages.hint,
        errorMessage = Some(PropertyStartDateMessages.minDate),
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

  private def page(propertyStartDateForm: Form[DateModel]): Html = {
    propertyStartDate(
      propertyStartDateForm,
      testCall,
      testBackUrl
    )(FakeRequest(), implicitly)
  }

  private def document(propertyStartDateForm: Form[DateModel] = PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)
                      ): Document = {
    Jsoup.parse(page(propertyStartDateForm).body)
  }
}
