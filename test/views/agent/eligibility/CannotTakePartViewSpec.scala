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

  def page(exemptionReason: Option[String]): HtmlFormat.Appendable =
    view(
      clientName = clientName,
      clientNino = clientNino,
      exemptionReason = exemptionReason
    )

  val cannotSignUp: Option[String] = Some("")
  val enduringExemption: Option[String] = Some("MTD Exempt Enduring")
  val mtdExempt2627: Option[String] = Some("MTD Exempt 26/27")
  val digitalExemption: Option[String] = Some("Digitally Exempt")
  val noData: Option[String] = Some("No Data")

  "CannotSignUp" must {

    lazy val pageView = page(cannotSignUp)
    lazy val document: Document = Jsoup.parse(pageView.body)

    "use the correct template" in new TemplateViewTest(
      view = pageView,
      title = CannotTakePartMessages.heading,
      isAgent = true,
      hasSignOutLink = true
    )

    "have a heading and caption" in {
      document.mainContent.mustHaveHeadingAndCaption(
        heading = CannotTakePartMessages.heading,
        caption = s"$clientName – $clientNino",
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

  "MTD Exempt Enduring" must {

    lazy val pageView = page(enduringExemption)
    lazy val document: Document = Jsoup.parse(pageView.body)

    "use the correct template" in new TemplateViewTest(
      view = pageView,
      title = MTDExemptEnduring.heading,
      isAgent = true,
      hasSignOutLink = true
    )

    "have a heading and caption" in {
      document.mainContent.mustHaveHeadingAndCaption(
        heading = MTDExemptEnduring.heading,
        caption = s"$clientName – $clientNino",
        isSection = false
      )
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

  "MTD Exempt 26/27" must {

    lazy val pageView = page(mtdExempt2627)
    lazy val document: Document = Jsoup.parse(pageView.body)

    "use the correct template" in new TemplateViewTest(
      view = pageView,
      title = MTDExempt2627.heading,
      isAgent = true,
      hasSignOutLink = true
    )

    "have a heading and caption" in {
      document.mainContent.mustHaveHeadingAndCaption(
        heading = MTDExempt2627.heading,
        caption = s"$clientName – $clientNino",
        isSection = false
      )
    }

    "have a first paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe MTDExempt2627.paragraph1
    }

    "have a second paragraph" in {
      val paragraph: Element = document.mainContent.selectNth("p", 2)
      paragraph.text mustBe MTDExempt2627.paragraph2

      val link: Element = paragraph.selectHead("a")
      link.attr("href") mustBe "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax#who-is-exempt-from-making-tax-digital-for-income-tax"
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

  "No Data" must {

    lazy val pageView = page(noData)
    lazy val document: Document = Jsoup.parse(pageView.body)

    "use the correct template" in new TemplateViewTest(
      view = pageView,
      title = NoDataFound.heading,
      isAgent = true,
      hasSignOutLink = true
    )

    "have a heading and caption" in {
      document.mainContent.mustHaveHeadingAndCaption(
        heading = NoDataFound.heading,
        caption = s"$clientName – $clientNino",
        isSection = false
      )
    }

    "have a first paragraph" in {
      val paragraph: Element = document.mainContent.selectNth("p", 1)
      paragraph.text mustBe NoDataFound.paragraph1
    }

    "have a second paragraph" in {
      val paragraph: Element = document.mainContent.selectNth("p", 2)
      paragraph.text mustBe NoDataFound.paragraph2
    }

    "have a first list" in {
      val list: Element = document.mainContent.selectNth("li", 1)
      list.text mustBe NoDataFound.list1
    }

    "have a second list" in {
      val list: Element = document.mainContent.selectNth("li", 2)
      list.text mustBe NoDataFound.list2
    }

    "have a third list" in {
      val list: Element = document.mainContent.selectNth("li", 3)
      list.text mustBe NoDataFound.list3
    }

    "have a third paragraph" in {
      val paragraph: Element = document.mainContent.selectNth("p", 3)
      paragraph.text mustBe NoDataFound.paragraph3

      val link: Element = paragraph.selectHead("a")
      link.text mustBe NoDataFound.paragraph3LinkText
      link.attr("href") mustBe "https://www.gov.uk/find-hmrc-contacts/self-assessment-general-enquiries"
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

  "Digitally Exempt" must {

    lazy val pageView = page(digitalExemption)
    lazy val document: Document = Jsoup.parse(pageView.body)

    "use the correct template" in new TemplateViewTest(
      view = pageView,
      title = DigitallyExempt.heading,
      isAgent = true,
      hasSignOutLink = true
    )

    "have a heading and caption" in {
      document.mainContent.mustHaveHeadingAndCaption(
        heading = DigitallyExempt.heading,
        caption = s"$clientName – $clientNino",
        isSection = false
      )
    }

    "have a first paragraph" in {
      val paragraph: Element = document.mainContent.selectNth("p", 1)
      paragraph.text mustBe DigitallyExempt.paragraph
    }

    "have a second paragraph" in {
      val paragraph: Element = document.mainContent.selectNth("p", 2)
      paragraph.text mustBe DigitallyExempt.paragraph1

      val link: Element = paragraph.selectHead("a")
      link.attr("href") mustBe DigitallyExempt.paragraph1LinkHref
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

  object MTDExemptEnduring {
    val heading = "Your client is exempt from Making Tax Digital for Income Tax"
    val paragraph1 = "This means you do not have to use it unless your client’s circumstances change. If their circumstances change, you or your client must let HMRC know. (opens in new tab)"
    val paragraph1LinkText = "you or your client must let HMRC know. (opens in new tab)"
    val paragraph2 = "You can find out about exemptions. (opens in new tab)"
    val signUpAnotherClientLink = "Sign up another client"
  }

  object MTDExempt2627 {
    val heading = "Your client is exempt from Making Tax Digital for Income Tax"
    val paragraph1 = "This means you do not need to use it unless your circumstances change. You or your client must let HMRC know if their circumstances change."
    val paragraph2 = "Find out if and when your client may be able to use Making Tax Digital for Income Tax in the future. (opens in new tab)"
    val signUpAnotherClientLink = "Sign up another client"
  }

  object NoDataFound {
    val heading = "Your client cannot use Making Tax Digital for Income Tax"
    val paragraph1 = "Our records show your client cannot use Making Tax Digital for Income Tax."
    val paragraph2 = "This could be because they:"
    val list1 = "haven’t submitted a tax return within the last 2 years. You can sign up after you have submitted their first tax return"
    val list2 = "have never submitted a tax return"
    val list3 = "are insolvent"
    val paragraph3 = "If your client has received a letter from HMRC asking them to sign up to Making Tax Digital For Income Tax, please contact us (opens in new tab)."
    val paragraph3LinkText = "contact us (opens in new tab)"
  }

  object DigitallyExempt {
    val heading = "Your client is exempt from Making Tax Digital for Income Tax"
    val paragraph = "HMRC has agreed that your client is digitally excluded. This means you do not need to use it unless your circumstances change."
    val paragraph1 = "If your client’s circumstances change, you or your client must let HMRC know. (opens in new tab)"
    val paragraph1LinkHref = "https://www.gov.uk/guidance/apply-for-an-exemption-from-making-tax-digital-for-income-tax"
    val signUpAnotherClientLink = "Sign up another client"
  }
}
