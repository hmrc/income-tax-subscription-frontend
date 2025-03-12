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

package views.agent.tasklist

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.html.agent.tasklist.IncomeSourcesIncomplete

class IncomeSourcesIncompleteViewSpec extends ViewSpec {

  "IncomeSourcesIncomplete" must {

    "have the correct template details" in new TemplateViewTest(
      view = page(),
      isAgent = true,
      title = IncomeSourcesIncompleteMessages.heading
    )

    "have a heading and caption" in {
      document.mustHaveHeadingAndCaption(
        IncomeSourcesIncompleteMessages.heading,
        IncomeSourcesIncompleteMessages.caption,
        isSection = false
      )
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
    val heading = "There is a problem with your client’s income sources"
    val caption = s"${testClientDetails.name} | ${testClientDetails.formattedNino}"
    val paraOne = "You need to check that you have entered all the information for your client’s income sources."
    val paraTwo = "This includes income sources:"
    val bulletOne = "that you added"
    val bulletTwo = "that we added for you"
    val paraThree = "When you have entered all the information for an income source, it will show as ‘completed’."
    val buttonText = "Check your income sources"
  }

  private def page(clientDetails: ClientDetails = testClientDetails): Html = {
    incomeSourcesIncomplete(
      backUrl = testBackUrl,
      postAction = testCall,
      clientDetails = clientDetails
    )
  }

  private def document: Document = Jsoup.parse(page().body)

  private lazy val incomeSourcesIncomplete: IncomeSourcesIncomplete = app.injector.instanceOf[IncomeSourcesIncomplete]

  private lazy val testClientDetails: ClientDetails = ClientDetails(
    name = "FirstName LastName",
    nino = "AA000000A"
  )

}

