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

import messagelookup.individual.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.claimenrolment.ClaimEnrolmentConfirmation

class ClaimEnrolmentConfirmationViewSpec extends ViewSpec {

  val claimEnrolmentConfirmation: ClaimEnrolmentConfirmation = app.injector.instanceOf[ClaimEnrolmentConfirmation]

  val origins = Seq(
    "bta",
    "pta",
    "sign-up"
  )

  def page(origin: String): Html =
    claimEnrolmentConfirmation(testCall, origin)(request, implicitly)

  def document(origin: String): Document =
    Jsoup.parse(page(origin).body)

  "The Claim Enrolment Confirmation view" should {

    s"have the title '${MessageLookup.Confirmation.title}'" in {
      val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"
      origins.foreach { origin =>
        document(origin).title() must be(MessageLookup.ClaimEnrollmentConfirmation.title(origin) + serviceNameGovUk)
      }
    }

    "have a successful confirmation banner" which {

      "has a turquoise background" in {
        origins.foreach { origin =>
          document(origin).select("#confirmation-panel").hasClass("govuk-panel--confirmation") mustBe true
        }
      }
    }

    s"has a heading (H2)" which {

      def heading(origin: String) =
        document(origin).mainContent.select("H2")

      s"has the text '${MessageLookup.ClaimEnrollmentConfirmation.heading}'" in {
        origins.foreach { origin =>
          heading(origin).text() must startWith(MessageLookup.ClaimEnrollmentConfirmation.heading)
        }
      }

      "has the class 'transaction-banner__heading'" in {
        origins.foreach { origin =>
          heading(origin).hasClass("govuk-heading-m") mustBe true
        }
      }
    }

    "have a 'What you must do' section" which {

      "has a first paragraph" in {
        origins.foreach { origin =>
          document(origin).mainContent.selectHead("#whatHappensNow").selectHead("p").text mustBe MessageLookup.ClaimEnrollmentConfirmation.para1
        }
      }

      "has a second paragraph with hyper link" in {
        origins.foreach { origin =>
          document(origin).mainContent.selectNth("p", 2).text mustBe MessageLookup.ClaimEnrollmentConfirmation.para2
        }
      }

      "has a third paragraph" in {
        origins.foreach { origin =>
          document(origin).mainContent.selectNth("p", 4).text mustBe MessageLookup.ClaimEnrollmentConfirmation.para3
        }
      }

      "have a Continue to Online Services account button" in {
        origins.foreach { origin =>
          val actionToContinueOnlineServices = document(origin).mainContent.selectHead("button")
          actionToContinueOnlineServices.text() mustBe MessageLookup.ClaimEnrollmentConfirmation.ContinueToOnlineServicesButton(origin)
        }
      }

      "has a first bullet" in {
        origins.foreach { origin =>
          document(origin).mainContent.selectNth("li", 1).text mustBe MessageLookup.ClaimEnrollmentConfirmation.b1
        }
      }

      "has a second bullet" in {
        origins.foreach { origin =>
          document(origin).mainContent.selectNth("li", 2).text mustBe MessageLookup.ClaimEnrollmentConfirmation.b2
        }
      }

      "has a third bullet" in {
        origins.foreach { origin =>
          document(origin).mainContent.selectNth("li", 3).text mustBe MessageLookup.ClaimEnrollmentConfirmation.b3
        }
      }
    }
  }
}
