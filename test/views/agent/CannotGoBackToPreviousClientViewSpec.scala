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

package views.agent

import agent.assets.MessageLookup
import forms.agent.CannotGoBackToPreviousClientForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.data.FormError
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utilities.ViewSpec
import views.html.agent.CannotGoBackToPreviousClient

class CannotGoBackToPreviousClientViewSpec extends ViewSpec {

  private val cannotGoBackToPreviousClient: CannotGoBackToPreviousClient = app.injector.instanceOf[CannotGoBackToPreviousClient]


  val testFormError: FormError = FormError(CannotGoBackToPreviousClientForm.cannotGoBackToPreviousClient, "agent.cannot-go-back-previous-client.error.empty")

  "cannot go back to previous client" must {
    "have the correct template details" when {
      "the page has no error" in new TemplateViewTest(
        view = page(),
        isAgent = true,
        title = CannotGoBack.heading
      )

      "the page has error" in new TemplateViewTest(
        view = page(hasError = true),
        isAgent = true,
        title = CannotGoBack.heading,
        error = Some(testFormError)
      )
    }

    "have a heading" in {
      document().getH1Element.text mustBe CannotGoBack.heading
    }

    "have a body" which {
      val paragraphs: Elements = document().select(".govuk-body").select("p")

      "has paragraph " in {
        paragraphs.get(0).text() mustBe CannotGoBack.radioOptionHeading
      }
    }


    "have a form" which {
      def form = document().selectHead("form")
      "has correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a set of radio inputs" in {
        form.mustHaveRadioInput(
          name = CannotGoBackToPreviousClientForm.cannotGoBackToPreviousClient,
          radioItems = Seq(
            RadioItem(
              content = Text(CannotGoBack.agentServiceAccountOptionText),
              value = Some(CannotGoBack.agentServiceAccountOptionLabel),
              id = Some(CannotGoBackToPreviousClientForm.cannotGoBackToPreviousClient)
            ),
            RadioItem(
              content = Text(CannotGoBack.reenterClientDetailsOptionText),
              value = Some(CannotGoBack.reenterClientDetailsOptionLabel),
              id = Some(s"${CannotGoBackToPreviousClientForm.cannotGoBackToPreviousClient}-2")
            ),
            RadioItem(
              content = Text(CannotGoBack.signUpAnotherClientOptionText),
              value = Some(CannotGoBack.signUpAnotherClientOptionLabel),
              id = Some(s"${CannotGoBackToPreviousClientForm.cannotGoBackToPreviousClient}-3")
            )
          )
        )
      }

      "has a continue button" in {
          document().select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
      }
    }

  }

  private def page(hasError: Boolean = false): Html =
    cannotGoBackToPreviousClient(
      if (hasError) CannotGoBackToPreviousClientForm.cannotGoBackToPreviousClientForm.withError(testFormError) else CannotGoBackToPreviousClientForm.cannotGoBackToPreviousClientForm,
      postAction = testCall
    )

  private def document(hasError: Boolean = false): Document =
    Jsoup.parse(page(hasError).body)

  private object CannotGoBack {
    val heading = "You cannot go back to previous client"
    val radioOptionHeading = "To manage your client’s information you can:"
    val agentServiceAccountOptionLabel = "agent-service-account"
    val agentServiceAccountOptionText = "Go to your agent service account, if you finished signing the client up"
    val reenterClientDetailsOptionLabel = "re-enter-client-details"
    val reenterClientDetailsOptionText = "Re-enter the details of a client you did not finish signing up"
    val signUpAnotherClientOptionLabel = "sign-up-another-client"
    val signUpAnotherClientOptionText = "Sign up another client"

  }

}
