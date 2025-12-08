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
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.individual.matching.NotEligibleForIncomeTax

class NotEligibleForIncomeTaxViewSpec extends ViewSpec {
  private val view = app.injector.instanceOf[NotEligibleForIncomeTax]
  def page(exemptionReason: Option[String]): HtmlFormat.Appendable = view(exemptionReason)

  val cannotSignUp: Option[String] = Some("")
  val enduringExemption: Option[String] = Some("MTD Exempt Enduring")
  val mtdExempt2627: Option[String] = Some("MTD Exempt 26/27")

  "Cannot Sign Up View" should {

    lazy val pageView = page(cannotSignUp)
    lazy val document: Document = Jsoup.parse(pageView.body)
    
    "have a title" in {
      document.title mustBe s"${CannotSignUpYetMessages.heading} - Sign up for Making Tax Digital for Income Tax - GOV.UK"
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

  "MTD Exempt Enduring" must {

    lazy val pageView = page(enduringExemption)
    lazy val document: Document = Jsoup.parse(pageView.body)

    "have a title" in {
      document.title mustBe s"${MTDExemptEnduring.heading} - Sign up for Making Tax Digital for Income Tax - GOV.UK"
    }
    
    "have a heading" in {
      document.mainContent.select("h1").text() mustBe MTDExemptEnduring.heading
    }

    "have a first paragraph" in {
      val paragraph: Element = document.mainContent.selectNth("p", 1)
      paragraph.text mustBe MTDExemptEnduring.paragraph1

      val link: Element = paragraph.selectHead("a")
      link.text mustBe MTDExemptEnduring.paragraph1LinkText
      link.attr("href") mustBe "https://www.gov.uk/find-hmrc-contacts/self-assessment-general-enquiries"
    }

    "have a second paragraph" in {
      val paragraph: Element = document.mainContent.selectNth("p", 2)
      paragraph.text mustBe MTDExemptEnduring.paragraph2

      val link: Element = paragraph.selectHead("a")
      link.attr("href") mustBe "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax#who-is-exempt-from-making-tax-digital-for-income-tax"
    }

    "has a sign out button" in {
      document.mainContent.selectHead(".govuk-button").text mustBe MTDExemptEnduring.signoutButton
    }
  }

  "MTD Exempt 26/27" must {

    lazy val pageView = page(mtdExempt2627)
    lazy val document: Document = Jsoup.parse(pageView.body)

    "have a title" in {
      document.title mustBe s"${MTDExempt2627.heading} - Sign up for Making Tax Digital for Income Tax - GOV.UK"
    }

    "have a heading" in {
      document.mainContent.select("h1").text() mustBe MTDExempt2627.heading
    }

    "have a first paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe MTDExempt2627.paragraph1
    }

    "have a second paragraph" in {
      document.mainContent.selectNth("p", 2).text mustBe MTDExempt2627.paragraph2
    }

    "have a third paragraph" in {
      val paragraph: Element = document.mainContent.selectNth("p", 3)
      paragraph.text mustBe MTDExempt2627.paragraph3

      val link: Element = paragraph.selectHead("a")
      link.attr("href") mustBe "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax#who-is-exempt-from-making-tax-digital-for-income-tax"
    }

    "has a sign out button" in {
      document.mainContent.selectHead(".govuk-button").text mustBe MTDExempt2627.signoutButton
    }
  }

  object CannotSignUpYetMessages {
    val heading = "You cannot sign up yet"
    val paragraph1 = "People with some types of income or deductions cannot sign up to Making Tax Digital for Income Tax. (opens in new tab)"
    val paragraph2 = "In the future, we may extend this service to more people."
    val paragraph3 = "Meanwhile, you must continue to submit your Self Assessment tax return as normal."
    val linkHref = "https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax#who-can-sign-up-voluntarily"
    val signoutButton = "Sign out"
  }

  object MTDExemptEnduring {
    val heading = "You are exempt from Making Tax Digital for Income Tax"
    val paragraph1 = "This means you do not have to use it unless your circumstances change. If your circumstances change, you must let HMRC know. (opens in new tab)"
    val paragraph1LinkText = "you must let HMRC know. (opens in new tab)"
    val paragraph2 = "You can find out about exemptions. (opens in new tab)"
    val signoutButton = "Sign out"
  }

  object MTDExempt2627 {
    val heading = "You are exempt from Making Tax Digital for Income Tax"
    val paragraph1 = "You are exempt from using Making Tax Digital for Income Tax for the 2026 to 2027 tax year."
    val paragraph2 = "This means you do not need to use it unless your circumstances change. You must let HMRC know if your circumstances change."
    val paragraph3 = "Find out if and when you may be able to use Making Tax Digital for Income Tax in the future. (opens in new tab)"
    val signoutButton = "Sign out"
  }
}
