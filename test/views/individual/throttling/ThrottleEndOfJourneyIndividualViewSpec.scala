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

package views.individual.throttling

import messagelookup.individual.MessageLookup.ThrottleEndofJourney._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.throttling.ThrottleEndOfJourney

class ThrottleEndOfJourneyIndividualViewSpec extends ViewSpec {

  val throttleEndOfJourneyView: ThrottleEndOfJourney = app.injector.instanceOf[ThrottleEndOfJourney]
  lazy val postAction: Call = controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show
  private val backLinkTarget = Math.random().toString

  def page(): Html = {
    throttleEndOfJourneyView(
      backLink = backLinkTarget,
      postAction = postAction
    )(request, implicitly)
  }

  def document(): Document = Jsoup.parse(page().body)

  "business individual end of journey throttle view" must {
    val document1 = document()
    "have a title" in {
      document1.title mustBe title
    }
    "have a heading" in {
      document1.getH1Element.text() mustBe heading
    }
    "have a line_1" in {
      document1.mainContent.selectNth("p",1).text() mustBe line_1
    }
    "have a line_2" in {
      document1.mainContent.selectNth("p",2).text() mustBe line_2
    }
    "have a line_3" in {
      document1.mainContent.selectNth("p",3).text() mustBe line_3
    }
    "have a continueButton" in {
      document1.mainContent.selectHead(".form-group").text() mustBe continueButton
    }
    "must have a sign out link in the banner" in {
      val signOut = Option(document1.getElementById("logOutNavHref")).orElse(Option(document1.select(".hmrc-sign-out-nav__link")).filter(e => !e.isEmpty).map(e => e.get(0)))
      if (signOut.isEmpty) fail("Signout link was not located in the banner\nIf this is the expected behaviour then please set 'signOutInBanner' to true when creating the TestView object")
      signOut.get.attr("href") must startWith(controllers.SignOutController.signOut.url)
    }
    "must have text that says Sign out" in {
      val signOut = Option(document1.getElementById("logOutNavHref")).orElse(Option(document1.select(".hmrc-sign-out-nav__link")).filter(e => !e.isEmpty).map(e => e.get(0)))
      if (signOut.isEmpty) fail("Signout link was not located in the banner\nIf this is the expected behaviour then please set 'signOutInBanner' to true when creating the TestView object")
      signOut.get.text() mustBe signOutText
    }
    "have a form that continues" in {
      document1.select("main").select("form").attr("action") mustBe controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
    "have a backlink that goes back" in {
      document1.select(".govuk-back-link").attr("href") mustBe backLinkTarget
    }
  }
}
