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

package views.individual

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.LoadingSpinner

class LoadingSpinnerViewSpec extends ViewSpec {

  private val loadingSpinner: LoadingSpinner = app.injector.instanceOf[LoadingSpinner]

  "LoadingSpinner" must {
    "have the correct template details" in new TemplateViewTest(
      view = page,
      isAgent = false,
      title = LoadingSpinner.heading
    )
    "have the correct heading" in {
      document.mainContent.selectHead("h1").text mustBe LoadingSpinner.heading
    }
    "have the correct first paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe LoadingSpinner.paragraphOne
    }
  }

  private object LoadingSpinner {
    val heading = "Confirming, please wait"
    val paragraphOne = "Do not refresh this page."
  }

  private def page: Html = {
    loadingSpinner()
  }

  private def document: Document =
    Jsoup.parse(page.body)

}