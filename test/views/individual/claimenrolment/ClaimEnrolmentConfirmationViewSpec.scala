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

package views.individual.claimenrolment

import assets.MessageLookup
import controllers.SignOutController
import org.jsoup.Jsoup
import play.twirl.api.Html
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.individual.claimenrolment.ClaimEnrolmentConfirmation


class ClaimEnrolmentConfirmationViewSpec extends ViewSpec {

  val claimEnrolmentConfirmation: ClaimEnrolmentConfirmation = app.injector.instanceOf[ClaimEnrolmentConfirmation]
  val action = ViewSpecTrait.testCall
  override val request = ViewSpecTrait.viewTestRequest

  val page: Html = claimEnrolmentConfirmation(action)(request, implicitly, appConfig)
  val document = Jsoup.parse(page.body)

  "The Claim Enrolment Confirmation view" should {

    s"have the title '${MessageLookup.Confirmation.title}'" in {
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      document.title() must be(MessageLookup.ClaimEnrollmentConfirmation.title + serviceNameGovUk)
    }

    "have a successful confirmation banner" which {

      "has a turquoise background" in {
        document.select("#confirmation-panel").hasClass("govuk-panel--confirmation") mustBe true
      }
    }

    s"has a heading (H2)" which {

      lazy val heading = document.mainContent.select("H2")

      s"has the text '${MessageLookup.ClaimEnrollmentConfirmation.heading}'" in {

        heading.text() must startWith(MessageLookup.ClaimEnrollmentConfirmation.heading)
      }

      "has the class 'transaction-banner__heading'" in {
        heading.hasClass("govuk-heading-m") mustBe true
      }
    }


    "have a 'What happens now' section" which {
      s"has first numbered point '${MessageLookup.ClaimEnrollmentConfirmation.bullet1}'" in {
        val point1 = document.select("ol.govuk-list--number").select("li").first()
        val actual = point1.text
        val expected = MessageLookup.ClaimEnrollmentConfirmation.bullet1
        actual mustBe expected
        point1 select "a" attr "href" mustBe appConfig.softwareUrl
      }

      s"has second numbered point '${MessageLookup.ClaimEnrollmentConfirmation.bullet2}'" in {
        val point2 = document.select("ol.govuk-list--number").select("li").get(1)
        point2.text() mustBe MessageLookup.ClaimEnrollmentConfirmation.bullet2
      }

      s"has third numbered point '${MessageLookup.ClaimEnrollmentConfirmation.bullet3}'" in {
        val point3 = document.select("ol.govuk-list--number").select("li").get(2)
        point3.text() mustBe MessageLookup.ClaimEnrollmentConfirmation.bullet3
      }

      "have a sign out link" in {
        val actionSignOut = document.getElementById("sign-out-button")
        actionSignOut.text() mustBe MessageLookup.Base.signOut
        actionSignOut.attr("href") mustBe SignOutController.signOut.url
      }

    }
  }
}
