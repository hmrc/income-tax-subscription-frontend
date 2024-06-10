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

package views.agent.tasklist.addbusiness

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.agent.tasklist.addbusiness.BusinessAlreadyRemoved

class BusinessAlreadyRemovedViewSpec extends ViewSpec {

  val businessAlreadyRemoved: BusinessAlreadyRemoved = app.injector.instanceOf[BusinessAlreadyRemoved]

  val page: HtmlFormat.Appendable = businessAlreadyRemoved()

  val document: Document = Jsoup.parse(page.body)

  "BusinessAlreadyRemoved" must {
    "be using the correct template details" in new TemplateViewTest(
      view = page,
      isAgent = true,
      title = BusinessAlreadyRemovedMessages.heading
    )

    "have a heading" in {
      document.mainContent.getH1Element.text mustBe BusinessAlreadyRemovedMessages.heading
    }

    "contains a paragraph with link" in {
      val link = document.mainContent.selectHead("a")
      link.attr("href") mustBe controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
      link.text mustBe BusinessAlreadyRemovedMessages.paraLinkText
      document.mainContent.selectHead("p").text() mustBe BusinessAlreadyRemovedMessages.para
    }
  }

  object BusinessAlreadyRemovedMessages {
    val heading: String = "You have already removed this income source"
    val paraLinkText: String = "your client’s income sources"
    val para: String = s"Go back to $paraLinkText"
  }

}