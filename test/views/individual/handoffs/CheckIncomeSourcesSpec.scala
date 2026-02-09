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

package views.individual.handoffs

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.handoffs.CheckIncomeSources
import config.AppConfig

class CheckIncomeSourcesSpec extends ViewSpec {
  private val checkIncomeSources: CheckIncomeSources = app.injector.instanceOf[CheckIncomeSources]

  "CheckIncomeSources" must {
    "have the correct template details" in
      {
        new TemplateViewTest(
          view = page(),
          title = CheckIncomeSources.title
        )
      }

    "have the correct heading" in {
      {
        document().mainContent.selectHead("h1").text mustBe CheckIncomeSources.heading
      }
    }

    "have the correct first paragraph" in {
      {
        document().mainContent.selectNth("p", 1).text mustBe CheckIncomeSources.p1
      }
    }

    "have the correct second heading" in {
      {
        document().mainContent.selectHead("h2").text mustBe CheckIncomeSources.heading2
      }
    }

    "have the correct second paragraph" in {
      document().mainContent.selectNth("p", 2).text mustBe CheckIncomeSources.p2
    }

    "has a continue button" in {
      document().mainContent.selectHead(".govuk-button").text mustBe CheckIncomeSources.continue
    }
  }

  private object CheckIncomeSources {
    val title = "You must confirm your income sources"
    val heading = "You must confirm your income sources"
    val p1 = "You’re already signed up to Making Tax Digital for Income Tax."
    val heading2 = "Next steps"
    val p2 = "You need to confirm your sole trader and property income sources are up to date."
    val continue = "Continue"
  }

  private def page(): Html = {
    checkIncomeSources(
      postAction = Call("", ""),
    )
  }

  private def document(): Document =
    Jsoup.parse(page().body)

}
