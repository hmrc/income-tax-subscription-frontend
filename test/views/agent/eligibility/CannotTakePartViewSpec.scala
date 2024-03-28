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
import org.jsoup.nodes.Document
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


  "Cannot Sign Up View" should {
    "have a title" in {
      document.title mustBe s"${CannotTakePartMessages.heading} - Use software to report your client’s Income Tax - GOV.UK"
    }

    "have a heading caption" in {
      document.mainContent.selectHead("span.govuk-caption-l").text mustBe s"$clientName | $clientNino"
    }

    "have a heading" in {
      document.mainContent.select("h1").text() mustBe CannotTakePartMessages.heading
    }

    "have paragraph 1" in {
      document.mainContent.selectNth("p", 1).text() mustBe CannotTakePartMessages.paragraph1
    }

    "have paragraph 2" in {
      document.mainContent.selectNth("p", 2).text() mustBe CannotTakePartMessages.paragraph2
    }

    "have paragraph 3" in {
      document.mainContent.selectNth("p", 3).text() mustBe CannotTakePartMessages.paragraph3
    }


    "have a sign up another client link" in {
      val link = document.mainContent.selectHead(".govuk-button")
      link.text() mustBe CannotTakePartMessages.signUpAnotherClientLink
    }
  }

  object CannotTakePartMessages {
    val heading = "You cannot sign up this client yet"
    val paragraph1 = "People with some types of income or deductions cannot sign up to Making Tax Digital for Income Tax."
    val paragraph2 = "In the future, we may extend this service to more people."
    val paragraph3 = "Meanwhile, you or your client must continue to submit their Self Assessment tax return as normal."
    val signUpAnotherClientLink = "Sign up another client"
  }
}
