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
import views.html.individual.incometax.eligibility.NotEligibleForIncomeTax

class NotEligibleForIncomeTaxViewSpec extends ViewSpec {
  private val view = app.injector.instanceOf[NotEligibleForIncomeTax]

  "Cannot Sign Up View" should {
    "have a title" in {
      document.title mustBe s"${CannotTakePartMessages.heading} - Use software to send Income Tax updates - GOV.UK"
    }

    "have a heading" in {
      document.mainContent.select("h1").text() mustBe CannotTakePartMessages.heading
    }

    "have paragraph 1" in {
      document.mainContent.selectFirst("p").text() mustBe CannotTakePartMessages.paragraph1
    }

     "have paragraph 2" in {
      document.mainContent.selectNth("p", 2).text() mustBe CannotTakePartMessages.paragraph2
    }

    "have link in paragraph 2" in {
      val link = document.mainContent.selectNth("p", 2).selectFirst("a")
      link.text() mustBe CannotTakePartMessages.link
      link.attr("href") mustBe CannotTakePartMessages.linkHref
    }

    "have a inset text" in {
      document.mainContent.selectHead(".govuk-inset-text").text() mustBe CannotTakePartMessages.insetText
    }


      "has a sign out button" in {
        document.mainContent.selectHead(".govuk-button").text mustBe CannotTakePartMessages.signoutButton
      }

  }

  private def document = Jsoup.parse(view().body)

  object CannotTakePartMessages {
    val heading = "You cannot take part in this pilot yet"
    val paragraph1 = "The Making Tax Digital for Income Tax pilot is not currently available to people."
    val paragraph2 = "However, you may be able to sign up to Making Tax Digital for Income Tax in the future. Learn more about who’s eligible for Making Tax Digital for Income Tax."
    val link = "who’s eligible for Making Tax Digital for Income Tax"
    val linkHref = "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax"
    val insetText = "You must submit your Self Assessment tax return as normal."
    val signoutButton = "Sign out"
  }
}
