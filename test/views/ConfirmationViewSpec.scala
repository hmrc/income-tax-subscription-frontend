/*
 * Copyright 2017 HM Revenue & Customs
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

package views

import java.text.SimpleDateFormat
import assets.MessageLookup
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import java.util.Date

class ConfirmationViewSpec extends PlaySpec with OneAppPerTest {

  val submissionReferenceValue = "000-032407"
  val submissionDateValue: Date = new SimpleDateFormat("dd/MM/yyyy").parse("01/01/2016")
  val expectedOutputDateFormat = "1 January 2016"

  lazy val page = views.html.confirmation(
    submissionReference = submissionReferenceValue,
    submissionDate = submissionDateValue
  )(FakeRequest(), applicationMessages)
  lazy val document = Jsoup.parse(page.body)

  "The Confirmation view" should {

    s"have the title '${MessageLookup.Confirmation.title}'" in {
      document.title() must be(MessageLookup.Confirmation.title)
    }

    "have a successful transaction confirmation banner" which {

      "has a turquoise background" in {
        document.select("#confirmation-heading").hasClass("transaction-banner--complete") mustBe true
      }

      s"has a heading (H1)" which {

        lazy val heading = document.select("H1")

        s"has the text ${MessageLookup.Confirmation.heading}'" in {
          heading.text() mustBe MessageLookup.Confirmation.heading
        }

        "has the class 'transaction-banner__heading'" in {
          heading.hasClass("transaction-banner__heading") mustBe true
        }
      }

      s"has a submission reference label '${MessageLookup.Confirmation.submissionReferenceLabel}'" in {
        document.select("#submission-reference-label").text() mustBe MessageLookup.Confirmation.submissionReferenceLabel
      }

      s"has a submission reference value '$submissionReferenceValue'" in {
        document.select("#submission-reference-value").text() mustBe submissionReferenceValue
      }

      s"has a submission date label '${MessageLookup.Confirmation.submissionDateLabel}'" in {
        document.select("#submission-date-label").text() mustBe MessageLookup.Confirmation.submissionDateLabel
      }

      s"has a submission date value '$submissionDateValue'" in {
        document.select("#submission-date-value").text() mustBe expectedOutputDateFormat
      }
    }

    "have a message which states an email confirmation has been sent" in {
      document.select("#emailConfirmation").text() mustBe MessageLookup.Confirmation.emailConfirmation
    }

    "have a 'What happens next' section" which {

      s"has the section heading '${MessageLookup.Confirmation.whatHappensNext.heading}'" in {
        document.select("#whatHappensNext h2").text() mustBe MessageLookup.Confirmation.whatHappensNext.heading
      }

      s"has a paragraph stating HMRC process' ${MessageLookup.Confirmation.whatHappensNext.para1}'" in {
        document.select("#para1").text() mustBe MessageLookup.Confirmation.whatHappensNext.para1
      }


    }

  }
}
