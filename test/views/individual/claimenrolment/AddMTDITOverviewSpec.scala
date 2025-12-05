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

package views.individual.claimenrolment

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.individual.claimenrolment.AddMTDITOverview

class AddMTDITOverviewSpec extends ViewSpec {

  val addMTDITOverview: AddMTDITOverview = app.injector.instanceOf[AddMTDITOverview]
  val page: HtmlFormat.Appendable = addMTDITOverview(testCall)
  val document: Document = Jsoup.parse(page.body)
  val mainContent: Element = document.mainContent

  "AddMTDITOverview" must {
    "use the correct page template" in new TemplateViewTest(
      view = page,
      title = AddMTDITOverviewMessages.heading,
      hasSignOutLink = true
    )

    "have a heading" in {
      mainContent.getH1Element.text mustBe AddMTDITOverviewMessages.heading
    }

    "have a initial paragraph" in {
      mainContent.getNthParagraph(1).text mustBe AddMTDITOverviewMessages.paraOne
    }

    "have a next steps section heading" in {
      mainContent.selectHead("h2").text mustBe AddMTDITOverviewMessages.NextSteps.heading
    }

    "have a next steps first paragraph" in {
      mainContent.getNthParagraph(2).text mustBe AddMTDITOverviewMessages.NextSteps.paraOne
    }

    "have a next steps second paragraph" which {
      "has the correct text" in {
        mainContent.getNthParagraph(3).text mustBe AddMTDITOverviewMessages.NextSteps.paraTwo
      }
      "has a link to the business tax account which opens in a new tab" in {
        val link: Element = mainContent.getNthParagraph(3).selectHead("a")

        link.text mustBe AddMTDITOverviewMessages.NextSteps.paraTwoLinkText
        link.attr("href") mustBe "https://www.tax.service.gov.uk/business-account"
        link.attr("target") mustBe "_blank"
        link.attr("rel") mustBe "noopener noreferrer"
      }
    }

    "have a next steps third paragraph" in {
      mainContent.getNthParagraph(4).text mustBe AddMTDITOverviewMessages.NextSteps.paraThree
    }

    "have a form" which {
      def form: Element = mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }
      "has a continue button" in {
        form.getGovukSubmitButton.text mustBe AddMTDITOverviewMessages.continue
      }
    }
  }

  object AddMTDITOverviewMessages {
    val heading: String = "Add Making Tax Digital for Income Tax to your business tax account"
    val paraOne = "Your agent has signed you up for Making Tax Digital for Income Tax."

    object NextSteps {
      val heading: String = "Next steps"
      val paraOne: String = "You can now add it to your account and manage it with other taxes. You’ll need the user ID and password you got when you signed up for Self Assessment."
      val paraTwoLinkText: String = "business tax account (opens in new tab)"
      val paraTwo: String = s"You can check your existing account details in your $paraTwoLinkText."
      val paraThree: String = "You may be asked to provide further proof of your identity to add Making Tax Digital for Income Tax to your HMRC online services account."
    }

    val continue: String = "Continue"
  }

}
