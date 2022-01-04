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

package views.individual.incometax.business

import assets.MessageLookup.ProgressSaved
import controllers.SignOutController
import org.jsoup.Jsoup
import utilities.ViewSpec
import views.html.individual.incometax.business.ProgressSaved

class ProgressSavedViewSpec extends ViewSpec {
  val progressSavedView: ProgressSaved = app.injector.instanceOf[ProgressSaved]

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
