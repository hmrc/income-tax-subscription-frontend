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

package views.individual

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.individual.WhatYouNeedToDo

class WhatYouNeedToDoSpec extends ViewSpec {

  val whatYouNeedToDo: WhatYouNeedToDo = app.injector.instanceOf[WhatYouNeedToDo]

  def page: HtmlFormat.Appendable = whatYouNeedToDo(testCall)

  def document: Document = Jsoup.parse(page.body)

  object WhatYouNeedToDoMessages {
    val heading: String = "What you need to do"
    val paraOne: String = "You can choose to stop using Making Tax Digital for Income Tax at any time until 6 April 2026." +
      " You do not have to let us know and you can ignore any secure message from the service." +
      " But you must file your Self Assessment tax return at the end of the year as normal."

    object MandationYear {
      val paraOne: String = "You must meet the Making Tax Digital for Income Tax requirements for 6 April 2026, if all of the following apply:"
      val bulletOneLinkText: String = "find out more about registering and sending a Self Assessment tax return (opens in new tab)"
      val bulletOne: String = s"you are registered for Self Assessment - $bulletOneLinkText"
      val bulletTwo: String = "you get income from self-employment or property, or both"
      val bulletThree: String = "your total qualifying income is more than £50,000"
    }

    object MandationPlusOneYear {
      val paraOne: String = "You must meet the Making Tax Digital for Income Tax requirements for 6 April 2027, if all of the following apply:"
      val bulletOne: String = "you are registered for Self Assessment"
      val bulletTwo: String = "you get income from self-employment or property, or both"
      val bulletThree: String = "your total qualifying income is more than £30,000"
    }

    val paraTwo: String = "You can stop using Making Tax Digital for Income Tax when the pilot ends on 5 April 2026 if your income is less than £50,000."
  }

  "WhatYouNeedToDo" must {
    "use the correct template details" in new TemplateViewTest(
      view = page,
      title = WhatYouNeedToDoMessages.heading,
      isAgent = false,
      backLink = None,
      hasSignOutLink = true
    )
    "has a page heading" in {
      document.mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMessages.heading
    }
    "has a first paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.paraOne
    }
    "for the 2026 year" must {
      "have a paragraph" in {
        document.mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.MandationYear.paraOne
      }
      "have a bullet list" which {
        def list: Element = document.mainContent.selectNth("ul", 1)

        "has a first bullet with a link that opens in a new tab" in {
          val listItem = list.selectNth("li", 1)
          listItem.text mustBe WhatYouNeedToDoMessages.MandationYear.bulletOne
          val link = listItem.selectHead("a")
          link.text mustBe WhatYouNeedToDoMessages.MandationYear.bulletOneLinkText
          link.attr("href") mustBe appConfig.govUkSendingReturnsUrl
          link.attr("target") mustBe "_blank"
        }
        "has a second bullet" in {
          list.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.MandationYear.bulletTwo
        }
        "has a third bullet" in {
          list.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.MandationYear.bulletThree
        }
      }
    }
  }

}
