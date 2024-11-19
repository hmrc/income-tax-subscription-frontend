/*
 * Copyright 2024 HM Revenue & Customs
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

package views.individual

import forms.individual.UsingSoftwareForm
import messagelookup.individual.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import utilities.ViewSpec
import views.html.individual.UsingSoftware

class UsingSoftwareViewSpec extends ViewSpec {

  private val usingSoftware: UsingSoftware = app.injector.instanceOf[UsingSoftware]

  private val testFormError: FormError = FormError(UsingSoftwareForm.fieldName, "individual.using-software.form-error")

  "using software" must {
    import UsingSoftwareMessages._

    "have the correct template details" when {

      "the page has no error" in new TemplateViewTest(
        view = page(),
        isAgent = false,
        title = heading
      )

      "the page has an error" in new TemplateViewTest(
        view = page(hasError = true),
        isAgent = false,
        title = heading,
        error = Some(testFormError)
      )
    }

    "have a heading" in {
      document().getH1Element.text mustBe heading
    }

    "have the correct first paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe UsingSoftwareMessages.paraOne
    }

    "have the correct second paragraph" in {
      document().mainContent.selectNth("p", 2).text mustBe UsingSoftwareMessages.paraTwo
    }

    "have the correct third paragraph" in {
      document().mainContent.selectNth("p", 3).text mustBe UsingSoftwareMessages.paraThree
    }

    "have a link " which {
      val link = document().mainContent.selectNth("p",4).selectHead("a")
      "displays the correct text" in {
        link.text mustBe linkText
      }
      "redirects to the correct url" in {
        link.attr("href") mustBe linkUrl
      }

    }

    "have a form" which {
      def form: Element = document().selectHead("form")

      "has correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has the correct radio inputs" in {
        form.mustHaveYesNoRadioInputs(selector = "fieldset")(
          name = radioName,
          legend = UsingSoftwareMessages.radioLegend,
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

  private def page(hasError: Boolean = false): Html = {
    usingSoftware(
      if (hasError) UsingSoftwareForm.usingSoftwareForm.withError(testFormError) else UsingSoftwareForm.usingSoftwareForm,
      testCall
    )
  }

  private def document(hasError: Boolean = false): Document =
    Jsoup.parse(page(hasError).body)

  private object UsingSoftwareMessages {
    val heading: String = "Check you have compatible software"
    val paraOne: String = "To use this service, you must use software that works with Making Tax Digital for Income Tax."
    val paraTwo: String = "If someone helps you with your tax (like an agent), check which software they want you to use."
    val paraThree: String = "If you already use software to keep digital records, you may need to ask your software provider if it works with Making Tax Digital for Income Tax."
    val linkText: String = "Find software that works with Making Tax Digital for Income Tax (opens in new tab)"
    val linkUrl: String = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
    val radioName: String = "yes-no"
    val radioLegend: String = "Are you using software that works with Making Tax Digital for Income Tax?"
  }
}
