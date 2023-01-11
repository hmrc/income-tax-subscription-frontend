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

import models.DateModel
import models.common.AccountingPeriodModel
import org.jsoup.Jsoup
import utilities.ViewSpec
import views.html.agent.eligibility.CannotSignUpThisYear

class CannotSignUpThisYearViewSpec extends ViewSpec {
  private val view = app.injector.instanceOf[CannotSignUpThisYear]
  private val nextTaxYear = AccountingPeriodModel(
    startDate = DateModel("6", "4", "2023"),
    endDate = DateModel("5", "4", "2024")
  )

  "Cannot Sign Up View" should {
    "have a title" in {
      document.title mustBe s"${CannotSignUpMessages.heading} - Use software to report your client’s Income Tax - GOV.UK"
    }

    "have a heading" in {
      document.mainContent.select("h1").text() mustBe CannotSignUpMessages.heading
    }

    "have paragraph 1" in {
      document.mainContent.selectNth("p", 1).text() mustBe CannotSignUpMessages.paragraph1
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

    "have link 1" in {
      val link = document.mainContent.selectNth("a", 1)
      link.text() mustBe CannotSignUpMessages.link1
      link.attr("href") mustBe "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax#who-can-use-making-tax-digital-for-income-tax"
    }

    "have subheading" in {
      document.mainContent.selectHead("h2").text() mustBe CannotSignUpMessages.subheading
    }

    "have paragraph 4" in {
      document.mainContent.selectNth("p", 4).text() mustBe CannotSignUpMessages.paragraph4
    }

    "have link 2" in {
      val link = document.mainContent.selectNth("a", 2)
      link.text() mustBe CannotSignUpMessages.link2
      link.attr("href") mustBe "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax#find-out-about-qualifying-income"
    }

    "have bullet 3" in {
      document.mainContent.selectNth("ul li", 3).text() mustBe CannotSignUpMessages.bullet3
    }

    "have bullet 4" in {
      document.mainContent.selectNth("ul li", 4).text() mustBe CannotSignUpMessages.bullet4
    }

    "have paragraph 5" in {
      document.mainContent.selectNth("p", 5).text() mustBe CannotSignUpMessages.paragraph5
    }

    "have link 3" in {
      val link = document.mainContent.selectNth("a", 3)
      link.text() mustBe CannotSignUpMessages.link3
      link.attr("href") mustBe "https://www.gov.uk/self-assessment-tax-returns/sending-return"
    }

    "have a continue button" in {
      val link = document.mainContent.selectNth("a", 4)
      link.text() mustBe CannotSignUpMessages.continueButton
      link.attr("href") mustBe "/report-quarterly/income-and-expenses/sign-up/client/business/task-list"
    }

    "have a sign up another client link" in {
      val link = document.mainContent.selectNth("a", 5)
      link.text() mustBe CannotSignUpMessages.signUpAnotherClientLink
      link.attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
    }
  }

  private def document = Jsoup.parse(view(nextTaxYear).body)

  object CannotSignUpMessages {
    val heading = "Your client can sign up to this pilot from 6 April 2023"
    val paragraph1 = "Your client cannot sign up to Making Tax Digital for Income tax yet." +
      " This is because it’s not currently available to people with certain types of:"
    val bullet1 = "Income"
    val bullet2 = "Deductions"
    val paragraph2 = "Your client can still sign up for Making Tax Digital for Income Tax for the next tax year, which starts on 6 April 2023."
    val paragraph3 = "Your client may still be able to join the pilot if their circumstances have recently changed. " +
      "Check Who can use Making Tax Digital for Income Tax to see if they can sign up."
    val link1 = "Who can use Making Tax Digital for Income Tax"
    val subheading = "When your client must use this service"
    val paragraph4 = "From 6 April 2026, your client must use Making Tax Digital for Income Tax" +
      " if their total qualifying income (opens in new tab) is above £50,000 for:"
    val link2 = "qualifying income (opens in new tab)"
    val bullet3 = "Self-employment"
    val bullet4 = "Property"
    val paragraph5 = "Your client still needs to send HMRC a Self Assessment tax return for the tax year" +
      " before your client signs up to use Making Tax Digital for Income Tax."
    val link3 = "send HMRC a Self Assessment tax return"
    val continueButton = "Continue to sign up client for 6 April 2023"
    val signUpAnotherClientLink = "Sign up another client"
  }
}
