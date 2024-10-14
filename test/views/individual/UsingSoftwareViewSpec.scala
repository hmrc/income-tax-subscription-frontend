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
    import UsingSoftware._

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

    "have a link " in {
      val link = document().mainContent.getNthParagraph(1).selectHead("a")
      link.text mustBe linkText
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
          legend = heading,
          isHeading = false,
          isLegendHidden = true,
          hint = None,
          errorMessage = None,
          yesHintId = Some(s"$radioName-hint"),
          yesHint = Some(Text(yesHint)),
          noHintId = Some(s"$radioName-hint"),
          noHint = Some(Text(noHint)),
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

  private object UsingSoftware {
    val heading = "Are you using software that works with Making Tax Digital for Income Tax?"
    val linkText = "Find software that works with Making Tax Digital for Income Tax (opens in new tab)"
    val radioName = "yes-no"
    val yesHint = "You’re using software to keep digital records and that software works with Making Tax Digital for Income Tax"
    val noHint: String = "You are not using software to keep digital records." +
      " Or you use software to keep digital records but that software " +
      "does not work with Making Tax Digital for Income Tax"
  }

}
