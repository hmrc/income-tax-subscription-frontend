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

package views.individual.claimenrolment

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.claimenrolment.NotSubscribed

class NotSubscribedViewSpec extends ViewSpec {

  val notSubscribed: NotSubscribed = app.injector.instanceOf[NotSubscribed]

  val view: Html = notSubscribed()
  val page: Document = Jsoup.parse(view.body)
  val mainContent: Element = page.mainContent

  object Messages {
    val heading: String = "Sorry, there is a problem with the service"
    val info: String = "You are not signed up for Making Tax Digital for Income Tax. Return to your business tax account."
    val linkText: String = "business tax account."
  }

  "NotSubscribed" must {
    "display the template correctly" in new TemplateViewTest(
      view = view,
      title = Messages.heading,
      isAgent = false,
      hasSignOutLink = true
    )
    "display the page heading" in {
      mainContent.getH1Element.text mustBe Messages.heading
    }
    "display an info paragraph with a link" which {
      "has the correct full text" in {
        mainContent.getNthParagraph(1).text mustBe Messages.info
      }
      "has the correct link to bta" in {
        val btaLink: Element = mainContent.getNthParagraph(1).selectHead("a")
        btaLink.text mustBe Messages.linkText
        btaLink.attr("href") mustBe appConfig.btaUrl
      }
    }
  }
}
