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
import org.jsoup.nodes.Element
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.incometax.eligibility.NotEligibleForIncomeTax

class NotEligibleForIncomeTaxViewSpec extends ViewSpec {
  private val view = app.injector.instanceOf[NotEligibleForIncomeTax]

  "Cannot Sign Up View" should {
    "have a title" in {
      document.title mustBe s"${CannotSignUpMessages.heading} - Use software to send Income Tax updates - GOV.UK"
    }

    "have a heading" in {
      document.mainContent.select("h1").text() mustBe CannotSignUpMessages.heading
    }

    "have paragraph 1" in {
      document.mainContent.selectFirst("p").text() mustBe CannotSignUpMessages.paragraph1
    }

    "have bullet 1" in {
      document.mainContent.selectNth("ul li", 1).text() mustBe CannotSignUpMessages.bullet1
    }

    "have bullet 2" in {
      document.mainContent.selectNth("ul li", 2).text() mustBe CannotSignUpMessages.bullet2
    }

    "have paragraph 2" in {
      document.mainContent.selectHead(".govuk-inset-text").text() mustBe CannotSignUpMessages.paragraph2
    }

    "have paragraph 3" in {
      document.mainContent.selectNth("p", 2).text() mustBe CannotSignUpMessages.paragraph3
    }

    "have link 1 in paragraph 3" in {
      val link = document.mainContent.selectNth("p", 2).selectFirst("a")
      link.text() mustBe CannotSignUpMessages.link1
      link.attr("href") mustBe appConfig.govukGuidanceITSAQualifyingIncomeLink
    }

    "have subheading" in {
      document.mainContent.selectHead("h2").text() mustBe CannotSignUpMessages.subheading
    }

    "have paragraph 4" in {
      document.mainContent.selectNth("p", 3).text() mustBe CannotSignUpMessages.paragraph4
    }

    "have bullet 3" in {
      document.mainContent.selectNth("ul li", 3).text() mustBe CannotSignUpMessages.bullet3
    }

    "have bullet 4" in {
      document.mainContent.selectNth("ul li", 4).text() mustBe CannotSignUpMessages.bullet4
    }

      "has a sign out button" in {
        document.mainContent.selectHead(".govuk-button").text mustBe CannotSignUpMessages.signoutButton
      }

  }

  private def document = Jsoup.parse(view().body)

  object CannotSignUpMessages {
    val heading = "You cannot take part in this pilot yet"
    val paragraph1 = "You cannot sign up to Making Tax Digital for Income Tax this year as it’s currently unavailable to people with certain types of:"
    val bullet1 = "incomes"
    val bullet2 = "deductions"
    val paragraph2 = "You must submit your Self Assessment tax return until the end of the tax year 2026 as normal."
    val paragraph3 = "You may still be able to use the pilot if your circumstances have recently changed. Check who can use Making Tax Digital for Income Tax (opens in new tab) to see if you can sign up."
    val link1 = "Check who can use Making Tax Digital for Income Tax (opens in new tab)"
    val subheading = "When you have to use Making Tax Digital for Income Tax"
    val paragraph4 = "If you get income from self employment or property, you’ll need to use Making Tax Digital for Income Tax to submit your records if you earn:"
    val bullet3 = s"£50,000 or more from the tax year starting 6 April 2026"
    val bullet4 = "£30,000 or more from the tax year starting 6 April 2027"
    val signoutButton = "Sign out"
  }
}
