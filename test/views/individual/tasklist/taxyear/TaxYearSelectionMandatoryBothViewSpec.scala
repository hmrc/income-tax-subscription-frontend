/*
 * Copyright 2025 HM Revenue & Customs
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

package views.individual.tasklist.taxyear

import messagelookup.individual.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.tasklist.taxyear.TaxYearSelectionMandatoryBoth

class TaxYearSelectionMandatoryBothViewSpec extends ViewSpec {
  "Tax Year Selection Mandatory Both" must {

    "have the correct template details" in new TemplateViewTest(
      view = page(),
      isAgent = false,
      title = TaxYearSelectionMandatoryBothMessages.heading
    )

    "have a heading" in {
      document.mainContent.getH1Element.text mustBe TaxYearSelectionMandatoryBothMessages.heading
    }

    "have the paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe TaxYearSelectionMandatoryBothMessages.paragraph
    }

    "have a form" which {
      def form: Element = document.mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a continue button" in {
        form.select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
      }
    }

  }

  private object TaxYearSelectionMandatoryBothMessages {
    val heading = "You must use Making Tax Digital for Income Tax now"
    val paragraph = "As your total annual income from self-employment or property is over £50,000, you must use Making Tax Digital for Income Tax from 2026 to 2027."
  }

  val taxYearSelectionMandatoryBoth: TaxYearSelectionMandatoryBoth = app.injector.instanceOf[TaxYearSelectionMandatoryBoth]

  private def page(): Html = {
    taxYearSelectionMandatoryBoth(
      postAction = testCall,
      backUrl = Some(testBackUrl),
    )
  }

  private def document: Document = Jsoup.parse(page().body)

}
