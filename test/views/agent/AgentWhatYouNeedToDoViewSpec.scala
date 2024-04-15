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

import config.featureswitch.FeatureSwitch.EOPSContent
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.WhatYouNeedToDo

import java.time.format.DateTimeFormatter
import scala.util.Random

class AgentWhatYouNeedToDoViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EOPSContent)
  }

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
        title = WhatYouNeedToDoMessages.title,
        isAgent = true,
        backLink = None,
        hasSignOutLink = true
      )

      "have a client Name and Nino" in {
        mainContent.selectHead(".govuk-caption-l").text contains clientName
        mainContent.selectHead(".govuk-caption-l").text contains clientNino
      }

      "have a page heading" in {
        mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.heading
      }

      "have a first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.paraOne
      }

      "have a second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.paraTwo
      }

      "has a numbered list" which {
        def numberedList: Element = mainContent.selectHead("ol.govuk-list--bullet")

        "has a first point" in {
          numberedList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.bulletOne
        }

        "has a second point" in {
          numberedList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.bulletTwo
        }

        "has a third point" in {
          numberedList.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.bulletThree
        }

        "has a fourth point" in {
          numberedList.selectNth("li", 4).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.bulletFour
        }
      }

      "have a third paragraph" in {
        mainContent.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.paraThree
      }

      "has a second numbered list" which {
        def numberedList: Element = mainContent.selectNth("ol", 2)

        "has a fifth point" in {
          numberedList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.bulletFive
        }

        "has a sixth point" in {
          numberedList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.bulletSix
        }
      }

      "have a fourth paragraph" in {
        mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.paraFour
      }

      "has a sub heading" in {
        mainContent.selectHead("h2").text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.subHeading
      }

      "have a fifth paragraph" in {
        mainContent.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.paraFive
      }

      "have a sixth paragraph" in {
        mainContent.selectNth("p", 6).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.paraSix
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

      "have a heading" in {
        mainContent.selectHead("h1").text contains WhatYouNeedToDoMessages.VoluntaryAndEligible.heading
      }

      "have a first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.paraOne
      }

      "have a numbered list" which {
        def numberedList: Element = mainContent.selectHead("ol.govuk-list--bullet")

        "has a first point" in {
          numberedList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.bulletOne
        }

        "has a second point" in {
          numberedList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.bulletTwo
        }

        "has a third point" in {
          numberedList.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.bulletThree
        }

        "has a forth point " in {
          numberedList.selectNth("li", 4).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.bulletFour
        }

        "has a fifth point " in {
          numberedList.selectNth("li", 5).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.bulletFive
        }

      }

      "have a second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.paraTwo
      }

      "have a second numbered list" which {
        def numberedList: Element = mainContent.selectNth("ol", 2)

        "has a sixth point " in {
          numberedList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.bulletSix
        }

        "has a seventh point " in {
          numberedList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.bulletSeven
        }
      }

      "have a third paragraph" in {
        mainContent.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.paraThree
      }

      "have a sub heading" in {
        mainContent.selectHead("h2").text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.subHeading
      }

      "have a fourth paragraph" in {
        mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.paraFour
      }

      "have a fifth paragraph" in {
        mainContent.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.paraFive
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
    val title: String = "What you need to do"
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
      val heading: String = "What you are agreeing to"

      val paraOne: String = "If you continue to sign up this client, you need to submit their Self Assessment tax return as normal for the current tax year."
      val paraTwo: String = "From the tax year starting on 6 April 2025, you’re agreeing to:"
      val bulletOne: String = "use software that works with Making Tax Digital for Income Tax"
      val bulletTwo: String = "keep digital records of your client’s business income and expenses"
      val bulletThree: String = "use compatible software to send us quarterly updates"
      val bulletFour: String = "make their final declaration by 31 January after the end of each tax year"

      val paraThree: String = "You’re also agreeing that our new penalties will apply to your client if they miss deadlines for:"
      val bulletFive: String = "submitting their tax return"
      val bulletSix: String = "paying their bill"

      val paraFour: String = "We’ll write to your client when they are liable for these penalties."

      val subHeading: String = "Opting out"
      val paraFive: String = "Making Tax Digital for Income Tax is voluntary until 6 April 2026. Your client can opt out of sending quarterly updates. But if we’ve told your client they’re liable for our new penalties, they’ll continue to be liable for them."
      val paraSix: String = "From 6 April 2026, some people will need to use Making Tax Digital for Income Tax. They will not be able to opt out. We’ll write to your client if this applies to them."
    }

    object VoluntaryAndEligible {
      val heading: String = "What you are agreeing to"
      val paraOne: String = "If you continue to voluntarily sign up this client, you’re agreeing to meet their tax obligations using Making Tax Digital for Income Tax. These include:"
      val bulletOne: String = "using software that works with Making Tax Digital for Income Tax"
      val bulletTwo: String = "keeping digital records of your client’s business income and expenses"
      val bulletThree: String = "using compatible software to send us quarterly updates"
      val bulletFour: String = "sending any missed quarterly updates - if you’re signing up your client part way through the current tax year"
      val bulletFive: String = "making their final declaration by 31 January after the end of the tax year"
      val paraTwo: String = "You’re also agreeing that our new penalties will apply to your client if they miss deadlines for:"
      val bulletSix: String = "submitting their tax return"
      val bulletSeven: String = "paying their bill"
      val paraThree: String = "We’ll write to your client when they’re liable for these penalties."
      val subHeading: String = "Opting out"
      val paraFour: String = "Making Tax Digital for Income Tax is voluntary until 6 April 2026. Your client can opt out of sending quarterly updates. But if we’ve told your client they’re liable for our new penalties, they’ll continue to be liable for them."
      val paraFive: String = "From 6 April 2026, some people will need to use Making Tax Digital for Income Tax. They will not be able to opt out. We’ll write to your client if this applies to them."
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