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

package views.agent.business

import forms.individual.business.OverseasPropertyStartDateForm
import models.DateModel
import org.jsoup.Jsoup
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.agent.business.OverseasPropertyStartDate

import java.time.LocalDate

class OverseasPropertyStartDateViewSpec extends ViewSpec {
  object OverseasPropertyStartDateMessages {
    val heading = "When did your client’s overseas property business start trading?"
    val para = "This is the date that letting or renting out any overseas property first started."
    val hint = "For example, 17 8 2014."
    val saveAndContinue = "Save and continue"
    val backLink = "Back"
    val maxDate = "The date the overseas property business started trading must be on or before 11 April 2021"
    val minDate = "The date your client’s property business started trading must be on or after 11 April 2021"
  }

  val overseasPropertyStartDate: OverseasPropertyStartDate = app.injector.instanceOf[OverseasPropertyStartDate]

  val backUrl: String = testBackUrl
  val action: Call = testCall
  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("startDate", "error.overseas.property.day-month-year.empty")
  val titleSuffix = " - Use software to report your client’s Income Tax - GOV.UK"

  private val defaultForm: Form[DateModel] = OverseasPropertyStartDateForm.overseasPropertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)

  private def document(
               isEditMode: Boolean = false,
               overseasPropertyStartDateForm: Form[DateModel] = defaultForm) = {

    val page: HtmlFormat.Appendable = overseasPropertyStartDate(
      overseasPropertyStartDateForm,
      action,
      backUrl,
      isEditMode
    )(FakeRequest(), implicitly, appConfig)

    Jsoup.parse(page.body)
  }

  "Overseas Start Date page" must {
    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = overseasPropertyStartDate(
          defaultForm,
          testCall,
          testBackUrl,
          isEditMode = false
        ),
        title = OverseasPropertyStartDateMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
      )

      "there is an error" in new TemplateViewTest(
        view = overseasPropertyStartDate(
          defaultForm.withError(testError),
          testCall,
          testBackUrl,
          isEditMode = false
        ),
        title = OverseasPropertyStartDateMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a title" in {
      document().title mustBe OverseasPropertyStartDateMessages.heading + titleSuffix
    }

    "have a heading" in {
      document().getH1Element.text mustBe OverseasPropertyStartDateMessages.heading
    }

    "have a Form" in {
      document().getForm.attr("method") mustBe testCall.method
      document().getForm.attr("action") mustBe testCall.url
    }

    "have a fieldset with dateInputs" in {
      document().mustHaveGovukDateField(
        "startDate",
        OverseasPropertyStartDateMessages.heading,
        OverseasPropertyStartDateMessages.hint)
    }

    "have a save and continue button when not in edit mode" in {
      document().getGovukSubmitButton.text mustBe OverseasPropertyStartDateMessages.saveAndContinue
    }

    "have a backlink " in {
      document().getGovukBackLink.text mustBe OverseasPropertyStartDateMessages.backLink
      document().getGovukBackLink.attr("href") mustBe testBackUrl
    }

    "display max date error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.overseas.property.day-month-year.max-date", List("11 April 2021"))
      val doc = document(overseasPropertyStartDateForm = defaultForm.withError(dateValidationError))
      doc.mustHaveGovukDateField(
        "startDate",
        OverseasPropertyStartDateMessages.heading,
        OverseasPropertyStartDateMessages.hint,
        Some(OverseasPropertyStartDateMessages.maxDate)
      )
    }

    "display min date error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.overseas.property.day-month-year.min-date", List("11 April 2021"))
      val doc = document(overseasPropertyStartDateForm = defaultForm.withError(dateValidationError))
      doc.mustHaveGovukDateField(
        "startDate",
        OverseasPropertyStartDateMessages.heading,
        OverseasPropertyStartDateMessages.hint,
        Some(OverseasPropertyStartDateMessages.minDate)
      )
    }
  }
}
