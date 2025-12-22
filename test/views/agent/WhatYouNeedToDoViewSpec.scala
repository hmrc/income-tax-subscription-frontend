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

import scala.util.Random

class WhatYouNeedToDoViewSpec extends ViewSpec {

  val clientName: String = "FirstName LastName"
  val clientNino: String = "AA 11 11 11 A"

  val whatYouNeedToDo: WhatYouNeedToDo = app.injector.instanceOf[WhatYouNeedToDo]

  "WhatYouNeedToDo" when {
    "the body of the content" should {
      "have a heading and caption" in {
        document().mainContent.mustHaveHeadingAndCaption(
          heading = WhatYouNeedToDoMessages.heading,
          caption = s"$clientName | $clientNino",
          isSection = false
        )
      }

      "have a page first sub-heading" in {
        document().mainContent.selectNth("h2", 2).text mustBe WhatYouNeedToDoMessages.h2_1
      }

      "have the first paragraph" in {
        document().mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.p1
      }

      "have a second paragraph" in {
        document().mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.p2
      }

      "have a page second sub-heading" in {
        document().mainContent.selectNth("h2", 3).text mustBe WhatYouNeedToDoMessages.h2_2
      }

      "have the third paragraph" in {
        document().mainContent.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.p3
      }

      "have the fourth paragraph" in {
        document().mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.p4
      }

      "have the fifth paragraph" in {
        document().mainContent.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.p5
      }

      "have the sixth paragraph" in {
        val paragraph: Element = document().mainContent.selectNth("p", 6)
        paragraph.text mustBe WhatYouNeedToDoMessages.p6

        val link: Element = paragraph.selectHead("a")
        link.attr("href") mustBe WhatYouNeedToDoMessages.p6_link
      }

      "have a form" which {
        "has the correct attributes" in {
          document().selectHead("form").attr("method") mustBe testCall.method
          document().selectHead("form").attr("action") mustBe testCall.url
        }

        "has an accept and continue button to submit the form" in {
          document().selectHead("form").selectHead("button").text mustBe WhatYouNeedToDoMessages.agreeAndContinue
        }
      }
    }
  }

  def page(clientName: String, clientNino: String): HtmlFormat.Appendable = {
    whatYouNeedToDo(
      postAction = testCall,
      clientName = clientName,
      clientNino = clientNino,
      backUrl = testBackUrl
    )
  }

  def document(): Document = {
    Jsoup.parse(page(clientName, clientNino).body)
  }

  object WhatYouNeedToDoMessages {
    val title = "What penalties apply in Making Tax Digital for Income Tax"
    val heading = "What penalties apply in Making Tax Digital for Income Tax"
    val h2_1 = "If your client is required to use Making Tax Digital for Income Tax"
    val p1 = "If your client is required to use Making Tax Digital for Income Tax from 6 April 2026, HMRC will not apply penalty points for late quarterly updates for the first tax year (2026 to 2027)."
    val p2 = "Penalties will still apply for late tax returns or if a tax bill is paid after the due date."
    val h2_2 = "If your client is signing up voluntarily"
    val p3 = "You and your client are agreeing that our new penalties will apply if your client’s tax return is sent late, or their tax bill is paid late."
    val p4 = "Whilst your client is a volunteer, penalties will not apply for submitting quarterly updates late."
    val p5 = "Your client can opt out of Making Tax Digital For Income Tax at any time. If they do this, the new penalties will still apply."
    val p6 = "Read more about penalties that apply if you are volunteering. (opens in new tab)"
    val p6_link = "https://www.gov.uk/guidance/penalties-for-income-tax-self-assessment-volunteers"
    val agreeAndContinue: String = "Agree and continue"
  }
}
