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

package views.agent.tasklist.ukproperty

import forms.agent.PropertyStartDateForm
import models.DateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.api.data.{Form, FormError}
import play.api.test.FakeRequest
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.tasklist.ukproperty.PropertyStartDate

import java.time.LocalDate


class UkPropertyStartDateViewSpec extends ViewSpec {

  val testError: FormError = FormError("startDate", "agent.error.property.day-month-year.empty")
  val propertyStartDateView: PropertyStartDate = app.injector.instanceOf[PropertyStartDate]
  val defaultForm: Form[DateModel] = PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)

  private def document(propertyStartDateForm: Form[DateModel] = PropertyStartDateForm.propertyStartDateForm(
    minStartDate = LocalDate.now(),
    maxStartDate = LocalDate.now(),
    f = d => d.toString
  )) = Jsoup.parse(page(propertyStartDateForm).body)

  private def page(propertyStartDateForm: Form[DateModel]) =
    propertyStartDateView(
      propertyStartDateForm,
      testCall,
      testBackUrl,
      ClientDetails("FirstName LastName", "ZZ111111Z")
    )(FakeRequest(), implicitly)

  object PropertyStartDateMessages {
    val title = "Start date for income from UK property"
    val heading: String = title
    val caption: String = "FirstName LastName | ZZ 11 11 11 Z"
    val para1 = "We need to know the exact date."
    val hint = s"For example, 27 9 ${AccountingPeriodUtil.getStartDateLimit.getYear}"
    val continue = "Continue"
    val saveAndContinue = "Save and continue"
    val saveAndComeBackLater = "Save and come back later"
    val backLink = "Back"
    val update = "Update"
    val maxDate = "The date cannot be more than 7 days in the future"
    val minDate = "The date your client’s property business started trading must be on or after 11 April 2021"
  }

  "agent UK property business start" must {
    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = propertyStartDateView(
          propertyStartDateForm = defaultForm,
          postAction = testCall,
          backUrl = testBackUrl,
          clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = PropertyStartDateMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
      )

      "there is an error" in new TemplateViewTest(
        view = propertyStartDateView(
          propertyStartDateForm = defaultForm.withError(testError),
          postAction = testCall,
          backUrl = testBackUrl,
          clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = PropertyStartDateMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a heading and caption" in {
      document().mainContent.mustHaveHeadingAndCaption(
        heading = PropertyStartDateMessages.heading,
        caption = PropertyStartDateMessages.caption,
        isSection = false
      )
    }

    "have a paragraph one" in {
      document().mainContent.selectNth("p", 1).text() mustBe PropertyStartDateMessages.para1
    }

    "have a form" which {
      def form: Element = document().mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a date input" in {
        form.mustHaveDateInput(
          id = PropertyStartDateForm.startDate,
          legend = PropertyStartDateMessages.heading,
          exampleDate = PropertyStartDateMessages.hint,
          isHeading = false,
          isLegendHidden = true,
          dateInputsValues = Seq(
            DateInputFieldValues("Day", None),
            DateInputFieldValues("Month", None),
            DateInputFieldValues("Year", None)
          )
        )
      }

      "has a save & continue button" in {
        form.getGovukSubmitButton.text mustBe PropertyStartDateMessages.saveAndContinue
      }

      "has a save & come back later button" in {
        val saveAndComeBackLater: Element = form.selectHead(".govuk-button--secondary")

        saveAndComeBackLater.text mustBe PropertyStartDateMessages.saveAndComeBackLater
        saveAndComeBackLater.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(location = Some("uk-property-start-date")).url
      }
    }



    "must display max date error form error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.property.day-month-year.max-date")
      val formWithError = PropertyStartDateForm
        .propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)
        .withError(dateValidationError)

      val doc = document(propertyStartDateForm = formWithError)

      doc.mustHaveDateInput(
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

    "must display min date error form error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.property.day-month-year.min-date", List("11 April 2021"))
      val formWithError = PropertyStartDateForm
        .propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)
        .withError(dateValidationError)

      val doc = document(propertyStartDateForm = formWithError)

      doc.mustHaveDateInput(
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
}
