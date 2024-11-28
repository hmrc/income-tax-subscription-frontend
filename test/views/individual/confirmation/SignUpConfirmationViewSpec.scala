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

package views.individual.confirmation

import models.DateModel
import models.common.AccountingPeriodModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.{AccountingPeriodUtil, ImplicitDateFormatter, ImplicitDateFormatterImpl, ViewSpec}
import views.html.individual.confirmation.SignUpConfirmation

//scalastyle:off
class SignUpConfirmationViewSpec extends ViewSpec {

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  val testName = "Lisa Khan"
  val testNino = "QQ123456L"
  private val startDate: DateModel = DateModel(getRandomDate, "4", "2010")

  private def getRandomDate = (Math.random() * 10 + 1).toInt.toString

  private val endDate: DateModel = DateModel(getRandomDate, "4", "2011")
  val testAccountingPeriodModel: AccountingPeriodModel = AccountingPeriodModel(startDate, endDate)

  def page(mandatedCurrentYear: Boolean, selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String], preference: Option[Boolean], usingSoftwareStatus: Boolean): Html =
    signUpConfirmation(mandatedCurrentYear, selectedTaxYearIsNext, userNameMaybe, testNino, preference, usingSoftwareStatus)

  def document(mandatedCurrentYear: Boolean,
               selectedTaxYearIsNext: Boolean,
               userNameMaybe: Option[String] = Some(testName),
               preference: Option[Boolean] = None,
               usingSoftwareStatus: Boolean): Document = {
    Jsoup.parse(page(mandatedCurrentYear, selectedTaxYearIsNext, userNameMaybe, preference, usingSoftwareStatus).body)
  }

  "The sign up confirmation view" when {
    "the user has software and eligible for current year" should {
      def mainContent(preference: Option[Boolean] = None): Element = document(mandatedCurrentYear = false, selectedTaxYearIsNext = false, preference = preference, usingSoftwareStatus = true).mainContent

      "has a header panel" which {
        "contains the panel heading" in {
          mainContent().select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }
        "contains the user name and nino" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(0)
            .text() mustBe SignUpConfirmationMessages.panelUserDetails
        }

        "contains the description" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(1)
            .text() mustBe SignUpConfirmationMessages.panelDescription(false)
        }
      }

      "have a print link" in {
        val link = mainContent().selectNth(".govuk-link", 1)
        link.text mustBe SignUpConfirmationMessages.printLink
        link.attr("data-module") mustBe "hmrc-print-link"
        link.attr("href") mustBe "#"
      }

      "contains what you must do heading" in {
        mainContent().selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "contains a first paragraph and with a link" in {
        mainContent().select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.paraOne
        val link = mainContent().selectNth(".govuk-link", 2)
        link.text mustBe SignUpConfirmationMessages.linkTextOne
        link.attr("href") mustBe "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax"
      }

      "contains a second paragraph" in {
        mainContent().select(".govuk-body").select("p").get(2).text() mustBe SignUpConfirmationMessages.whatYouMustDoYesAndCurrentYear
      }

      "contains a third paragraph" in {
        mainContent().select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.paraTwo
      }

      "contains a bullet list of what software will tell you to do" which {
        def bulletList = mainContent().selectNth("ul", 1)

        "has a first item" in {
          bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.bullet1
        }
        "has a second item" in {
          bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.bullet2
        }
        "has a third item" in {
          bulletList.selectNth("li", 3).text mustBe SignUpConfirmationMessages.bullet3
        }
        "has a fourth item" in {
          bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.bullet5
        }
      }

      "contains a fourth paragraph" in {
        mainContent().select(".govuk-body").select("p").get(4).text() mustBe SignUpConfirmationMessages.paraThree
      }

      "contains a report previous tax year section" which {

        "contains a heading" in {
          mainContent().selectNth("h2", 2).text() mustBe SignUpConfirmationMessages.previousTaxYearHeading
        }

        "has a paragraph" in {
          mainContent().select(".govuk-body").select("p").get(5).text() mustBe SignUpConfirmationMessages.paragraphThisYear
        }
      }

      "contains a preference section" which {

        def preferenceSection(preference: Option[Boolean] = None): Element = mainContent(preference).selectNth("div", 6)

        "has no retrieved preference content when no preference was provided to the view" in {
          preferenceSection().selectOptionalNth("p", 1) mustBe None
        }

        "has an online preference when their opt in preference was true" in {
          preferenceSection(preference = Some(true)).selectNth("h2", 1).text mustBe SignUpConfirmationMessages.onlinePreferenceHeading
          preferenceSection(preference = Some(true)).selectNth("p", 1).text mustBe SignUpConfirmationMessages.onlinePreferenceParaOne
        }
      }
    }

    "the user has software and for next year only" should {
      def mainContent(preference: Option[Boolean] = None): Element = document(mandatedCurrentYear = false, selectedTaxYearIsNext = true, preference = preference, usingSoftwareStatus = true).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          mainContent().select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }

        "contains the user name and nino" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(0)
            .text() mustBe SignUpConfirmationMessages.panelUserDetails
        }

        "contains the description" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(1)
            .text() mustBe SignUpConfirmationMessages.panelDescription(true)
        }
      }

      "have a print link" in {
        val link = mainContent().selectNth(".govuk-link", 1)
        link.text mustBe SignUpConfirmationMessages.printLink
        link.attr("data-module") mustBe "hmrc-print-link"
        link.attr("href") mustBe "#"
      }

      "contains what you must do heading" in {
        mainContent().selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "contains a first paragraph and with a link" in {
        mainContent().select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.paraOne
        val link = mainContent().selectNth(".govuk-link", 2)
        link.text mustBe SignUpConfirmationMessages.linkTextOne
        link.attr("href") mustBe "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax"
      }

      "contains a second paragraph" in {
        mainContent().select(".govuk-body").select("p").get(2).text() mustBe SignUpConfirmationMessages.whatYouMustDoYesAndNextYear
      }

      "contains a third paragraph" in {
        mainContent().select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.paraTwo
      }

      "contains a bullet list of what software will tell you to do" which {
        def bulletList = mainContent().selectNth("ul", 1)

        "has a first item" in {
          bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.bullet1
        }
        "has a second item" in {
          bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.bullet2
        }
        "has a third item" in {
          bulletList.selectNth("li", 3).text mustBe SignUpConfirmationMessages.bullet3
        }
        "has a fourth item" in {
          bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.bullet5
        }
      }

      "contains a fourth paragraph" in {
        mainContent().select(".govuk-body").select("p").get(4).text() mustBe SignUpConfirmationMessages.paraThree
      }

      "contains a report previous tax year section" which {

        "has a heading" in {
          mainContent().selectNth("h2", 2).text() mustBe SignUpConfirmationMessages.previousNextTaxYearHeading
        }

        "has a paragraph" in {
          mainContent().select(".govuk-body").select("p").get(5).text() mustBe SignUpConfirmationMessages.paragraphNextYear
        }
      }

      "contains a preference section" which {

        def preferenceSection(preference: Option[Boolean] = None): Element = mainContent(preference).selectNth("div", 6)

        "has no retrieved preference content when no preference was provided to the view" in {
          preferenceSection().selectOptionalNth("p", 1) mustBe None
        }

        "has an online preference when their opt in preference was true" in {
          preferenceSection(preference = Some(true)).selectNth("h2", 1).text mustBe SignUpConfirmationMessages.onlinePreferenceHeading
          preferenceSection(preference = Some(true)).selectNth("p", 1).text mustBe SignUpConfirmationMessages.onlinePreferenceParaOne
        }
      }
    }

    "the user has no software and for this year" should {
      def mainContent(preference: Option[Boolean] = None): Element = document(mandatedCurrentYear = false, selectedTaxYearIsNext = false, preference = preference, usingSoftwareStatus = false).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          mainContent().select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }

        "contains the user name and nino" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(0)
            .text() mustBe SignUpConfirmationMessages.panelUserDetails
        }

        "contains the description" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(1)
            .text() mustBe SignUpConfirmationMessages.panelDescription(false)
        }
      }

      "have a print link" in {
        val link = mainContent().selectNth(".govuk-link", 1)
        link.text mustBe SignUpConfirmationMessages.printLink
        link.attr("data-module") mustBe "hmrc-print-link"
        link.attr("href") mustBe "#"
      }

      "contains what you must do heading" in {
        mainContent().selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "contains a first paragraph and with a link" in {
        mainContent().select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.paraOne
        val link = mainContent().selectNth(".govuk-link", 2)
        link.text mustBe SignUpConfirmationMessages.linkTextOne
        link.attr("href") mustBe "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax"
      }

      "contains a second paragraph and contains link" in {
        mainContent().select(".govuk-body").select("p").get(2).text() mustBe SignUpConfirmationMessages.whatYouMustDoNoAndCurrentYear
        val link = mainContent().selectNth(".govuk-link", 3)
        link.text mustBe SignUpConfirmationMessages.linkTextNoAndCurrentYear
        link.attr("href") mustBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "contains a third paragraph" in {
        mainContent().select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.paraTwo
      }

      "contains a bullet list of what software will tell you to do" which {
        def bulletList = mainContent().selectNth("ul", 1)

        "has a first item" in {
          bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.bullet1
        }
        "has a second item" in {
          bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.bullet2
        }
        "has a third item" in {
          bulletList.selectNth("li", 3).text mustBe SignUpConfirmationMessages.bullet3
        }
        "has a fourth item" in {
          bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.bullet4NoThisYear
        }
      }

      "contains a fourth paragraph" in {
        mainContent().select(".govuk-body").select("p").get(4).text() mustBe SignUpConfirmationMessages.paraThree
      }

      "contains a report previous tax year section" which {

        "has a heading" in {
          mainContent().selectNth("h2", 2).text() mustBe SignUpConfirmationMessages.previousTaxYearHeading
        }

        "has a paragraph" in {
          mainContent().select(".govuk-body").select("p").get(5).text() mustBe SignUpConfirmationMessages.paragraphThisYear
        }
      }

      "contains a preference section" which {

        def preferenceSection(preference: Option[Boolean] = None): Element = mainContent(preference).selectNth("div", 6)

        "has no retrieved preference content when no preference was provided to the view" in {
          preferenceSection().selectOptionalNth("p", 1) mustBe None
        }

        "has an online preference when their opt in preference was true" in {
          preferenceSection(preference = Some(true)).selectNth("h2", 1).text mustBe SignUpConfirmationMessages.onlinePreferenceHeading
          preferenceSection(preference = Some(true)).selectNth("p", 1).text mustBe SignUpConfirmationMessages.onlinePreferenceParaOne
        }
      }
    }

    "the user has no software and for next year only" should {
      def mainContent(preference: Option[Boolean] = None): Element = document(mandatedCurrentYear = false, selectedTaxYearIsNext = true, preference = preference, usingSoftwareStatus = false).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          mainContent().select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }

        "contains the user name and nino" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(0)
            .text() mustBe SignUpConfirmationMessages.panelUserDetails
        }

        "contains the description" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(1)
            .text() mustBe SignUpConfirmationMessages.panelDescription(true)
        }
      }

      "have a print link" in {
        val link = mainContent().selectNth(".govuk-link", 1)
        link.text mustBe SignUpConfirmationMessages.printLink
        link.attr("data-module") mustBe "hmrc-print-link"
        link.attr("href") mustBe "#"
      }

      "contains what you must do heading" in {
        mainContent().selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "contains a first paragraph and with a link" in {
        mainContent().select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.paraOne
        val link = mainContent().selectNth(".govuk-link", 2)
        link.text mustBe SignUpConfirmationMessages.linkTextOne
        link.attr("href") mustBe "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax"
      }

      "contains a second paragraph and contains link" in {
        mainContent().select(".govuk-body").select("p").get(2).text() mustBe SignUpConfirmationMessages.whatYouMustDoNoAndNextYear
        val link = mainContent().selectNth(".govuk-link", 3)
        link.text mustBe SignUpConfirmationMessages.linkTextNoAndNextYear
        link.attr("href") mustBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "contains a third paragraph" in {
        mainContent().select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.paraTwo
      }

      "contains a bullet list of what software will tell you to do" which {
        def bulletList = mainContent().selectNth("ul", 1)

        "has a first item" in {
          bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.bullet1
        }
        "has a second item" in {
          bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.bullet2
        }
        "has a third item" in {
          bulletList.selectNth("li", 3).text mustBe SignUpConfirmationMessages.bullet3
        }
        "has a fourth item" in {
          bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.bullet5
        }
      }

      "contains a fourth paragraph" in {
        mainContent().select(".govuk-body").select("p").get(4).text() mustBe SignUpConfirmationMessages.paraThree
      }

      "contains a report previous tax year section" which {

        "has a heading" in {
          mainContent().selectNth("h2", 2).text() mustBe SignUpConfirmationMessages.previousNextTaxYearHeading
        }

        "has a paragraph" in {
          mainContent().select(".govuk-body").select("p").get(5).text() mustBe SignUpConfirmationMessages.paragraphNextYear
        }
      }

      "contains a preference section" which {

        def preferenceSection(preference: Option[Boolean] = None): Element = mainContent(preference).selectNth("div", 6)

        "has no retrieved preference content when no preference was provided to the view" in {
          preferenceSection().selectOptionalNth("p", 1) mustBe None
        }

        "has an online preference when their opt in preference was true" in {
          preferenceSection(preference = Some(true)).selectNth("h2", 1).text mustBe SignUpConfirmationMessages.onlinePreferenceHeading
          preferenceSection(preference = Some(true)).selectNth("p", 1).text mustBe SignUpConfirmationMessages.onlinePreferenceParaOne
        }
      }
    }
  }

  private object SignUpConfirmationMessages {
    val whatYouMustDoHeading = "What you must do now"
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

    val printLink = "Print this page"

    val thisYear = AccountingPeriodUtil.getCurrentTaxEndYear - 1
    val nextYear = AccountingPeriodUtil.getNextTaxEndYear - 1

    val paraOne = s"Read how to use Making Tax Digital for Income Tax (opens in new tab)"
    val linkTextOne = "use Making Tax Digital for Income Tax (opens in new tab)"

    val whatYouMustDoYesAndCurrentYear = "You must use your software that works with Making Tax Digital for Income Tax."
    val whatYouMustDoNoAndCurrentYear = s"You must find and use software that works with Making Tax Digital for Income Tax (opens in new tab)"
    val linkTextNoAndCurrentYear = "software that works with Making Tax Digital for Income Tax (opens in new tab)"

    val whatYouMustDoYesAndNextYear = s"From 6 April $nextYear, you must use your software that works with Making Tax Digital for Income Tax."
    val whatYouMustDoNoAndNextYear = s"From 6 April $nextYear, you must find and use software that works with Making Tax Digital for Income Tax (opens in new tab)"
    val linkTextNoAndNextYear = "software that works with Making Tax Digital for Income Tax (opens in new tab)"

    val paraTwo = "Your chosen software will tell you what else you need to do, including:"
    val bullet1 = "how to authorise and connect the software to the Government Gateway user ID you use for your Self Assessment"
    val bullet2 = "how to keep digital records"
    val bullet3 = "when and how to send quarterly updates"
    val bullet4NoThisYear = "if you need to send any missed or backdated updates for the current tax year - and how to send them"
    val bullet5 = "when and how to make your final declaration after the end of the tax year"
    val paraThree = "And you will need to pay the tax you owe."

    val previousTaxYearHeading = "Report previous tax year"
    val previousNextTaxYearHeading = "Report current and previous tax years"
    val paragraphThisYear = s"You must submit your Self Assessment tax returns for the years ended 5 April $thisYear using your HMRC online services account as normal."
    val paragraphNextYear = s"You must submit your Self Assessment tax returns for the years ended 5 April $nextYear using your HMRC online services account as normal."

    val onlinePreferenceHeading = "Your communication preferences"
    val onlinePreferenceParaOne = "If you’ve chosen to get your tax letters online, make sure you have verified your email address."
  }

}