/*
 * Copyright 2022 HM Revenue & Customs
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

package views.individual.incometax.business

import forms.individual.business.RemoveUkPropertyForm
import forms.individual.business.RemoveUkPropertyForm.yesNo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.FormError
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.incometax.business.RemoveUkProperty

class RemoveUkPropertyViewSpec extends ViewSpec {

  object RemoveUkPropertyMessages {
    val title = "Are you sure you want to delete your UK property business?"
    val heading: String = title
    val hint = "All your current sole trader and property businesses need to be added to Making Tax Digital for " +
      "Income Tax at the same time. You will need to re-enter this information if you remove it by mistake."
    val backLink = "Back"
    val agreeAndContinue = "Agree and continue"
  }

  val removeUkProperty: RemoveUkProperty = app.injector.instanceOf[RemoveUkProperty]

  val testFormError: FormError = FormError(yesNo, "test message")

  def view(hasError: Boolean = false): Html = {
    removeUkProperty(
      if (hasError) {
        RemoveUkPropertyForm.removeUkPropertyForm.withError(testFormError)
      } else {
        RemoveUkPropertyForm.removeUkPropertyForm
      },
      postAction = testCall,
      backUrl = testBackUrl
    )
  }

  class ViewTest(hasError: Boolean = false) {

    val document: Document = Jsoup.parse(view(
      hasError = hasError
    ).body)

  }

  "Remove Uk property confirmation view" should {

    "display the template correctly" when {
      "there is an error" in new TemplateViewTest(
        view = view(hasError = true),
        title = RemoveUkPropertyMessages.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        error = Some(testFormError)
      )
      "there is no error" in new TemplateViewTest(
        view = view(false),
        title = RemoveUkPropertyMessages.title,
        isAgent = false,
        backLink = Some(testBackUrl)
      )
    }

    "have a fieldset" which {
      "have a legend with the page heading" when {
        "there is an error" in new ViewTest(true) {
          document.selectHead(".govuk-fieldset__heading").text mustBe RemoveUkPropertyMessages.heading
        }
      }
      "have a hint" when {
        "there is an error" in new ViewTest(true) {
          document.selectHead(".govuk-hint").text mustBe RemoveUkPropertyMessages.hint
        }
      }
    }
    "have a legend with the page heading" in new ViewTest {
      document.selectHead("fieldset").selectHead("legend").text mustBe RemoveUkPropertyMessages.heading
    }
    "have a hint" in new ViewTest {
      document.selectHead("fieldset").selectHead(".govuk-hint").text mustBe RemoveUkPropertyMessages.hint
    }


    "have a agree and continue button" in new ViewTest {
      document.selectHead(".govuk-button").text mustBe RemoveUkPropertyMessages.agreeAndContinue
    }

  }
}
