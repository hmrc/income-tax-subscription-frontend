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

package views.individual.incometax.subscription

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.incometax.subscription.SignUpConfirmation

class SignUpConfirmationViewSpec extends ViewSpec {
  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  def page(selectedTaxYearIsNext: Boolean): Html = signUpConfirmation(selectedTaxYearIsNext)

  def document(selectedTaxYearIsNext: Boolean = false): Document = Jsoup.parse(page(selectedTaxYearIsNext).body)

  "The sign up confirmation view" must {
    "have a heading" in {
      document().mainContent.selectHead("h1").text() mustBe SignUpConfirmationMessages.heading
    }

    "have a section 1" which {
      "contains a heading" in {
        document().mainContent.selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.section1heading
      }

      "not contains a hint" when {
        "the Current tax year is selected" in {
          document().mainContent.select(".govuk-warning-text .govuk-warning-text__text").isEmpty mustBe true
        }
      }

      "contains a hint" when {
        "the Next tax year is selected" in {
          document(true).mainContent.selectHead(".govuk-warning-text .govuk-warning-text__text").text() mustBe SignUpConfirmationMessages.section1hint
        }
      }
    }



    "have a section 2" which {
      "contains a heading" in {
        document().mainContent.selectNth("h2", 2).text() mustBe SignUpConfirmationMessages.section2heading
      }

      "contains the current tax year online HMRC services section" which {
        def onlineHmrcServices: Element = document().mainContent.selectNth(".govuk-grid-column-one-half", 2)

        "contains a heading" in {
          onlineHmrcServices.selectHead("h3").text() mustBe SignUpConfirmationMessages.section2onlineServicesHeading
        }

        "contains a paragraph" in {
          onlineHmrcServices.selectHead("p").text() mustBe SignUpConfirmationMessages.section2onlineServicesParagraph
        }

        "contains a link" in {
          onlineHmrcServices.selectHead("a").text() mustBe SignUpConfirmationMessages.section2onlineServicesLink
          onlineHmrcServices.selectHead("a").attr("href") mustBe "https://www.tax.service.gov.uk/account"
        }
      }

      "contains the next tax year online HMRC services section" which {
        def onlineHmrcServices: Element = document(true).mainContent.selectNth(".govuk-grid-column-one-half", 2)

        "contains a heading" in {
          onlineHmrcServices.selectHead("h3").text() mustBe SignUpConfirmationMessages.section2onlineServicesHeading
        }

        "contains a paragraph 1" in {
          onlineHmrcServices.selectNth("p", 1).text() mustBe SignUpConfirmationMessages.section2onlineServicesNextYearParagraph1
        }

        "contains a link" in {
          onlineHmrcServices.selectHead("a").text() mustBe SignUpConfirmationMessages.section2onlineServicesNextYearParagraph1Link
          onlineHmrcServices.selectHead("a").attr("href") mustBe "https://www.tax.service.gov.uk/account"
        }

        "contains a paragraph 2" in {
          onlineHmrcServices.selectNth("p", 2).text() mustBe SignUpConfirmationMessages.section2onlineServicesNextYearParagraph2
        }
      }
    }
  }

  private object SignUpConfirmationMessages {
    val heading = "Sign up complete"
    val section1heading = "What you will have to do"
    val section1hint = "Warning Continue to submit your Self Assessment tax return, as normal, until 2024."
    val section2heading = "Find software and check your account"
    val section2onlineServicesHeading = "Check HMRC online services"
    val section2onlineServicesParagraph = "You can review or change the answers you have just entered, and to get updates."
    val section2onlineServicesLink = "Go to your HMRC online services account"
    val section2onlineServicesNextYearParagraph1 = "Go to your HMRC online services account to review or change the answers you have entered," +
      " and to get updates."
    val section2onlineServicesNextYearParagraph1Link = "HMRC online services account"
    val section2onlineServicesNextYearParagraph2 = "It may take a few hours for new information to appear."
  }
}
