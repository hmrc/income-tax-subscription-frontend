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
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.errors.ContactHMRC

class ContactHMRCViewSpec extends ViewSpec {

  def contactHMRC: ContactHMRC = app.injector.instanceOf[ContactHMRC]

  def page(): HtmlFormat.Appendable =
    contactHMRC(
      testCall
    )

  def document(): Document = Jsoup.parse(page().body)

  object ContactHMRCMessages {
    val title = "There is a problem"
    val heading = "You need to contact us before you can sign up"
    val p1 = "HMRC needs to make a small change to the service before you can finish signing up."
    val p2 = "To contact us:"
    val b1 = "Open the contact us form."
    val b2 = "Fill in your name and email address."
    val b3 = "In the box labelled “What do you need help with?”, type “Tell me when I can sign up”."
    val p3 = "We’ll contact you with any updates and let you know when you can sign up."
    val contactUs = "Contact us form"
  }

  "ContactHMRC" must {
    "have a page heading" in {
      document().mainContent.selectHead("h1").text mustBe ContactHMRCMessages.heading
    }

    "have a first paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe ContactHMRCMessages.p1
    }

    "have a second paragraph" in {
      document().mainContent.selectNth("p", 2).text mustBe ContactHMRCMessages.p2
    }

    "have a third paragraph" in {
      document().mainContent.selectNth("p", 3).text mustBe ContactHMRCMessages.p3
    }

    "have a first list item" in {
      document().mainContent.selectNth("li", 1).text mustBe ContactHMRCMessages.b1
    }

    "have a second list item" in {
      document().mainContent.selectNth("li", 2).text mustBe ContactHMRCMessages.b2
    }

    "have a third list item" in {
      document().mainContent.selectNth("li", 3).text mustBe ContactHMRCMessages.b3
    }

    "have a form" which {
      val form = document().selectHead("form")

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has an accept and continue button to submit the form" in {
        form.selectHead("button").text mustBe ContactHMRCMessages.contactUs
      }
    }
  }
}
