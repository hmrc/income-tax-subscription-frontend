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

package views.agent.throttling

import messagelookup.agent.MessageLookup.Base.tryAgain
import messagelookup.individual.MessageLookup.{ThrottleStartOfJourneyAgent => messages}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.agent.throttling.ThrottleStartOfJourney

class ThrottleStartOfJourneyViewSpec extends ViewSpec {

  val throttleStartOfJourneyView: ThrottleStartOfJourney = app.injector.instanceOf[ThrottleStartOfJourney]
  lazy val postAction: Call = controllers.agent.matching.routes.HomeController.index

  def page(): Html = {
    throttleStartOfJourneyView(
      postAction = postAction
    )(request, implicitly)
  }

  def document(): Document = Jsoup.parse(page().body)

  "business agent start of throttle view" must {
    val document1 = document()
    "have a title" in {
      document1.title mustBe messages.title
    }
    "have a heading" in {
      document1.select("main").select("h1").text() mustBe messages.heading
    }
    "have line 1" in {
      document1.select("main").select("p").first().text() mustBe messages.line1
    }
    "have a continue button that says try again" in {
      document1.mainContent.select("button").text() mustBe tryAgain
    }
    "must have a sign out link in the banner" in {
      val signOut = Option(document1.getElementById("logOutNavHref")).orElse(Option(document1.select(".hmrc-sign-out-nav__link")).filter(e => !e.isEmpty).map(e => e.get(0)))
      if (signOut.isEmpty) fail("Signout link was not located in the banner\nIf this is the expected behaviour then please set 'signOutInBanner' to true when creating the TestView object")
      signOut.get.attr("href") must startWith(controllers.SignOutController.signOut.url)
    }
    "must have text that says Sign out" in {
      val signOut = Option(document1.getElementById("logOutNavHref")).orElse(Option(document1.select(".hmrc-sign-out-nav__link")).filter(e => !e.isEmpty).map(e => e.get(0)))
      if (signOut.isEmpty) fail("Signout link was not located in the banner\nIf this is the expected behaviour then please set 'signOutInBanner' to true when creating the TestView object")
      signOut.get.text() mustBe messages.signOutText
    }
    "have a form that continues" in {
      document1.select("main").select("form").attr("action") mustBe controllers.agent.matching.routes.HomeController.index.url
    }
  }
}
