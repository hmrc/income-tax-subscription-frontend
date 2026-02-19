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

package views.agent.handoffs

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.agent.handoffs.CheckClientIncomeSources

class CheckClientIncomeSourcesViewSpec extends ViewSpec {
  private val checkClientIncomeSources: CheckClientIncomeSources = app.injector.instanceOf[CheckClientIncomeSources]

  "CheckClientIncomeSources" must {
    "have the correct template details" in
      new TemplateViewTest(
        view = page(),
        isAgent = true,
        title = CheckClientIncomeSources.title
      )

    "have the correct heading" in {
      document().mainContent.selectHead("h1").text mustBe CheckClientIncomeSources.heading
    }

    "have the correct first paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe CheckClientIncomeSources.p1
    }

    "have the correct second paragraph" in {
      document().mainContent.selectNth("p", 2).text mustBe CheckClientIncomeSources.p2
    }

    "have the correct first bullet point with text and link" in {
      val firstBullet = document().mainContent.selectHead("ul").selectNth("li", 1)
      val link = firstBullet.selectHead("a")

      link.text mustBe CheckClientIncomeSources.bullet1
      link.attr("href") mustBe appConfig.getClientUTRUrl
    }

    "have the correct second bullet point with text and link" in {
      val secondBullet = document().mainContent.selectHead("ul").selectNth("li", 2)
      val link = secondBullet.selectHead("a")

      link.text mustBe CheckClientIncomeSources.bullet2
      link.attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
    }
  }

  private object CheckClientIncomeSources {
    val title = "Confirm your client’s income sources"
    val heading = "Confirm your client’s income sources"
    val p1 = "Your client is already signed up to use Making Tax Digital for Income Tax."
    val p2 = "You can either:"
    val bullet1 = "confirm your client’s income sources"
    val bullet2 = "sign up another client"
  }

  private def page(): Html = {
    checkClientIncomeSources()
  }

  private def document(): Document =
    Jsoup.parse(page().body)

}