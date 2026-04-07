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

class WhatYouNeedToDoViewSpec extends ViewSpec {

  def whatYouNeedToDo: WhatYouNeedToDo = app.injector.instanceOf[WhatYouNeedToDo]

  def page(): HtmlFormat.Appendable = whatYouNeedToDo(testCall, backUrl = "backUrl")

  def document(): Document = Jsoup.parse(page().body)

  object WhatYouNeedToDoMessages {
    val title = "What penalties apply to you in Making Tax Digital for Income Tax"
    val heading = "What penalties apply to you in Making Tax Digital for Income Tax"
    val h2_1 = "If you are required to use Making Tax Digital for Income Tax"
    val p1 = "If you’re required to use Making Tax Digital for Income Tax from 6 April 2026, HMRC will not apply penalty points for late quarterly updates for the first tax year (2026 to 2027)."
    val p2 = "Penalties will still apply for late tax returns or if you pay your tax bill after the due date."
    val h2_2 = "If you are signing up voluntarily"
    val p3 = "Read more about changes to penalties (opens in new tab)"
    val p3_link = "https://www.gov.uk/guidance/penalties-for-making-tax-digital-for-income-tax"
    val p4 = "You are agreeing that our new penalties will apply if you are late submitting your tax return or paying your tax bill."
    val p5 = "Whilst you are a volunteer, penalties will not apply for sending quarterly updates late."
    val p6 = "You can opt out of Making Tax Digital for Income Tax at any time. If you do this, the new penalties will still apply to you."
    val p7 = "Read more about penalties that apply if you are volunteering. (opens in new tab)"
    val p7_link = "https://www.gov.uk/guidance/penalties-for-income-tax-self-assessment-volunteers"
    val agreeAndContinue: String = "Agree and continue"
  }

  "WhatYouNeedToDo" must {
    "have a page heading" in {
      document().mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMessages.heading
    }

    "have a page first sub-heading" in {
      document().mainContent.selectNth("h2", 1).text mustBe WhatYouNeedToDoMessages.h2_1
    }

    "have the first paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.p1
    }

    "have a second paragraph" in {
      document().mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.p2
    }

    "have a page second sub-heading" in {
      document().mainContent.selectNth("h2", 2).text mustBe WhatYouNeedToDoMessages.h2_2
    }

    "have a third paragraph" in {
      val paragraph: Element = document().mainContent.selectNth("p", 3)
      paragraph.text mustBe WhatYouNeedToDoMessages.p3

      val link: Element = paragraph.selectHead("a")
      link.attr("href") mustBe WhatYouNeedToDoMessages.p3_link
      link.attr("target") mustBe "_blank"
      link.attr("rel") mustBe "noopener noreferrer"
    }

    "have the fourth paragraph" in {
      document().mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.p4
    }

    "have the fifth paragraph" in {
      document().mainContent.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.p5
    }

    "have the sixth paragraph" in {
      document().mainContent.selectNth("p", 6).text mustBe WhatYouNeedToDoMessages.p6
    }

    "have the seventh paragraph" in {
      val paragraph: Element = document().mainContent.selectNth("p", 7)
      paragraph.text mustBe WhatYouNeedToDoMessages.p7

      val link: Element = paragraph.selectHead("a")
      link.attr("href") mustBe WhatYouNeedToDoMessages.p7_link
      link.attr("target") mustBe "_blank"
      link.attr("rel") mustBe "noopener noreferrer"
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
