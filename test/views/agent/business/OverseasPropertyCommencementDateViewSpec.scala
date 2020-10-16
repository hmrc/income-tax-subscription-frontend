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

import forms.individual.business.OverseasPropertyCommencementDateForm
import models.common.OverseasPropertyCommencementDateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.ViewSpec

class OverseasPropertyCommencementDateViewSpec extends ViewSpec {

  object OverseasPropertyCommencementDateMessages {
    val heading = "When did your client’s foreign property business start trading?"
    val para = "This is the date that letting or renting out any foreign property first started."
    val exampleStartDate = "For example, 1 8 2014"
    val continue = "Continue"
    val backLink = "Back"
  }

  val backUrl: String = testBackUrl
  val action: Call = testCall
  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("startDate", "testError")
  val titleSuffix = " - Report your income and expenses quarterly - GOV.UK"

  class Setup(isEditMode: Boolean = false, foreignPropertyCommencementDateForm: Form[OverseasPropertyCommencementDateModel] =
  OverseasPropertyCommencementDateForm.overseasPropertyCommencementDateForm("testMessage")) {

    val page: HtmlFormat.Appendable = views.html.agent.business.overseas_property_commencement_date(
      foreignPropertyCommencementDateForm,
      action,
      isEditMode,
      backUrl
    )(FakeRequest(), implicitly, appConfig)

    val document: Document = Jsoup.parse(page.body)
  }

  "Overseas Commencement Date page" must {
    "have a title" in new Setup {
      document.title mustBe OverseasPropertyCommencementDateMessages.heading + titleSuffix
    }

    "have a heading" in new Setup {
      document.getH1Element.text mustBe OverseasPropertyCommencementDateMessages.heading
    }

    "have a Form" in new Setup {
      document.getForm.attr("method") mustBe testCall.method
      document.getForm.attr("action") mustBe testCall.url
    }

    "have a fieldset with dateInputs" in new Setup {
      document.mustHaveDateField("startDate", "", OverseasPropertyCommencementDateMessages.exampleStartDate)
    }

    "have a continue button when not in edit mode" in new Setup {
      document.getSubmitButton.text mustBe OverseasPropertyCommencementDateMessages.continue
    }

    "have a backlink " in new Setup {
      document.getBackLink.text mustBe OverseasPropertyCommencementDateMessages.backLink
      document.getBackLink.attr("href") mustBe testBackUrl
    }

    "must display form error on page" in new Setup(false, OverseasPropertyCommencementDateForm.overseasPropertyCommencementDateForm(
      "testMessage").withError(testError)) {
      document.mustHaveErrorSummary(List[String](testError.message))
      document.mustHaveDateField("startDate", "", OverseasPropertyCommencementDateMessages.exampleStartDate, Some(testError.message))
    }
  }
}
