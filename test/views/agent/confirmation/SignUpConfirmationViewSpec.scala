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

import models.DateModel
import models.common.AccountingPeriodModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.{AccountingPeriodUtil, ImplicitDateFormatterImpl, ViewSpec}
import views.html.agent.confirmation.SignUpConfirmation

import java.time.LocalDate

class SignUpConfirmationViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  implicit val implicitDateFormatter: ImplicitDateFormatterImpl = app.injector.instanceOf[ImplicitDateFormatterImpl]

  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  val testName = "Lisa Khan"
  val testNino = "QQ123456L"
  private val startDate: DateModel = DateModel(getRandomDate, "4", "2010")

  private def getRandomDate = (Math.random() * 10 + 1).toInt.toString

  private val endDate: DateModel = DateModel(getRandomDate, "4", "2011")
  val testAccountingPeriodModel: AccountingPeriodModel = AccountingPeriodModel(startDate, endDate)

  def page(mandatedCurrentYear: Boolean,
           mandatedNextYear: Boolean,
           selectedTaxYearIsNext: Boolean,
           userNameMaybe: Option[String],
           usingSoftwareStatus: Boolean,
           signedUpDate: LocalDate): Html =
    signUpConfirmation(mandatedCurrentYear,
      mandatedNextYear,
      selectedTaxYearIsNext,
      userNameMaybe,
      testNino,
      testAccountingPeriodModel,
      usingSoftwareStatus,
      signedUpDate)

  def document(eligibleNextYearOnly: Boolean,
               mandatedCurrentYear: Boolean,
               mandatedNextYear: Boolean,
               selectedTaxYearIsNext: Boolean,
               userNameMaybe: Option[String] = Some(testName),
               usingSoftwareStatus: Boolean,
               signedUpDate: LocalDate): Document =
    Jsoup.parse(page(mandatedCurrentYear, mandatedNextYear, selectedTaxYearIsNext, userNameMaybe, usingSoftwareStatus, signedUpDate).body)

  "The sign up confirmation view" when {
      "the user has software and for current year. Content identical across all scenarios" should {
      def mainContent: Element = document(eligibleNextYearOnly = false,
        mandatedCurrentYear = false,
        mandatedNextYear = false,
        selectedTaxYearIsNext = false,
        usingSoftwareStatus = false,
        signedUpDate = LocalDate.now()).mainContent

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

      "contains date field" in {
        mainContent.select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.dateField
      }

      "contains what happens next heading" in {
        mainContent.selectHead("h2").text() mustBe SignUpConfirmationMessages.whatToDoHeading
      }

      "contains a first paragraph with a link" in {
        val mustUsePara = mainContent.selectNth("p", 5)
        val expectedText =
          s"${SignUpConfirmationMessages.mustUsePara} ${SignUpConfirmationMessages.mustUseParaLink} ${SignUpConfirmationMessages.mustUseParaEnd}"
        mustUsePara.text() mustBe expectedText
        val link = mustUsePara.select("a")
        link.text() mustBe SignUpConfirmationMessages.mustUseParaLink
        link.attr("href") mustBe SignUpConfirmationMessages.mustUseParaLinkHref
      }

      "contains the quarterly updates section correctly" must {
        def quarterlyUpdatesSection: Element = mainContent.selectNth("div", 2)

        "have the correct heading" in {
          quarterlyUpdatesSection.select("h3").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesHeading
        }

        "have the correct intro paragraph" in {
          quarterlyUpdatesSection.selectNth("p", 6).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesPara1
        }

        "have the correct table" which {
          "has the correct headers" in {
            val tableHeaders = quarterlyUpdatesSection.select("th")
            tableHeaders.get(0).text() mustBe SignUpConfirmationMessages.updateDeadline
            tableHeaders.get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod
            tableHeaders.get(2).text() mustBe SignUpConfirmationMessages.standardPeriod
          }

          "has the correct first row" in {
            val tableRows = quarterlyUpdatesSection.select("tbody tr")
            tableRows.get(0).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline1
            tableRows.get(0).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod1
            tableRows.get(0).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod1
          }

          "has the correct second row" in {
            val tableRows = quarterlyUpdatesSection.select("tbody tr")
            tableRows.get(1).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline2
            tableRows.get(1).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod2
            tableRows.get(1).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod2
          }

          "has the correct third row" in {
            val tableRows = quarterlyUpdatesSection.select("tbody tr")
            tableRows.get(2).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline3
            tableRows.get(2).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod3
            tableRows.get(2).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod3
          }

          "has the correct fourth row" in {
            val tableRows = quarterlyUpdatesSection.select("tbody tr")
            tableRows.get(3).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline4
            tableRows.get(3).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod4
            tableRows.get(3).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod4
          }
        }

        "have the correct read more paragraph" in {
          val readMorePara = quarterlyUpdatesSection.selectNth("p", 7)
          readMorePara.text() must include(SignUpConfirmationMessages.quarterlyUpdatesPara2)
          readMorePara.select("a").attr("href") mustBe SignUpConfirmationMessages.quarterlyUpdatesPara2Link
        }
      }

      "contains mtd heading" in {
        mainContent.selectNth("h2", 2).text() mustBe SignUpConfirmationMessages.usingMtdHeading
      }

      "contains a mtd paragraph with a link" in {

        val usingMtdPara = mainContent.selectNth("p", 8)
        val expectedText =
          s"${SignUpConfirmationMessages.usingMtdPara} ${SignUpConfirmationMessages.usingMtdLink} ${SignUpConfirmationMessages.usingMtdParaEnd}"
        usingMtdPara.text() mustBe expectedText
        val link = usingMtdPara.select("a")
        link.text() mustBe SignUpConfirmationMessages.usingMtdLink
        link.attr("href") mustBe SignUpConfirmationMessages.usingMtdLinkHref
      }

      "contains a bullet list for mtd" which {
        def bulletList: Element = mainContent.selectNth("ul", 1)

        "has a first item" in {
          bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.usingMtdBullet1
        }
        "has a second item" in {
          bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.usingMtdBullet2
        }
        "has a third item" in {
          bulletList.selectNth("li", 3).text mustBe SignUpConfirmationMessages.usingMtdBullet3
        }
        "has a fourth item" in {
          bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.usingMtdBullet4
        }
      }

      "have a button to sign up another client" in {
        mainContent.selectHead(".govuk-button").text() mustBe SignUpConfirmationMessages.signUpAnotherClient
        mainContent.selectHead(".govuk-button").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
      }

      "contains survey link" which {
        "has a link for survey" in {
          mainContent.selectNth(".govuk-link", 5).text mustBe SignUpConfirmationMessages.surveyText
          mainContent.selectNth(".govuk-link", 5).attr("href") mustBe SignUpConfirmationMessages.surveyLink
          mainContent.select("p.govuk-body").last().text mustBe SignUpConfirmationMessages.surveyText + SignUpConfirmationMessages.surveyTextEnd
        }
      }
    }
  }

  private object SignUpConfirmationMessages {
    val whatToDoHeading = "What happens next"
    val panelHeading = "Sign up complete"
    val panelUserDetails = s"$testName | $testNino"
    private val panelDescriptionThis =
      s"Your client is signed up for Making Tax Digital for Income Tax from ${startDate.day} April 2010 to ${endDate.day} April 2011 onwards"
    private val panelDescriptionNext =
      s"Your client is signed up for Making Tax Digital for Income Tax from ${startDate.day} April 2010 to ${endDate.day} April 2011 onwards"

    def panelDescription(yearIsNext: Boolean): String = if (yearIsNext)
      SignUpConfirmationMessages.panelDescriptionNext
    else
      SignUpConfirmationMessages.panelDescriptionThis

    val printLink = "Print or save this page."

    val dateField: String = {
      val date = implicitDateFormatter.LongDate(LocalDate.now()).toLongDate
      s"Date: $date"
    }

    val nextYear = AccountingPeriodUtil.getNextTaxEndYear - 1

    val mustUsePara = "You must use"
    val mustUseParaEnd = "to submit your client’s Self Assessment tax return."
    val mustUseParaLink = "software that works with Making Tax Digital for Income Tax (opens in new tab)"
    val mustUseParaLinkHref = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"

    val quarterlyUpdatesHeading = "Sending your quarterly updates"
    val quarterlyUpdatesPara1 = "You need to send your quarterly updates for each of your sole trader and property income sources by:"
    val quarterlyUpdatesPara2 = "You can read more about quarterly updates"
    val quarterlyUpdatesPara2Link = "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax/send-quarterly-updates"

    val updateDeadline = "Update deadline"
    val deadline1 = "7 August"
    val deadline2 = "7 November"
    val deadline3 = "7 February"
    val deadline4 = "7 May"

    val calendarPeriod = "Calendar period"
    val calendarPeriod1 = "1 April to 30 June"
    val calendarPeriod2 = "1 April to 30 September"
    val calendarPeriod3 = "1 April to 31 December"
    val calendarPeriod4 = "1 April to 31 March"

    val standardPeriod = "Standard period"
    val standardPeriod1 = "6 April to 5 July"
    val standardPeriod2 = "6 April to 5 October"
    val standardPeriod3 = "6 April to 5 January"
    val standardPeriod4 = "6 April to 5 April"

    val usingMtdHeading = "Using Making Tax Digital for Income Tax"
    val usingMtdPara = "Read"
    val usingMtdParaEnd = "to find out more information about:"
    val usingMtdLink = "use Making Tax Digital for Income Tax (opens in new tab)"
    val usingMtdLinkHref = "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax"
    val usingMtdBullet1 = "how to submit your client’s Self Assessment tax return."
    val usingMtdBullet2 = "what to expect after you sign up your client"
    val usingMtdBullet3 = "the different steps you will need to take during the tax year"
    val usingMtdBullet4 = "help and support"

    val surveyText = "What did you think of this service (opens in new tab)"
    val surveyTextEnd = " (takes 30 seconds)"
    val surveyLink = appConfig.feedbackFrontendRedirectUrl

    val signUpAnotherClient = "Sign up another client"
  }
}
