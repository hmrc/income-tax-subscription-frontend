/*
 * Copyright 2021 HM Revenue & Customs
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

package views.individual.incometax.subscription

import assets.MessageLookup
import controllers.SignOutController
import models.DateModel
import org.jsoup.Jsoup
import views.ViewSpecTrait

class AlreadyEnrolledViewSpec extends ViewSpecTrait {

  val submissionDateValue = DateModel("1", "1", "2016")
  val action = ViewSpecTrait.testCall
  val request = ViewSpecTrait.viewTestRequest

  lazy val page = views.html.individual.incometax.subscription.enrolled.already_enrolled()(request, implicitly, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Already Enrolled view" should {

    s"have the title '${MessageLookup.AlreadyEnrolled.title}'" in {
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      document.title() must be(MessageLookup.AlreadyEnrolled.title + serviceNameGovUk )
    }

          s"has a heading (H1)" which {

        lazy val heading = document.select("H1")

        s"has the text '${MessageLookup.AlreadyEnrolled.heading}'" in {
          heading.text() mustBe MessageLookup.AlreadyEnrolled.heading
        }

        s"has a line '${MessageLookup.AlreadyEnrolled.line1}'" in {
          document.select(".form-group").text must be(MessageLookup.AlreadyEnrolled.line1)
        }
      }

    "have a sign out button" in {
      val actionSignOut = document.getElementById("sign-out-button")
      actionSignOut.attr("role") mustBe "button"
      actionSignOut.text() mustBe MessageLookup.Base.signOut
      actionSignOut.attr("href") mustBe SignOutController.signOut.url
    }

  }
}
