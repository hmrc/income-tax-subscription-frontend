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

package views.individual.email

import forms.individual.email.CaptureConsentForm
import messagelookup.individual.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.email.CaptureConsent

class CaptureConsentViewSpec extends ViewSpec {

  "CaptureConsent" must {
    "be using the correct template" when {

      "the page has no error" in new TemplateViewTest(
        view = page(),
        isAgent = false,
        title = CaptureConsentMessages.heading
      )

      "the page has an error" in new TemplateViewTest(
        view = page(hasError = true),
        isAgent = false,
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

      "contains a subheading" in {
        document().mainContent.select("h2").text() mustBe CaptureConsentMessages.subheading
      }

      "contains the first paragraph" in {
        document().mainContent.selectNth("p", 1).text mustBe CaptureConsentMessages.para1
      }

      "contains the second paragraph" in {
        document().mainContent.selectNth("p", 2).text mustBe CaptureConsentMessages.para2
      }

      "contains a bullet list of what we will ask" which {
        def bulletList = document().mainContent.selectNth("ul", 1)

        "has a first item" in {
          bulletList.selectNth("li", 1).text mustBe CaptureConsentMessages.bullet1
        }
        "has a second item" in {
          bulletList.selectNth("li", 2).text mustBe CaptureConsentMessages.bullet2
        }
        "has a third item" in {
          bulletList.selectNth("li", 3).text mustBe CaptureConsentMessages.bullet3
        }
      }

      "contains a third paragraph" in {
        document().mainContent.selectNth("p", 3).text mustBe CaptureConsentMessages.para3
      }

      "contains a bullet list of what we will update" which {
        def bulletList = document().mainContent.selectNth("ul", 2)

        "has a first item" in {
          bulletList.selectNth("li", 1).text mustBe CaptureConsentMessages.bullet4
        }
        "has a second item" in {
          bulletList.selectNth("li", 2).text mustBe CaptureConsentMessages.bullet5
        }
        "has a third item" in {
          bulletList.selectNth("li", 3).text mustBe CaptureConsentMessages.bullet6
        }
        "has a fourth item" in {
          bulletList.selectNth("li", 4).text mustBe CaptureConsentMessages.bullet7
        }
      }

      "contains a fourth paragraph" in {
        document().mainContent.selectNth("p", 4).text mustBe CaptureConsentMessages.para4
      }

      "contains a bullet list of what we will never do" which {
        def bulletList = document().mainContent.selectNth("ul", 3)

        "has a first item" in {
          bulletList.selectNth("li", 1).text mustBe CaptureConsentMessages.bullet8
        }
        "has a second item" in {
          bulletList.selectNth("li", 2).text mustBe CaptureConsentMessages.bullet9
        }
      }

      "contains a fifth paragraph" in {
        document().mainContent.selectNth("p", 5).text mustBe CaptureConsentMessages.para5
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

  private val testFormError: FormError = FormError(CaptureConsentForm.fieldName, "individual.capture-consent.form-error")

  private def page(hasError: Boolean = false): Html = {
    captureConsent(
      if (hasError) CaptureConsentForm.captureConsentForm.withError(testFormError) else CaptureConsentForm.captureConsentForm,
      testCall,
      controllers.individual.accountingperiod.routes.AccountingPeriodController.show.url
    )
  }

  private def document(hasError: Boolean = false): Document =
    Jsoup.parse(page(hasError).body)

  private object CaptureConsentMessages {
    val heading: String = "Can HMRC contact you by email about Making Tax Digital for Income Tax?"
    val errorInvalid: String = "Select yes if we can contact you by email about Making Tax Digital for Income Tax."
    val radioName: String = "yes-no"
    val radioLegend: String = "Can HMRC contact you by email about Making Tax Digital for Income Tax?"
    val subheading: String = "What we will contact you about"
    val para1: String = "We would like to ask for your help to improve Making Tax Digital for Income Tax."
    val para2: String = "We will ask:"
    val para3: String = "We will update you on:"
    val para4: String = "We will never:"
    val para5: String = "If you change your mind about us contacting you, you can withdraw your consent at any time. Information on how to withdraw your consent will be included in the emails we send to you."
    val bullet1: String = "for your feedback on your experience using Making Tax Digital for Income Tax"
    val bullet2: String = "what software you’re using with Making Tax Digital for Income Tax"
    val bullet3: String = "to arrange one-on-one contact with you for optional user research sessions"
    val bullet4: String = "issues affecting your use of Making Tax Digital for Income Tax"
    val bullet5: String = "how your feedback is helping to improve Making Tax Digital for Income Tax"
    val bullet6: String = "new features added to Making Tax Digital for Income Tax"
    val bullet7: String = "when to submit your quarterly updates"
    val bullet8: String = "contact you to ask for your personal information"
    val bullet9: String = "send you a direct link to sign into Making Tax Digital for Income Tax"
  }
}
