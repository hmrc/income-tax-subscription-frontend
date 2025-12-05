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

package views.individual.tasklist

import org.jsoup.Jsoup
import utilities.ViewSpec
import views.html.individual.tasklist.ProgressSaved

class ProgressSavedViewSpec extends ViewSpec {

  val progressSavedView: ProgressSaved = app.injector.instanceOf[ProgressSaved]

  object ProgressSaved {
    val title = "Save and come back later - Sign up for Making Tax Digital for Income Tax - GOV.UK"
    val bannerTitle = "Progress saved"

    def contentSummary(expirationDate: String) = s"We’ll save your data until $expirationDate"

    val subheading = "What you can do next"
    val paragraph1 = "If you sign out, you’ll need to sign in again using the user ID and password you use for your Self Assessment."
    val paragraph2 = "Or you can continue signing up"
  }

  "Progress saved view" must {
    "have a title" in {
      document().title mustBe ProgressSaved.title
    }

    "have a summary" in {
      document().select(".govuk-notification-banner__heading strong").text mustBe ProgressSaved.contentSummary("Monday, 20 October 2021")
    }

    "have a subheading" in {
      document().select("h1.govuk-heading-l").text mustBe ProgressSaved.subheading
    }

    "have a paragraph 1" in {
      document().select("p.govuk-body").get(0).text mustBe ProgressSaved.paragraph1
    }

    "have a paragraph 2" in {
      document().select("p.govuk-body").get(1).text mustBe ProgressSaved.paragraph2
    }

    "sign up link" in {
      document().select("a.sign-up-link").attr("href") mustBe controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

  private def page(expirationDate: String) = progressSavedView(expirationDate, "sign-in")

  private def document(expirationDate: String = "Monday, 20 October 2021") = Jsoup.parse(page(expirationDate).body)
}
