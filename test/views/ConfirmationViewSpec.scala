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

import assets.MessageLookup
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import models.DateModel

class ConfirmationViewSpec extends PlaySpec with OneAppPerTest {

  val submissionReferenceValue = "000-032407"
  val submissionDateValue = DateModel("1","1","2016")

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

        s"has the text '${MessageLookup.Confirmation.heading}'" in {
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
        document.select("#submission-date-value").text() mustBe submissionDateValue.toOutputDateFormat
      }
    }

    "have a message which states an email confirmation has been sent" in {
      document.select("#emailConfirmation").text() mustBe MessageLookup.Confirmation.emailConfirmation
    }

    "have a 'What happens next' section" which {

      s"has the section heading '${MessageLookup.Confirmation.whatHappensNext.heading}'" in {
        document.select("#whatHappensNext h2").text() mustBe MessageLookup.Confirmation.whatHappensNext.heading
      }

      s"has a paragraph stating HMRC process '${MessageLookup.Confirmation.whatHappensNext.para1}'" in {
        document.select("#whatHappensNext p").text() mustBe MessageLookup.Confirmation.whatHappensNext.para1
      }

      s"has a bullet point relating to correspondence '${MessageLookup.Confirmation.whatHappensNext.bullet1}'" in {
        document.select("#whatHappensNext li:nth-child(1)").text() mustBe MessageLookup.Confirmation.whatHappensNext.bullet1
      }

      s"has a bullet point relating to implications and obligations '${MessageLookup.Confirmation.whatHappensNext.bullet2}'" in {
        document.select("#whatHappensNext li:nth-child(2)").text() mustBe MessageLookup.Confirmation.whatHappensNext.bullet2
      }

    }
    "have a 'Register for more tax' section" which {

      s"has the section heading '${MessageLookup.Confirmation.registerForMoreTax.heading}'" in {
        document.select("#registerForMoreTax h2").text() mustBe MessageLookup.Confirmation.registerForMoreTax.heading
      }

      s"has a link stating PAYE '${MessageLookup.Confirmation.registerForMoreTax.link1}'" in {
        document.select("#registerForMoreTax a:nth-child(1)").text() mustBe MessageLookup.Confirmation.registerForMoreTax.link1
      }

      s"has a link stating VAT '${MessageLookup.Confirmation.registerForMoreTax.link2}'" in {
        document.select("#registerForMoreTax a:nth-child(2)").text() mustBe MessageLookup.Confirmation.registerForMoreTax.link2
      }

    }

    "have a 'Guidance' section" which {

      s"has the section heading '${MessageLookup.Confirmation.guidanceSection.heading}'" in {
        document.select("#guidanceSection h2").text() mustBe MessageLookup.Confirmation.guidanceSection.heading
      }

      s"has a link stating Quarterly filing '${MessageLookup.Confirmation.guidanceSection.link1}'" in {
        document.select("#guidanceSection a:nth-child(1)").text() mustBe MessageLookup.Confirmation.guidanceSection.link1
      }

      s"has a link stating Downloading software '${MessageLookup.Confirmation.guidanceSection.link2}'" in {
        document.select("#guidanceSection a:nth-child(2)").text() mustBe MessageLookup.Confirmation.guidanceSection.link2
      }

      s"has a link stating Further reading '${MessageLookup.Confirmation.guidanceSection.link3}'" in {
        document.select("#guidanceSection a:nth-child(3)").text() mustBe MessageLookup.Confirmation.guidanceSection.link3
      }

    }

    "have a 'Give us feedback' section" which {

      s"has the section heading '${MessageLookup.Confirmation.giveUsFeedback.heading}'" in {
        document.select("#giveUsFeedback h2").text() mustBe MessageLookup.Confirmation.giveUsFeedback.heading
      }

      s"has a link stating service question '${MessageLookup.Confirmation.giveUsFeedback.link1}'" in {
        document.select("#giveUsFeedback a").text() mustBe MessageLookup.Confirmation.giveUsFeedback.link1
      }

      s"has the text stating feedback duration '${MessageLookup.Confirmation.giveUsFeedback.feedbackDuration}'" in {
        document.select("#giveUsFeedback span").text() mustBe MessageLookup.Confirmation.giveUsFeedback.feedbackDuration
      }
    }

  }
}
