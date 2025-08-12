/*
 * Copyright 2025 HM Revenue & Customs
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

package views.individual.accountingperiod

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.accountingPeriod.AccountingPeriodNotSupported

//scalastyle:off magic.number
class AccountingPeriodNotSupportedViewSpec extends ViewSpec {

  private val accountingPeriodNotSupported: AccountingPeriodNotSupported = app.injector.instanceOf[AccountingPeriodNotSupported]

  "AccountingPeriod" must {
    import AccountingPeriodNotSupportedMessages._

    "have the correct template details" in new TemplateViewTest(
        view = page,
        isAgent = false,
        title = heading,
        backLink = Some(controllers.individual.accountingperiod.routes.AccountingPeriodController.show.url)
      )

    "have a heading" in {
      document.getH1Element.text mustBe AccountingPeriodNotSupportedMessages.heading
    }

    "have the correct first paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe paraOne
    }

    "have the correct bullet points" in {
      val bulletList = document.mainContent.selectNth("ul", 1)

      bulletList.selectNth("li", 1).text mustBe bulletOne
      bulletList.selectNth("li", 2).text mustBe bulletTwo
    }

    "have the correct second paragraph" in {
      document.mainContent.selectNth("p", 2).text mustBe paraTwo
    }

    "have the correct third paragraph" in {
      document.mainContent.selectNth("p", 3).text mustBe paraThree
    }

    "have the correct fourth paragraph" which {
      "has the correct text" in {
        document.mainContent.selectNth("p", 4).text mustBe paraFour
      }
      "has a link" in {
        val link = document.mainContent.selectNth("p", 4).selectHead("a")
        link.text mustBe paraFourLinkText
        link.attr("href") mustBe paraFourLinkHref
      }
    }
  }

  private def page: Html = {
    accountingPeriodNotSupported(
      controllers.individual.accountingperiod.routes.AccountingPeriodController.show.url
    )
  }

  private def document: Document = Jsoup.parse(page.body)

  object AccountingPeriodNotSupportedMessages {
    val heading = "You cannot sign up yet"
    val paraOne = "Your business does not use an accounting period that runs from either:"
    val bulletOne = "1 April to 31 March"
    val bulletTwo = "6 April to 5 April"
    val paraTwo = "Making Tax Digital for Income Tax is only available to people who use these business accounting periods."
    val paraThree = "In the future, we may extend this service to more people."
    val paraFour = "Continue submitting your Self Assessment tax return as normal."
    val paraFourLinkText = "Self Assessment tax return"
    val paraFourLinkHref = "https://www.gov.uk/self-assessment-tax-returns/sending-return"
  }
}
// scalastyle:on magic.number
