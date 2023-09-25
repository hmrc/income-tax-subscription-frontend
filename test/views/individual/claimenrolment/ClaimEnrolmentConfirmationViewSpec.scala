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


    "have a 'What you must do' section" which {

      "has a first paragraph" in {
        document.mainContent.selectHead("#whatHappensNow").selectHead("p").text mustBe MessageLookup.ClaimEnrollmentConfirmation.para1
      }

      "has a second paragraph with hyper link" in {
        document.mainContent.selectNth("p", 2).text mustBe MessageLookup.ClaimEnrollmentConfirmation.para2
        document.mainContent.selectNth("p",2).selectHead("a").attr("href").contains("https://www.gov.uk/government/collections/making-tax-digital-for-income-tax")
      }

      "has a third paragraph" in {
        document.mainContent.selectNth("p", 3).text mustBe MessageLookup.ClaimEnrollmentConfirmation.para3
      }


      "have a Continue to Online Services account button" in {
        val actionToContinueOnlineServices = document.mainContent.selectHead("button")
        actionToContinueOnlineServices.text() mustBe MessageLookup.ClaimEnrollmentConfirmation.ContinueToOnlineServicesButton
      }

    }
  }
}
