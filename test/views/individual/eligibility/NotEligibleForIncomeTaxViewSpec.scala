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
import utilities.ViewSpec
import views.html.individual.matching.NotEligibleForIncomeTax

class NotEligibleForIncomeTaxViewSpec extends ViewSpec {
  private val view = app.injector.instanceOf[NotEligibleForIncomeTax]

  "Cannot Sign Up View" should {
    "have a title" in {
      document.title mustBe s"${CannotSignUpYetMessages.heading} - Use software to send Income Tax updates - GOV.UK"
    }

    "have a heading" in {
      document.mainContent.select("h1").text() mustBe CannotSignUpYetMessages.heading
    }

    "have paragraph 1 and link" in {
      val paragraph = document.mainContent.selectHead("p")
      val link = document.mainContent.selectHead("a")
      paragraph.text mustBe CannotSignUpYetMessages.paragraph1
      link.attr("href") mustBe CannotSignUpYetMessages.linkHref
    }

    "have paragraph 2" in {
      document.mainContent.selectNth("p", 2).text() mustBe CannotSignUpYetMessages.paragraph2
    }

    "have paragraph 3" in {
      document.mainContent.selectNth("p",3).text() mustBe CannotSignUpYetMessages.paragraph3
    }

    "has a sign out button" in {
      document.mainContent.selectHead(".govuk-button").text mustBe CannotSignUpYetMessages.signoutButton
    }

  }

  private def document = Jsoup.parse(view().body)

  object CannotSignUpYetMessages {
    val heading = "You cannot sign up yet"
    val paragraph1 = "People with some types of income or deductions cannot sign up to Making Tax Digital for Income Tax. (opens in new tab)"
    val paragraph2 = "In the future, we may extend this service to more people."
    val paragraph3 = "Meanwhile, you must continue to submit your Self Assessment tax return as normal."
    val linkHref = "https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax#who-can-sign-up-voluntarily"
    val signoutButton = "Sign out"
  }
}
