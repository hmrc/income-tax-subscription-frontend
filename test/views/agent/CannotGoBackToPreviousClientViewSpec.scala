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

package views.agent

import config.MockConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.agent.matching.CannotGoBackToPreviousClient

class CannotGoBackToPreviousClientViewSpec extends ViewSpec {

  private val cannotGoBackToPreviousClient: CannotGoBackToPreviousClient = app.injector.instanceOf[CannotGoBackToPreviousClient]

  val messages = config.MockConfig.mockMessages

  "cannot go back to previous client" must {
    "have the correct template details" when {
      "the page has no error" in new TemplateViewTest(
        view = page(),
        isAgent = true,
        title = CannotGoBack.heading
      )
    }

    "have a heading" in {
      document().getH1Element.text mustBe CannotGoBack.heading
    }

    "have a body" which {
      val paragraphs: Elements = document().select(".govuk-body").select("p")
      val bullets: Elements = document().select(".govuk-list").select("li")

      "has paragraph " in {
        paragraphs.get(0).text() mustBe CannotGoBack.para1
        paragraphs.get(1).text() mustBe CannotGoBack.para2
      }

      "has bulleted list" which {
        "has correct text" in {
          bullets.get(0).text() mustBe CannotGoBack.agentServiceAccountOptionText
          bullets.get(1).text() mustBe CannotGoBack.reenterClientDetailsOptionText
          bullets.get(2).text() mustBe CannotGoBack.signUpAnotherClientOptionText
        }
        "has correct links" in {
          bullets.select(".govuk-link").get(0).attr("href") mustBe CannotGoBack.agentServiceAccountOptionLink
          bullets.select(".govuk-link").get(1).attr("href") mustBe CannotGoBack.reenterClientDetailsOptionLink
          bullets.select(".govuk-link").get(2).attr("href") mustBe CannotGoBack.signUpAnotherClientOptionLink
        }
      }
    }


  }

  private def page(): Html =
    cannotGoBackToPreviousClient()(request,messages,MockConfig)

  private def document(): Document =
    Jsoup.parse(page().body)

  private object CannotGoBack {
    val heading = "Sorry, there is a problem"
    val para1 = "Choose what you want to do next."
    val para2 = "You can:"
    val agentServiceAccountOptionLink = "/agent-services-account"
    val agentServiceAccountOptionText = "Go to your agent service account, if you finished signing the client up."
    val reenterClientDetailsOptionLink = "/report-quarterly/income-and-expenses/sign-up/client/add-another"
    val reenterClientDetailsOptionText = "Re-enter the details of a client you did not finish signing up"
    val signUpAnotherClientOptionLink = "/report-quarterly/income-and-expenses/sign-up/client/add-another"
    val signUpAnotherClientOptionText = "Sign up another client"
  }

}
