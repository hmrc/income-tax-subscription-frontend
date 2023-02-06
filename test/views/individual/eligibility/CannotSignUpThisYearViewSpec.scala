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

import models.DateModel
import models.common.AccountingPeriodModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.eligibility.CannotSignUpThisYear

class CannotSignUpThisYearViewSpec extends ViewSpec {
  private val view = app.injector.instanceOf[CannotSignUpThisYear]
  private val nextTaxYear = AccountingPeriodModel(
    startDate = DateModel("6", "4", "2023"),
    endDate = DateModel("5", "4", "2024")
  )

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
      document.mainContent.selectNth("p", 2).text() mustBe CannotSignUpMessages.paragraph2
    }

    "have paragraph 3" in {
      document.mainContent.selectNth("p", 3).text() mustBe CannotSignUpMessages.paragraph3
    }

    "have link 1 in paragraph 3" in {
      val link = document.mainContent.selectNth("p", 3).selectFirst("a")
      link.text() mustBe CannotSignUpMessages.link1
      link.attr("href") mustBe "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax#who-can-use-making-tax-digital-for-income-tax"
    }

    "have subheading" in {
      document.mainContent.selectHead("h2").text() mustBe CannotSignUpMessages.subheading
    }

    "have paragraph 4" in {
      document.mainContent.selectNth("p", 4).text() mustBe CannotSignUpMessages.paragraph4
    }

    "have bullet 3" in {
      document.mainContent.selectNth("ul li", 3).text() mustBe CannotSignUpMessages.bullet3
    }

    "have link 2 in bullet 3" in {
      val link = document.mainContent.selectNth("ul li", 3).selectNth("a", 1)
      link.text() mustBe CannotSignUpMessages.link2
      link.attr("href") mustBe "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax#find-out-about-qualifying-income"
    }

    "have bullet 4" in {
      document.mainContent.selectNth("ul li", 4).text() mustBe CannotSignUpMessages.bullet4
    }

    "have bullet 5" in {
      document.mainContent.selectNth("ul li", 5).text() mustBe CannotSignUpMessages.bullet5
    }

    "have paragraph 5" in {
      document.mainContent.selectNth("p", 5).text() mustBe CannotSignUpMessages.paragraph5
    }

    "have bullet 6" in {
      document.mainContent.selectNth("ul li", 6).text() mustBe CannotSignUpMessages.bullet6
    }

    "have bullet 7" in {
      document.mainContent.selectNth("ul li", 7).text() mustBe CannotSignUpMessages.bullet7
    }

    "have bullet 8" in {
      document.mainContent.selectNth("ul li", 8).text() mustBe CannotSignUpMessages.bullet8
    }

    "have paragraph 6" in {
      document.mainContent.selectNth("p", 6).text() mustBe CannotSignUpMessages.paragraph6
    }

    "have link 3 in paragrpah 6" in {
      val link = document.mainContent.selectNth("p", 6).selectFirst("a")
      link.text() mustBe CannotSignUpMessages.link3
      link.attr("href") mustBe "https://www.gov.uk/self-assessment-tax-returns/sending-return"
    }

    "have a form" which {
      def form: Element = document.mainContent.selectHead("form")
      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a submit button" in {
        form.selectHead("button").text mustBe CannotSignUpMessages.continueButton
      }

    }

  }

  private def document = Jsoup.parse(view(testCall, nextTaxYear).body)

  object CannotSignUpMessages {
    val heading = "You can sign up to this pilot from 6 April 2023"
    val paragraph1 = "You cannot sign up to Making Tax Digital for Income Tax this year as it is not currently available to those who have certain types of:"
    val bullet1 = "Income"
    val bullet2 = "Deductions"
    val paragraph2 = s"However, you can still sign up for this service in the next tax year, starting 6 April ${AccountingPeriodUtil.getCurrentTaxYear.taxEndYear}."
    val paragraph3 = "You may still be able to use the pilot if your circumstances have recently changed. Check who can use Making Tax Digital for Income Tax to see if you can sign up."
    val link1 = "who can use Making Tax Digital for Income Tax"
    val subheading = "When you must use this service"
    val paragraph4 = "You must meet the Making Tax Digital for Income Tax requirements for 6 April 2026, if all of the following apply:"
    val link2 = "find out more about registering and sending a Self Assessment tax return (opens in new tab)"
    val bullet3 = s"you are registered for Self Assessment ($link2)"
    val bullet4 = "you get income from self-employment or property, or both"
    val bullet5 = "your total qualifying income is more than £50,000"
    val paragraph5 = "You must meet the Making Tax Digital for Income Tax requirements for 6 April 2027, if all of the following apply:"
    val bullet6 = "you are registered for Self Assessment"
    val bullet7 = "you get income from self-employment or property, or both"
    val bullet8 = "your total qualifying income is more than £30,000"
    val link3 = "send HMRC a Self Assessment tax return"
    val paragraph6 = s"You’ll still need to ${link3} for the tax year before you signed up to use Making Tax Digital for Income Tax."
    val continueButton = "Continue to sign up for next year"
  }
}
