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
    val title = "Are you sure you want to delete your client’s foreign property business?"
    val heading: String = title
    val hint = "All your client’s current sole trader and property businesses need to be added to Making Tax Digital for Income Tax at the same time. You will need to re-enter this information if you remove it by mistake."
    val backLink = "Back"
    val agreeAndContinue = "Agree and continue"
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
      postAction = testCall,
      backUrl = testBackUrl
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
        backLink = Some(testBackUrl),
        hasSignOutLink = true,
        error = Some(testFormError)
      )
      "there is no error" in new TemplateViewTest(
        view = view(),
        title = RemoveClientOverseasPropertyMessages.title,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
    }

    "have a fieldset" when {
      "there is an error" should {
        "have a legend with the page heading" in new ViewTest(true) {
          document.getElementsByClass("govuk-fieldset__legend").text mustBe RemoveClientOverseasPropertyMessages.heading
        }
      }
    }

    "have the correct yes-no radio inputs" in new ViewTest {
      document.mustHaveYesNoRadioInputs(selector = "fieldset")(
        name = "yes-no",
        legend = RemoveClientOverseasPropertyMessages.heading,
        isHeading = false,
        isLegendHidden = false,
        hint = Some(RemoveClientOverseasPropertyMessages.hint),
        errorMessage = None
      )
    }

    "have a agree and continue button" in new ViewTest {
      document.mainContent.selectHead(".govuk-button").text mustBe RemoveClientOverseasPropertyMessages.agreeAndContinue
    }

  }
}