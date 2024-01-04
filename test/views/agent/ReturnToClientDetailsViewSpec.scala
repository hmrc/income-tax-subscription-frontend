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

import forms.agent.ReturnToClientDetailsForm
import messagelookup.agent.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.FormError
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utilities.ViewSpec
import views.html.agent.matching.ReturnToClientDetails

class ReturnToClientDetailsViewSpec extends ViewSpec {

  private val returnToClientDetails: ReturnToClientDetails = app.injector.instanceOf[ReturnToClientDetails]


  val testFormError: FormError = FormError(ReturnToClientDetailsForm.returnToClientDetails, "agent.return-to-client-details.error.empty")

  "return to client details page" must {
    "have the correct template details" when {
      "the page has no error" in new TemplateViewTest(
        view = page(),
        isAgent = true,
        title = ReturnToClientDetails.heading
      )

      "the page has error" in new TemplateViewTest(
        view = page(hasError = true),
        isAgent = true,
        title = ReturnToClientDetails.heading,
        error = Some(testFormError)
      )
    }

    "have a heading" in {
      document().getH1Element.text mustBe ReturnToClientDetails.heading
    }


    "have a form" which {
      def form = document().selectHead("form")

      "has correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a set of radio inputs" in {
        form.mustHaveRadioInput(
          name = ReturnToClientDetailsForm.returnToClientDetails,
          radioItems = Seq(
            RadioItem(
              content = Text(ReturnToClientDetails.continueWithCurrentClientOptionText),
              value = Some(ReturnToClientDetails.continueWithCurrentClientOptionLabel),
              id = Some(ReturnToClientDetailsForm.returnToClientDetails)
            ),
            RadioItem(
              content = Text(ReturnToClientDetails.signUpAnotherClientOptionText),
              value = Some(ReturnToClientDetails.signUpAnotherClientOptionLabel),
              id = Some(s"${ReturnToClientDetailsForm.returnToClientDetails}-2")
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
    returnToClientDetails(
      if (hasError) ReturnToClientDetailsForm.returnToClientDetailsForm.withError(testFormError) else ReturnToClientDetailsForm.returnToClientDetailsForm,
      postAction = testCall,
      ReturnToClientDetails.clientName
    )

  private def document(hasError: Boolean = false): Document =
    Jsoup.parse(page(hasError).body)

  private object ReturnToClientDetails {
    val clientName = "FirstName LastName"
    val heading = "What do you want to do next?"
    val continueWithCurrentClientOptionLabel = "continue-signing-up"
    val continueWithCurrentClientOptionText = s"Continue signing up $clientName"
    val signUpAnotherClientOptionLabel = "sign-up-another-client"
    val signUpAnotherClientOptionText = "Sign up another client"

  }

}
