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

import models.common.AccountingPeriodModel
import models.{DateModel, UpdateDeadline}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.{AccountingPeriodUtil, ImplicitDateFormatter, ImplicitDateFormatterImpl, ViewSpec}
import views.html.individual.incometax.subscription.SignUpConfirmation

import java.time.LocalDate
import java.time.Month._
import java.time.format.DateTimeFormatter
import scala.util.Random


class SignUpConfirmationViewSpec extends ViewSpec {

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  import implicitDateFormatter.LongDate

  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  val testName = "Lisa Khan"
  val testNino = "QQ123456L"
  private val startDate: DateModel = DateModel(getRandomDate, "4", "2010")

  private def getRandomDate = (Math.random() * 10 + 1).toInt.toString

  private val endDate: DateModel = DateModel(getRandomDate, "4", "2011")
  val testAccountingPeriodModel: AccountingPeriodModel = AccountingPeriodModel(startDate, endDate)

  def page(mandatedCurrentYear: Boolean, selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String]): Html =
    signUpConfirmation(mandatedCurrentYear, selectedTaxYearIsNext, userNameMaybe, testNino)

  def document(eligibleNextYearOnly: Boolean, mandatedCurrentYear: Boolean, mandatedNextYear: Boolean, selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String] = Some(testName)): Document =
    Jsoup.parse(page(mandatedCurrentYear, selectedTaxYearIsNext, userNameMaybe).body)

  "The sign up confirmation view" when {
    "the user is voluntary and eligible for this year" should {
      def mainContent: Element = document(eligibleNextYearOnly = false, mandatedCurrentYear = false, mandatedNextYear = false, selectedTaxYearIsNext = false).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          mainContent.select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }
        "contains the user name and nino" in {
          mainContent.select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(0)
            .text() mustBe SignUpConfirmationMessages.panelUserDetails
        }

        "contains the description" in {
          mainContent.select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(1)
            .text() mustBe SignUpConfirmationMessages.panelDescription(false)
        }
      }

      "contains what you must do heading" in {
        mainContent.selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "contains the find software section in first position" which {

        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 1).selectHead("h3").text() contains SignUpConfirmationMessages.findSoftwareHeading
        }

        "contains a paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 1).selectHead("p").text() mustBe SignUpConfirmationMessages.findSoftwareParagraph
        }

        "contains a link" in {
          val link = mainContent.selectNth(".govuk-link", 1)
          link.attr("href") mustBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
          link.text mustBe SignUpConfirmationMessages.findSoftwareLink
        }
      }

      "have a Quarterly updates section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 2).selectHead("h3").text() contains SignUpConfirmationMessages.quarterlyUpdatesHeading
        }

        "contains Quarterly Updates initial paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 2).selectHead("p").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesParagraph
        }

        "contains a table" in {
          mainContent.selectHead("ol").selectNth("li", 2).mustHaveTable(
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

        "contains a warning message" in {
          mainContent.selectHead(".govuk-warning-text").text() contains SignUpConfirmationMessages.warningMessage
        }
      }

      "have End of Period section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 3).selectHead("h3").text() contains SignUpConfirmationMessages.endOfPeriodStatementHeading
        }

        "contains end of period paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 3).selectHead("p").text() mustBe SignUpConfirmationMessages.endOfPeriodStatementThisYearParagraph
        }
      }

      "have Final Declaration section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 4).selectHead("h3").text() contains SignUpConfirmationMessages.finalDeclarationCurrentYearHeading
        }

        "contains final declaration paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 4).selectHead("p").text() mustBe SignUpConfirmationMessages.finalDeclarationThisYearParagraph
        }
      }

      "contains the online HMRC services section in second position" which {

        "contains a heading" in {
          mainContent.selectHead("#check-online-section").selectHead("h2").text mustBe SignUpConfirmationMessages.onlineServicesHeading
        }

        "contains a paragraph" in {
          mainContent.selectHead("#check-online-section").selectHead("p").text() mustBe SignUpConfirmationMessages.onlineServicesThisYearParagraph
        }

        "contains a link" in {
          val link = mainContent.selectHead("#check-online-section").selectHead("a.govuk-link")
          link.attr("href") mustBe "https://www.tax.service.gov.uk/account"
          link.text mustBe SignUpConfirmationMessages.onlineServicesLink
        }
      }


      "have a button to print the page" in {
        mainContent.selectHead(".print-link").text() mustBe SignUpConfirmationMessages.printThisPage
        mainContent.selectHead(".print-link").attr("href") mustBe "javascript:window.print()"
      }
    }

    "the user is voluntary and eligible for next year only" should {
      def mainContent: Element = document(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = false, selectedTaxYearIsNext = true).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          mainContent.select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }

        "contains the user name and nino" in {
          mainContent.select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(0)
            .text() mustBe SignUpConfirmationMessages.panelUserDetails
        }

        "contains the description" in {
          mainContent.select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(1)
            .text() mustBe SignUpConfirmationMessages.panelDescription(true)
        }
      }

      "contains what you will have to do heading" in {
        mainContent.selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "have Self Tax Assessment section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 1).selectHead("h3").text() contains SignUpConfirmationMessages.submitSelfAssessmentHeading
        }

        "contains a paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 1).selectHead("p").text() mustBe SignUpConfirmationMessages.submitSelfAssessmentPara
        }
      }

      "have Get prepared section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 2).selectHead("h3").text() contains SignUpConfirmationMessages.gettingPreparedHeading
        }

        "contains a paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 2).selectHead("p").text() mustBe SignUpConfirmationMessages.gettingPreparedParagraph
        }

        "contains a link" in {
          mainContent.selectHead("ol").selectNth("li", 2).selectHead("a").text() mustBe SignUpConfirmationMessages.gettingPreparedLink
          mainContent.selectHead("a").attr("href") mustBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
        }
      }

      "have a Quarterly updates section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 3).selectHead("h3").text() contains SignUpConfirmationMessages.quarterlyUpdatesHeading
        }

        "contains Quarterly Updates initial paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 3).selectHead("p").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesParagraph
        }

        "contains a table" in {
          mainContent.selectHead("ol").selectNth("li", 3).mustHaveTable(
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
      }

      "have End of Period section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 4).selectHead("h3").text() contains SignUpConfirmationMessages.endOfPeriodStatementHeading
        }

        "contains end of period paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 4).selectHead("p").text() mustBe SignUpConfirmationMessages.endOfPeriodStatementNextYearParagraph
        }
      }

      "have Final Declaration section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 5).selectHead("h3").text() contains SignUpConfirmationMessages.finalDeclarationNextYearHeading
        }

        "contains final declaration paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 5).selectHead("p").text() mustBe SignUpConfirmationMessages.finalDeclarationNextYearParagraph
        }
      }

      "have a button to print the page" in {
        mainContent.selectHead(".print-link").text() mustBe SignUpConfirmationMessages.printThisPage
        mainContent.selectHead(".print-link").attr("href") mustBe "javascript:window.print()"
      }
    }

    "the user is mandated and eligible for next year only" should {
      def mainContent: Element = document(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = false, selectedTaxYearIsNext = true).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          mainContent.select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }

        "contains the user name and nino" in {
          mainContent.select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(0)
            .text() mustBe SignUpConfirmationMessages.panelUserDetails
        }

        "contains the description" in {
          mainContent.select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(1)
            .text() mustBe SignUpConfirmationMessages.panelDescription(true)
        }
      }

      "contains what you will have to do heading" in {
        mainContent.selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "have Self Tax Assessment section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 1).selectHead("h3").text() contains SignUpConfirmationMessages.submitSelfAssessmentHeading
        }

        "contains a paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 1).selectHead("p").text() mustBe SignUpConfirmationMessages.submitSelfAssessmentPara
        }
      }

      "have Get prepared section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 2).selectHead("h3").text() contains SignUpConfirmationMessages.gettingPreparedHeading
        }

        "contains a paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 2).selectHead("p").text() mustBe SignUpConfirmationMessages.gettingPreparedParagraph
        }

        "contains a link" in {
          mainContent.selectHead("ol").selectNth("li", 2).selectHead("a").text() mustBe SignUpConfirmationMessages.gettingPreparedLink
          mainContent.selectHead("a").attr("href") mustBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
        }
      }

      "have a Quarterly updates section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 3).selectHead("h3").text() contains SignUpConfirmationMessages.quarterlyUpdatesHeading
        }

        "contains Quarterly Updates initial paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 3).selectHead("p").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesParagraph
        }

        "contains a table" in {
          mainContent.selectHead("ol").selectNth("li", 3).mustHaveTable(
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
      }

      "have End of Period section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 4).selectHead("h3").text() contains SignUpConfirmationMessages.endOfPeriodStatementHeading
        }

        "contains end of period paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 4).selectHead("p").text() mustBe SignUpConfirmationMessages.endOfPeriodStatementNextYearParagraph
        }
      }

      "have Final Declaration section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 5).selectHead("h3").text() contains SignUpConfirmationMessages.finalDeclarationNextYearHeading
        }

        "contains final declaration paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 5).selectHead("p").text() mustBe SignUpConfirmationMessages.finalDeclarationNextYearParagraph
        }
      }

      "have a button to print the page" in {
        mainContent.selectHead(".print-link").text() mustBe SignUpConfirmationMessages.printThisPage
        mainContent.selectHead(".print-link").attr("href") mustBe "javascript:window.print()"
      }
    }

    "the user is mandated and eligible for this year" should {
      def mainContent: Element = document(eligibleNextYearOnly = false, mandatedCurrentYear = true, mandatedNextYear = false, selectedTaxYearIsNext = false).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          mainContent.select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }

        "contains the user name and nino" in {
          mainContent.select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(0)
            .text() mustBe SignUpConfirmationMessages.panelUserDetails
        }

        "contains the description" in {
          mainContent.select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(1)
            .text() mustBe SignUpConfirmationMessages.panelDescription(false)
        }
      }

      "contains what you will have to do heading" in {
        mainContent.selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "contains the find software section in first position" which {

        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 1).selectHead("h3").text() contains SignUpConfirmationMessages.findSoftwareHeading
        }

        "contains a paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 1).selectHead("p").text() mustBe SignUpConfirmationMessages.findSoftwareParagraph
        }

        "contains a link" in {
          val link = mainContent.selectNth(".govuk-link", 1)
          link.attr("href") mustBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
          link.text mustBe SignUpConfirmationMessages.findSoftwareLink
        }
      }

      "have a Quarterly updates section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 2).selectHead("h3").text() contains SignUpConfirmationMessages.quarterlyUpdatesHeading
        }

        "contains Quarterly Updates initial paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 2).selectHead("p").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesParagraph
        }

        "contains a table" in {
          mainContent.selectHead("ol").selectNth("li", 2).mustHaveTable(
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
      }

      "have End of Period section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 3).selectHead("h3").text() contains SignUpConfirmationMessages.endOfPeriodStatementHeading
        }

        "contains end of period paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 3).selectHead("p").text() mustBe SignUpConfirmationMessages.endOfPeriodStatementThisYearParagraph
        }
      }

      "have Final Declaration section" which {
        "contains a heading" in {
          mainContent.selectHead("ol").selectNth("li", 4).selectHead("h3").text() contains SignUpConfirmationMessages.finalDeclarationCurrentYearHeading
        }

        "contains final declaration paragraph" in {
          mainContent.selectHead("ol").selectNth("li", 4).selectHead("p").text() mustBe SignUpConfirmationMessages.finalDeclarationThisYearParagraph
        }
      }

      "have Notification Panel" which {
        def notificationBanner: Element = mainContent.selectHead(".govuk-notification-banner")

        "contains Notification Header" in {
          notificationBanner.selectHead(".govuk-notification-banner__header").text mustBe SignUpConfirmationMessages.notificationBannerHeader
        }

        "contains a paragraph" in {
          notificationBanner.selectHead("p").text() mustBe SignUpConfirmationMessages.notificationBannerParagraph
        }

        "has a bullet list" which {
          def bulletList: Element = notificationBanner.selectHead("ul")

          "has a first bullet" in {
            bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.notificationBannerBulletOne
          }

          "has a second bullet" in {
            bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.notificationBannerBulletTwo
          }

          "has a thrid bullet" in {
            bulletList.selectNth("li", 3).text mustBe SignUpConfirmationMessages.notificationBannerBulletThree
          }

          "has a fourth bullet" in {
            bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.notificationBannerBulletFour
          }
        }
      }
      "contains the online HMRC services section in second position" which {

        "contains a heading" in {
          mainContent.selectHead("#check-online-section").selectHead("h2").text() contains SignUpConfirmationMessages.onlineServicesHeading
        }

        "contains a paragraph" in {
          mainContent.selectHead("#check-online-section").selectHead("p").text() mustBe SignUpConfirmationMessages.onlineServicesThisYearParagraph
        }

        "contains a link" in {
          mainContent.selectNth(".govuk-link", 2).attr("href") mustBe "https://www.tax.service.gov.uk/account"
        }
      }

      "have a button to print the page" in {
        mainContent.selectHead(".print-link").text() mustBe SignUpConfirmationMessages.printThisPage
        mainContent.selectHead(".print-link").attr("href") mustBe "javascript:window.print()"
      }
    }
  }

  private object SignUpConfirmationMessages {
    val whatYouMustDoHeading = "What you must do"
    val panelHeading = "Sign up complete"
    val panelUserDetails = s"$testName | $testNino"
    val panelDescriptionThis: String = {
      val yearStart = AccountingPeriodUtil.getCurrentTaxYear.startDate.year
      val yearEnd = AccountingPeriodUtil.getCurrentTaxYear.endDate.year
      s"is now signed up for Making Tax Digital for Income Tax for the current tax year (6 April $yearStart to 5 April $yearEnd)"
    }
    val panelDescriptionNext: String = {
      val yearStart = AccountingPeriodUtil.getNextTaxYear.startDate.year
      val yearEnd = AccountingPeriodUtil.getNextTaxYear.endDate.year
      s"is now signed up for Making Tax Digital for Income Tax for the next tax year (6 April $yearStart to 5 April $yearEnd)"
    }

    def panelDescription(yearIsNext: Boolean): String = if (yearIsNext)
      SignUpConfirmationMessages.panelDescriptionNext
    else
      SignUpConfirmationMessages.panelDescriptionThis

    val submitSelfAssessmentHeading = "Submit your Self Assessment tax return for this year"
    val submitSelfAssessmentPara = s"You must submit your Self Assessment tax return as normal until 31 January ${AccountingPeriodUtil.getCurrentTaxEndYear + 1}."

    val quarterlyUpdatesHeading = "Send quarterly updates"
    val quarterlyUpdatesParagraph = "You must send quarterly updates of your income and expenses using compatible software by the following deadlines:"
    val quarterlyUpdatesTableCaption = "Quarterly updates by the deadline"
    val quarterlyUpdate = "Quarterly update"
    val deadline = "Deadline"

    val warningMessage = "You must make updates for any quarters you’ve missed."

    val endOfPeriodStatementHeading = "Send an end of period statement"
    val endOfPeriodStatementThisYearDate: String = AccountingPeriodUtil.getEndOfPeriodStatementDate(false).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
    val endOfPeriodStatementNextYearDate: String = AccountingPeriodUtil.getEndOfPeriodStatementDate(true).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
    val endOfPeriodStatementThisYearParagraph = s"You must submit an end of period statement using your software by $endOfPeriodStatementThisYearDate."
    val endOfPeriodStatementNextYearParagraph = s"You must submit an end of period statement using your software by $endOfPeriodStatementNextYearDate."

    val finalDeclarationCurrentYearHeading = "Submit a final declaration and pay your tax"
    val finalDeclarationNextYearHeading = "Submit a final declaration and pay tax"
    val finalDeclarationThisYearDate: String = AccountingPeriodUtil.getFinalDeclarationDate(false).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
    val finalDeclarationNextYearDate: String = AccountingPeriodUtil.getFinalDeclarationDate(true).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
    val finalDeclarationThisYearParagraph = s"You must submit your final declaration and pay the tax you owe by $finalDeclarationThisYearDate."
    val finalDeclarationNextYearParagraph = s"You must submit your final declaration and pay the tax you owe by $finalDeclarationNextYearDate."

    val notificationBannerHeader = "Important"
    val notificationBannerParagraph = "You may be penalised if you fail to meet the deadlines for your:"
    val notificationBannerBulletOne = "quarterly updates"
    val notificationBannerBulletTwo = "end of period statement"
    val notificationBannerBulletThree = "final declaration"
    val notificationBannerBulletFour = "tax payment"

    val onlineServicesHeading = "Check HMRC online services"
    val onlineServicesThisYearParagraph = "You can review or change the answers you have just entered, and to get updates."
    val onlineServicesLink = "Go to your HMRC online services account"

    val findSoftwareHeading = "Find software"
    val findSoftwareParagraph =
      "Before you can use Making Tax Digital for Income Tax you need to choose software and allow it to interact with this service."
    val findSoftwareLink = "Find software"

    val gettingPreparedHeading = "Getting prepared"
    val gettingPreparedParagraph: String =
      s"You need to get compatible software before you start using Making Tax Digital for Income Tax on April 6 ${AccountingPeriodUtil.getCurrentTaxEndYear}."
    val gettingPreparedLink = "Find software (opens in new tab)"

    val printThisPage = "Print this page"
  }

  private val CURRENT_TAX_YEAR: Int = Random.between(1900, 2100)
  private val FIFTH: Int = 5
  private val SIXTH: Int = 6
  private lazy val q1Update: UpdateDeadline = UpdateDeadline(
    AccountingPeriodModel(
      LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, SIXTH),
      LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, FIFTH)
    ),
    LocalDate.of(CURRENT_TAX_YEAR - 1, AUGUST, FIFTH))
  private lazy val q2Update: UpdateDeadline = UpdateDeadline(
    AccountingPeriodModel(
      LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, SIXTH),
      LocalDate.of(CURRENT_TAX_YEAR - 1, OCTOBER, FIFTH)
    ),
    LocalDate.of(CURRENT_TAX_YEAR - 1, NOVEMBER, FIFTH))
  private lazy val q3Update: UpdateDeadline = UpdateDeadline(
    AccountingPeriodModel(
      LocalDate.of(CURRENT_TAX_YEAR - 1, OCTOBER, SIXTH),
      LocalDate.of(CURRENT_TAX_YEAR - 1, JANUARY, FIFTH)
    ),
    LocalDate.of(CURRENT_TAX_YEAR - 1, FEBRUARY, FIFTH))
  private lazy val q4Update: UpdateDeadline = UpdateDeadline(
    AccountingPeriodModel(
      LocalDate.of(CURRENT_TAX_YEAR - 1, JANUARY, SIXTH),
      LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, FIFTH)
    ),
    LocalDate.of(CURRENT_TAX_YEAR - 1, MAY, FIFTH))
}