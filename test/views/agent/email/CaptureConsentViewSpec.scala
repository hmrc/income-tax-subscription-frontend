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

    "have a heading" in {
      document().mainContent.getH1Element.text mustBe CaptureConsentMessages.heading
    }

    "have a subheading" in {
      document().mainContent.selectHead("h2").text mustBe CaptureConsentMessages.subheading
    }

    "have a bullet list detailing what will be asked about" which {
      def bulletList: Element = document().mainContent.selectNth("ul", 1)

      "has a paragraph before describing the bullets" in {
        document().mainContent.selectNth("p", 1).text mustBe CaptureConsentMessages.AskAbout.para
      }
      "has a first bullet point" in {
        bulletList.selectNth("li", 1).text mustBe CaptureConsentMessages.AskAbout.bulletOne
      }
      "has a second bullet point" in {
        bulletList.selectNth("li", 2).text mustBe CaptureConsentMessages.AskAbout.bulletTwo
      }
      "has a third bullet point" in {
        bulletList.selectNth("li", 3).text mustBe CaptureConsentMessages.AskAbout.bulletThree
      }
    }

    "have a bullet list detailing what they will be updated on" which {
      def bulletList: Element = document().mainContent.selectNth("ul", 2)

      "has a paragraph before describing the bullets" in {
        document().mainContent.selectNth("p", 2).text mustBe CaptureConsentMessages.UpdateOn.para
      }
      "has a first bullet point" in {
        bulletList.selectNth("li", 1).text mustBe CaptureConsentMessages.UpdateOn.bulletOne
      }
      "has a second bullet point" in {
        bulletList.selectNth("li", 2).text mustBe CaptureConsentMessages.UpdateOn.bulletTwo
      }
      "has a third bullet point" in {
        bulletList.selectNth("li", 3).text mustBe CaptureConsentMessages.UpdateOn.bulletThree
      }
      "has a forth bullet point" in {
        bulletList.selectNth("li", 4).text mustBe CaptureConsentMessages.UpdateOn.bulletFour
      }
    }

    "have a bullet list detailing what will never happen" which {
      def bulletList: Element = document().mainContent.selectNth("ul", 3)

      "has a paragraph before describing the bullets" in {
        document().mainContent.selectNth("p", 3).text mustBe CaptureConsentMessages.WillNever.para
      }
      "has a first bullet point" in {
        bulletList.selectNth("li", 1).text mustBe CaptureConsentMessages.WillNever.bulletOne
      }
      "has a second bullet point" in {
        bulletList.selectNth("li", 2).text mustBe CaptureConsentMessages.WillNever.bulletTwo
      }
    }

    "have a end paragraph" in {
      document().mainContent.selectNth("p", 4).text mustBe CaptureConsentMessages.paraEnd
    }

    "have a form" which {
      def form(hasError: Boolean = false): Element = document(hasError = hasError).selectHead("form")

      "has correct attributes" in {
        form().attr("method") mustBe testCall.method
        form().attr("action") mustBe testCall.url
      }

      "has the correct radio inputs" when {
        "there is no error" in {
          form().mustHaveYesNoRadioInputs(selector = "fieldset")(
            name = CaptureConsentMessages.radioName,
            legend = CaptureConsentMessages.radioLegend,
            isHeading = false,
            isLegendHidden = false,
            hint = None,
            errorMessage = None,
          )
        }
        "there is a form error" in {
          form(hasError = true).mustHaveYesNoRadioInputs(selector = "fieldset")(
            name = CaptureConsentMessages.radioName,
            legend = CaptureConsentMessages.radioLegend,
            isHeading = false,
            isLegendHidden = false,
            hint = None,
            errorMessage = Some(CaptureConsentMessages.errorInvalid),
          )
        }
      }

      "has a continue button" in {
        form().getGovukSubmitButton.text mustBe MessageLookup.Base.continue
      }
    }
  }


  private val captureConsent: CaptureConsent = app.injector.instanceOf[CaptureConsent]
  private val fullName = "FirstName LastName"
  private val nino = "ZZ 11 11 11 Z"

  private lazy val testFormError: FormError = FormError(CaptureConsentForm.fieldName, "agent.capture-consent.form-error")

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
    val heading: String = "Help HMRC improve Making Tax Digital for Income Tax"
    val paraOne: String = "We would like to ask for your help to improve Making Tax Digital for Income Tax."
    val subheading: String = "What we will contact you about"

    object AskAbout {
      val para: String = "We will ask:"
      val bulletOne: String = "for your feedback on your experience using Making Tax Digital for Income Tax"
      val bulletTwo: String = "what software you’re using with Making Tax Digital for Income Tax"
      val bulletThree: String = "to arrange one-on-one contact with you for optional user research sessions"
    }

    object UpdateOn {
      val para: String = "We will update you on:"
      val bulletOne: String = "issues affecting your use of Making Tax Digital for Income Tax"
      val bulletTwo: String = "how your feedback is helping to improve Making Tax Digital for Income Tax"
      val bulletThree: String = "new features added to Making Tax Digital for Income Tax"
      val bulletFour: String = "when to submit your client’s quarterly updates"
    }

    object WillNever {
      val para: String = "We will never:"
      val bulletOne: String = "contact you or your client to ask for your personal information"
      val bulletTwo: String = "send you or your client a direct link to sign into Making Tax Digital for Income Tax"
    }

    val paraEnd: String = "If you change your mind about us contacting you, you can withdraw your consent at any time. Information on how to withdraw your consent will be included in the emails we send to you."

    val errorInvalid: String = "Select yes if we can contact you by email about Making Tax Digital for Income Tax."
    val radioName: String = "yes-no"
    val radioLegend: String = "Can HMRC contact you by email about Making Tax Digital for Income Tax?"
  }
}
