/*
 * Copyright 2026 HM Revenue & Customs
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
import views.html.individual.tasklist.taxyear.NonEligibleMandated

class NonEligibleMandatedViewSpec extends ViewSpec {

  val startYear = 2025
  val endYear = 2026

  def nonEligibleVoluntary: NonEligibleMandated = app.injector.instanceOf[NonEligibleMandated]

  def page(): HtmlFormat.Appendable =
    nonEligibleVoluntary(
      testCall, startYear, endYear
    )

  def document(): Document = Jsoup.parse(page().body)

  "NonEligibleMandated" must {
    "have a page heading" in {
      document().mainContent.selectHead("h1").text mustBe NonEligibleMandatedMessages.heading
    }

    "have a paragraph with a link" in {
      document().mainContent.selectNth("p", 1).text mustBe NonEligibleMandatedMessages.para

      val link = document().mainContent.selectHead(".govuk-link")
      link.text mustBe NonEligibleMandatedMessages.linkText
      link.attr("href") mustBe "https://www.gov.uk/self-assessment-tax-returns/sending-return"
    }

    "have a continue button" in {
      document().mainContent.selectHead(".govuk-button").text mustBe NonEligibleMandatedMessages.continue
    }
  }

  object NonEligibleMandatedMessages {
    val title = s"You must start using Making Tax Digital for Income Tax next tax year, ${startYear + 1} to ${endYear + 1}"
    val heading = s"You must start using Making Tax Digital for Income Tax next tax year, ${startYear + 1} to ${endYear + 1}"
    val para = s"You must still submit your Self Assessment tax return (opens in new tab) for this tax year, $startYear to $endYear, as normal."
    val linkText = "Self Assessment tax return (opens in new tab)"
    val continue: String = "Continue"
  }
}