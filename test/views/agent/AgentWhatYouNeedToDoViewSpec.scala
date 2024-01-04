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

package views.agent

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.WhatYouNeedToDo

import java.time.format.DateTimeFormatter
import scala.util.Random

class AgentWhatYouNeedToDoViewSpec extends ViewSpec {

  private val nameLengthCharacters = 10
  private val clientName = Random.alphanumeric.take(nameLengthCharacters).mkString
  private val clientNino = Random.alphanumeric.take(nameLengthCharacters).mkString

  val whatYouNeedToDo: WhatYouNeedToDo = app.injector.instanceOf[WhatYouNeedToDo]

  "WhatYouNeedToDo" when {
    "the user is mandated for the current year" should {
      def mainContent: Element = document(
        eligibleNextYearOnly = false,
        mandatedCurrentYear = true,
        mandatedNextYear = false,
        clientName,
        clientNino
      ).mainContent

      "use the correct template details" in new TemplateViewTest(
        view = page(eligibleNextYearOnly = false, mandatedCurrentYear = true, mandatedNextYear = false, clientName, clientNino),
        title = WhatYouNeedToDoMessages.heading,
        isAgent = true,
        backLink = None,
        hasSignOutLink = true
      )

      "have a client Name and Nino" in {
        mainContent.selectHead(".govuk-caption-l").text contains clientName
        mainContent.selectHead(".govuk-caption-l").text contains clientNino
      }

      "have a first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.MandatedCurrentYear.paraOne
      }

      "have a second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.MandatedCurrentYear.paraTwo
      }

      "have a numbered list" which {
        def numberedList: Element = mainContent.selectHead("ol.govuk-list--number")

        "has a first point" in {
          numberedList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.MandatedCurrentYear.NotificationBanner.bulletOne
        }

        "has a second point" in {
          numberedList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.MandatedCurrentYear.NotificationBanner.bulletTwo
        }

        "has a third point" in {
          numberedList.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.MandatedCurrentYear.NotificationBanner.bulletThree
        }

      }

      "have a warning text" in {
        mainContent.selectHead(".govuk-warning-text__text").text mustBe WhatYouNeedToDoMessages.MandatedCurrentYear.WarningText.para
      }

      "have a form" which {
        def form: Element = mainContent.selectHead("form")

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has an accept and continue button to submit the form" in {
          form.selectHead("button").text mustBe WhatYouNeedToDoMessages.acceptAndContinue
        }
      }
    }
    "the user is mandated for next tax year and only eligible for the next tax year" should {
      def mainContent: Element = document(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = true, clientNino, clientName).mainContent

      "use the correct template details" in new TemplateViewTest(
        view = page(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = true, clientName, clientNino),
        title = WhatYouNeedToDoMessages.heading,
        isAgent = true,
        backLink = None,
        hasSignOutLink = true
      )

      // rest of the tests here for mandated next year and eligible next year content

      "have a client Name and Nino" in {
        mainContent.selectHead(".govuk-caption-l").text contains clientName
        mainContent.selectHead(".govuk-caption-l").text contains clientNino
      }

      "have a first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.MandatedAndEligibleForNextYearOnly.paraOne
      }

      "have a second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.MandatedAndEligibleForNextYearOnly.paraTwo
      }

      "have a numbered list" which {
        def numberedList: Element = mainContent.selectHead("ol.govuk-list--number")

        "has a first point" in {
          numberedList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.MandatedAndEligibleForNextYearOnly.NotificationBanner.bulletOne
        }

        "has a second point" in {
          numberedList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.MandatedAndEligibleForNextYearOnly.NotificationBanner.bulletTwo
        }

        "has a third point" in {
          numberedList.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.MandatedAndEligibleForNextYearOnly.NotificationBanner.bulletThree
        }
      }

      "have a form" which {
        def form: Element = mainContent.selectHead("form")

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has an accept and continue button to submit the form" in {
          form.selectHead("button").text mustBe WhatYouNeedToDoMessages.acceptAndContinue
        }
      }
    }
    "the user is voluntary for both years but only eligible for next year" should {
      def mainContent: Element = document(
        eligibleNextYearOnly = true,
        mandatedCurrentYear = false,
        mandatedNextYear = false,
        clientName,
        clientNino
      ).mainContent

      "use the correct template details" in new TemplateViewTest(
        view = page(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = false, clientName, clientNino),
        title = WhatYouNeedToDoMessages.heading,
        isAgent = true,
        backLink = None,
        hasSignOutLink = true
      )

      "have a client Name and Nino" in {
        mainContent.selectHead(".govuk-caption-l").text contains clientName
        mainContent.selectHead(".govuk-caption-l").text contains clientNino
      }

      "have a first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.paraOne
      }

      "have a second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.paraTwo
      }

      "have a numbered list" which {
        def numberedList: Element = mainContent.selectHead("ol.govuk-list--number")

        "has a first point" in {
          numberedList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.NotificationBanner.bulletOne
        }

        "has a second point" in {
          numberedList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.NotificationBanner.bulletTwo
        }

        "has a third point" in {
          numberedList.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.NotificationBanner.bulletThree
        }
      }

      "have an inset text" in {
        mainContent.selectHead(".govuk-inset-text").text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.InsetText.para
      }

      "have a form" which {
        def form: Element = mainContent.selectHead("form")

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has an accept and continue button to submit the form" in {
          form.selectHead("button").text mustBe WhatYouNeedToDoMessages.acceptAndContinue
        }
      }
    }
    "the user is voluntary and eligible for both years" should {
      def mainContent: Element = document(
        eligibleNextYearOnly = false,
        mandatedCurrentYear = false,
        mandatedNextYear = false,
        clientName,
        clientNino
      ).mainContent

      "use the correct template details" in new TemplateViewTest(
        view = page(eligibleNextYearOnly = false, mandatedCurrentYear = false, mandatedNextYear = false, clientName, clientNino),
        title = WhatYouNeedToDoMessages.heading,
        isAgent = true,
        backLink = None,
        hasSignOutLink = true
      )

      "have a client Name and Nino" in {
        mainContent.selectHead(".govuk-caption-l").text contains clientName
        mainContent.selectHead(".govuk-caption-l").text contains clientNino
      }

      "have a first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.paraOne
      }

      "have a numbered list" which {
        def numberedList: Element = mainContent.selectHead("ol.govuk-list--number")

        "has a first point" in {
          numberedList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.NotificationBanner.bulletOne
        }

        "has a second point" in {
          numberedList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.NotificationBanner.bulletTwo
        }

        "has a third point" in {
          numberedList.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.NotificationBanner.bulletThree
        }

        "has a forth point" in {
          numberedList.selectNth("li", 4).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.NotificationBanner.bulletFour
        }

      }

      "have an inset text" in {
        mainContent.selectHead(".govuk-inset-text").text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.InsetText.para
      }

      "have a second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.paraTwo
      }

      "have a form" which {
        def form: Element = mainContent.selectHead("form")

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has an accept and continue button to submit the form" in {
          form.selectHead("button").text mustBe WhatYouNeedToDoMessages.acceptAndContinue
        }
      }
    }
  }

  def page(
            eligibleNextYearOnly: Boolean,
            mandatedCurrentYear: Boolean,
            mandatedNextYear: Boolean,
            clientName: String,
            clientNino: String
          ): HtmlFormat.Appendable = {
    whatYouNeedToDo(
      postAction = testCall,
      eligibleNextYearOnly = eligibleNextYearOnly,
      mandatedCurrentYear = mandatedCurrentYear,
      mandatedNextYear = mandatedNextYear,
      clientName = clientName,
      clientNino = clientNino
    )
  }

  def document(eligibleNextYearOnly: Boolean, mandatedCurrentYear: Boolean, mandatedNextYear: Boolean, clientName: String, clientNino: String): Document = {
    Jsoup.parse(page(eligibleNextYearOnly, mandatedCurrentYear, mandatedNextYear, clientName, clientNino).body)
  }

  object WhatYouNeedToDoMessages {
    val heading: String = "What you need to do"

    object NotificationBanner {
      val heading: String = "Important"
    }

    object MandatedCurrentYear {
      val paraOne: String = "Based on your client’s previous tax returns, they must use Making Tax Digital for Income Tax."
      val paraTwo: String = "Either you or your client must:"

      object NotificationBanner {
        val bulletOne: String = "Record income and expenses using compatible software."
        val bulletTwo: String = "Use software to send us quarterly updates."
        val date: String = AccountingPeriodUtil.getFinalDeclarationDate(false).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
        val bulletThree: String = s"Send an end of period statement and submit a final declaration by $date."
      }

      object WarningText {
        val para: String = "Warning Your client may be penalised if they do not use Making Tax Digital for Income Tax."
      }

    }

    object EligibleNextYearOnly {
      val paraOne: String = {
        val year: Int = AccountingPeriodUtil.getCurrentTaxEndYear
        s"Your client can sign up to use Making Tax Digital for Income Tax from 6 April $year."
      }
      val paraTwo: String = "By signing up you agree that either you or your client will:"

      object NotificationBanner {
        val bulletOne: String = "Record income and expenses using compatible software."
        val bulletTwo: String = "Use software to send us quarterly update."
        val bulletThree: String = {
          val date: String = AccountingPeriodUtil.getFinalDeclarationDate(true).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
          s"Send an end of period statement and submit a final declaration by $date."
        }
        val bulletFour: String = "Tell HMRC if they stop trading or start a new business."
      }

      object InsetText {
        val para: String = {
          val year: Int = AccountingPeriodUtil.getCurrentTaxEndYear
          s"Your client’s Self Assessment tax return must be submitted at the end of the $year tax year as normal."
        }
      }
    }

    object VoluntaryAndEligible {
      val paraOne: String = "By signing up you agree that either you or your client will:"

      object NotificationBanner {
        val bulletOne: String = "Record income and expenses using compatible software."
        val bulletTwo: String = "Use software to send us quarterly updates."
        val bulletThree: String = "Complete any missing quarterly updates (if you have chosen to sign up for the current tax year)."
        val bulletFour: String = "Send an end of period statement and submit a final declaration by 31 January following the end of the tax year."
        val bulletFive: String = "Tell HMRC if they stop trading or start a new business."
      }

      object InsetText {
        val para: String = "Using Making Tax Digital for Income Tax is currently voluntary. Your client can opt out and go back to Self Assessment at any time."
      }

      val paraTwo: String = "It will be compulsory for some people to use Making Tax Digital for Income Tax from April 2026," +
        " depending on their total qualifying income. If this applies to your client, we’ll send them a letter."
    }

    object MandatedAndEligibleForNextYearOnly {
      val currentYearDate: Int = AccountingPeriodUtil.getCurrentTaxEndYear
      val paraOne: String = s"You can sign up your client to use Making Tax Digital for Income Tax from 6 April $currentYearDate."
      val paraTwo: String = "By signing up you agree that either you or your client will:"

      object NotificationBanner {
        val bulletOne: String = "Get compatible software to record your income and expenses."
        val bulletTwo: String = "Use your compatible software to send us quarterly updates."
        val bulletThree: String = {
          val finalDeclarationDate = AccountingPeriodUtil.getFinalDeclarationDate(true).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
          s"Send an end of period statement and submit your final declaration by $finalDeclarationDate."
        }
      }

    }

    val acceptAndContinue: String = "Accept and continue"

  }

}