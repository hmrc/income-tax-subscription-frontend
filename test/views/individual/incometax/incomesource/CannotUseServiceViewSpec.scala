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

package views.individual.incometax.incomesource

import assets.MessageLookup
import controllers.SignOutController
import org.jsoup.Jsoup
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.ViewSpecTrait
import views.html.individual.incometax.incomesource.CannotUseService

class CannotUseServiceViewSpec extends ViewSpecTrait {

  implicit val request: Request[_] = FakeRequest()

  val action = ViewSpecTrait.testCall

  val cannotUseServiceView: CannotUseService = app.injector.instanceOf[CannotUseService]

  lazy val page = cannotUseServiceView(action)(request, implicitly, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Cannot Use Service view" should {

    s"have the title '${MessageLookup.AlreadyEnrolled.title}'" in {
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
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
