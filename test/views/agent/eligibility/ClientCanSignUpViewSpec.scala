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

package views.agent.eligibility

import forms.agent.ClientCanSignUpForm
import models.{No, Yes}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utilities.ViewSpec
import views.html.agent.eligibility.ClientCanSignUp

class ClientCanSignUpViewSpec extends ViewSpec {

  private val view = app.injector.instanceOf[ClientCanSignUp]
  val testFormError: FormError = FormError(ClientCanSignUpForm.fieldName, "error.agent.client-can-sign-up.invalid")
  val clientName: String = "FirstName LastName"
  val clientNino: String = "AA 11 11 11 A"

  class ViewTest(hasError: Boolean = false) {
    val page: HtmlFormat.Appendable = view(
      clientCanSignUpForm = if (hasError) {
        ClientCanSignUpForm.clientCanSignUpForm.withError(testFormError)
      } else {
        ClientCanSignUpForm.clientCanSignUpForm
      },
      postAction = testCall,
      clientName = clientName,
      clientNino = clientNino,
      backUrl = testBackUrl
    )

    val document: Document = Jsoup.parse(page.body)
    val mainContent: Element = document.mainContent
  }

  "Client Can Sign Up View" should {

    "be using the correct template details" when {
      "There is no error" in new ViewTest {
        new TemplateViewTest(
          view = page,
          title = ClientCanSignUpMessages.heading,
          isAgent = true,
          backLink = Some(testBackUrl),
          backLinkText = Some(ClientCanSignUpMessages.backLinkText),
          hasSignOutLink = true
        )
      }
      "There is an error" in new ViewTest(hasError = true) {
        new TemplateViewTest(
          view = page,
          title = ClientCanSignUpMessages.heading,
          isAgent = true,
          backLink = Some(testBackUrl),
          backLinkText = Some(ClientCanSignUpMessages.backLinkText),
          hasSignOutLink = true,
          error = Some(testFormError)
        )
      }
    }

    "have a heading caption" in new ViewTest {
      mainContent.selectHead("span.govuk-caption-l").text mustBe s"$clientName | $clientNino"
    }

    "have a heading" in new ViewTest {
      mainContent.selectFirst("h1").text() mustBe ClientCanSignUpMessages.heading
    }

    "have a form with the correct attributes" which {
      "has a legend" that {
        "is the question for the page" in new ViewTest {
          mainContent.getForm.getFieldset.getElementsByClass("govuk-fieldset__legend").text mustBe ClientCanSignUpMessages.question
        }
      }

      "has a set of radio buttons" in new ViewTest {
        mainContent.getForm.getFieldset.mustHaveRadioInput(
          name = ClientCanSignUpForm.fieldName,
          radioItems = Seq(
            RadioItem(
              id = Some(s"${ClientCanSignUpForm.fieldName}"),
              content = Text(ClientCanSignUpMessages.optionYes("FirstName LastName")),
              value = Some(Yes.toString)
            ),
            RadioItem(
              id = Some(s"${ClientCanSignUpForm.fieldName}-2"),
              content = Text(ClientCanSignUpMessages.optionNo),
              value = Some(No.toString)
            )
          )
        )
      }
    }

    "have a continue button" in new ViewTest {
      mainContent.getForm.getGovukSubmitButton.text mustBe ClientCanSignUpMessages.continueButton
    }

  }

  object ClientCanSignUpMessages {
    val heading = "You can sign up this client"
    val question = "What do you want to do next?"

    def optionYes(clientName: String): String = s"Sign up $clientName"

    val optionNo = "Check if I can sign up another client"
    val continueButton = "Continue"

    val backLinkText = "Back to enter client details"
  }
}
