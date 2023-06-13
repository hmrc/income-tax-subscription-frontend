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

package views.agent.business

import forms.agent.ClientRemoveUkPropertyForm
import forms.agent.ClientRemoveUkPropertyForm._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.FormError
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.agent.business.ClientRemoveUkProperty

class ClientRemoveUkPropertyViewSpec extends ViewSpec {

  object ClientRemoveUkPropertyMessages {
    val title = "Are you sure you want to delete your client’s UK property business?"
    val heading: String = title
    val hint = "All your client’s current sole trader and property businesses need to be added to Making Tax Digital for Income Tax at the same time. You will need to re-enter this information if you remove it by mistake."
    val backLink = "Back"
    val agreeAndContinue = "Agree and continue"
  }

  val removeUkProperty: ClientRemoveUkProperty = app.injector.instanceOf[ClientRemoveUkProperty]

  val testFormError: FormError = FormError(yesNo, "agent.error.remove-uk-property-business.invalid")

  def view(hasError: Boolean = false): Html = {
    removeUkProperty(
      if (hasError) {
        ClientRemoveUkPropertyForm.removeUkPropertyForm.withError(testFormError)
      } else {
        ClientRemoveUkPropertyForm.removeUkPropertyForm
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

  "Client Remove Uk property confirmation view" should {

    "display the template correctly" when {
      "there is an error" in new TemplateViewTest(
        view = view(hasError = true),
        title = ClientRemoveUkPropertyMessages.title,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true,
        error = Some(testFormError)
      )
      "there is no error" in new TemplateViewTest(
        view = view(false),
        title = ClientRemoveUkPropertyMessages.title,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
    }

    "have a fieldset" when {
      "there is an error" should {
        "have a legend with the page heading" in new ViewTest(true) {
          document.getElementsByClass("govuk-fieldset__legend").text mustBe ClientRemoveUkPropertyMessages.heading
        }

        "have a hint" in new ViewTest(true) {
          document.selectHead(".govuk-hint").text mustBe ClientRemoveUkPropertyMessages.hint
        }
      }


      "have a legend and the page heading" in new ViewTest {
        document.selectHead("fieldset").selectHead("legend").text mustBe ClientRemoveUkPropertyMessages.heading
      }

      "have a hint" in new ViewTest {
        document.selectHead("fieldset").selectHead(".govuk-hint").text mustBe ClientRemoveUkPropertyMessages.hint
      }
    }

    "have radio button yes and no" in new ViewTest {
      document.mustHaveYesNoRadioInputs("yes-no")
    }

    "have a agree and continue button" in new ViewTest {
      document.mainContent.selectHead(".govuk-button").text mustBe ClientRemoveUkPropertyMessages.agreeAndContinue
    }

  }
}
