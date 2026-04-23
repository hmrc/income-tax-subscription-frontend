/*
 * Copyright 2026 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.agent.ClientLoadingSpinner

class ClientLoadingSpinnerViewSpec extends ViewSpec {

  private val clientLoadingSpinner: ClientLoadingSpinner = app.injector.instanceOf[ClientLoadingSpinner]

  "ClientLoadingSpinner" must {
    "have the correct template details" in new TemplateViewTest(
      view = page,
      isAgent = true,
      hasBackLink = false,
      title = ClientLoadingSpinner.heading
    )
    "have the correct heading" in {
      document.mainContent.selectHead("h1").text mustBe ClientLoadingSpinner.heading
    }
    "have the correct first paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe ClientLoadingSpinner.paragraphOne
    }
  }

  private object ClientLoadingSpinner {
    val heading = "Confirming, please wait"
    val paragraphOne = "Do not refresh this page."
  }

  private def page: Html = {
    clientLoadingSpinner()
  }

  private def document: Document =
    Jsoup.parse(page.body)

}
