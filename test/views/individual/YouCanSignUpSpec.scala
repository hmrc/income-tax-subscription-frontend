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

package views.individual

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.eligibility.YouCanSignUp

class YouCanSignUpSpec extends ViewSpec {
  private val youCanSignUp: YouCanSignUp = app.injector.instanceOf[YouCanSignUp]

  "YouCanSignUp" must {
    "have the correct template details" in new TemplateViewTest(
      view = page,
      isAgent = false,
      title = YouCanSignUp.heading
    )
    "have the correct heading" in {
      document.mainContent.selectHead("h1").text mustBe YouCanSignUp.heading
    }
    "have the correct first paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe YouCanSignUp.paragraphOne
    }
    "have the correct second paragraph" in {
      document.mainContent.selectNth("p", 2).text mustBe YouCanSignUp.paragraphTwo
    }
    "have the correct bullet list" in {
      val bulletList = document.mainContent.selectNth("ul", 1)
      bulletList.selectNth("li", 1).text mustBe YouCanSignUp.bulletOne
      bulletList.selectNth("li", 2).text mustBe YouCanSignUp.bulletTwo
      bulletList.selectNth("li", 3).text mustBe YouCanSignUp.bulletThree
      bulletList.selectNth("li", 4).text mustBe YouCanSignUp.bulletFour
    }
    "have third paragraph" in {
      document.mainContent.selectNth("p", 3).text mustBe YouCanSignUp.paragraphThree
    }
    "have the correct fourth paragraph" in {
      document.mainContent.selectNth("p", 4).text mustBe YouCanSignUp.paragraphFour
    }
  }

  private object YouCanSignUp {
    val heading = "You can sign up now"
    val paragraphOne = "If you continue, we’ll ask you about your sole trader business and income from property."
    val paragraphTwo = "If you’re a sole trader, you’ll need the following:"
    val bulletOne = "trade (the nature of your business)"
    val bulletTwo = "business trading name (if it is not your own name)"
    val bulletThree = "business start date - if it started within the last 2 tax years"
    val bulletFour = "business address"
    val paragraphThree = "If you get income from property, we’ll ask whether the property is in the UK or another country. We’ll also ask when you started getting income from property."
    val paragraphFour = "You’ll also need to choose if you want to sign up during the current tax year or from next tax year."
  }

  private def page: Html = {
    youCanSignUp(
      postAction = controllers.individual.sps.routes.SPSHandoffController.redirectToSPS
    )
  }

  private def document: Document =
    Jsoup.parse(page.body)

}
