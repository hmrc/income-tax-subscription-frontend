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

package views.individual.tasklist.taxyear

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.individual.tasklist.taxyear.NonEligibleVoluntary

class NonEligibleVoluntaryViewSpec extends ViewSpec {

  val startYear = 2025
  val endYear = 2026

  def nonEligibleVoluntary: NonEligibleVoluntary = app.injector.instanceOf[NonEligibleVoluntary]

  def page(): HtmlFormat.Appendable =
    nonEligibleVoluntary(
      testCall, startYear, endYear
    )

  def document(): Document = Jsoup.parse(page().body)

  object NonEligibleVoluntaryMessages {
    val title = s"You can sign up next tax year, ${startYear + 1} to ${endYear + 1}"
    val heading = s"You can use Making Tax Digital for Income Tax next tax year, ${startYear + 1} to ${endYear + 1}"
    val para = s"You must still submit your Self Assessment tax return for this tax year, $startYear to $endYear, as normal."
    val continue: String = "Continue"
  }

  "MomEligibleVoluntary" must {
    "have a page heading" in {
      document().mainContent.selectHead("h1").text mustBe NonEligibleVoluntaryMessages.heading
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
    }
  }
}
