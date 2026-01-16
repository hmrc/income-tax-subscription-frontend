/*
 * Copyright 2024 HM Revenue & Customs
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

import messagelookup.agent.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.agent.UsingSoftware

class UsingSoftwareViewSpec extends ViewSpec {

  private val usingSoftware: UsingSoftware = app.injector.instanceOf[UsingSoftware]

  "using software" must {
    import UsingSoftware.*

    "have the correct template details" when {
      "the page has a heading" in new TemplateViewTest(
        view = page(),
        isAgent = true,
        title = heading
      )
    }

    "have a first paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe paraOne
    }

    "have a second paragraph" in {
      document().mainContent.selectNth("p", 2).text mustBe paraTwo
    }

    "have the link text" in {
      document().mainContent.selectNth("p", 3).text mustBe linkText
    }

    "have a link " in {
      document().mainContent.selectNth("p", 3).selectHead("a").attr("href") mustBe linkHref
    }

    "has a continue button" in {
      document().select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
    }

  }

  private def page(): Html = {
    usingSoftware(
      testCall,
      backUrl = testBackUrl
    )
  }

  private def document(): Document =
    Jsoup.parse(page().body)

  private object UsingSoftware {
    val heading = "Check you have compatible software"
    val paraOne = "To use this service, you or your client must use software that works with Making Tax Digital for Income Tax."
    val paraTwo = "If you already use software to keep digital records for your clients, you may need to ask your software provider if it works with Making Tax Digital for Income Tax."
    val linkText = "Find software that works with Making Tax Digital for Income Tax (opens in new tab)"
    val linkHref = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
  }

}
