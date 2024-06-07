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

package views.agent.confirmation

import models.common.AccountingPeriodModel
import models.{DateModel, UpdateDeadline}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.{AccountingPeriodUtil, ImplicitDateFormatter, ImplicitDateFormatterImpl, ViewSpec}
import views.html.agent.confirmation.SignUpConfirmation

import java.time.LocalDate
import java.time.Month._
import java.time.format.DateTimeFormatter
import scala.util.Random

class SignUpConfirmationViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  import implicitDateFormatter.LongDate

  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  val testName = "Lisa Khan"
  val testNino = "QQ123456L"
  private val startDate: DateModel = DateModel(getRandomDate, "4", "2010")

  private val CURRENT_TAX_YEAR: Int = Random.between(1900, 2100)
  private val FIFTH: Int = 5
  private val SIXTH: Int = 6
  private val q1Update: UpdateDeadline = UpdateDeadline(
    AccountingPeriodModel(
      LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, SIXTH),
      LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, FIFTH)
    ),
    LocalDate.of(CURRENT_TAX_YEAR - 1, AUGUST, FIFTH))
  private val q2Update: UpdateDeadline = UpdateDeadline(
    AccountingPeriodModel(
      LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, SIXTH),
      LocalDate.of(CURRENT_TAX_YEAR - 1, OCTOBER, FIFTH)
    ),
    LocalDate.of(CURRENT_TAX_YEAR - 1, NOVEMBER, FIFTH))
  private val q3Update: UpdateDeadline = UpdateDeadline(
    AccountingPeriodModel(
      LocalDate.of(CURRENT_TAX_YEAR - 1, OCTOBER, SIXTH),
      LocalDate.of(CURRENT_TAX_YEAR - 1, JANUARY, FIFTH)
    ),
    LocalDate.of(CURRENT_TAX_YEAR - 1, FEBRUARY, FIFTH))
  private val q4Update: UpdateDeadline = UpdateDeadline(
    AccountingPeriodModel(
      LocalDate.of(CURRENT_TAX_YEAR - 1, JANUARY, SIXTH),
      LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, FIFTH)
    ),
    LocalDate.of(CURRENT_TAX_YEAR - 1, MAY, FIFTH))

  private def getRandomDate = (Math.random() * 10 + 1).toInt.toString

  private val endDate: DateModel = DateModel(getRandomDate, "4", "2011")
  val testAccountingPeriodModel: AccountingPeriodModel = AccountingPeriodModel(startDate, endDate)

  def page(mandatedCurrentYear: Boolean, mandatedNextYear: Boolean, selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String]): Html =
    signUpConfirmation(mandatedCurrentYear, mandatedNextYear, selectedTaxYearIsNext, userNameMaybe, testNino, testAccountingPeriodModel)

  def document(eligibleNextYearOnly: Boolean, mandatedCurrentYear: Boolean, mandatedNextYear: Boolean, selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String] = Some(testName)): Document =
    Jsoup.parse(page(mandatedCurrentYear, mandatedNextYear, selectedTaxYearIsNext, userNameMaybe).body)

  "The sign up confirmation view" when {
    "the user is voluntary and eligible for both years and feature switch is disabled" should {
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

      "have a print link" in {
        val link = mainContent.selectNth(".govuk-link", 1)
        link.text mustBe SignUpConfirmationMessages.printLink
        link.attr("data-module") mustBe "hmrc-print-link"
        link.attr("href") mustBe "#"
      }

      "contains what you will have to do heading" in {
        mainContent.selectHead("h2").text() mustBe SignUpConfirmationMessages.whatToDoHeading
      }

      "have a Quarterly updates section" which {

        "contains a heading" in {
          mainContent.selectHead("h3").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesHeading
        }

        "contains Quarterly Updates initial paragraph" in {
          mainContent.selectNth("p", 4).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesParagraph
        }

        "contains a table" in {
          mainContent.mustHaveTable(
            tableHeads = List(SignUpConfirmationMessages.quarterlyUpdate, SignUpConfirmationMessages.deadline),
            tableRows = List(
              List(q1Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q1Update.deadline.toLongDateNoYear),
              List(q2Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q2Update.deadline.toLongDateNoYear),
              List(q3Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q3Update.deadline.toLongDateNoYear),
              List(q4Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q4Update.deadline.toLongDateNoYear)
            ),
            maybeCaption = Some(SignUpConfirmationMessages.quarterlyUpdatesTableCaptionTitle),
            hiddenTableCaption = false
          )
        }

        "contains a warning message" in {
          mainContent.selectHead(".govuk-warning-text").text() mustBe SignUpConfirmationMessages.warningMessage
        }

        "contains details para1" in {
          mainContent.selectNth(".govuk-details", 1).selectHead("p").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesDetailsp1
        }

        "contains details para2" in {
          mainContent.selectNth(".govuk-details", 1).selectNth("p", 2).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesDetailsp2
        }
      }

      "have Final Declaration section" which {

        "contains a heading" in {
          mainContent.selectNth("h3", 2).text() mustBe SignUpConfirmationMessages.finalDeclarationHeading
        }

        "contains final declaration paragraph" in {
          mainContent.selectNth("p", 7).text() mustBe SignUpConfirmationMessages.finalDeclarationThisYearParagraph
        }
      }

      "have a button to sign up another client" in {
        mainContent.selectHead(".govuk-button").text() mustBe SignUpConfirmationMessages.signUpAnotherClient
        mainContent.selectHead(".govuk-button").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
      }
    }

    "the user is voluntary and eligible for both years and feature switch is enabled" should {

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

      "contains what you will have to do heading" in {
        mainContent.selectHead("h2").text() mustBe SignUpConfirmationMessages.whatToDoHeading
      }

      "have a Quarterly updates section" which {

        "contains a heading" in {
          mainContent.selectHead("h3").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesHeading
        }

        "contains Quarterly Updates initial paragraph" in {
          mainContent.selectNth("p", 4).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesParagraph
        }

        "contains a table" in {
          mainContent.mustHaveTable(
            tableHeads = List(SignUpConfirmationMessages.quarterlyUpdate, SignUpConfirmationMessages.deadline),
            tableRows = List(
              List(q1Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q1Update.deadline.toLongDateNoYear),
              List(q2Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q2Update.deadline.toLongDateNoYear),
              List(q3Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q3Update.deadline.toLongDateNoYear),
              List(q4Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q4Update.deadline.toLongDateNoYear)
            ),
            maybeCaption = Some(SignUpConfirmationMessages.quarterlyUpdatesTableCaptionTitle),
            hiddenTableCaption = false
          )
        }

        "contains a warning message" in {
          mainContent.selectHead(".govuk-warning-text").text() mustBe SignUpConfirmationMessages.warningMessage
        }

        "contains details para1" in {
          mainContent.selectNth(".govuk-details", 1).selectHead("p").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesDetailsp1
        }

        "contains details para2" in {
          mainContent.selectNth(".govuk-details", 1).selectNth("p", 2).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesDetailsp2
        }
      }

      "have Final Declaration section" which {
        "contains a heading" in {
          mainContent.selectNth("h3", 2).text() mustBe SignUpConfirmationMessages.finalDeclarationHeading
        }

        "contains final declaration paragraph" in {
          mainContent.selectNth("p", 7).text() mustBe SignUpConfirmationMessages.finalDeclarationThisYearParagraph
        }
      }

      "have a button to sign up another client" in {
        mainContent.selectHead(".govuk-button").text() mustBe SignUpConfirmationMessages.signUpAnotherClient
        mainContent.selectHead(".govuk-button").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
      }
    }

    "the user is voluntary and eligible for next year only and feature switch is disabled" should {
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
        mainContent.selectHead("h2").text() mustBe SignUpConfirmationMessages.whatToDoHeading
      }

      "have Self Assessment section" which {

        "contains a heading" in {
          mainContent.selectHead("h3").text() mustBe SignUpConfirmationMessages.continueSelfAssessmentHeading
        }

        "contains a paragraph" in {
          mainContent.selectNth("p", 4).text() mustBe SignUpConfirmationMessages.continueSelfAssessmentPara
        }
      }

      "have a Quarterly updates section" which {

        "contains a heading" in {
          mainContent.selectNth("h3", 2).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesHeading
        }

        "contains Quarterly Updates initial paragraph" in {
          mainContent.selectNth("p", 5).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesParagraph
        }

        "contains a table" in {
          mainContent.mustHaveTable(
            tableHeads = List(SignUpConfirmationMessages.quarterlyUpdate, SignUpConfirmationMessages.deadline),
            tableRows = List(
              List(q1Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q1Update.deadline.toLongDateNoYear),
              List(q2Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q2Update.deadline.toLongDateNoYear),
              List(q3Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q3Update.deadline.toLongDateNoYear),
              List(q4Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q4Update.deadline.toLongDateNoYear)
            ),
            maybeCaption = Some(SignUpConfirmationMessages.quarterlyUpdatesTableCaptionTitle),
            hiddenTableCaption = false
          )
        }

        "contains details para1" in {
          mainContent.selectNth(".govuk-details", 1).selectHead("p").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesDetailsp1
        }

        "contains details para2" in {
          mainContent.selectNth(".govuk-details", 1).selectNth("p", 2).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesDetailsp2
        }
      }

      "have Final Declaration section" which {

        "contains a heading" in {
          mainContent.selectNth("h3", 3).text() mustBe SignUpConfirmationMessages.finalDeclarationHeading
        }

        "contains final declaration paragraph" in {
          mainContent.selectNth("p", 8).text() mustBe SignUpConfirmationMessages.finalDeclarationNextYearParagraph
        }
      }

      "have a button to sign up another client" in {
        mainContent.selectHead(".govuk-button").text() mustBe SignUpConfirmationMessages.signUpAnotherClient
        mainContent.selectHead(".govuk-button").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
      }
    }

    "the user is voluntary and eligible for next year only and feature switch is enabled" should {
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
        mainContent.selectHead("h2").text() mustBe SignUpConfirmationMessages.whatToDoHeading
      }

      "have Self Assessment section" which {

        "contains a heading" in {
          mainContent.selectHead("h3").text() mustBe SignUpConfirmationMessages.continueSelfAssessmentHeading
        }

        "contains a paragraph" in {
          mainContent.selectNth("p", 4).text() mustBe SignUpConfirmationMessages.continueSelfAssessmentPara
        }
      }

      "have a Quarterly updates section" which {

        "contains a heading" in {
          mainContent.selectNth("h3", 2).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesHeading
        }

        "contains Quarterly Updates initial paragraph" in {
          mainContent.selectNth("p", 5).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesParagraph
        }

        "contains a table" in {
          mainContent.mustHaveTable(
            tableHeads = List(SignUpConfirmationMessages.quarterlyUpdate, SignUpConfirmationMessages.deadline),
            tableRows = List(
              List(q1Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q1Update.deadline.toLongDateNoYear),
              List(q2Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q2Update.deadline.toLongDateNoYear),
              List(q3Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q3Update.deadline.toLongDateNoYear),
              List(q4Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q4Update.deadline.toLongDateNoYear)
            ),
            maybeCaption = Some(SignUpConfirmationMessages.quarterlyUpdatesTableCaptionTitle),
            hiddenTableCaption = false
          )
        }

        "contains details para1" in {
          mainContent.selectNth(".govuk-details", 1).selectHead("p").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesDetailsp1
        }

        "contains details para2" in {
          mainContent.selectNth(".govuk-details", 1).selectNth("p", 2).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesDetailsp2
        }
      }

      "have Final Declaration section" which {

        "contains a heading" in {
          mainContent.selectNth("h3", 3).text() mustBe SignUpConfirmationMessages.finalDeclarationHeading
        }

        "contains final declaration paragraph" in {
          mainContent.selectNth("p", 8).text() mustBe SignUpConfirmationMessages.finalDeclarationNextYearParagraph
        }
      }

      "have a button to sign up another client" in {
        mainContent.selectHead(".govuk-button").text() mustBe SignUpConfirmationMessages.signUpAnotherClient
        mainContent.selectHead(".govuk-button").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
      }
    }

    "the user is mandated and eligible for next year only" should {
      def mainContent: Element = document(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = true, selectedTaxYearIsNext = true).mainContent

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
        mainContent.selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatToDoHeading
      }

      "have Self Assessment section" which {

        "contains a heading" in {
          mainContent.selectHead("h3").text() mustBe SignUpConfirmationMessages.continueSelfAssessmentHeading
        }

        "contains a paragraph" in {
          mainContent.selectNth("p", 4).text() mustBe SignUpConfirmationMessages.continueSelfAssessmentPara
        }
      }

      "have a Quarterly updates section" which {


        "contains a heading" in {
          mainContent.selectNth("h3", 2).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesHeading
        }

        "contains Quarterly Updates initial paragraph" in {
          mainContent.selectNth("p", 5).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesParagraph
        }

        "contains a table" in {
          mainContent.mustHaveTable(
            tableHeads = List(SignUpConfirmationMessages.quarterlyUpdate, SignUpConfirmationMessages.deadline),
            tableRows = List(
              List(q1Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q1Update.deadline.toLongDateNoYear),
              List(q2Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q2Update.deadline.toLongDateNoYear),
              List(q3Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q3Update.deadline.toLongDateNoYear),
              List(q4Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q4Update.deadline.toLongDateNoYear)
            ),
            maybeCaption = Some(SignUpConfirmationMessages.quarterlyUpdatesTableCaptionTitle),
            hiddenTableCaption = false
          )
        }
      }

      "have Final Declaration section" which {

        "contains a heading" in {
          mainContent.selectNth("h3", 3).text() mustBe SignUpConfirmationMessages.finalDeclarationHeading
        }

        "contains final declaration paragraph" in {
          mainContent.selectNth("p", 8).text() mustBe SignUpConfirmationMessages.finalDeclarationNextYearParagraph
        }
      }

      "have a button to sign up another client" in {
        mainContent.selectHead(".govuk-button").text() mustBe SignUpConfirmationMessages.signUpAnotherClient
        mainContent.selectHead(".govuk-button").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
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
        mainContent.selectHead("h2").text() mustBe SignUpConfirmationMessages.whatToDoHeading
      }

      "have a Quarterly updates section" which {

        "contains a heading" in {
          mainContent.selectHead("h3").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesHeading
        }

        "contains Quarterly Updates initial paragraph" in {
          mainContent.selectNth("p", 4).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesParagraph
        }

        "contains a table" in {
          mainContent.mustHaveTable(
            tableHeads = List(SignUpConfirmationMessages.quarterlyUpdate, SignUpConfirmationMessages.deadline),
            tableRows = List(
              List(q1Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q1Update.deadline.toLongDateNoYear),
              List(q2Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q2Update.deadline.toLongDateNoYear),
              List(q3Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q3Update.deadline.toLongDateNoYear),
              List(q4Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q4Update.deadline.toLongDateNoYear)
            ),
            maybeCaption = Some(SignUpConfirmationMessages.quarterlyUpdatesTableCaptionTitle),
            hiddenTableCaption = false
          )
        }
      }

      "have Final Declaration section" which {

        "contains a heading" in {
          mainContent.selectNth("h3", 2).text() mustBe SignUpConfirmationMessages.finalDeclarationHeading
        }

        "contains final declaration paragraph" in {
          mainContent.selectNth("p", 7).text() mustBe SignUpConfirmationMessages.finalDeclarationThisYearParagraph
        }
      }

      "have a button to sign up another client" in {
        mainContent.selectHead(".govuk-button").text() mustBe SignUpConfirmationMessages.signUpAnotherClient
        mainContent.selectHead(".govuk-button").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
      }
    }
  }

  private object SignUpConfirmationMessages {
    val whatToDoHeading = "What you must do now"
    val panelHeading = "Client sign up complete"
    val panelUserDetails = s"$testName | $testNino"
    val panelDescriptionThis = s"is now signed up for Making Tax Digital for Income Tax for the current tax year (${startDate.day} April 2010 to ${endDate.day} April 2011)"
    val panelDescriptionNext = s"is now signed up for Making Tax Digital for Income Tax for the next tax year (${startDate.day} April 2010 to ${endDate.day} April 2011)"

    def panelDescription(yearIsNext: Boolean): String = if (yearIsNext)
      SignUpConfirmationMessages.panelDescriptionNext
    else
      SignUpConfirmationMessages.panelDescriptionThis

    val printLink = "Print this client’s sign up confirmation"

    val continueSelfAssessmentHeading = "Continue Self Assessment for this year"
    val continueSelfAssessmentPara = s"Your client’s Self Assessment tax return must be submitted as normal on 31 January ${AccountingPeriodUtil.getNextTaxEndYear + 1}."

    val quarterlyUpdatesHeading = "Send quarterly updates"
    val quarterlyUpdatesParagraph = "You must send quarterly updates of your client’s income and expenses using compatible software."
    val quarterlyUpdatesTableCaption = "Quarterly update deadlines"
    val quarterlyUpdatesTableCaptionTitle = "Submit quarterly updates by the deadline"
    val quarterlyUpdate = "Quarterly update"
    val deadline = "Deadline"
    val quarterlyUpdatesDetailsp1 = "You can choose to send your client’s updates by calendar quarterly period dates. This must be selected in the compatible software before the first update is made."
    val quarterlyUpdatesDetailsp2 = "The deadline for both quarterly period dates are the same."

    val warningMessage = "! Warning You must make updates for any quarters you’ve missed."

    val finalDeclarationHeading = "Submit a final declaration and pay"
    val finalDeclarationThisYearDate: String = AccountingPeriodUtil.getFinalDeclarationDate(false).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
    val finalDeclarationNextYearDate: String = AccountingPeriodUtil.getFinalDeclarationDate(true).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
    val finalDeclarationThisYearParagraph = s"A final declaration must be submitted by either you or your client by $finalDeclarationThisYearDate. They must also pay the tax they owe."
    val finalDeclarationNextYearParagraph = s"A final declaration must be submitted by either you or your client by $finalDeclarationNextYearDate. They must also pay the tax they owe."

    val signUpAnotherClient = "Sign up another client"

  }


}
