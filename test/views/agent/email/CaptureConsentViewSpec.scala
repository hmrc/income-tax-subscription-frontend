/*
 * Copyright 2025 HM Revenue & Customs
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

package views.agent.email

import forms.agent.email.CaptureConsentForm
import messagelookup.agent.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.agent.email.CaptureConsent

class CaptureConsentViewSpec extends ViewSpec {

  "CaptureConsent" must {
    "be using the correct template" when {

      "the page has no error" in new TemplateViewTest(
        view = page(),
        isAgent = true,
        title = CaptureConsentMessages.heading
      )

      "the page has an error" in new TemplateViewTest(
        view = page(hasError = true),
        isAgent = true,
        title = CaptureConsentMessages.heading,
        error = Some(testFormError)
      )
    }

    "have a form" which {
      def form: Element = document().selectHead("form")

      "has correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "have a heading" in {
        document().mustHaveHeadingAndCaption(
          CaptureConsentMessages.heading,
          CaptureConsentMessages.caption,
          isSection = false
        )
      }

      "has the correct radio inputs" in {
        form.mustHaveYesNoRadioInputs(selector = "fieldset")(
          name = CaptureConsentMessages.radioName,
          legend = CaptureConsentMessages.radioLegend,
          isHeading = false,
          isLegendHidden = false,
          hint = None,
          errorMessage = None,
        )
      }

      "has a continue button" in {
        form.select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
      }
    }
  }



  private val captureConsent: CaptureConsent = app.injector.instanceOf[CaptureConsent]
  private val fullName = "FirstName LastName"
  private val nino = "ZZ 11 11 11 Z"

  private val testFormError: FormError = FormError(CaptureConsentForm.fieldName, "agent.capture-consent.form-error")

  private def page(hasError: Boolean = false, clientName: String = fullName, clientNino: String = nino): Html = {
    captureConsent(
      if (hasError) CaptureConsentForm.captureConsentForm.withError(testFormError) else CaptureConsentForm.captureConsentForm,
      testCall,
      clientName,
      clientNino,
      testBackUrl
    )
  }

  private def document(hasError: Boolean = false): Document =
    Jsoup.parse(page(hasError).body)

  private object CaptureConsentMessages {
    val heading: String = "Capture consent"
    val caption = s"$fullName | $nino"
    val errorInvalid: String = "Select yes or no"
    val radioName: String = "yes-no"
    val radioLegend: String = "Does your client consent?"
  }
}
