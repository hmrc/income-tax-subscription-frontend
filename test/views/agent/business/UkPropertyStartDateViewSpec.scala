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

import forms.agent.PropertyStartDateForm
import models.DateModel
import org.jsoup.Jsoup
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.html.agent.business.PropertyStartDate

import java.time.LocalDate


class UkPropertyStartDateViewSpec extends ViewSpec {

  val backUrl: String = testBackUrl
  val action: Call = testCall
  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("startDate", "agent.error.property.day-month-year.empty")
  private val propertyStartDateView = app.injector.instanceOf[PropertyStartDate]

  private val defaultForm = PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)

  private def document(
                        isEditMode: Boolean = false,
                        propertyStartDateForm: Form[DateModel] = PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)
                      ) = Jsoup.parse(page(isEditMode, propertyStartDateForm).body)

  private def page(isEditMode: Boolean, propertyStartDateForm: Form[DateModel]) =
    propertyStartDateView(
      propertyStartDateForm,
      testCall,
      isEditMode,
      testBackUrl,
      ClientDetails("FirstName LastName", "ZZ111111Z")
    )(FakeRequest(), implicitly, appConfig)

  object PropertyStartDateMessages {
    val title = "When did your client’s UK property business start?"
    val heading: String = title
    val caption: String = "FirstName LastName | ZZ 11 11 11 Z"
    val para: String = "This is when your client started letting any UK property."
    val hint = "For example, 17 8 2014."
    val continue = "Continue"
    val saveAndContinue = "Save and continue"
    val backLink = "Back"
    val update = "Update"
    val maxDate = "The date the UK property business started trading must be on or before 11 April 2021"
    val minDate = "The date your client’s property business started trading must be on or after 11 April 2021"
  }

  "agent UK property business start" must {
    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = propertyStartDateView(
          defaultForm,
          testCall,
          isEditMode = false,
          testBackUrl,
          ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = PropertyStartDateMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
      )

      "there is an error" in new TemplateViewTest(
        view = propertyStartDateView(
          defaultForm.withError(testError),
          testCall,
          isEditMode = false,
          testBackUrl,
          ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = PropertyStartDateMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a heading" in {
      document().mainContent.selectHead("h1.govuk-heading-l").text mustBe PropertyStartDateMessages.heading
    }

    "have a caption" in {
      document().mainContent.selectHead("span.govuk-caption-l").text mustBe PropertyStartDateMessages.caption
    }

    "have a Form" in {
      document().getForm.attr("method") mustBe testCall.method
      document().getForm.attr("action") mustBe testCall.url
    }

    "have a fieldset with dateInputs" in {
      document().mustHaveGovukDateField("startDate", PropertyStartDateMessages.heading, PropertyStartDateMessages.hint)
      document().selectNth("p", 2).text mustBe PropertyStartDateMessages.para
    }

    "have a save & continue button" in {
      document().mainContent.getGovukSubmitButton.text mustBe PropertyStartDateMessages.saveAndContinue
    }

    "have a backlink " in {
      document().getGovukBackLink.text mustBe PropertyStartDateMessages.backLink
      document().getGovukBackLink.attr("href") mustBe testBackUrl
    }

    "must display max date error form error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.property.day-month-year.max-date", List("11 April 2021"))
      val formWithError = PropertyStartDateForm
        .propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)
        .withError(dateValidationError)

      val doc = document(propertyStartDateForm = formWithError)

      doc.mustHaveGovukDateField(
        "startDate",
        PropertyStartDateMessages.heading,
        PropertyStartDateMessages.hint,
        Some(PropertyStartDateMessages.maxDate)
      )
      document().selectNth("p", 2).text mustBe PropertyStartDateMessages.para
    }

    "must display min date error form error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.property.day-month-year.min-date", List("11 April 2021"))
      val formWithError = PropertyStartDateForm
        .propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)
        .withError(dateValidationError)

      val doc = document(propertyStartDateForm = formWithError)

      doc.mustHaveGovukDateField(
        "startDate",
        PropertyStartDateMessages.heading,
        PropertyStartDateMessages.hint,
        Some(PropertyStartDateMessages.minDate)
      )
    }
  }
}
