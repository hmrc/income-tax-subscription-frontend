/*
 * Copyright 2024 HM Revenue & Customs
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

package views.individual

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual
import views.html.individual.NoSoftware

class NoSoftwareViewSpec extends ViewSpec {

  private val noSoftware: NoSoftware = app.injector.instanceOf[individual.NoSoftware]

  "NoSoftware" must {
    "have the correct template details" in new TemplateViewTest(
      view = page,
      isAgent = false,
      title = NoSoftwareMessages.heading
    )
    "have the correct heading" in {
      document.mainContent.selectHead("h1").text mustBe NoSoftwareMessages.heading
    }
    "have the correct first paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe NoSoftwareMessages.paraOne
    }
    "have the correct first subheading" in {
      document.mainContent.selectNth("h2", 1).text mustBe NoSoftwareMessages.subheadingOne
    }
    "have the correct second paragraph" in {
      document.mainContent.selectNth("p", 2).text mustBe NoSoftwareMessages.paraTwo
    }
    "have the correct second subheading" in {
      document.mainContent.selectNth("h2", 2).text mustBe NoSoftwareMessages.subheadingTwo
    }
    "have third paragraph" which {

      "displays the correct text" in {
        document.mainContent.selectNth("p", 3).text mustBe NoSoftwareMessages.paraThreeText
      }
      "has the correct link href" in {
        document.mainContent.selectNth("p", 3).selectHead("a").attr("href") mustBe NoSoftwareMessages.paraThreeLinkHref
      }

    }
    "have the correct fourth paragraph" in {
      document.mainContent.selectNth("p", 4).text mustBe NoSoftwareMessages.paraFour
    }
    "have the correct fifth paragraph" in {
      document.mainContent.selectNth("p", 5).text mustBe NoSoftwareMessages.paraFive
    }
    "have a sixth paragraph" which {
      "displays the correct text" in {
        document.mainContent.selectNth("p", 6).text mustBe NoSoftwareMessages.paraSixText
      }
      "has the correct link" in {
        document.mainContent.selectNth("p", 6).selectHead("a").attr("href") mustBe NoSoftwareMessages.paraSixLinkHref
      }
    }
  }

  private object NoSoftwareMessages {
    val heading = "You need compatible software"
    val paraOne = "To use this service, you must find and use software that works with Making Tax Digital for Income Tax."
    val subheadingOne = "If you have an agent"
    val paraTwo = "If someone helps you with your tax (like an agent), check which software they want you to use."
    val subheadingTwo = "Find compatible software"
    val paraThreeText = "Find software that works with Making Tax Digital for Income Tax (opens in new tab)"
    val paraThreeLinkHref = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
    val paraFour = "If you already use software to keep digital records, you may need to ask your software provider if it works with Making Tax Digital for Income Tax."
    val paraFive = "When you have compatible software, you can come back to sign up."
    val paraSixText = "Meanwhile, you need to continue submitting your Self Assessment tax return as normal."
    val paraSixLinkHref = "https://www.gov.uk/self-assessment-tax-returns/sending-return"
  }

  private def page: Html = {
    noSoftware(
      backUrl = testBackUrl
    )
  }
  private def document: Document =
    Jsoup.parse(page.body)

}
