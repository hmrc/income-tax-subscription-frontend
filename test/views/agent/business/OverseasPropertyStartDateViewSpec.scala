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

package views.agent.business

import forms.individual.business.OverseasPropertyStartDateForm
import models.DateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.ViewSpec

class OverseasPropertyStartDateViewSpec extends ViewSpec {

  object OverseasPropertyStartDateMessages {
    val heading = "When did your client’s overseas property business start trading?"
    val para = "This is the date that letting or renting out any overseas property first started."
    val exampleStartDate = "For example, 1 8 2014"
    val continue = "Continue"
    val backLink = "Back"
  }

  val backUrl: String = testBackUrl
  val action: Call = testCall
  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("startDate", "testError")
  val titleSuffix = " - Use software to report your client’s Income Tax - GOV.UK"

  class Setup(isEditMode: Boolean = false, overseasPropertyStartDateForm: Form[DateModel] =
  OverseasPropertyStartDateForm.overseasPropertyStartDateForm("minStartDateError", "maxStartDateError")) {

    val page: HtmlFormat.Appendable = views.html.agent.business.overseas_property_start_date(
      overseasPropertyStartDateForm,
      action,
      isEditMode,
      backUrl
    )(FakeRequest(), implicitly, appConfig)

    val document: Document = Jsoup.parse(page.body)
  }

  "Overseas Start Date page" must {
    "have a title" in new Setup {
      document.title mustBe OverseasPropertyStartDateMessages.heading + titleSuffix
    }

    "have a heading" in new Setup {
      document.getH1Element.text mustBe OverseasPropertyStartDateMessages.heading
    }

    "have a Form" in new Setup {
      document.getForm.attr("method") mustBe testCall.method
      document.getForm.attr("action") mustBe testCall.url
    }

    "have a fieldset with dateInputs" in new Setup {
      document.mustHaveDateField("startDate", OverseasPropertyStartDateMessages.heading, OverseasPropertyStartDateMessages.exampleStartDate)
    }

    "have a continue button when not in edit mode" in new Setup {
      document.getSubmitButton.text mustBe OverseasPropertyStartDateMessages.continue
    }

    "have a backlink " in new Setup {
      document.getBackLink.text mustBe OverseasPropertyStartDateMessages.backLink
      document.getBackLink.attr("href") mustBe testBackUrl
    }

    "must display form error on page" in new Setup(false, OverseasPropertyStartDateForm.overseasPropertyStartDateForm(
      "minStartDateError", "maxStartDateError").withError(testError)) {
      document.mustHaveErrorSummary(List[String](testError.message))
      document.mustHaveDateField(
        "startDate",
        OverseasPropertyStartDateMessages.heading,
        OverseasPropertyStartDateMessages.exampleStartDate,
        Some(testError.message)
      )
    }
  }
}
