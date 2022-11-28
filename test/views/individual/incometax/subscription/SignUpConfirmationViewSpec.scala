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

import models.UpdateDeadline
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.{ImplicitDateFormatter, ImplicitDateFormatterImpl, ViewSpec}
import views.html.individual.incometax.subscription.SignUpConfirmation

import java.time.LocalDate
import java.time.Month._
import scala.util.Random

class SignUpConfirmationViewSpec extends ViewSpec {
  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  import implicitDateFormatter.LongDate

  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  val testName = "Lisa Khan"
  val testNino = "QQ123456L"

  def page(selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String]): Html = signUpConfirmation(selectedTaxYearIsNext, userNameMaybe, testNino)

  def document(selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String] = Some(testName)): Document =
    Jsoup.parse(page(selectedTaxYearIsNext, userNameMaybe).body)

  "The sign up confirmation view" when {
    for (yearIsNext <- Seq(true, false)) {
      s"nextYear flag is $yearIsNext" must {
        val testMainContent = document(yearIsNext).mainContent
        "have a heading panel" which {
          "contains a page heading" in {
            testMainContent.selectHead("h1").text() mustBe SignUpConfirmationMessages.heading
          }

          "contains a panel body 1" that {
            "has the user full name" when {
              "the user full name is provided to the view" in {
                testMainContent.selectNth(".govuk-panel__body p", 1).text() mustBe s"$testName | $testNino"
              }
            }

            "doesn't have the user full name" when {
              "the user full name is not provided to the view" in {
                document(yearIsNext, None).mainContent.selectNth(".govuk-panel__body p", 1).text() mustBe s"$testNino"
              }
            }
          }

          "contains a panel body 2" in {
            testMainContent.selectNth(".govuk-panel__body p", 2).text() mustBe SignUpConfirmationMessages.headingPanelBody
          }
        }

        "have a section 1" which {
          "contains a heading" in {
            testMainContent.selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.section1heading
          }

          if (yearIsNext) {
            "contains a hint" in {
              testMainContent.selectHead(".govuk-warning-text .govuk-warning-text__text").text() mustBe SignUpConfirmationMessages.section1hint
            }
          } else {
            "does not contain a hint" in {
              testMainContent.select(".govuk-warning-text .govuk-warning-text__text").isEmpty mustBe true
            }
          }

          "contains the quarterly updates section" which {
            def quarterlyUpdates: Element = testMainContent.selectNth(".govuk-grid-column-full .govuk-grid-column-full", 1)

            if (yearIsNext) {
              "contains a heading" in {
                quarterlyUpdates.selectHead("h3").text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesNextYearHeading
              }
            } else {
              "contains a heading" in {
                quarterlyUpdates.selectHead("h3").text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesThisYearHeading
              }
            }

            if(yearIsNext){
              "contains next year paragraph" in {
                quarterlyUpdates.selectNth("p", 1).text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesParagraph
              }
            } else {
              "contains this year paragraph" in {
                quarterlyUpdates.selectNth("p", 1).text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesParagraph
              }
            }

            "contains a table" in {
              quarterlyUpdates.mustHaveTable(
                tableHeads = List(SignUpConfirmationMessages.quarterlyUpdate, SignUpConfirmationMessages.deadline),
                tableRows = List(
                  List(q1Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q1Update.deadline.toLongDateNoYear),
                  List(q2Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q2Update.deadline.toLongDateNoYear),
                  List(q3Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q3Update.deadline.toLongDateNoYear),
                  List(q4Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q4Update.deadline.toLongDateNoYear)
                ),
                maybeCaption = Some(SignUpConfirmationMessages.quarterlyUpdatesTableCaption),
                hiddenTableCaption = false
              )
            }

            if (yearIsNext) {
              "contains next year paragraph 1" in {
                quarterlyUpdates.selectNth("p", 2).text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesNextYearParagraph1
              }

              "contains next year paragraph 2" in {
                quarterlyUpdates.selectNth("p", 3).text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesNextYearParagraph2
              }

              "contains next year paragraph 3" in {
                quarterlyUpdates.selectNth("p", 4).text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesNextYearParagraph3
              }
            } else {
              "contains this year paragraph 1" in {
                quarterlyUpdates.selectNth("p", 2).text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesThisYearParagraph1
              }

              "contains next year paragragh 2" in {
                quarterlyUpdates.selectNth("p", 3).text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesThisYearParagraph2
              }

               "contains this year paragraph 3" in {
                 quarterlyUpdates.selectNth("p", 4).text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesThisYearParagraph3
               }
            }
          }
        }

        "have a section 2" which {
          "contains a heading" in {
            testMainContent.selectNth("h2", 2).text() mustBe SignUpConfirmationMessages.section2heading
          }

          "contains the online HMRC services section" which {
            def onlineHmrcServices: Element = testMainContent.selectNth(".govuk-grid-column-one-half", 2)

            "contains a heading" in {
              onlineHmrcServices.selectHead("h3").text() mustBe SignUpConfirmationMessages.section2onlineServicesHeading
            }
            if (yearIsNext) {
              "contains next year paragraph 1" in {
                onlineHmrcServices.selectNth("p", 1).text() mustBe SignUpConfirmationMessages.section2onlineServicesNextYearParagraph1
              }

              "contains next year link" in {
                onlineHmrcServices.selectHead("a").text() mustBe SignUpConfirmationMessages.section2onlineServicesNextYearParagraph1Link
                onlineHmrcServices.selectHead("a").attr("href") mustBe "https://www.tax.service.gov.uk/account"
              }

              "contains next year paragraph 2" in {
                onlineHmrcServices.selectNth("p", 2).text() mustBe SignUpConfirmationMessages.section2onlineServicesNextYearParagraph2
              }
            } else {
              "contains this year paragraph" in {
                onlineHmrcServices.selectHead("p").text() mustBe SignUpConfirmationMessages.section2onlineServicesThisYearParagraph
              }

              "contains this year link" in {
                onlineHmrcServices.selectHead("a").text() mustBe SignUpConfirmationMessages.section2onlineServicesLink
                onlineHmrcServices.selectHead("a").attr("href") mustBe "https://www.tax.service.gov.uk/account"
              }
            }
          }
          "contains the find software section" which {
            def findSoftware: Element = testMainContent.selectNth(".govuk-grid-column-one-half", 1)

            "contains a heading" in {
              findSoftware.selectHead("h3").text() mustBe SignUpConfirmationMessages.section2FindSoftwareHeading
            }
            if (yearIsNext) {

              // Next year "find software" section tests go here.

            } else {

              "contains this year paragraph" in {
                findSoftware.selectHead("p").text() mustBe SignUpConfirmationMessages.section2FindSoftwareParagraph
              }

              "contains this year link" in {
                findSoftware.selectHead("a").text() mustBe SignUpConfirmationMessages.section2FindSoftwareLink
                findSoftware.selectHead("a").attr("href") mustBe
                  "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
              }
            }
          }
        }
      }
    }
  }

  private object SignUpConfirmationMessages {
    val heading = "Sign up complete"
    val headingPanelBody = "is signed up for Making Tax Digital for Income Tax for the current year"
    val section1heading = "What you will have to do"
    val section1hint = "Warning Continue to submit your Self Assessment tax return, as normal, until 2024."
    val section2heading = "Find software and check your account"

    val quarterlyUpdate = "Quarterly update"
    val deadline = "Deadline"
    val quarterlyUpdatesTableCaption = "Quarterly updates by the deadline"

    val section1QuarterlyUpdatesNextYearHeading = "1. Then update us every quarter"
    val section1QuarterlyUpdatesParagraph = "You can file as many updates as you want but you must submit them on time, each quarter."
    val section1QuarterlyUpdatesNextYearParagraph1 = "You can start sending quarterly updates during the next tax year. It will not affect the amount you pay."
    val section1QuarterlyUpdatesNextYearParagraph2 = "After you have sent an update you will get a year-to-date Income Tax estimate."
    val section1QuarterlyUpdatesNextYearParagraph3 = "There is no penalty if you start making updates mid-way through the next tax year but you will need to make updates for the quarters you’ve missed."

    val section1QuarterlyUpdatesThisYearHeading = "1. Update us every quarter"
    val section1QuarterlyUpdatesThisYearParagraph1 = "You can start sending quarterly updates during the current tax year. It will not affect the amount you pay."
    val section1QuarterlyUpdatesThisYearParagraph2 = "After you have sent an update you will get a year-to-date Income Tax estimate."
    val section1QuarterlyUpdatesThisYearParagraph3 = "There is no penalty if you start making updates mid-way through the current tax year but you will need to make updates for the quarters you’ve missed."

    val section2onlineServicesHeading = "Check HMRC online services"
    val section2onlineServicesThisYearParagraph = "You can review or change the answers you have just entered, and to get updates."
    val section2onlineServicesLink = "Go to your HMRC online services account"
    val section2onlineServicesNextYearParagraph1 =
      "Go to your HMRC online services account to review or change the answers you have entered, and to get updates."
    val section2onlineServicesNextYearParagraph1Link = "HMRC online services account"
    val section2onlineServicesNextYearParagraph2 = "It may take a few hours for new information to appear."

    val section2FindSoftwareHeading = "Find software"
    val section2FindSoftwareParagraph =
      "Before you can use Making Tax Digital for Income Tax you need to choose software and allow it to interact with this service."
    val section2FindSoftwareLink = "Find software"
  }

  private val CURRENT_TAX_YEAR: Int = Random.between(1900, 2100)
  private val FIFTH: Int = 5
  private val SIXTH: Int = 6
  private val q1Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, AUGUST, FIFTH))
  private val q2Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, OCTOBER, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, NOVEMBER, FIFTH))
  private val q3Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, OCTOBER, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, JANUARY, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, FEBRUARY, FIFTH))
  private val q4Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, JANUARY, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, MAY, FIFTH))
}
