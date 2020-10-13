/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.agent.PropertyCommencementDateForm
import models.common.PropertyCommencementDateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.ViewSpec


class UkPropertyCommencementDateViewSpec extends ViewSpec  {

  object PropertyCommencementDateMessages {
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
              propertyCommencementDateForm: Form[PropertyCommencementDateModel] = PropertyCommencementDateForm.propertyCommencementDateForm("testMessage")) {
    val page: HtmlFormat.Appendable = views.html.agent.business.property_commencement_date(
      propertyCommencementDateForm,
      testCall,
      isEditMode,
      testBackUrl
    )(FakeRequest(), implicitly, appConfig)

    val document: Document = Jsoup.parse(page.body)
  }


  "agent UK property business start" must {

    "have a title" in new Setup {
      document.title mustBe PropertyCommencementDateMessages.title
    }
    "have a heading" in new Setup {
      document.getH1Element.text mustBe PropertyCommencementDateMessages.heading
    }
    "have a Form" in new Setup {
      document.getForm.attr("method") mustBe testCall.method
      document.getForm.attr("action") mustBe testCall.url
    }
    "have a fieldset with dateInputs" in new Setup {
      document.mustHaveDateField("startDate", "", PropertyCommencementDateMessages.exampleStartDate)
    }
    "have a continue button when not in edit mode" in new Setup {
      document.getSubmitButton.text mustBe PropertyCommencementDateMessages.continue
    }
    "have update button when in edit mode" in new Setup(true) {
      document.getSubmitButton.text mustBe PropertyCommencementDateMessages.update
    }
    "have a backlink " in new Setup {
      document.getBackLink.text mustBe PropertyCommencementDateMessages.backLink
      document.getBackLink.attr("href") mustBe testBackUrl
    }
    "must display form error on page" in new Setup(false, PropertyCommencementDateForm.propertyCommencementDateForm("testMessage").withError(testError)) {
      document.mustHaveErrorSummary(List[String](testError.message))
      document.mustHaveDateField("startDate", "", PropertyCommencementDateMessages.exampleStartDate, Some(testError.message))
    }

  }

}
