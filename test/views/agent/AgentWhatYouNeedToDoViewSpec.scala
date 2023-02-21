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

package views.agent

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.agent.WhatYouNeedToDo

class AgentWhatYouNeedToDoViewSpec extends ViewSpec {

  val whatYouNeedToDo: WhatYouNeedToDo = app.injector.instanceOf[WhatYouNeedToDo]

  def page: HtmlFormat.Appendable = whatYouNeedToDo(testCall)

  def document: Document = Jsoup.parse(page.body)

  object WhatYouNeedToDoMessages {
    val heading: String = "What you need to do"
    val paraOne: String = "By taking part in this pilot you agree that you will:"
    val bulletOne: String = "use compatible software to record your client’s income and expenses"
    val bulletTwo: String = "send quarterly updates from the start of their accounting period"
    val bulletThree: String = "submit their final declaration by 31 January following their current tax year"
    val bulletFour: String = "tell HMRC if they stop trading or start a new business"

    val subHeading: String = "Your client can stop using this pilot at any time"
    val paraTwo: String = "Your client can choose to stop using Making Tax Digital for Income Tax at any time until 6 April 2026." +
      " You do not have to let us know and they can ignore any secure messages from the service."+
      " But they must file their Self Assessment by 31 January following the end of the tax year as normal."

    val openNewTab: String = "(opens in new tab)"
    val qualifyingIncomeLink = "qualifying income"
    val saTaxReturnLinkText = s" find out more about registering and sending a Self Assessment tax return $openNewTab"
    val saTaxReturnLink: String = "find out more about registering and sending a Self Assessment tax return"

    val paraThree: String = "Your client must meet the Making Tax Digital for Income Tax requirements for 6 April 2026, if all of the following apply:"
    val bulletFive: String = s"they are registered for Self Assessment ($saTaxReturnLinkText)"
    val bulletSix: String = "they get income from self-employment or property, or both"
    val bulletSeven: String = s"their total $qualifyingIncomeLink" + s" $openNewTab is more than £50,000"

    val paraFour: String = "Your client must meet the Making Tax Digital for Income Tax requirements for 6 April 2027, if all of the following apply:"
    val bulletEight: String = "they are registered for Self Assessment"
    val bulletNine: String = "they get income from self-employment or property, or both"
    val bulletTen: String = s"their total $qualifyingIncomeLink" +s" $openNewTab is more than £30,000"

    val paraFive: String = "Your client can stop using Making Tax Digital for Income Tax after the pilot ends on 5 April 2026, if their qualifying income is less than £50,000."

    val acceptAndContinue: String = "Accept and continue"

  }

  "WhatYouNeedToDo" must {
    "use the correct template details" in new TemplateViewTest(
      view = page,
      title = WhatYouNeedToDoMessages.heading,
      isAgent = true,
      backLink = None,
      hasSignOutLink = true
    )

    "has a page heading" in {
      document.mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMessages.heading
    }

    "has a first paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.paraOne
    }

    "has a first bullet point" in {
      document.mainContent.selectNth("ul", 1).selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.bulletOne
    }

    "has a second bullet point" in {
      document.mainContent.selectNth("ul", 1).selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.bulletTwo
    }

    "has a third bullet point" in {
      document.mainContent.selectNth("ul", 1).selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.bulletThree
    }

    "has a four bullet point" in {
      document.mainContent.selectNth("ul", 1).selectNth("li", 4).text mustBe WhatYouNeedToDoMessages.bulletFour
    }

    "has a subheading" in {
      document.mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.subHeading
    }

    "has a second paragraph" in {
      document.mainContent.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.paraTwo
    }

    "has a thrid paragraph" in {
      document.mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.paraThree
    }

    "has a fifth bullet point" in {
      document.mainContent.selectNth("ul", 2).selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.bulletFive
    }

    "has a sixth bullet point" in {
      document.mainContent.selectNth("ul", 2).selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.bulletSix
    }

    "has a seventh bullet point" in {
      document.mainContent.selectNth("ul", 2).selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.bulletSeven
    }

    "has a fourth paragraph" in {
      document.mainContent.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.paraFour
    }

    "has a eighth bullet point" in {
      document.mainContent.selectNth("ul", 3).selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.bulletEight
    }

    "has a ninth bullet point" in {
      document.mainContent.selectNth("ul", 3).selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.bulletNine
    }

    "has a tenth bullet point" in {
      document.mainContent.selectNth("ul", 3).selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.bulletTen
    }

    "has a fifth paragraph" in {
      document.mainContent.selectNth("p", 6).text mustBe WhatYouNeedToDoMessages.paraFive
    }

    "has a accept and continue button" in {
      document.mainContent.selectHead("button").text mustBe WhatYouNeedToDoMessages.acceptAndContinue
    }
  }

}