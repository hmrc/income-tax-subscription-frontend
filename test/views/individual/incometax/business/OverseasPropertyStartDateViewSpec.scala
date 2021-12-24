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

import forms.individual.business.OverseasPropertyStartDateForm
import models.DateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.incometax.business.OverseasPropertyStartDate

class OverseasPropertyStartDateViewSpec extends ViewSpec {

  object OverseasPropertyStartDateMessages {
    val title = "When did your overseas property business start trading?"
    val heading: String = title
    val exampleStartDate = "For example, 1 8 2014"
    val continue = "Continue"
    val saveAndContinue = "Save and continue"
    val backLink = "Back"
    val update = "Update"
  }

  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("startDate", "testError")

  val overseasPropertyStartDate: OverseasPropertyStartDate = app.injector.instanceOf[OverseasPropertyStartDate]

  class Setup(isEditMode: Boolean = false,
              form: Form[DateModel] = OverseasPropertyStartDateForm.overseasPropertyStartDateForm("testMessage", "testMessage"),
              isSaveAndRetrieveEnabled: Boolean = false) {

    val page: Html = overseasPropertyStartDate(
      form,
      testCall,
      isEditMode,
      testBackUrl,
      isSaveAndRetrieveEnabled
    )

    val document: Document = Jsoup.parse(page.body)

  }

  "overseas property start date page" must {

    "have the correct template" when {
      "there is no error" in new TemplateViewTest(
        view = overseasPropertyStartDate(
          overseasPropertyStartDateForm = OverseasPropertyStartDateForm.overseasPropertyStartDateForm("testMinMessage", "testMaxMessage"),
          postAction = testCall,
          isEditMode = false,
          backUrl = testBackUrl
        ),
        title = OverseasPropertyStartDateMessages.title,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "there is an error" in new TemplateViewTest(
        view = overseasPropertyStartDate(
          overseasPropertyStartDateForm = OverseasPropertyStartDateForm.overseasPropertyStartDateForm("testMinMessage", "testMaxMessage").withError(testError),
          postAction = testCall,
          isEditMode = false,
          backUrl = testBackUrl
        ),
        title = OverseasPropertyStartDateMessages.title,
        backLink = Some(testBackUrl),
        hasSignOutLink = true,
        error = Some(testError)
      )
    }

    "have a heading" in new Setup {
      document.getH1Element.text mustBe OverseasPropertyStartDateMessages.heading
    }

    "have a form" in new Setup {
      document.getForm.attr("method") mustBe testCall.method
      document.getForm.attr("action") mustBe testCall.url
    }

    "have a date input" when {
      "there is no error" in new Setup {
        document.mustHaveDateInput(
          name = "startDate",
          label = OverseasPropertyStartDateMessages.heading,
          hint = Some(OverseasPropertyStartDateMessages.exampleStartDate)
        )
      }
      "there is an error" in new Setup(
        form = OverseasPropertyStartDateForm.overseasPropertyStartDateForm("testMinMessage", "testMaxMessage").withError(testError)
      ) {
        document.mustHaveDateInput(
          name = "startDate",
          label = OverseasPropertyStartDateMessages.heading,
          hint = Some(OverseasPropertyStartDateMessages.exampleStartDate),
          error = Some(testError)
        )
      }
    }

    "have a continue button when not in edit mode" in new Setup {
      document.selectHead("#continue-button").text mustBe OverseasPropertyStartDateMessages.continue
    }

    "have a Save and Continue button when not in edit mode and feature SaveAndRetrieve is enabled" in new Setup( isSaveAndRetrieveEnabled = true ) {
      document.selectHead("#continue-button").text mustBe OverseasPropertyStartDateMessages.saveAndContinue
    }

    "have update button when in edit mode" in new Setup(true) {
      document.selectHead("#continue-button").text mustBe OverseasPropertyStartDateMessages.update
    }

  }

}
