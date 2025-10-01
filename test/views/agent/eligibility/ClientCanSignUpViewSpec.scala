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
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.data.FormError
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.agent.eligibility.ClientCanSignUp

class ClientCanSignUpViewSpec extends ViewSpec {

  private val view = app.injector.instanceOf[ClientCanSignUp]
  val testFormError: FormError = FormError(ClientCanSignUpForm.fieldName, "error.agent.client-can-sign-up.invalid")
  val clientName: String = "FirstName LastName"
  val clientNino: String = "AA 11 11 11 A"

  class ViewTest {
    val page: HtmlFormat.Appendable = view(
      postAction = testCall,
      backUrl = testBackUrl,
      clientName = clientName,
      clientNino = clientNino
    )

    val document: Document = Jsoup.parse(page.body)
    val mainContent: Element = document.mainContent
  }

  "Client Can Sign Up View" when {
    "Using correct template details" in new ViewTest {
      new TemplateViewTest(
        view = page,
        title = ClientCanSignUpMessages.heading,
        isAgent = true,
        backLink = None,
        backLinkText = None,
        hasSignOutLink = true
      )
    }

    "have a heading and caption" in new ViewTest {
      mainContent.mustHaveHeadingAndCaption(
        heading = ClientCanSignUpMessages.heading,
        caption = s"$clientName | $clientNino",
        isSection = false
      )
    }

    "have the first paragraph" in new ViewTest {
      mainContent.getNthParagraph(1).text mustBe ClientCanSignUpMessages.para1
    }

    "have the first subheading" in new ViewTest {
      mainContent.mainContent.getSubHeading("h2", 1).text() mustBe ClientCanSignUpMessages.subheading1
    }


    "have the second paragraph" in new ViewTest {
      mainContent.getNthParagraph(2).text mustBe ClientCanSignUpMessages.para2
    }

    "have the third paragraph" in new ViewTest {
      mainContent.getNthParagraph(3).text mustBe ClientCanSignUpMessages.para3
    }

    "have a first bullet list" in new ViewTest {
      def bulletList: Element = mainContent.selectNth("ul", 1)

      bulletList.selectNth("li", 1).text mustBe ClientCanSignUpMessages.bullet1
      bulletList.selectNth("li", 2).text mustBe ClientCanSignUpMessages.bullet2

    }

    "have the second subheading" in new ViewTest {
      mainContent.getSubHeading("h2", 2).text() mustBe ClientCanSignUpMessages.subheading2
    }

    "have the fourth paragraph" in new ViewTest {
      mainContent.getNthParagraph(4).text mustBe ClientCanSignUpMessages.para4
    }

    "have a second bullet list" in new ViewTest {
      def bulletList: Element = mainContent.selectNth("ul", 2)

      bulletList.selectNth("li", 1).text mustBe ClientCanSignUpMessages.bullet3
      bulletList.selectNth("li", 2).text mustBe ClientCanSignUpMessages.bullet4

    }

    "have a sign up client button" in new ViewTest {
      mainContent.getGovukSubmitButton.text mustBe ClientCanSignUpMessages.continueButton
    }

    "have the fifth paragraph" in new ViewTest {
      val paragraph: Elements = mainContent.select(".govuk-form-group").select(".govuk-body")
      paragraph.text must include(ClientCanSignUpMessages.signUpAnotherClient)
      paragraph.select("a.govuk-link").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
    }

  }

  object ClientCanSignUpMessages {
    val heading = "You can sign up this client"
    val para1 = "You can sign up this client for Making Tax Digital for Income Tax now."
    val subheading1 = "What happens next"
    val para2 = "For each of your client’s businesses or income from property, you’ll need their start date if it started within the last 2 tax years."
    val para3 = "If they’re a sole trader, you’ll also need your client’s:"
    val bullet1 = "business trading name and address"
    val bullet2 = "trade (the nature of their business)"
    val subheading2 = "If you do not sign up your client now"
    val para4 = "If you do not sign up your client now, you’ll need to:"
    val bullet3 = "make sure they continue to submit their Self Assessment tax returns as normal"
    val bullet4 = "re-enter their details if you return to sign them up later"

    val continueButton = "Sign up this client"
    val signUpAnotherClient = "Or you can check if you can sign up another client. We will not save the details you entered about FirstName LastName."
  }

}
