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

import forms.individual.business.RemoveOverseasPropertyForm
import forms.individual.business.RemoveOverseasPropertyForm.yesNo
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.incometax.business.RemoveOverseasProperty

class RemoveOverseasPropertyViewSpec extends ViewSpec {

  val removeOverseasProperty: RemoveOverseasProperty = app.injector.instanceOf[RemoveOverseasProperty]

  val testFormError: FormError = FormError(yesNo, "test message")

  def view(hasError: Boolean = false): Html = {
    removeOverseasProperty(
      if (hasError) {
        RemoveOverseasPropertyForm.removeOverseasPropertyForm.withError(testFormError)
      } else {
        RemoveOverseasPropertyForm.removeOverseasPropertyForm
      },
      postAction = testCall,
      backUrl = testBackUrl
    )
  }

  class ViewTest(hasError: Boolean = false) {

    val document: Document = Jsoup.parse(view(
      hasError = hasError
    ).body)

    val mainContent: Element = document.mainContent

  }

  object RemoveOverseasPropertyMessages {
    val heading: String = "Are you sure you want to delete your overseas property business?"
    val hint: String = "All your current sole trader and property businesses need to be added to Making Tax Digital for Income Tax at the same time. You will need to re-enter this information if you remove it by mistake."
    val agreeAndContinue = "Agree and continue"
  }

  "RemoveOverseasProperty view" must {

    "display the template correctly" when {
      "there is an error" in new TemplateViewTest(
        view = view(hasError = true),
        title = RemoveOverseasPropertyMessages.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
        error = Some(testFormError)
      )
      "there is no error" in new TemplateViewTest(
        view = view(),
        title = RemoveOverseasPropertyMessages.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
      )
    }

    "have a form with the correct attributes" which {

      "has a fieldset" which {

        "is described correctly" when {
          "there is no error" in {

          }
          "there is an error" in {

          }
        }

        "has a legend" that {
          "is the heading for the page" in new ViewTest {
            mainContent.getForm.getFieldset.getH1Element.text mustBe RemoveOverseasPropertyMessages.heading
          }
        }

        "has a hint" in new ViewTest {
          mainContent.getHintText
        }

        "has a set of yes no radio buttons" in new ViewTest {
          mainContent.getForm.getFieldset.mustHaveYesNoRadioInputs(RemoveOverseasPropertyForm.yesNo)
        }
      }

      "have a Accept and Continue button" in new ViewTest {
        mainContent.getForm.getGovukSubmitButton.text mustBe RemoveOverseasPropertyMessages.agreeAndContinue
      }

    }
  }
}
