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

import config.featureswitch.FeatureSwitch.EmailCaptureConsent
import models.DateModel
import models.common.AccountingPeriodModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.{AccountingPeriodUtil, ImplicitDateFormatter, ImplicitDateFormatterImpl, ViewSpec}
import views.html.agent.confirmation.SignUpConfirmation

import java.time.LocalDate
import utilities.ImplicitDateFormatterImpl

class SignUpConfirmationViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  val testName = "Lisa Khan"
  val testNino = "QQ123456L"
  private val startDate: DateModel = DateModel(getRandomDate, "4", "2010")

  private def getRandomDate = (Math.random() * 10 + 1).toInt.toString

  private val endDate: DateModel = DateModel(getRandomDate, "4", "2011")
  val testAccountingPeriodModel: AccountingPeriodModel = AccountingPeriodModel(startDate, endDate)

  def page(mandatedCurrentYear: Boolean, mandatedNextYear: Boolean, selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String], usingSoftwareStatus: Boolean, signedUpDate: LocalDate): Html =
    signUpConfirmation(mandatedCurrentYear, mandatedNextYear, selectedTaxYearIsNext, userNameMaybe, testNino, testAccountingPeriodModel, usingSoftwareStatus, signedUpDate)

  def document(eligibleNextYearOnly: Boolean, mandatedCurrentYear: Boolean, mandatedNextYear: Boolean, selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String] = Some(testName), usingSoftwareStatus: Boolean, signedUpDate: LocalDate): Document =
    Jsoup.parse(page(mandatedCurrentYear, mandatedNextYear, selectedTaxYearIsNext, userNameMaybe, usingSoftwareStatus, signedUpDate).body)

  "The sign up confirmation view" when {
    "the user has software and for current year" should {
      def mainContent: Element = document(eligibleNextYearOnly = false, mandatedCurrentYear = false, mandatedNextYear = false, selectedTaxYearIsNext = false, usingSoftwareStatus = true).mainContent

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

      "contains what you must do now heading" in {
        mainContent.selectHead("h2").text() mustBe SignUpConfirmationMessages.whatToDoHeading
      }

      "contains a first paragraph and with a link" in {
        mainContent.select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.paraOne
        val link = mainContent.selectNth(".govuk-link", 2)
        link.text mustBe SignUpConfirmationMessages.linkTextOne
        link.attr("href") mustBe "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax"
      }

      "contains a second paragraph" in {
        mainContent.select(".govuk-body").select("p").get(2).text() mustBe SignUpConfirmationMessages.whatYouMustDoYesAndCurrentYear
      }

      "contains a third paragraph" in {
        mainContent.select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.paraTwo
      }

      "contains a bullet list of what software will tell you to do" which {
        def bulletList = mainContent.selectNth("ul", 1)

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
          bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.bullet4CurrentYear
        }
        "has a fifth item" in {
          bulletList.selectNth("li", 5).text mustBe SignUpConfirmationMessages.bullet5
        }
      }

      "contains a fourth paragraph" in {
        mainContent.select(".govuk-body").select("p").get(4).text() mustBe SignUpConfirmationMessages.paraThree
      }

      "have a manage your clients' account section" which {

        "contains a heading" in {
          mainContent.selectNth("h2", 2).text mustBe SignUpConfirmationMessages.manageAccountsHeading
        }

        "contains paragraph with a link" in {
          mainContent.select(".govuk-body").select("p").get(5).text() mustBe SignUpConfirmationMessages.manageAccountsPara
        }
      }

      "contains a CST contact section" which {
        "has a heading" in {
          enable(EmailCaptureConsent)
          mainContent.selectNth("h2", 3).text mustBe SignUpConfirmationMessages.cstContactHeading
        }
        "has the contact details" in {
          enable(EmailCaptureConsent)
          mainContent.selectNth("p.govuk-body", 7).text mustBe SignUpConfirmationMessages.cstContactPara
        }
        "has a link for call charges" in {
          enable(EmailCaptureConsent)
          mainContent.selectNth(".govuk-link", 4).text mustBe SignUpConfirmationMessages.cstContactLinkText
          mainContent.selectNth(".govuk-link", 4).attr("href") mustBe SignUpConfirmationMessages.cstContactLinkHref
        }
      }

      "have a button to sign up another client" in {
        mainContent.selectHead(".govuk-button").text() mustBe SignUpConfirmationMessages.signUpAnotherClient
        mainContent.selectHead(".govuk-button").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
      }
    }

    "the user has software and is for next year" should {

      def mainContent: Element = document(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = false, selectedTaxYearIsNext = true, usingSoftwareStatus = true).mainContent

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

      "contains what you must do now heading" in {
        mainContent.selectHead("h2").text() mustBe SignUpConfirmationMessages.whatToDoHeading
      }

      "contains a first paragraph and with a link" in {
        mainContent.select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.paraOne
        val link = mainContent.selectNth(".govuk-link", 2)
        link.text mustBe SignUpConfirmationMessages.linkTextOne
        link.attr("href") mustBe "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax"
      }

      "contains a second paragraph" in {
        mainContent.select(".govuk-body").select("p").get(2).text() mustBe SignUpConfirmationMessages.whatYouMustDoYesAndNextYear
      }

      "contains a third paragraph" in {
        mainContent.select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.paraTwo
      }

      "contains a bullet list of what software will tell you to do" which {
        def bulletList = mainContent.selectNth("ul", 1)

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
        mainContent.select(".govuk-body").select("p").get(4).text() mustBe SignUpConfirmationMessages.paraThree
      }

      "have a manage your clients' account section" which {

        "contains a heading" in {
          mainContent.selectNth("h2", 2).text mustBe SignUpConfirmationMessages.manageAccountsHeading
        }

        "contains paragraph with a link" in {
          mainContent.select(".govuk-body").select("p").get(5).text() mustBe SignUpConfirmationMessages.manageAccountsPara
        }
      }

      "does not contain a CST contact section" in {
        enable(EmailCaptureConsent)
        mainContent.selectOptionalNth("h2", 3) mustBe None
        mainContent.selectOptionalNth("p.govuk-body", 7) mustBe None
      }

      "have a button to sign up another client" in {
        mainContent.selectHead(".govuk-button").text() mustBe SignUpConfirmationMessages.signUpAnotherClient
        mainContent.selectHead(".govuk-button").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
      }
    }

    "the user has no software and for this year" should {
      def mainContent: Element = document(eligibleNextYearOnly = false, mandatedCurrentYear = false, mandatedNextYear = false, selectedTaxYearIsNext = false, usingSoftwareStatus = false).mainContent

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

      "contains what you must do now heading" in {
        mainContent.selectHead("h2").text() mustBe SignUpConfirmationMessages.whatToDoHeading
      }

      "contains a first paragraph and with a link" in {
        mainContent.select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.paraOne
        val link = mainContent.selectNth(".govuk-link", 2)
        link.text mustBe SignUpConfirmationMessages.linkTextOne
        link.attr("href") mustBe "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax"
      }

      "contains a second paragraph with a link" in {
        mainContent.select(".govuk-body").select("p").get(2).text() mustBe SignUpConfirmationMessages.whatYouMustDoNoAndCurrentYear
        val link = mainContent.selectNth(".govuk-link", 3)
        link.text mustBe SignUpConfirmationMessages.linkTextNoAndCurrentYear
        link.attr("href") mustBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "contains a third paragraph" in {
        mainContent.select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.paraTwo
      }

      "contains a bullet list of what software will tell you to do" which {
        def bulletList = mainContent.selectNth("ul", 1)

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
          bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.bullet4CurrentYear
        }
        "has a fifth item" in {
          bulletList.selectNth("li", 5).text mustBe SignUpConfirmationMessages.bullet5
        }
      }

      "contains a fourth paragraph" in {
        mainContent.select(".govuk-body").select("p").get(4).text() mustBe SignUpConfirmationMessages.paraThree
      }

      "have a manage your clients' account section" which {

        "contains a heading" in {
          mainContent.selectNth("h2", 2).text mustBe SignUpConfirmationMessages.manageAccountsHeading
        }

        "contains paragraph with a link" in {
          mainContent.select(".govuk-body").select("p").get(5).text() mustBe SignUpConfirmationMessages.manageAccountsPara
        }
      }

      "have a button to sign up another client" in {
        mainContent.selectHead(".govuk-button").text() mustBe SignUpConfirmationMessages.signUpAnotherClient
        mainContent.selectHead(".govuk-button").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
      }
    }

    "the user has no software and for next year only" should {
      def mainContent: Element = document(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = false, selectedTaxYearIsNext = true, usingSoftwareStatus = false).mainContent

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

      "contains what you must do now heading" in {
        mainContent.selectHead("h2").text() mustBe SignUpConfirmationMessages.whatToDoHeading
      }

      "contains a first paragraph and with a link" in {
        mainContent.select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.paraOne
        val link = mainContent.selectNth(".govuk-link", 2)
        link.text mustBe SignUpConfirmationMessages.linkTextOne
        link.attr("href") mustBe "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax"
      }

      "contains a second paragraph with a link" in {
        mainContent.select(".govuk-body").select("p").get(2).text() mustBe SignUpConfirmationMessages.whatYouMustDoNoAndNextYear
        val link = mainContent.selectNth(".govuk-link", 3)
        link.text mustBe SignUpConfirmationMessages.linkTextNoAndNextYear
        link.attr("href") mustBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "contains a third paragraph" in {
        mainContent.select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.paraTwo
      }

      "contains a bullet list of what software will tell you to do" which {
        def bulletList = mainContent.selectNth("ul", 1)

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
        mainContent.select(".govuk-body").select("p").get(4).text() mustBe SignUpConfirmationMessages.paraThree
      }

      "have a manage your clients' account section" which {

        "contains a heading" in {
          mainContent.selectNth("h2", 2).text mustBe SignUpConfirmationMessages.manageAccountsHeading
        }

        "contains paragraph with a link" in {
          mainContent.select(".govuk-body").select("p").get(5).text() mustBe SignUpConfirmationMessages.manageAccountsPara
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
    val panelHeading = "Sign up complete"
    val panelUserDetails = s"$testName | $testNino"
    val panelDescriptionThis = s"Your client is signed up for Making Tax Digital for Income Tax from ${startDate.day} April 2010 to ${endDate.day} April 2011 onwards"
    val panelDescriptionNext = s"Your client is signed up for Making Tax Digital for Income Tax from ${startDate.day} April 2010 to ${endDate.day} April 2011 onwards"

    def panelDescription(yearIsNext: Boolean): String = if (yearIsNext)
      SignUpConfirmationMessages.panelDescriptionNext
    else
      SignUpConfirmationMessages.panelDescriptionThis

    val printLink = "Print or save this page."

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
    val bullet1 = "how to authorise and connect the software to your agent services account"
    val bullet2 = "how to keep digital records"
    val bullet3 = "when and how to send quarterly updates"
    val bullet4CurrentYear = "if you need to send any missed or backdated updates for the current tax year - and how to send them"
    val bullet5 = "when and how to make your client’s tax return after the end of the tax year"
    val paraThree = "And your client will need to pay the tax they owe."

    val manageAccountsHeading = "Manage your clients’ accounts"
    val manageAccountsPara = "Go to your agent services account (opens in new tab)"

    val cstContactHeading = "Get help with Making Tax Digital for Income Tax"
    val cstContactPara = "Telephone: 03003 229 619 Monday to Friday, 8am to 6pm (except public holidays)"
    val cstContactLinkText = "Find out about call charges (opens in new tab)"
    val cstContactLinkHref = "https://www.gov.uk/call-charges"

    val signUpAnotherClient = "Sign up another client"
  }
}
