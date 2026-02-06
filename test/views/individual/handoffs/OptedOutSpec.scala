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

package views.individual.handoffs

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.eligibility.YouCanSignUp
import views.html.individual.handoffs.OptedOut

class OptedOutSpec extends ViewSpec {
  private val optedOut: OptedOut = app.injector.instanceOf[OptedOut]

  "OptedOut" must {
    "have the correct template details" in
      Seq(false, true).foreach { noEnrolment =>
        new TemplateViewTest(
          view = page(noEnrolment),
          isAgent = false,
          title = OptedOut.title,
          hasSignOutLink = false
        )
      }

    "have the correct heading" in {
      Seq(false, true).foreach { noEnrolment =>
        document(noEnrolment).mainContent.selectHead("h1").text mustBe OptedOut.heading
      }
    }

    "have the correct first paragraph" in {
      Seq(false, true).foreach { noEnrolment =>
        document(noEnrolment).mainContent.selectNth("p", 1).text mustBe OptedOut.p1
      }
    }

    "have the correct second paragraph if no enrolment" in {
      document(true).mainContent.selectNth("p", 2).text mustBe OptedOut.p2
    }

    "not have a second paragraph if there is an enrolment" in {
      try {
        document(false).mainContent.selectNth("p", 2)
        fail()
      } catch {
        case e: Exception =>
      }
    }
  }

  private object OptedOut {
    val title = "Manage your reporting obligations"
    val heading = "Manage your reporting obligations"
    val p1 = "If you want to use Making Tax Digital for Income Tax, you need to change your reporting obligations."
    val p2 = "You’ll need to use the same sign in details that you use for your Self Assessment tax return."
  }

  private def page(noEnrolment: Boolean): Html = {
    optedOut(
      postAction = Call("", ""),
      noEnrolment = noEnrolment
    )
  }

  private def document(noEnrolment: Boolean): Document =
    Jsoup.parse(page(noEnrolment).body)

}
