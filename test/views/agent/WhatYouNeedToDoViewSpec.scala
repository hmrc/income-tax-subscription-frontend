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
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.agent.WhatYouNeedToDo

import scala.util.Random

class WhatYouNeedToDoViewSpec extends ViewSpec {

  private val nameLengthCharacters = 10
  private val clientName = Random.alphanumeric.take(nameLengthCharacters).mkString
  private val clientNino = Random.alphanumeric.take(nameLengthCharacters).mkString

  val whatYouNeedToDo: WhatYouNeedToDo = app.injector.instanceOf[WhatYouNeedToDo]

  "WhatYouNeedToDo" when {
    "the body of the content" should {
      "have a page heading" in {
        document().mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMessages.heading
      }

      "have a page first sub-heading" in {
        document().mainContent.selectNth("h2", 2).text mustBe WhatYouNeedToDoMessages.h2_1
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
        document().mainContent.selectNth("h2", 3).text mustBe WhatYouNeedToDoMessages.h2_2
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
    val h2_1 = "If your client is signing up voluntarily"
    val p1_1 = "You and your client are agreeing that our new penalties (opens in new tab)"
    val p1_2 = "will apply if your client’s tax return is sent late, or their tax bill is paid late."
    val p2 = "Whilst your client is a volunteer, penalties will not apply for submitting quarterly updates late."
    val p3 = "Your client can opt out of Making Tax Digital For Income Tax at any time. If they do this, the new penalties will still apply."
    val h2_2 = "If your client is required to use Making Tax Digital for Income Tax"
    val p4 = "The new penalties will apply to your client if their quarterly update or tax return is sent late, or payment is made after the due date."
    val acceptAndContinue: String = "Accept and continue"
  }
}
