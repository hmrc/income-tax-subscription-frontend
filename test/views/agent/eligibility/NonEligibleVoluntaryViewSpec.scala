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

package views.agent.eligibility

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.agent.WhatYouNeedToDo
import views.html.agent.eligibility.NonEligibleVoluntary

import scala.util.Random

class NonEligibleVoluntaryViewSpec extends ViewSpec {

  val clientName: String = "FirstName LastName"
  val clientNino: String = "AA 11 11 11 A"

  val nonEligibleVoluntary: NonEligibleVoluntary = app.injector.instanceOf[NonEligibleVoluntary]

  def page(clientName: String, clientNino: String): HtmlFormat.Appendable = {
    nonEligibleVoluntary(
      postAction = testCall,
      clientName = clientName,
      clientNino = clientNino
    )
  }

  def document(): Document = {
    Jsoup.parse(page(clientName, clientNino).body)
  }

  object NonEligibleVoluntaryMessages {
    val title = "Your client can sign up next tax year, 2026 to 2027"
    val heading = "Your client can use Making Tax Digital for Income Tax next tax year, 2026 to 2027 if they choose"
    val caption = s"$clientName – $clientNino"
    val para = "They must still submit their Self Assessment tax return for this tax year, 2025 to 2026 as normal."
    val continue: String = "Sign up this client"
    val continuePara = s"Or you can check if you can sign up another client. We will not save the details you entered about $clientName."
  }

  "NonEligibleVoluntary" when {
    "the body of the content" should {
      "have a heading and caption" in {
        document().mainContent.mustHaveHeadingAndCaption(
          heading = NonEligibleVoluntaryMessages.heading,
          caption = NonEligibleVoluntaryMessages.caption,
          isSection = false
        )
      }

      "have a paragraph" in {
        document().mainContent.selectNth("p", 1).text mustBe NonEligibleVoluntaryMessages.para
      }

      "have a form" which {
        "has the correct attributes" in {
          document().selectHead("form").attr("method") mustBe testCall.method
          document().selectHead("form").attr("action") mustBe testCall.url
        }

        "has an accept and continue button to submit the form" in {
          document().selectHead("form").selectHead("button").text mustBe NonEligibleVoluntaryMessages.continue
        }

        "have a paragraph" in {
          document().selectHead("form").selectNth("p", 1).text mustBe NonEligibleVoluntaryMessages.continuePara
        }
      }
    }
  }
}
