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

package views.individual.incometax.subscription

import models.UpdateDeadline
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.{AccountingPeriodUtil, ImplicitDateFormatter, ImplicitDateFormatterImpl, ViewSpec}
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

  private val quarterlyUpdateRowNumber = 1
  private val endOfPeriodRowNumber = quarterlyUpdateRowNumber + 1
  private val onlineHmrcServicesRowNumber = endOfPeriodRowNumber + 1
  private val gettingPreparedRowNumber = endOfPeriodRowNumber + 1
  private val findSoftwareRowNumber = endOfPeriodRowNumber + 1

  private val onlineHmrcServicesColNumberNextYear = 1
  private val onlineHmrcServicesColNumberThisYear = 2
  private val findSoftwareColNumber = 1
  private val gettingPreparedColNumber = 2

  def page(selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String]): Html =
    signUpConfirmation(selectedTaxYearIsNext, userNameMaybe, testNino)

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
          if (yearIsNext) {
            "contains a panel body paragraph for next year" in {
              testMainContent.selectNth(".govuk-panel__body p", 2).text() mustBe SignUpConfirmationMessages.headingPanelBodyNext
            }
          } else {
            "contains a panel body paragraph for current year" in {
              testMainContent.selectNth(".govuk-panel__body p", 2).text() mustBe SignUpConfirmationMessages.headingPanelBodyCurrent
            }
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
            def quarterlyUpdates: Element = {
              testMainContent.selectNth(".row", quarterlyUpdateRowNumber).selectHead(".col")
            }

            if (yearIsNext) {
              "contains a heading" in {
                quarterlyUpdates.selectHead("h3").text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesNextYearHeading
              }
            } else {
              "contains a heading" in {
                quarterlyUpdates.selectHead("h3").text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesThisYearHeading
              }
            }

            "contains Quarterly Updates initial paragraph" in {
              quarterlyUpdates.selectNth("p", 1).text() mustBe SignUpConfirmationMessages.section1QuarterlyUpdatesParagraph
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

          "contains the end-of-period section" which {
            def endOfPeriod: Element = {
              testMainContent.selectNth(".row", endOfPeriodRowNumber).selectHead(".col")
            }

            if (yearIsNext) {
              "contains a heading" in {
                endOfPeriod.selectHead("h3").text() mustBe SignUpConfirmationMessages.section1EndOfPeriodNextYearHeading
              }
              "contains paragraph 1" in {
                endOfPeriod.selectNth("p", 1).text() mustBe SignUpConfirmationMessages.section1EndOfPeriodNextYearParagraph1
              }
              "contains paragraph 2" in {
                endOfPeriod.selectNth("p", 2).text() mustBe SignUpConfirmationMessages.section1EndOfPeriodNextYearParagraph2
              }
              "contains bullet 1" in {
                endOfPeriod.selectHead("ul").selectNth("li", 1).text() mustBe SignUpConfirmationMessages.section1EndOfPeriodNextYearBullet1
              }
              "contains bullet 2" in {
                endOfPeriod.selectHead("ul").selectNth("li", 2).text() mustBe SignUpConfirmationMessages.section1EndOfPeriodNextYearBullet2
              }
              "contains bullet 3" in {
                endOfPeriod.selectHead("ul").selectNth("li", 3).text() mustBe SignUpConfirmationMessages.section1EndOfPeriodNextYearBullet3
              }
            } else {
              // this year tests to come here
            }

          }
        }

        "have a section 2" which {

          "contains a heading" in {
            testMainContent.selectNth("h2", 2).text() mustBe SignUpConfirmationMessages.section2heading
          }

          if (yearIsNext) {
            "contains the online HMRC services section in first position" which {
              def onlineHmrcServices: Element = {
                testMainContent.selectNth(".row", onlineHmrcServicesRowNumber).selectNth(".col", onlineHmrcServicesColNumberNextYear)
              }

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
            "contains the getting prepared section in second position" which {
              def gettingPrepared: Element = {
                testMainContent.selectNth(".row", gettingPreparedRowNumber).selectNth(".col", gettingPreparedColNumber)
              }

              "contains a heading" in {
                gettingPrepared.selectHead("h3").text() mustBe SignUpConfirmationMessages.section2GettingPreparedHeading
              }

              "contains a paragraph" in {
                gettingPrepared.selectHead("p").text() mustBe SignUpConfirmationMessages.section2GettingPreparedParagraph
              }

              "contains a link" in {
                val link = gettingPrepared.selectHead("a")
                link.text() mustBe SignUpConfirmationMessages.section2GettingPreparedLink
                link.attr("href") mustBe
                  "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
              }

            }
          } else {
            "contains the find software section in first position" which {
              def findSoftware: Element = {
                testMainContent.selectNth(".row", findSoftwareRowNumber).selectNth(".col", findSoftwareColNumber)
              }

              "contains a heading" in {
                findSoftware.selectHead("h3").text() mustBe SignUpConfirmationMessages.section2FindSoftwareHeading
              }

              "contains a paragraph" in {
                findSoftware.selectHead("p").text() mustBe SignUpConfirmationMessages.section2FindSoftwareParagraph
              }

              "contains a link" in {
                findSoftware.selectHead("a").text() mustBe SignUpConfirmationMessages.section2FindSoftwareLink
                findSoftware.selectHead("a").attr("href") mustBe
                  "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
              }

            }
            "contains the online HMRC services section in second position" which {
              def onlineHmrcServices: Element = {
                testMainContent.selectNth(".row", onlineHmrcServicesRowNumber).selectNth(".col", onlineHmrcServicesColNumberThisYear)
              }

              "contains a heading" in {
                onlineHmrcServices.selectHead("h3").text() mustBe SignUpConfirmationMessages.section2onlineServicesHeading
              }

              "contains this year paragraph" in {
                onlineHmrcServices.selectHead("p").text() mustBe SignUpConfirmationMessages.section2onlineServicesThisYearParagraph
              }

              "contains this year link" in {
                onlineHmrcServices.selectHead("a").text() mustBe SignUpConfirmationMessages.section2onlineServicesLink
                onlineHmrcServices.selectHead("a").attr("href") mustBe "https://www.tax.service.gov.uk/account"
              }

            }
          }

        }
        "has a link to print the page" in {
          testMainContent.selectHead(".print-link").text() mustBe SignUpConfirmationMessages.printPage
          testMainContent.selectHead(".print-link").attr("href") mustBe "javascript:window.print()"
        }
      }
    }
  }

  private object SignUpConfirmationMessages {
    val heading = "Sign up complete"
    val headingPanelBodyCurrent = "is signed up for Making Tax Digital for Income Tax for the current tax year"
    val headingPanelBodyNext = "is signed up for Making Tax Digital for Income Tax for the next tax year"
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

    val section1EndOfPeriodNextYearHeading = "2. Send us an end of period statement"
    val section1EndOfPeriodNextYearParagraph1 = {
      val year = AccountingPeriodUtil.getNextTaxYear.endDate.year
      s"Use your software to send us an end of period statement, by 31 January $year."
    }
    val section1EndOfPeriodNextYearParagraph2 = "For each income source, you must:"
    val section1EndOfPeriodNextYearBullet1 = "make any accounting adjustments"
    val section1EndOfPeriodNextYearBullet2 = "claim any tax reliefs"
    val section1EndOfPeriodNextYearBullet3 = "confirm that the information you’ve sent is correct and complete"

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

    val section2GettingPreparedHeading = "Getting prepared"
    val section2GettingPreparedParagraph: String =
      "You can not sign up to Making Tax Digital for Income Tax until next year 2024. But, to help you get prepared, " +
        "we made a service to help you find the right software - so you are ready when you can sign up."
    val section2GettingPreparedLink = "Find software"

    val printPage = "Print this page"
  }

  private val CURRENT_TAX_YEAR: Int = Random.between(1900, 2100)
  private val FIFTH: Int = 5
  private val SIXTH: Int = 6
  private val q1Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, AUGUST, FIFTH))
  private val q2Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, OCTOBER, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, NOVEMBER, FIFTH))
  private val q3Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, OCTOBER, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, JANUARY, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, FEBRUARY, FIFTH))
  private val q4Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, JANUARY, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, MAY, FIFTH))

}
