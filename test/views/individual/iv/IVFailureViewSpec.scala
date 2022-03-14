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

package views.individual.iv

import controllers.SignOutController.signOut
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utilities.ViewSpec
import views.html.individual.iv.IVFailure

class IVFailureViewSpec extends ViewSpec {

  val ivFailure: IVFailure = app.injector.instanceOf[IVFailure]

  val document: Document = Jsoup.parse(ivFailure().body)

  object IVFailureMessages {
    val heading: String = "There’s a problem"
    val cannotAccess: String = "You cannot access this service. This may be because:"
    val tookTooLong: String = "you took too long to enter information and the service has timed out"
    val failedQuestions: String = "you have failed to answer enough questions correctly"
    val couldNotMatchDetails: String = "we could not match your details to our system"
    val signOut: String = "Sign out"
  }

  "IVFailure" must {
    "display the template correctly" in new TemplateViewTest(
      view = ivFailure(),
      title = IVFailureMessages.heading,
    )
    "have a heading" in {
      document.mainContent.getH1Element.text mustBe IVFailureMessages.heading
    }
    "have info on why the user can't access the service" which {
      "has text to start a list of reasons" in {
        document.mainContent.selectFirst("p").text mustBe IVFailureMessages.cannotAccess
      }
      "has a list of reasons" which {
        "has a first reason" in {
          document.mainContent.selectFirst("ul").getNthListItem(1).text mustBe IVFailureMessages.tookTooLong
        }
        "has a second reason" in {
          document.mainContent.selectFirst("ul").getNthListItem(2).text mustBe IVFailureMessages.failedQuestions
        }
        "has a third reason" in {
          document.mainContent.selectFirst("ul").getNthListItem(3).text mustBe IVFailureMessages.couldNotMatchDetails
        }
      }
    }
    "have something for the user to sign out" in {
      val signOutLink = document.mainContent.select("#sign-out-button")
      signOutLink.text mustBe IVFailureMessages.signOut
      signOutLink.attr("class") mustBe "govuk-button"
      signOutLink.attr("role") mustBe "button"
      signOutLink.attr("href") mustBe signOut.url
    }
  }

}
