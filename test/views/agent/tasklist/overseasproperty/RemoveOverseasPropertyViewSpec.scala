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

package views.agent.tasklist.overseasproperty

import forms.agent.RemoveClientOverseasPropertyForm
import forms.agent.RemoveClientOverseasPropertyForm._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.FormError
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.agent.tasklist.overseasproperty.RemoveOverseasPropertyBusiness

class RemoveOverseasPropertyViewSpec extends ViewSpec {

  object RemoveClientOverseasPropertyMessages {
    val title = "Delete foreign property"
    val heading: String = title
    val subheading: String = "Are you sure you want to delete this foreign property business?"
    val paragraph = "All of your client’s current sole trader and property businesses need to be added to Making Tax Digital for Income Tax at the same time. You will need to re-enter this information if you remove it by mistake."
    val continue = "Continue"
    val errorMessage = "Select if you want to delete this property business"
  }

  val removeOverseasProperty: RemoveOverseasPropertyBusiness = app.injector.instanceOf[RemoveOverseasPropertyBusiness]

  val testFormError: FormError = FormError(yesNo, "error.remove-overseas-property-business.invalid")

  def view(hasError: Boolean = false): Html = {
    removeOverseasProperty(
      if (hasError) {
        RemoveClientOverseasPropertyForm.removeClientOverseasPropertyForm.withError(testFormError)
      } else {
        RemoveClientOverseasPropertyForm.removeClientOverseasPropertyForm
      },
      postAction = testCall
    )
  }

  class ViewTest(hasError: Boolean = false) {

    val document: Document = Jsoup.parse(view(
      hasError = hasError
    ).body)

  }

  "Remove Client Overseas Property Confirmation view" should {

    "display the template correctly" when {
      "there is an error" in new TemplateViewTest(
        view = view(hasError = true),
        title = RemoveClientOverseasPropertyMessages.title,
        isAgent = true,
        hasSignOutLink = true,
        error = Some(testFormError)
      )
      "there is no error" in new TemplateViewTest(
        view = view(),
        title = RemoveClientOverseasPropertyMessages.title,
        isAgent = true,
        hasSignOutLink = true
      )
    }

    "have the correct yes-no radio inputs" when {
      "there is no error" in new ViewTest(hasError = false) {
        document.mustHaveYesNoRadioInputs(selector = "fieldset")(
          name = "yes-no",
          legend = RemoveClientOverseasPropertyMessages.subheading,
          isHeading = false,
          isLegendHidden = false,
          hint = None,
          errorMessage = None
        )
      }
      "there is an error" in new ViewTest(hasError = true) {
        document.mustHaveYesNoRadioInputs(selector = "fieldset")(
          name = "yes-no",
          legend = RemoveClientOverseasPropertyMessages.subheading,
          isHeading = false,
          isLegendHidden = false,
          hint = None,
          errorMessage = Some(RemoveClientOverseasPropertyMessages.errorMessage)
        )
      }
    }

    "have a paragraph" in new ViewTest {
      document.mainContent.selectNth("p", 1).text mustBe RemoveClientOverseasPropertyMessages.paragraph
    }

    "have a continue button" in new ViewTest {
      document.mainContent.selectHead(".govuk-button").text mustBe RemoveClientOverseasPropertyMessages.continue
    }

  }
}