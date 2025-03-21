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

    "have a form" which {
      def form(error: Boolean = false): Element = document(error).getForm

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
            hint = Some(EmailCaptureMessages.hint),
            error = None,
            autoComplete = Some("email")
          )
        }
        "there is an error in the text field" in {
          form(error = true).mustHaveTextInput(".govuk-form-group")(
            name = EmailCaptureForm.formKey,
            label = EmailCaptureMessages.heading,
            isLabelHidden = false,
            isPageHeading = true,
            hint = Some(EmailCaptureMessages.hint),
            error = Some(EmailCaptureMessages.errorInvalid),
            autoComplete = Some("email")
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
    val heading: String = "Enter your email address"
    val hint: String = "Email hint"
    val continue: String = "Continue"
    val errorInvalid: String = "Email is invalid error"
  }

}
