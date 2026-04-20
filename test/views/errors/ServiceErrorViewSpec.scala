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

package views.errors

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.errors.ServiceError

class ServiceErrorViewSpec extends ViewSpec {

  def serviceError: ServiceError = app.injector.instanceOf[ServiceError]

  def page(isAgent: Boolean): HtmlFormat.Appendable =
    serviceError(
      testCall,
      isAgent
    )

  def document(isAgent: Boolean): Document = Jsoup.parse(page(isAgent).body)

  object ServiceErrorMessages {
    val title = "Sorry, there is a problem with the service"
    val heading = "Sorry, there is a problem with the service"
    val paragraphAgent = "We could not confirm your client’s details. Please try again."
    val paragraphIndividual = "We could not confirm your details. Please try again."
    val TryAgain = "Try again"
  }

  "ContactHMRC" must {
    "has correct template for individuals" in new TemplateViewTest(
      view = page(false),
      isAgent = false,
      title = ServiceErrorMessages.title
    )

    "has correct template for agents" in new TemplateViewTest(
      view = page(true),
      isAgent = true,
      title = ServiceErrorMessages.title
    )

    "has a page heading" in {
      Seq(false, true).foreach { isAgent =>
        document(isAgent).mainContent.selectHead("h1").text mustBe ServiceErrorMessages.heading
      }
    }

    "has a first paragraph for agent" in {
      document(true).mainContent.selectNth("p", 1).text mustBe ServiceErrorMessages.paragraphAgent
    }

    "has a first paragraph for individual" in {
      document(false).mainContent.selectNth("p", 1).text mustBe ServiceErrorMessages.paragraphIndividual
    }

    "has a form" which {
      def form(isAgent: Boolean): Element =
        document(isAgent).selectHead("form")

      "has the correct attributes" in {
        Seq(false, true).foreach { isAgent =>
          form(isAgent).attr("method") mustBe testCall.method
          form(isAgent).attr("action") mustBe testCall.url
        }
      }

      "has an try again burron" in {
        Seq(false, true).foreach { isAgent =>
          form(isAgent).selectHead("button").text mustBe ServiceErrorMessages.TryAgain
        }
      }
    }
  }
}
