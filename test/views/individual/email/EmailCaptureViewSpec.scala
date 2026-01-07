/*
 * Copyright 2023 HM Revenue & Customs
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

package views.individual.email

import forms.individual.email.EmailCaptureForm
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.email.EmailCapture

class EmailCaptureViewSpec extends ViewSpec {

  "EmailCapture" must {
    "be using the correct template" when {
      "there is no error" in new TemplateViewTest(
        view = page(error = false),
        title = EmailCaptureMessages.heading,
        backLink = Some(testBackUrl),
        error = None
      )
      "there is an error" in new TemplateViewTest(
        view = page(error = true),
        title = EmailCaptureMessages.heading,
        backLink = Some(testBackUrl),
        error = Some(formError)
      )
    }

    "have a form" which {
      def form(error: Boolean = false): Element = document(error).getForm

      "has the correct attributes" in {
        form().attr("method") mustBe testCall.method
        form().attr("action") mustBe testCall.url
      }

      "contains a heading" in {
        document().mainContent.select("h1").text() mustBe EmailCaptureMessages.heading
      }

      "has a text input field with a heading label" when {
        "there is no error on the text field" in {
          form().mustHaveTextInput(".govuk-form-group")(
            name = EmailCaptureForm.formKey,
            label = "",
            isLabelHidden = false,
            isPageHeading = false,
            error = None,
            autoComplete = Some("email"),
            spellcheck = Some(false),
            inputType = "email"
          )
        }
        "there is an error on the text field" in {
          form(error = true).mustHaveTextInput(".govuk-form-group")(
            name = EmailCaptureForm.formKey,
            label = "",
            isLabelHidden = false,
            isPageHeading = false,
            error = Some(EmailCaptureMessages.errorInvalid),
            autoComplete = Some("email"),
            spellcheck = Some(false),
            inputType = "email"
          )
        }
      }

      "contains a paragraph and with a link for privacy notice" in {
        document().mainContent.selectNth("p", 1).text mustBe EmailCaptureMessages.para
        val link = document().mainContent.selectNth(".govuk-link", 1)
        link.text mustBe EmailCaptureMessages.linkTextTwo
        link.attr("href") mustBe "https://www.gov.uk/government/publications/data-protection-act-dpa-information-hm-revenue-and-customs-hold-about-you/data-protection-act-dpa-information-hm-revenue-and-customs-hold-about-you"
      }

      "has a continue button" in {
        form().selectHead(".govuk-button").text mustBe EmailCaptureMessages.continue
      }
    }
  }

  def document(error: Boolean = false): Document = Jsoup.parse(page(error).body)

  def page(error: Boolean): Html = view(
    emailForm = if (error) {
      EmailCaptureForm.form.withError(formError)
    } else {
      EmailCaptureForm.form
    },
    postAction = testCall,
    backUrl = testBackUrl
  )

  lazy val formError: FormError = FormError(EmailCaptureForm.formKey, s"error.individual.${EmailCaptureForm.formKey}.invalid")

  lazy val view: EmailCapture = app.injector.instanceOf[EmailCapture]

  object EmailCaptureMessages {
    val heading: String = "Enter your contact email address"
    val para: String = "Full details of how we use contact information are in the HMRC Privacy Notice (opens in new tab)"
    val linkTextTwo: String = "HMRC Privacy Notice (opens in new tab)"
    val continue: String = "Continue"
    val errorInvalid: String = "Enter an email address in the correct format, like name@example.com"
  }

}
