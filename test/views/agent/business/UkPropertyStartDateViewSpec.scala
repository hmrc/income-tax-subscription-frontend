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

package views.agent.business

import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import forms.agent.PropertyStartDateForm
import models.DateModel
import org.jsoup.Jsoup
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import utilities.ViewSpec
import views.html.agent.business.PropertyStartDate

import java.time.LocalDate


class UkPropertyStartDateViewSpec extends ViewSpec with FeatureSwitching {

  val backUrl: String = testBackUrl
  val action: Call = testCall
  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("startDate", "testError")
  private val propertyStartDateView = app.injector.instanceOf[PropertyStartDate]

  private def document(
                        isEditMode: Boolean = false,
                        propertyStartDateForm: Form[DateModel] = PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString)
                      ) = Jsoup.parse(page(isEditMode, propertyStartDateForm).body)

  private def page(isEditMode: Boolean, propertyStartDateForm: Form[DateModel]) =
    propertyStartDateView(
    propertyStartDateForm,
    testCall,
    isEditMode,
    testBackUrl
  )(FakeRequest(), implicitly, appConfig)

  object PropertyStartDateMessages {
    val title = "When did your client’s UK property business start trading?"
    val heading: String = title
    val exampleStartDate = "For example, 1 8 2014"
    val continue = "Continue"
    val saveAndContinue = "Save and continue"
    val backLink = "Back"
    val update = "Update"
  }

  "agent UK property business start" must {
    "have a title" in {
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      document().title mustBe PropertyStartDateMessages.title + serviceNameGovUk
    }

    "have a heading" in {
      document().getH1Element.text mustBe PropertyStartDateMessages.heading
    }

    "have a Form" in {
      document().getForm.attr("method") mustBe testCall.method
      document().getForm.attr("action") mustBe testCall.url
    }

    "have a fieldset with dateInputs" in {
      document().mustHaveGovukDateField("startDate", PropertyStartDateMessages.heading, PropertyStartDateMessages.exampleStartDate)
    }

    "have a continue button when not in edit mode" in {
      disable(SaveAndRetrieve)
      document().getGovukSubmitButton.text mustBe PropertyStartDateMessages.continue
    }

    "have update button when in edit mode" in {
      disable(SaveAndRetrieve)
      document(isEditMode = true).getGovukSubmitButton.text mustBe PropertyStartDateMessages.update
    }

    "have a save & continue button when save & retrieve feature is enabled" in {
      enable(SaveAndRetrieve)
      document().getGovukSubmitButton.text mustBe PropertyStartDateMessages.saveAndContinue
    }

    "have a backlink " in {
      document().getGovukBackLink.text mustBe PropertyStartDateMessages.backLink
      document().getGovukBackLink.attr("href") mustBe testBackUrl
    }

    "must display form error on page" in {
      val formWithError = PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString).withError(testError)
      val doc = document(propertyStartDateForm = formWithError)
      doc.mustHaveGovukErrorSummary(testError.message)
      doc.mustHaveGovukDateField("startDate", PropertyStartDateMessages.heading, PropertyStartDateMessages.exampleStartDate, Some(testError.message))
    }
  }
}
