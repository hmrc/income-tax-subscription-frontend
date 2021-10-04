/*
 * Copyright 2021 HM Revenue & Customs
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

import forms.individual.business.PropertyStartDateForm
import models.common.PropertyStartDateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.test.FakeRequest
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.incometax.business.PropertyStartDate


class UkPropertyStartDateViewSpec extends ViewSpec {

  object PropertyStartDateMessages {
    val heading: String = "When did your UK property business start trading?"
    val exampleStartDate = "For example, 1 8 2014"
    val continue = "Continue"
    val backLink = "Back"
    val update = "Update"
  }

  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("startDate", "testError")

  val propertyStartDate: PropertyStartDate = app.injector.instanceOf[PropertyStartDate]

  class Setup(isEditMode: Boolean = false,
              propertyStartDateForm: Form[PropertyStartDateModel] = PropertyStartDateForm.propertyStartDateForm("testMessage", "testMessage")) {
    val page: Html = propertyStartDate(
      propertyStartDateForm,
      testCall,
      isEditMode,
      testBackUrl
    )(FakeRequest(), implicitly)

    val document: Document = Jsoup.parse(page.body)
  }


  "UK property business start" must {

    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = propertyStartDate(
          PropertyStartDateForm.propertyStartDateForm("testMinMessage", "testMaxMessage"),
          testCall,
          isEditMode = false,
          testBackUrl
        ),
        title = PropertyStartDateMessages.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )

      "there is an error" in new TemplateViewTest(
        view = propertyStartDate(
          PropertyStartDateForm.propertyStartDateForm("testMinMessage", "testMaxMessage").withError(testError),
          testCall,
          isEditMode = false,
          testBackUrl
        ),
        title = PropertyStartDateMessages.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true,
        error = Some(testError)
      )
    }

    "have a heading" in new Setup {
      document.getH1Element.text mustBe PropertyStartDateMessages.heading
    }

    "have a form" in new Setup {
      document.getForm.attr("method") mustBe testCall.method
      document.getForm.attr("action") mustBe testCall.url
    }

    "have a fieldset with dateInputs" in new Setup() {
      document.mustHaveDateInput(
        name = PropertyStartDateForm.startDate,
        label = PropertyStartDateMessages.heading,
        hint = Some(PropertyStartDateMessages.exampleStartDate)
      )
    }

    "have a continue button when not in edit mode" in new Setup {
      document.selectHead("button").text mustBe PropertyStartDateMessages.continue
    }

    "have update button when in edit mode" in new Setup(true) {
      document.selectHead("button").text mustBe PropertyStartDateMessages.update
    }

    "must display form error on page" in new Setup(false, PropertyStartDateForm.propertyStartDateForm("testMessage", "testMessage").withError(testError)) {
      document.mustHaveDateInput(
        name = PropertyStartDateForm.startDate,
        label = PropertyStartDateMessages.heading,
        hint = Some(PropertyStartDateMessages.exampleStartDate),
        error = Some(testError)
      )
    }

  }


}
