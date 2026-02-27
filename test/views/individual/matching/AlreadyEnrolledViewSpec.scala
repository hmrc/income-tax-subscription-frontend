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

  def page(noEnrolment: Boolean): HtmlFormat.Appendable =
    alreadyEnrolled(Call("", ""), noEnrolment)(request, implicitly)

  def document(noEnrolment: Boolean): Document =
    Jsoup.parse(page(noEnrolment).body)

  def main(noEnrolment: Boolean): Elements =
    document(noEnrolment).select("main")

  "The Already Enrolled view" should {

    s"have the title '${MessageLookup.AlreadyEnrolled.title}'" in {
      val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"
      Seq(false, true).foreach { noEnrolment =>
        document(noEnrolment).title() must be(MessageLookup.AlreadyEnrolled.title + serviceNameGovUk)
      }
    }

    s"has a heading (H1)" should {
      s"has the text '${MessageLookup.AlreadyEnrolled.heading}'" in {
        Seq(false, true).foreach { noEnrolment =>
          main(noEnrolment).select("H1").text() mustBe MessageLookup.AlreadyEnrolled.heading
        }
      }

      s"has a line '${MessageLookup.AlreadyEnrolled.line1}'" in {
        Seq(false, true).foreach { noEnrolment =>
          main(noEnrolment).select("p").get(0).text must be(MessageLookup.AlreadyEnrolled.line1)
        }
      }

      s"has a bullet '${MessageLookup.AlreadyEnrolled.b1}'" in {
        Seq(false, true).foreach { noEnrolment =>
          main(noEnrolment).select("li").get(0).text must be(MessageLookup.AlreadyEnrolled.b1)
        }
      }

      s"has a bullet '${MessageLookup.AlreadyEnrolled.b2}'" in {
        Seq(false, true).foreach { noEnrolment =>
          main(noEnrolment).select("li").get(1).text must be(MessageLookup.AlreadyEnrolled.b2)
        }
      }
    }

    s"has a heading (H2)" should {
      s"has the text '${MessageLookup.AlreadyEnrolled.h2}'" in {
        Seq(false, true).foreach { noEnrolment =>
          main(noEnrolment).select("H2").text() mustBe MessageLookup.AlreadyEnrolled.h2
        }
      }

      s"has a line '${MessageLookup.AlreadyEnrolled.line2}'" in {
        Seq(false, true).foreach { noEnrolment =>
          main(noEnrolment).select("#line2").text must be(MessageLookup.AlreadyEnrolled.line2)
        }
      }

      "When noEnrolment is" should {
        s"true then has a line '${MessageLookup.AlreadyEnrolled.line3}'" in {
          main(true).select("#line3").text must be(MessageLookup.AlreadyEnrolled.line3)
        }

        "false then has no such line" in {
          try {
            main(false).select("#line3")
            fail()
          } catch {
            case e: Exception =>
          }
        }
      }
    }

    "have a continue button" in {
      Seq(false, true).foreach { noEnrolment =>
        main(noEnrolment).select(".govuk-button").text mustBe Base.continue
      }
    }

    "have a sign out link" in {
      Seq(false, true).foreach { noEnrolment =>
        val actionSignOut = document(noEnrolment).select(".hmrc-sign-out-nav__link")
        actionSignOut.text() mustBe MessageLookup.Base.signOut
        actionSignOut.attr("href") mustBe SignOutController.signOut.url
      }
    }

  }
}
