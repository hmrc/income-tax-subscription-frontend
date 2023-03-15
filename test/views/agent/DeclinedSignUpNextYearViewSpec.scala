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

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.agent.DeclinedSignUpNextYear

class DeclinedSignUpNextYearViewSpec extends ViewSpec {

  val declinedSignUpNextYear: DeclinedSignUpNextYear = app.injector.instanceOf[DeclinedSignUpNextYear]

  val page: HtmlFormat.Appendable = declinedSignUpNextYear(
    postAction = testCall,
    backUrl = testBackUrl
  )

  val document: Document = Jsoup.parse(page.body)

  "DeclinedSignUpNextYear" must {
    "be using the correct template details" in new TemplateViewTest(
      view = page,
      title = DeclinedSignUpNextYearMessages.heading,
      isAgent = true,
      backLink = Some(testBackUrl),
      hasSignOutLink = true
    )

    "have a heading" in {
      document.mainContent.getH1Element.text mustBe DeclinedSignUpNextYearMessages.heading
    }

    "have a paragraph" in {
      document.mainContent.selectHead("p").text mustBe DeclinedSignUpNextYearMessages.para
    }

    "have an inset text section" in {
      document.mainContent.selectHead(".govuk-inset-text").text mustBe DeclinedSignUpNextYearMessages.InsetText.para
    }

    "have a form" which {
      def form: Element = document.mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a sign up another client button" in {
        form.selectHead("button").text mustBe DeclinedSignUpNextYearMessages.signUpAnotherClient
      }
    }
  }


  object DeclinedSignUpNextYearMessages {
    val heading: String = "What you need to do"
    val para: String = "You’ve chosen not to sign up your client for Making Tax Digital for Income Tax."

    object InsetText {
      val para: String = "Your client’s Self Assessment tax return needs to be submitted as normal."
    }

    val signUpAnotherClient: String = "Sign up another client"
  }

}