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

package views.agent.matching

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.agent.matching.ClientDetailsError

class ClientDetailsErrorViewSpec extends ViewSpec {

  lazy val clientDetailsError: ClientDetailsError = app.injector.instanceOf[ClientDetailsError]
  lazy val page: HtmlFormat.Appendable = clientDetailsError()(request, implicitly)
  lazy val document: Document = Jsoup.parse(page.body)
  lazy val mainContent: Element = document.mainContent

  "ClientDetailsError" must {
    "be using the correct template" in new TemplateViewTest(
      page,
      title = ClientDetailsErrorMessages.heading,
      isAgent = true,
      backLink = None,
      hasSignOutLink = true
    )

    "have a heading" in {
      mainContent.getH1Element.text mustBe ClientDetailsErrorMessages.heading
    }

    "have a first line" in {
      mainContent.selectNth("p", 1).text mustBe ClientDetailsErrorMessages.lineOne
    }

    "have a second line" in {
      val secondLine = mainContent.selectNth("p", 2)
      secondLine.text mustBe ClientDetailsErrorMessages.lineTwo

      val link = secondLine.selectHead("a")
      link.text mustBe ClientDetailsErrorMessages.lineTwoLink
      link.attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
    }
  }

  object ClientDetailsErrorMessages {
    val heading: String = "There is a problem"
    val lineOne: String = "The details you entered do not match our records."
    val lineTwoLink: String = "try again"
    val lineTwo: String = s"Check the details and $lineTwoLink."
  }
}
