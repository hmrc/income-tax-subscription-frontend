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

package views.individual.matching

import controllers.SignOutController
import messagelookup.agent.MessageLookup.Base
import messagelookup.individual.MessageLookup
import models.DateModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait
import views.html.individual.matching.AlreadyEnrolled

class AlreadyEnrolledViewSpec extends ViewSpecTrait {

  val submissionDateValue: DateModel = DateModel("1", "1", "2016")
  val request: FakeRequest[AnyContentAsEmpty.type] = ViewSpecTrait.viewTestRequest

  val alreadyEnrolled: AlreadyEnrolled = app.injector.instanceOf[AlreadyEnrolled]
  lazy val page: HtmlFormat.Appendable = alreadyEnrolled(Call("", ""))(request, implicitly)
  lazy val document: Document = Jsoup.parse(page.body)
  lazy val main: Elements = document.select("main")

  "The Already Enrolled view" should {

    s"have the title '${MessageLookup.AlreadyEnrolled.title}'" in {
      val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"
      document.title() must be(MessageLookup.AlreadyEnrolled.title + serviceNameGovUk)
    }

    s"has a heading (H1)" which {

      lazy val heading = main.select("H1")

      s"has the text '${MessageLookup.AlreadyEnrolled.heading}'" in {
        heading.text() mustBe MessageLookup.AlreadyEnrolled.heading
      }

      s"has a line '${MessageLookup.AlreadyEnrolled.line1}'" in {
        main.select("p").get(0).text must be(MessageLookup.AlreadyEnrolled.line1)
      }

      s"has a bullet '${MessageLookup.AlreadyEnrolled.b1}'" in {
        main.select("li").get(0).text must be(MessageLookup.AlreadyEnrolled.b1)
      }

      s"has a bullet '${MessageLookup.AlreadyEnrolled.b2}'" in {
        main.select("li").get(1).text must be(MessageLookup.AlreadyEnrolled.b2)
      }
    }

    s"has a heading (H2)" which {

      lazy val heading = main.select("H2")

      s"has the text '${MessageLookup.AlreadyEnrolled.h2}'" in {
        heading.text() mustBe MessageLookup.AlreadyEnrolled.h2
      }

      s"has a line '${MessageLookup.AlreadyEnrolled.line2}'" in {
        main.select("#line2").text must be(MessageLookup.AlreadyEnrolled.line2)
      }
    }

    "have a continue button" in {
      main.select(".govuk-button").text mustBe Base.continue
    }

    "have a sign out link" in {
      val actionSignOut = document.select(".hmrc-sign-out-nav__link")
      actionSignOut.text() mustBe MessageLookup.Base.signOut
      actionSignOut.attr("href") mustBe SignOutController.signOut.url
    }

  }
}
