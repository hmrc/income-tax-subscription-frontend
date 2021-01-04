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

import forms.agent.PropertyStartDateForm
import models.common.PropertyStartDateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.ViewSpec


class UkPropertyStartDateViewSpec extends ViewSpec  {

  object PropertyStartDateMessages {
    val title = "When did your client’s UK property business start trading?"
    val heading: String = title
    val exampleStartDate = "For example, 1 8 2014"
    val continue = "Continue"
    val backLink = "Back"
    val update = "Update"
  }


  val backUrl: String = testBackUrl
  val action: Call = testCall
  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("startDate", "testError")

  class Setup(isEditMode: Boolean = false,
              propertyStartDateForm: Form[PropertyStartDateModel] = PropertyStartDateForm.propertyStartDateForm("testMessage", "testMessage")) {
    val page: HtmlFormat.Appendable = views.html.agent.business.property_start_date(
      propertyStartDateForm,
      testCall,
      isEditMode,
      testBackUrl
    )(FakeRequest(), implicitly, appConfig)

    val document: Document = Jsoup.parse(page.body)
  }


  "agent UK property business start" must {

    "have a title" in new Setup {
      val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
      document.title mustBe PropertyStartDateMessages.title + serviceNameGovUk
    }
    "have a heading" in new Setup {
      document.getH1Element.text mustBe PropertyStartDateMessages.heading
    }
    "have a Form" in new Setup {
      document.getForm.attr("method") mustBe testCall.method
      document.getForm.attr("action") mustBe testCall.url
    }
    "have a fieldset with dateInputs" in new Setup {
      document.mustHaveDateField("startDate", "", PropertyStartDateMessages.exampleStartDate)
    }
    "have a continue button when not in edit mode" in new Setup {
      document.getSubmitButton.text mustBe PropertyStartDateMessages.continue
    }
    "have update button when in edit mode" in new Setup(true) {
      document.getSubmitButton.text mustBe PropertyStartDateMessages.update
    }
    "have a backlink " in new Setup {
      document.getBackLink.text mustBe PropertyStartDateMessages.backLink
      document.getBackLink.attr("href") mustBe testBackUrl
    }
    "must display form error on page" in new Setup(false, PropertyStartDateForm.propertyStartDateForm("testMessage", "testMessage").withError(testError)) {
      document.mustHaveErrorSummary(List[String](testError.message))
      document.mustHaveDateField("startDate", "", PropertyStartDateMessages.exampleStartDate, Some(testError.message))
    }

  }

}
