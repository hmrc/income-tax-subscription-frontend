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

package views.agent

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.agent.NoSoftware

class NoSoftwareViewSpec extends ViewSpec {

  private val noSoftware: NoSoftware = app.injector.instanceOf[NoSoftware]
  private val fullName = "FirstName LastName"
  private val nino = "ZZ 11 11 11 Z"
  private val postAction: Call = controllers.agent.routes.AddAnotherClientController.addAnother()
  private val backUrl: String = controllers.agent.routes.UsingSoftwareController.show(false).url

  "NoSoftware" must {

    "have the correct template details" in new TemplateViewTest(
      view = page(),
      isAgent = true,
      title = NoSoftwareMessages.heading
    )

    "have a heading and caption" in {
      document.mustHaveHeadingAndCaption(
        NoSoftwareMessages.heading,
        NoSoftwareMessages.caption,
        isSection = false
      )
    }

    "have the first paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe NoSoftwareMessages.paraOne
    }

    "have the first subheading" in {
      document.mainContent.getSubHeading("h2", 1).text mustBe NoSoftwareMessages.subheadingOne
    }

    "have the second paragraph" in {
      document.mainContent.selectNth("p", 2).text mustBe NoSoftwareMessages.paraTwo
    }

    "have the second subheading" in {
      document.mainContent.getSubHeading("h2", 2).text mustBe NoSoftwareMessages.subheadingTwo
    }

    "have a third paragraph" which {

      "displays the correct text" in {
        document.mainContent.selectNth("p", 3).text mustBe NoSoftwareMessages.paraThreeText
      }

      "has the correct link" in {
        document.mainContent.selectNth("p", 3).selectHead("a").attr("href") mustBe NoSoftwareMessages.paraThreeLinkHref
      }
    }

    "have the fourth paragraph" in {
      document.mainContent.selectNth("p", 4).text mustBe NoSoftwareMessages.paraFour
    }

    "have the fifth paragraph" in {
      document.mainContent.selectNth("p", 5).text mustBe NoSoftwareMessages.paraFive
    }

    "have a form" which {

      "has the correct attributes" in {
        document.mainContent.getForm.attr("method") mustBe postAction.method
        document.mainContent.getForm.attr("action") mustBe postAction.url
      }

      "has an accept and continue button to submit the form" in {
        document.mainContent.getForm.selectNth(".govuk-button", 1).text mustBe NoSoftwareMessages.buttonText
      }
    }

  }

  private object NoSoftwareMessages {
    val heading = "You need compatible software"
    val caption = s"$fullName | $nino"
    val paraOne = "To use this service, you must find and use software that works with Making Tax Digital for Income Tax."
    val subheadingOne = "If you already use software"
    val paraTwo = "If you already use software to keep digital records for your clients, you need to ask your software provider if it works with Making Tax Digital for Income Tax."
    val subheadingTwo = "Find compatible software"
    val paraThreeText = "Find software that works with Making Tax Digital for Income Tax (opens in new tab)"
    val paraThreeLinkHref = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
    val paraFour = "When you have compatible software, you can come back to sign up your client."
    val paraFive = "Meanwhile, you’ll need to make sure your client submits their Self Assessment tax return as normal."
    val buttonText = "Sign up another client"
  }

  private def page(clientName: String = fullName, clientNino: String = nino): Html = {
    noSoftware(
      backUrl = backUrl,
      postAction = postAction,
      clientName = clientName,
      clientNino = clientNino
    )
  }

  private def document: Document = Jsoup.parse(page().body)
}

