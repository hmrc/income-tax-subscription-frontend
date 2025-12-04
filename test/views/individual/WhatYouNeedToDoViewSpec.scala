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
import org.jsoup.nodes.Document
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
    val h2_1 = "If you are signing up voluntarily"
    val p1_1 = "You are agreeing that our new penalties (opens in new tab)"
    val p1_2 = "will apply if you are late sending your tax return or paying your tax bill."
    val p2 = "Whilst you are a volunteer, penalties will not apply for submitting quarterly updates late."
    val p3 = "You can opt out of Making Tax Digital For Income Tax at any time. If they do this, the new penalties will still apply."
    val h2_2 = "If you are required to use Making Tax Digital for Income Tax"
    val p4 = "The new penalties will apply to you if you are late sending a quarterly update, your tax return or you pay after the due date."
    val acceptAndContinue: String = "Accept and continue"
  }

  "WhatYouNeedToDo" must {
    "have a page heading" in {
      document().mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMessages.heading
    }

    "have a page first sub-heading" in {
      document().mainContent.selectNth("h2", 1).text mustBe WhatYouNeedToDoMessages.h2_1
    }

    "have the correct first paragraph" in {
      document().mainContent.selectNth("a", 1).text mustBe WhatYouNeedToDoMessages.p1_1
      document().mainContent.selectNth("p", 1).text mustBe Seq(WhatYouNeedToDoMessages.p1_1, WhatYouNeedToDoMessages.p1_2).mkString(" ")
    }

    "have a second paragraph" in {
      document().mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.p2
    }

    "have the correct third paragraph" in {
      document().mainContent.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.p3
    }

    "have a page second sub-heading" in {
      document().mainContent.selectNth("h2", 2).text mustBe WhatYouNeedToDoMessages.h2_2
    }

    "have the correct fourth paragraph" in {
      document().mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.p4
    }

    "have a form" which {
      "has the correct attributes" in {
        document().selectHead("form").attr("method") mustBe testCall.method
        document().selectHead("form").attr("action") mustBe testCall.url
      }
      "has an accept and continue button to submit the form" in {
        document().selectHead("form").selectHead("button").text mustBe WhatYouNeedToDoMessages.acceptAndContinue
      }
    }
  }
}
