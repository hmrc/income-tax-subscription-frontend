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

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.agent.eligibility.CannotTakePart

class CannotTakePartViewSpec extends ViewSpec {

  val clientName: String = "FirstName LastName"
  val clientNino: String = "AA 11 11 11 A"

  private val view = app.injector.instanceOf[CannotTakePart]

  val page: HtmlFormat.Appendable = view(
    clientName = clientName,
    clientNino = clientNino
  )

  val document: Document = Jsoup.parse(page.body)

  "CannotSignUp" must {
    "use the correct template" in new TemplateViewTest(
      view = page,
      title = CannotTakePartMessages.heading,
      isAgent = true,
      hasSignOutLink = true
    )

    "have a heading and caption" in {
      document.mainContent.mustHaveHeadingAndCaption(
        heading = CannotTakePartMessages.heading,
        caption = s"$clientName | $clientNino",
        isSection = false
      )
    }

    "have a subheading" in {
      document.mainContent.getSubHeading("h2", 1).text mustBe CannotTakePartMessages.subheading
    }

    "have a first paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe CannotTakePartMessages.paragraph1
    }

    "have a second paragraph" in {
      val paragraph: Element = document.mainContent.selectNth("p", 2)
      paragraph.text mustBe CannotTakePartMessages.paragraph2

      val link: Element = paragraph.selectHead("a")
      link.text mustBe CannotTakePartMessages.paragraph2LinkText
      link.attr("href") mustBe "https://www.gov.uk/guidance/sign-up-your-client-for-making-tax-digital-for-income-tax#who-can-sign-up-voluntarily"
    }

    "have a third paragraph" in {
      val paragraph: Element = document.mainContent.selectNth("p", 3)
      paragraph.text mustBe CannotTakePartMessages.paragraph3

      val link: Element = paragraph.selectHead("a")
      link.text mustBe CannotTakePartMessages.paragraph3LinkText
      link.attr("href") mustBe "https://www.gov.uk/email/subscriptions/single-page/new?topic_id=sign-up-your-client-for-making-tax-digital-for-income-tax"
    }

    "have a form" which {
      def form: Element = document.mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe "GET"
        form.attr("action") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
      }

      "have a sign up another client link" in {
        form.selectHead(".govuk-button").text() mustBe CannotTakePartMessages.signUpAnotherClientLink
      }
    }
  }

  object CannotTakePartMessages {
    val heading = "You cannot sign up this client voluntarily"
    val subheading = "What happens next"
    val paragraph1 = "You’ll need to make sure your client submits their Self Assessment tax return as normal."
    val paragraph2 = "You can find out who can and cannot sign up voluntarily (opens in new tab)."
    val paragraph2LinkText = "who can and cannot sign up voluntarily (opens in new tab)"
    val paragraph3 = "You can also sign up to get emails when that page is updated (opens in new tab)."
    val paragraph3LinkText = "sign up to get emails when that page is updated (opens in new tab)"
    val signUpAnotherClientLink = "Sign up another client"
  }
}
