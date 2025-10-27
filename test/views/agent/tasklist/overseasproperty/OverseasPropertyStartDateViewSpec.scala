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

import forms.agent.OverseasPropertyStartDateForm
import models.DateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.tasklist.overseasproperty.OverseasPropertyStartDate

import java.time.LocalDate

class OverseasPropertyStartDateViewSpec extends ViewSpec {
  object OverseasPropertyStartDateMessages {
    val heading = "Start date for income from foreign property"
    val caption: String = "FirstName LastName | ZZ 11 11 11 Z"
    val para1 = "We need to know the exact start date."
    val hint = s"For example, 27 9 ${AccountingPeriodUtil.getStartDateLimit.getYear}"
    val saveAndContinue = "Save and continue"
    val saveAndComeBackLater = "Save and come back later"
    val backLink = "Back"
    val maxDate = "The date must be before 18 April 2021"
    val minDate = "The date must be on or after 11 April 2021"
  }

  val testError: FormError = FormError("startDate", "agent.error.overseas.property.empty")

  val overseasPropertyStartDate: OverseasPropertyStartDate = app.injector.instanceOf[OverseasPropertyStartDate]
  val defaultForm: Form[DateModel] = OverseasPropertyStartDateForm.overseasPropertyStartDateForm(LocalDate.now, LocalDate.now, _.toString)

  val backUrl: String = testBackUrl
  val action: Call = testCall
  val taxYearEnd: Int = 2020

  val titleSuffix = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"
  private def document(overseasPropertyStartDateForm: Form[DateModel] = OverseasPropertyStartDateForm.overseasPropertyStartDateForm(
    minStartDate = LocalDate.now(),
    maxStartDate = LocalDate.now(),
    f = d => d.toString
  )) = Jsoup.parse(page(overseasPropertyStartDateForm).body)

  private def page(overseasPropertyStartDateForm: Form[DateModel]) =
    overseasPropertyStartDate(
      overseasPropertyStartDateForm,
      testCall,
      testBackUrl,
      ClientDetails("FirstName LastName", "ZZ111111Z")
    )(FakeRequest(), implicitly)

  "Overseas Start Date Page" must {
    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = overseasPropertyStartDate(
          overseasPropertyStartDateForm = defaultForm,
          postAction = testCall,
          backUrl = testBackUrl,
          clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = OverseasPropertyStartDateMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
      )

      "there is an error" in new TemplateViewTest(
        view = overseasPropertyStartDate(
          overseasPropertyStartDateForm = defaultForm.withError(testError),
          postAction = testCall,
          backUrl = testBackUrl,
          clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = OverseasPropertyStartDateMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a heading and caption" in {
      document().mainContent.mustHaveHeadingAndCaption(
        heading = OverseasPropertyStartDateMessages.heading,
        caption = OverseasPropertyStartDateMessages.caption,
        isSection = false
      )
    }

    "have a paragraph one" in {
      document().mainContent.selectNth("p", 1).text() mustBe OverseasPropertyStartDateMessages.para1
    }

    "have a form" which {
      def form: Element = document().mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a date input" in {
        form.mustHaveDateInput(
          id = OverseasPropertyStartDateForm.startDate,
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

      "has a save & continue button" in {
        form.getGovukSubmitButton.text mustBe OverseasPropertyStartDateMessages.saveAndContinue
      }

      "has a save & come back later button" in {
        val saveAndComeBackLater: Element = form.selectHead(".govuk-button--secondary")

        saveAndComeBackLater.text mustBe OverseasPropertyStartDateMessages.saveAndComeBackLater
        saveAndComeBackLater.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(location = Some("overseas-property-start-date")).url
      }
    }



    "must display max date error form error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.overseas.property.day-month-year.max-date", List("18 April 2021"))
      val formWithError = OverseasPropertyStartDateForm
        .overseasPropertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)
        .withError(dateValidationError)

      val doc = document(overseasPropertyStartDateForm = formWithError)

      doc.mustHaveDateInput(
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

    "must display min date error form error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.overseas.property.day-month-year.min-date", List("11 April 2021"))
      val formWithError = OverseasPropertyStartDateForm
        .overseasPropertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)
        .withError(dateValidationError)

      val doc = document(overseasPropertyStartDateForm = formWithError)

      doc.mustHaveDateInput(
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
}
