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

package views.agent.tasklist.taxyear

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.agent.tasklist.taxyear.NonEligibleMandated

class NonEligibleMandatedViewSpec extends ViewSpec {

  val clientName: String = "FirstName LastName"
  val clientNino: String = "AA 11 11 11 A"

  val startYear = 2025
  val endYear = 2026

  val nonEligibleMandated: NonEligibleMandated = app.injector.instanceOf[NonEligibleMandated]

  def page(clientName: String, clientNino: String): HtmlFormat.Appendable = {
    nonEligibleMandated(
      testCall, clientName, clientNino, startYear, endYear
    )
  }

  def document(): Document = {
    Jsoup.parse(page(clientName, clientNino).body)
  }

  "NonEligibleMandated" when {
    "the body of the content" should {
      "have a heading and caption" in {
        document().mainContent.mustHaveHeadingAndCaption(
          heading = NonEligibleMandatedMessages.heading,
          caption = NonEligibleMandatedMessages.caption,
          isSection = false
        )
      }

      "have a paragraph" in {
        document().mainContent.selectNth("p", 1).text mustBe NonEligibleMandatedMessages.para
      }

      "have the correct inset" in {
        document().mainContent.selectNth(".govuk-inset-text", 1).text mustBe NonEligibleMandatedMessages.insert
      }

      "have a continue button" in {
        document().mainContent.selectHead(".govuk-button").text mustBe NonEligibleMandatedMessages.continue
      }

      "have a paragraph below the button" in {
        document().mainContent.selectNth("p", 2).text mustBe NonEligibleMandatedMessages.continuePara
      }

      "contain a link" in {
        val link = document().mainContent.selectNth("a.govuk-link", 1)
        link.text mustBe NonEligibleMandatedMessages.linkText
        link.attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
      }
    }
  }

  object NonEligibleMandatedMessages {
    val title = s"Your client must use Making Tax Digital for Income Tax next tax year, ${startYear + 1} to ${endYear + 1}"
    val heading = s"Your client must use Making Tax Digital for Income Tax next tax year, ${startYear + 1} to ${endYear + 1}"
    val caption = s"$clientName – $clientNino"
    val para = s"You can sign up this client for Making Tax Digital for Income Tax now. You’ll be signing them up for next tax year (6 April ${startYear + 1} to 5 April ${endYear + 1})."
    val insert = s"You’ll need to make sure your client submits their Self Assessment tax return for the current tax year (6 April $startYear to 5 April $endYear) as normal."
    val continue: String = "Sign up this client"
    val linkText = "check if you can sign up another client"
    val continuePara = s"Or you can $linkText. We will not save the details you entered about $clientName."
  }
}
