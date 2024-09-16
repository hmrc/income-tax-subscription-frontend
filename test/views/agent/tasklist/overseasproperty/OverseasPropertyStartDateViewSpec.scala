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

import forms.individual.business.OverseasPropertyStartDateForm
import models.DateModel
import org.jsoup.Jsoup
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.html.agent.tasklist.overseasproperty.OverseasPropertyStartDate

import java.time.LocalDate

class OverseasPropertyStartDateViewSpec extends ViewSpec {
  object OverseasPropertyStartDateMessages {
    val heading = "When did your client start their foreign property business?"
    val para1 = "The date your client’s business started trading can be today, in the past or up to 7 days in the future."
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
      isEditMode,
      ClientDetails("FirstName LastName", "ZZ111111Z")
    )(FakeRequest(), implicitly)

    Jsoup.parse(page.body)
  }

  "Overseas Start Date page" must {
    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = overseasPropertyStartDate(
          defaultForm,
          testCall,
          testBackUrl,
          isEditMode = false,
          ClientDetails("FirstName LastName", "ZZ111111Z")
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
          isEditMode = false,
          ClientDetails("FirstName LastName", "ZZ111111Z")
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
        caption = s"FirstName LastName | ZZ 11 11 11 Z",
        isSection = false
      )
    }

    "have a paragraph One" in {
      document().selectNth("p", 2).text() mustBe OverseasPropertyStartDateMessages.para1
    }

    "have a Form" in {
      document().getForm.attr("method") mustBe testCall.method
      document().getForm.attr("action") mustBe testCall.url
    }

    "have a save and continue button when not in edit mode" in {
      document().mainContent.getGovukSubmitButton.text mustBe OverseasPropertyStartDateMessages.saveAndContinue
    }

    "have a backlink " in {
      document().getGovukBackLink.text mustBe OverseasPropertyStartDateMessages.backLink
      document().getGovukBackLink.attr("href") mustBe testBackUrl
    }

    "display max date error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.overseas.property.day-month-year.max-date", List("11 April 2021"))
      val doc = document(overseasPropertyStartDateForm = defaultForm.withError(dateValidationError))
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

    "display min date error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.overseas.property.day-month-year.min-date", List("11 April 2021"))
      val doc = document(overseasPropertyStartDateForm = defaultForm.withError(dateValidationError))
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
