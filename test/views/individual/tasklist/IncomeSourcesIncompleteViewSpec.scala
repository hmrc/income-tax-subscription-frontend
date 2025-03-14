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

package views.individual.tasklist

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import views.html.individual.tasklist.IncomeSourcesIncomplete
import utilities.ViewSpec

class IncomeSourcesIncompleteViewSpec extends ViewSpec {
  "IncomeSourcesIncomplete" must {

    "have the correct template details" in new TemplateViewTest(
      view = page(),
      isAgent = false,
      title = IncomeSourcesIncompleteMessages.heading
    )

    "have a heading" in {
      document.mainContent.getH1Element.text mustBe IncomeSourcesIncompleteMessages.heading
    }

    "have the first paragraph" in {
      document.mainContent.selectNth("p", 1).text mustBe IncomeSourcesIncompleteMessages.paraOne
    }

    "have the second paragraph" in {
      document.mainContent.selectNth("p", 2).text mustBe IncomeSourcesIncompleteMessages.paraTwo
    }

    "have a bullet point list" which {
      def bulletList: Element = document.mainContent.selectHead("ul.govuk-list.govuk-list--bullet")

      "has a first bullet" in {
        bulletList.selectNth("li", 1).text mustBe IncomeSourcesIncompleteMessages.bulletOne
      }
      "has a second bullet" in {
        bulletList.selectNth("li", 2).text mustBe IncomeSourcesIncompleteMessages.bulletTwo
      }
    }

    "have a third paragraph" in {
      document.mainContent.selectNth("p", 3).text mustBe IncomeSourcesIncompleteMessages.paraThree
    }

    "have a form" which {
      def form: Element = document.mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a check your income sources button" in {
        form.selectHead(".govuk-button").text mustBe IncomeSourcesIncompleteMessages.buttonText
      }
    }

  }

  private object IncomeSourcesIncompleteMessages {
    val heading = "There is a problem with your income sources"
    val paraOne = "You need to check that you have entered all the information for your income sources."
    val paraTwo = "This includes income sources:"
    val bulletOne = "that you added"
    val bulletTwo = "that we added for you"
    val paraThree = "When you have entered all the information for an income source, it will show as ‘completed’."
    val buttonText = "Check your income sources"
  }

  private def page(): Html = {
    incomeSourcesIncomplete(
      backUrl = testBackUrl,
      postAction = testCall,
    )
  }

  private def document: Document = Jsoup.parse(page().body)

  private lazy val incomeSourcesIncomplete: IncomeSourcesIncomplete = app.injector.instanceOf[IncomeSourcesIncomplete]

}
