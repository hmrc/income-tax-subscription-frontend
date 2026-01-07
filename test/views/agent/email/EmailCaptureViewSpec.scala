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

import forms.agent.email.EmailCaptureForm
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.agent.email.EmailCapture

class EmailCaptureViewSpec extends ViewSpec {

  "EmailCapture" must {
    "use the correct template" when {
      "there is no error" in new TemplateViewTest(
        view = page(error = false),
        title = EmailCaptureMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        error = None
      )
      "there is an error" in new TemplateViewTest(
        view = page(error = true),
        title = EmailCaptureMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        error = Some(formError)
      )
    }

    "have a heading" in {
      document().mainContent.getH1Element.text mustBe EmailCaptureMessages.heading
    }

    "have a paragraph" which {
      def paragraph: Element = document().mainContent.selectNth("p", 1)

      "has the correct text" in {
        paragraph.text mustBe EmailCaptureMessages.para
      }
      "has a link which opens in a new tab" in {
        val link = paragraph.selectHead("a")
        link.text mustBe EmailCaptureMessages.hmrcPrivacyNotice
        link.attr("href") mustBe "https://www.gov.uk/government/publications/data-protection-act-dpa-information-hm-revenue-and-customs-hold-about-you/data-protection-act-dpa-information-hm-revenue-and-customs-hold-about-you"
        link.attr("rel") mustBe "noopener noreferrer"
        link.attr("target") mustBe "_blank"
      }
    }

    "have a form" which {
      def form(error: Boolean = false): Element = document(error).mainContent.getForm

      "has the correct attributes" in {
        form().attr("method") mustBe testCall.method
        form().attr("action") mustBe testCall.url
      }

      "has a text input field with a heading label" when {
        "there is no error on the text field" in {
          form().mustHaveTextInput(".govuk-form-group")(
            name = EmailCaptureForm.formKey,
            label = EmailCaptureMessages.heading,
            isLabelHidden = false,
            isPageHeading = true,
            error = None,
            autoComplete = Some("email"),
            spellcheck = Some(false),
            inputType = "email"
          )
        }
        "there is an error in the text field" in {
          form(error = true).mustHaveTextInput(".govuk-form-group")(
            name = EmailCaptureForm.formKey,
            label = EmailCaptureMessages.heading,
            isLabelHidden = false,
            isPageHeading = true,
            error = Some(EmailCaptureMessages.errorInvalid),
            autoComplete = Some("email"),
            spellcheck = Some(false),
            inputType = "email"
          )
        }
      }

      "has a continue button" in {
        form().selectHead(".govuk-button").text mustBe EmailCaptureMessages.continue
      }

    }
  }


  lazy val view: EmailCapture = app.injector.instanceOf[EmailCapture]

  def page(error: Boolean): Html = view(
    emailForm = if (error) {
      EmailCaptureForm.form.withError(formError)
    } else {
      EmailCaptureForm.form
    },
    postAction = testCall,
    backUrl = testBackUrl
  )

  def document(error: Boolean = false): Document = Jsoup.parse(page(error).body)

  lazy val formError: FormError = FormError(EmailCaptureForm.formKey, s"error.agent.${EmailCaptureForm.formKey}.invalid")

  object EmailCaptureMessages {
    val heading: String = "Enter your contact email address"
    val hmrcPrivacyNotice: String = "HMRC Privacy Notice (opens in new tab)"
    val para: String = s"Full details of how we use contact information are in the $hmrcPrivacyNotice"
    val continue: String = "Continue"
    val errorInvalid: String = "Enter an email address in the correct format, like name@example.com"
  }

}
