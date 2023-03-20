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

package views.individual.eligibility

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.individual.eligibility.DeclinedSignUpNextYear

class DeclinedSignUpNextYearViewSpec extends ViewSpec {

  val declinedSignUpNextYear: DeclinedSignUpNextYear = app.injector.instanceOf[DeclinedSignUpNextYear]

  val page: HtmlFormat.Appendable = declinedSignUpNextYear(
    backUrl = testBackUrl
  )

  val document: Document = Jsoup.parse(page.body)

  "DeclinedSignUpNextYear" must {
    "be using the correct template details" in new TemplateViewTest(
      view = page,
      title = DeclinedSignUpNextYearMessages.heading,
      backLink = Some(testBackUrl),
      hasSignOutLink = true
    )

    "have a heading" in {
      document.mainContent.getH1Element.text mustBe DeclinedSignUpNextYearMessages.heading
    }

    "have a paragraph" in {
      document.mainContent.selectHead("p").text mustBe DeclinedSignUpNextYearMessages.para
    }

    "has a sign out button" in {
      val link = document.mainContent.selectHead("a.govuk-button")
      link.text mustBe DeclinedSignUpNextYearMessages.signOut
      link.attr("href") mustBe controllers.SignOutController.signOut.url
    }
  }


  object DeclinedSignUpNextYearMessages {
    val heading: String = "What you need to do"
    val para: String = "You’ve chosen not to sign up for Making Tax Digital for Income Tax." +
      " Continue to submit your annual Self Assessment tax return as normal."
    val signOut: String = "Sign out"
  }

}