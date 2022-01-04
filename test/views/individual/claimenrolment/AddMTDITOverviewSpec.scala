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

import assets.MessageLookup.{AddMTDITOverview => messages}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait
import views.html.individual.claimenrolment.AddMTDITOverview

class AddMTDITOverviewSpec extends ViewSpecTrait {

  val addMTDITOverview: AddMTDITOverview = app.injector.instanceOf[AddMTDITOverview]

  val action: Call = ViewSpecTrait.testCall

  class Setup {
    val page: HtmlFormat.Appendable = addMTDITOverview(
      postAction = action
    )(FakeRequest(), implicitly, appConfig)

    val document: Document = Jsoup.parse(page.body)
  }

  "Add MTD IT Overview" must {
    "have a title" in new Setup {
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      document.title mustBe messages.title + serviceNameGovUk
    }

    "have a heading" in new Setup {
      document.select("h1").text mustBe messages.heading
    }

    "have a sub-heading" in new Setup {
      document.select("h3").text mustBe messages.subHeading
    }

    "have a inset text" in new Setup {
      document.select(".govuk-inset-text").text mustBe messages.insetText
    }

    "have content" in new Setup {
      val paragraphs: Elements = document.select(".govuk-body").select("p")
      paragraphs.get(0).text() mustBe messages.paragraph1
      paragraphs.get(1).text() mustBe messages.paragraph2
      paragraphs.get(2).text() mustBe messages.paragraph3
      paragraphs.get(3).text() mustBe messages.paragraph4
      paragraphs.get(4).text() mustBe messages.paragraph5
    }
  }
}
