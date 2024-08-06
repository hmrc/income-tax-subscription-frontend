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

package views.agent.tasklist

import org.jsoup.Jsoup
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.html.agent.tasklist.ProgressSaved

class ProgressSavedViewSpec extends ViewSpec {

  private val progressSavedView = app.injector.instanceOf[ProgressSaved]

  object ProgressSaved {
    val title = "Your progress has been saved - Use software to report your client’s Income Tax - GOV.UK"

    def contentSummary(expirationDate: String) = s"We will keep your client’s data until $expirationDate."

    val subheading = "What you can do next"
    val paragraph1 = "You can:"
    val bullet1 = "continue signing up this client"
    val bullet2 = "sign up another client"
    val paragraph2 = "If you sign out, you’ll need to sign in again using the same Government Gateway User ID."
  }

  "Progress saved view" must {
    "have a title" in {
      document().title mustBe ProgressSaved.title
    }

    "have a summary" in {
      document().select(".govuk-notification-banner__heading").text mustBe ProgressSaved.contentSummary("Monday, 20 October 2021")
    }

    "have a subheading" in {
      document().mainContent.select("h1").text mustBe ProgressSaved.subheading
    }

    "have a paragraph 1" in {
      document().mainContent.selectNth("p.govuk-body", 1).text mustBe ProgressSaved.paragraph1
    }

    "have a bullet 1" in {
      document().select(".govuk-list--bullet").select("li:nth-of-type(1)").text mustBe ProgressSaved.bullet1
    }

    "have a bullet 2" in {
      document().select(".govuk-list--bullet").select("li:nth-of-type(2)").text mustBe ProgressSaved.bullet2
    }

    "have a paragraph 2" in {
      document().mainContent.selectNth("p.govuk-body", 2).text mustBe ProgressSaved.paragraph2
    }

    "have a sign up link" in {
      document().mainContent.select("a.sign-up-link").attr("href")  mustBe controllers.agent.tasklist.routes.TaskListController.show().url
    }

    "have a sign out link" in {
      document().mainContent.select("a.sign-out-link").attr("href")  mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
    }
  }

  private def page(expirationDate: String) = progressSavedView(expirationDate, "sign-in", ClientDetails("FirstName LastName", "ZZ111111Z"))

  private def document(expirationDate: String = "Monday, 20 October 2021") = Jsoup.parse(page(expirationDate).body)
}
