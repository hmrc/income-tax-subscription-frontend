/*
 * Copyright 2022 HM Revenue & Customs
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

package views.agent.business

import controllers.SignOutController
import org.jsoup.Jsoup
import utilities.ViewSpec
import views.html.agent.business.ProgressSaved

class ProgressSavedViewSpec extends ViewSpec {
  private val progressSavedView = app.injector.instanceOf[ProgressSaved]

  object ProgressSaved {
    val title = "Your progress has been saved - Use software to report your client’s Income Tax - GOV.UK"
    val heading = "Your progress has been saved"

    def contentSummary(expirationDate: String) = s"We will keep your information until $expirationDate."

    val subheading = "What happens next"
    val paragraph1 = "You can:"
    val bullet1 = "return to sign up your client’s business for Making Tax Digital for Income Tax"
    val bullet2 = "sign out and come back later"
    val paragraph2 = "If you sign out, you will need to come back to your Government Gateway login to continue. We suggest you bookmark this to make it easier to find when you return."
  }

  "Progress saved view" must {
    "have a title" in {
      document().title mustBe ProgressSaved.title
    }

    "have a heading" in {
      document().select(".govuk-panel__title").text mustBe ProgressSaved.heading
    }

    "have a summary" in {
      document().select(".govuk-panel__body").text mustBe ProgressSaved.contentSummary("Monday, 20 October 2021")
    }

    "have a subheading" in {
      document().mainContent.select("h2").text mustBe ProgressSaved.subheading
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

    "have a paragraph 2 link" in {
      document().mainContent.selectNth("p.govuk-body", 2).select("a").attr("href") mustBe "sign-in"
    }

    "have a sign up link" in {
      document().mainContent.select("a.sign-up-link").attr("href")  mustBe controllers.individual.business.routes.TaskListController.show().url
    }

    "have a sign out link" in {
      document().mainContent.select("a.sign-out-link").attr("href")  mustBe SignOutController.signOut.url
    }
  }

  private def page(expirationDate: String) = progressSavedView(expirationDate, "sign-in")

  private def document(expirationDate: String = "Monday, 20 October 2021") = Jsoup.parse(page(expirationDate).body)
}
