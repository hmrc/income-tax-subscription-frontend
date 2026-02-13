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
import org.jsoup.nodes.{Document, Element}
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.handoffs.CheckIncomeSources

class CheckIncomeSourcesViewSpec extends ViewSpec {

  private val checkIncomeSources: CheckIncomeSources = app.injector.instanceOf[CheckIncomeSources]

  "CheckIncomeSources" when {
    "the user has an MTDITID enrolment" must {
      lazy val mainContent: Element = document(hasMTDITID = true).mainContent

      "have the correct template details" in new TemplateViewTest(
        view = page(hasMTDITID = true),
        title = CheckIncomeSourcesMessages.heading
      )

      "have the correct heading" in {
        mainContent.selectHead("h1").text mustBe CheckIncomeSourcesMessages.heading
      }

      "have the correct first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe CheckIncomeSourcesMessages.paraOne
      }

      "have the correct next steps heading" in {
        mainContent.selectHead("h2").text mustBe CheckIncomeSourcesMessages.NextSteps.heading
      }

      "have the correct next steps first paragraph" in {
        mainContent.selectNth("p", 2).text mustBe CheckIncomeSourcesMessages.NextSteps.paraOne
      }

      "have the correct next steps second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe CheckIncomeSourcesMessages.NextSteps.paraOne
      }

      "have a continue button" in {
        mainContent.selectHead(".govuk-button").text mustBe CheckIncomeSourcesMessages.continue
      }
    }
    "the user does not have an MTDITID enrolment" must {
      lazy val mainContent: Element = document().mainContent

      "have the correct template details" in new TemplateViewTest(
        view = page(hasMTDITID = false),
        title = CheckIncomeSourcesMessages.heading
      )

      "have the correct heading" in {
        mainContent.selectHead("h1").text mustBe CheckIncomeSourcesMessages.heading
      }

      "have the correct first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe CheckIncomeSourcesMessages.paraOne
      }

      "have the correct next steps heading" in {
        mainContent.selectHead("h2").text mustBe CheckIncomeSourcesMessages.NextSteps.heading
      }

      "have the correct next steps paragraph" in {
        mainContent.selectNth("p", 2).text mustBe CheckIncomeSourcesMessages.NextSteps.paraOne
      }

      "have a continue button" in {
        mainContent.selectHead(".govuk-button").text mustBe CheckIncomeSourcesMessages.continue
      }
    }
  }

  private object CheckIncomeSourcesMessages {
    val heading = "You must confirm your income sources"
    val paraOne = "You’re already signed up to Making Tax Digital for Income Tax."

    object NextSteps {
      val heading = "Next steps"
      val paraOne = "You need to confirm your sole trader and property income sources are up to date."
      val paraTwo = "You’ll need to sign in again using the same sign in details that you use for your Self Assessment tax return."
    }

    val continue = "Continue"
  }

  private def page(hasMTDITID: Boolean): Html = {
    checkIncomeSources(
      postAction = Call("", ""),
      hasMTDITID = hasMTDITID
    )
  }

  private def document(hasMTDITID: Boolean = false): Document =
    Jsoup.parse(page(hasMTDITID).body)

}
