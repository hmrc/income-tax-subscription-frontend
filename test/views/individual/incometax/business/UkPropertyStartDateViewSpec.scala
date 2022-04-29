/*
 * Copyright 2022 HM Revenue & Customs
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

package views.individual.incometax.business

import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import forms.individual.business.PropertyStartDateForm
import models.DateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.test.FakeRequest
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.incometax.business.PropertyStartDate

import java.time.LocalDate

class UkPropertyStartDateViewSpec extends ViewSpec  {

  object PropertyStartDateMessages {
    val heading: String = "When did your UK property business start trading?"
    val hint = "For example, 17 8 2014."
    val continue = "Continue"
    val saveAndContinue = "Save and continue"
    val backLink = "Back"
    val update = "Update"
    val maxDate = "The date your UK property business started trading must be the same as or before 11 April 2021"
    val minDate = "The date your property business started must be on or after 11 April 2021"
  }

  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("startDate", "testError")

  val propertyStartDate: PropertyStartDate = app.injector.instanceOf[PropertyStartDate]

  "UK property business start" must {

    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = propertyStartDate(
          PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString),
          testCall,
          isEditMode = false,
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
          isEditMode = false,
          testBackUrl
        ),
        title = PropertyStartDateMessages.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a heading" in {
      document().getH1Element.text mustBe PropertyStartDateMessages.heading
    }

    "have a form" in {
      document().getForm.attr("method") mustBe testCall.method
      document().getForm.attr("action") mustBe testCall.url
    }

    "have a fieldset with dateInputs" in {
      document().mustHaveDateInput(
        name = PropertyStartDateForm.startDate,
        label = PropertyStartDateMessages.heading,
        hint = Some(PropertyStartDateMessages.hint)
      )
    }

    "have a continue button when not in edit mode" in {
      disable(SaveAndRetrieve)
      document().selectHead("#continue-button").text mustBe PropertyStartDateMessages.continue
    }

    "have a save & continue button when save & retrieve feature is enabled" in {
      enable(SaveAndRetrieve)
      document().selectHead("#continue-button").text mustBe PropertyStartDateMessages.saveAndContinue
    }

    "have update button when in edit mode" in {
      disable(SaveAndRetrieve)
      document(isEditMode = true).selectHead("#continue-button").text mustBe PropertyStartDateMessages.update
    }

    "must display max date error on page" in {
      val dateValidationError = FormError("startDate", "error.property.day_month_year.max_date", List("11 April 2021"))
      val formWithError = PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString).withError(dateValidationError)
      document(propertyStartDateForm = formWithError).mustHaveGovukDateField(
        "startDate",
        PropertyStartDateMessages.heading,
        PropertyStartDateMessages.hint,
        Some(PropertyStartDateMessages.maxDate)
      )
    }

    "must display min date error on page" in {
      val dateValidationError = FormError("startDate", "error.property.day_month_year.min_date", List("11 April 2021"))
      val formWithError = PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString).withError(dateValidationError)
      document(propertyStartDateForm = formWithError).mustHaveGovukDateField(
        "startDate",
        PropertyStartDateMessages.heading,
        PropertyStartDateMessages.hint,
        Some(PropertyStartDateMessages.minDate)
      )
    }
  }

  private def page(isEditMode: Boolean, propertyStartDateForm: Form[DateModel]): Html = {
    propertyStartDate(
      propertyStartDateForm,
      testCall,
      isEditMode,
      testBackUrl
    )(FakeRequest(), implicitly)
  }

  private def document(
                        isEditMode: Boolean = false,
                        propertyStartDateForm: Form[DateModel] = PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)
                      ): Document = {
    Jsoup.parse(page(isEditMode, propertyStartDateForm).body)
  }
}
