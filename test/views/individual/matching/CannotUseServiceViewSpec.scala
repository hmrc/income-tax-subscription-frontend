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
import messagelookup.individual.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait
import views.html.individual.matching.CannotUseService

class CannotUseServiceViewSpec extends ViewSpecTrait {

  implicit val request: Request[_] = FakeRequest()

  val action: Call = ViewSpecTrait.testCall

  val cannotUseServiceView: CannotUseService = app.injector.instanceOf[CannotUseService]

  lazy val page: HtmlFormat.Appendable = cannotUseServiceView(action)(request, implicitly)
  lazy val document: Document = Jsoup.parse(page.body)

  "The Cannot Use Service view" should {

    s"have the title '${MessageLookup.AlreadyEnrolled.title}'" in {
      val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"
      document.title() must be(MessageLookup.CannotUseService.title + serviceNameGovUk)
    }

    s"has a heading (H1)" which {

      lazy val heading = document.select("H1")

      s"has the text '${MessageLookup.CannotSignUp.heading}'" in {
        heading.text() mustBe MessageLookup.CannotSignUp.heading
      }

      s"has a line '${MessageLookup.CannotSignUp.line1}'" in {
        document.select(".govuk-body").text must be(MessageLookup.CannotSignUp.line1)
      }
    }

    "have a sign out link" in {
      val actionSignOut = document.select(".hmrc-sign-out-nav__link")
      actionSignOut.text() mustBe MessageLookup.Base.signOut
      actionSignOut.attr("href") mustBe SignOutController.signOut.url
    }


  }
}
