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

  val startYear = 2025
  val endYear = 2026

  val nonEligibleVoluntary: NonEligibleVoluntary = app.injector.instanceOf[NonEligibleVoluntary]

  def page(clientName: String, clientNino: String): HtmlFormat.Appendable = {
    nonEligibleVoluntary(
      testCall, clientName, clientNino, startYear, endYear
    )
  }

  def document(): Document = {
    Jsoup.parse(page(clientName, clientNino).body)
  }

  object NonEligibleVoluntaryMessages {
    val title = s"Your client can sign up next tax year, ${startYear + 1} to ${endYear + 1}"
    val heading = s"Your client can use Making Tax Digital for Income Tax next tax year, ${startYear + 1} to ${endYear + 1} if they choose"
    val caption = s"$clientName – $clientNino"
    val para = s"They must still submit their Self Assessment tax return for this tax year, $startYear to $endYear as normal."
    val continue: String = "Sign up this client"
    val linkText = "check if you can sign up another client"
    val continuePara = s"Or you can $linkText. We will not save the details you entered about $clientName."
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
        val form = document().selectHead("form")

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has an accept and continue button to submit the form" in {
          form.selectHead("button").text mustBe NonEligibleVoluntaryMessages.continue
        }

        "have a paragraph" in {
          form.selectNth("p", 1).text mustBe NonEligibleVoluntaryMessages.continuePara
        }

        "contains a link" in {
          form.selectNth("a.govuk-link", 1).text mustBe NonEligibleVoluntaryMessages.linkText
        }
      }
    }
  }
}
