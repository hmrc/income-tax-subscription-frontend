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

import config.featureswitch.FeatureSwitch.PrePopulate
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
    disable(PrePopulate)
  }

  private val nameLengthCharacters = 10
  private val clientName = Random.alphanumeric.take(nameLengthCharacters).mkString
  private val clientNino = Random.alphanumeric.take(nameLengthCharacters).mkString

  val whatYouNeedToDo: WhatYouNeedToDo = app.injector.instanceOf[WhatYouNeedToDo]

  def mainContentPrePop(hasSoftware: Boolean, taxYearSelectionIsCurrent: Boolean,
                        mandatedCurrentYear: Boolean = false, mandatedNextYear: Boolean = false): Element = document(
    eligibleNextYearOnly = false,
    mandatedCurrentYear = mandatedCurrentYear,
    mandatedNextYear = mandatedNextYear,
    taxYearSelectionIsCurrent = taxYearSelectionIsCurrent,
    usingSoftwareStatus = hasSoftware,
    clientName,
    clientNino
  ).mainContent

  "WhatYouNeedToDo" when {
    "the user is mandated for the current year" should {
      def mainContent: Element = document(
        eligibleNextYearOnly = false,
        mandatedCurrentYear = true,
        mandatedNextYear = false,
        taxYearSelectionIsCurrent = true,
        usingSoftwareStatus = true,
        clientName,
        clientNino
      ).mainContent

      "use the correct template details" in new TemplateViewTest(
        view = page(eligibleNextYearOnly = false, mandatedCurrentYear = true, mandatedNextYear = false, taxYearSelectionIsCurrent = true,
          usingSoftwareStatus = true, clientName, clientNino),
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
      def mainContent: Element = document(
        eligibleNextYearOnly = true,
        mandatedCurrentYear = false,
        mandatedNextYear = true,
        taxYearSelectionIsCurrent = false,
        usingSoftwareStatus = true,
        clientNino,
        clientName
      ).mainContent

      "use the correct template details" in new TemplateViewTest(
        view = page(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = true, taxYearSelectionIsCurrent = true,
          usingSoftwareStatus = true, clientName, clientNino),
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
        taxYearSelectionIsCurrent = false,
        usingSoftwareStatus = true,
        clientName,
        clientNino
      ).mainContent

      "use the correct template details" in new TemplateViewTest(
        view = page(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = false, taxYearSelectionIsCurrent = true,
          usingSoftwareStatus = true, clientName, clientNino),
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
        taxYearSelectionIsCurrent = true,
        usingSoftwareStatus = true,
        clientName,
        clientNino
      ).mainContent

      "use the correct template details" in new TemplateViewTest(
        view = page(eligibleNextYearOnly = false, mandatedCurrentYear = false, mandatedNextYear = false, taxYearSelectionIsCurrent = true,
          usingSoftwareStatus = true, clientName, clientNino),
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

    "the pre-pop feature switch is enabled" should {
      import WhatYouNeedToDoMessages._

      def docHasSoftwareAndCTY: Element = mainContentPrePop(hasSoftware = true, taxYearSelectionIsCurrent = true)

      def docHasSoftwareAndCTYMandated = mainContentPrePop(hasSoftware = true, taxYearSelectionIsCurrent = true, mandatedCurrentYear = true)

      def docHasSoftwareAndNTY = mainContentPrePop(hasSoftware = true, taxYearSelectionIsCurrent = false)

      def docHasSoftwareAndNTYMandated = mainContentPrePop(hasSoftware = true, taxYearSelectionIsCurrent = false, mandatedNextYear = true)

      def docNoSoftwareAndNTY = mainContentPrePop(hasSoftware = false, taxYearSelectionIsCurrent = false)

      def docNoSoftwareAndNTYMandated = mainContentPrePop(hasSoftware = false, taxYearSelectionIsCurrent = false, mandatedNextYear = true)

      def docNoSoftwareAndCTY = mainContentPrePop(hasSoftware = false, taxYearSelectionIsCurrent = true)

      def docNoSoftwareAndCTYMandated = mainContentPrePop(hasSoftware = false, taxYearSelectionIsCurrent = true, mandatedCurrentYear = true)

      def allScenarios: List[Element] = {
        List(docHasSoftwareAndCTY,
          docHasSoftwareAndNTY,
          docHasSoftwareAndCTYMandated,
          docHasSoftwareAndNTYMandated,
          docNoSoftwareAndCTY,
          docNoSoftwareAndNTY,
          docNoSoftwareAndCTYMandated,
          docNoSoftwareAndNTYMandated)
      }

      def nextTaxYearScenarios: List[Element] = List(docHasSoftwareAndNTY, docHasSoftwareAndNTYMandated,
        docNoSoftwareAndNTY, docNoSoftwareAndNTYMandated)

      def currentTaxYearScenarios: List[Element] = List(docHasSoftwareAndCTY, docHasSoftwareAndCTYMandated,
        docNoSoftwareAndCTY, docNoSoftwareAndCTYMandated)


      "use the correct template details" when {
        "user has software signing up for current tax year" in new TemplateViewTest(
          view = page(eligibleNextYearOnly = false, mandatedCurrentYear = false, mandatedNextYear = false,
            taxYearSelectionIsCurrent = true, usingSoftwareStatus = true, clientName, clientNino),
          title = WhatYouNeedToDoMessages.heading,
          isAgent = true,
          backLink = Some(testBackUrl),
          hasSignOutLink = true
        )

        "user has software signing up for next tax year" in new TemplateViewTest(
          view = page(eligibleNextYearOnly = false, mandatedCurrentYear = false, mandatedNextYear = false,
            taxYearSelectionIsCurrent = false, usingSoftwareStatus = true, clientName, clientNino),
          title = WhatYouNeedToDoMessages.heading,
          isAgent = true,
          backLink = Some(testBackUrl),
          hasSignOutLink = true
        )

        "user has no software signing up for current tax year" in new TemplateViewTest(
          view = page(eligibleNextYearOnly = false, mandatedCurrentYear = false, mandatedNextYear = false,
            taxYearSelectionIsCurrent = true, usingSoftwareStatus = false, clientName, clientNino),
          title = WhatYouNeedToDoMessages.heading,
          isAgent = true,
          backLink = Some(testBackUrl),
          hasSignOutLink = true
        )

        "user has no software signing up for next tax year" in new TemplateViewTest(
          view = page(eligibleNextYearOnly = false, mandatedCurrentYear = false, mandatedNextYear = false,
            taxYearSelectionIsCurrent = false, usingSoftwareStatus = false, clientName, clientNino),
          title = WhatYouNeedToDoMessages.heading,
          isAgent = true,
          backLink = Some(testBackUrl),
          hasSignOutLink = true
        )
      }

      "have a client Name and Nino" in {
        enable(PrePopulate)
        allScenarios.foreach(_.selectHead(".govuk-caption-l").text contains clientName)
        allScenarios.foreach(_.selectHead(".govuk-caption-l").text contains clientNino)
      }

      "have a first paragraph" when {

        "user is signing up for current tax year" in {
          enable(PrePopulate)
          currentTaxYearScenarios.foreach(_.selectNth("p", 1).text mustBe PrePopEnabled.ParaOne.currentYear)
        }

        "user is signing up for next tax year" in {
          enable(PrePopulate)
          nextTaxYearScenarios.foreach(_.selectNth("p", 1).text mustBe PrePopEnabled.ParaOne.nextYear)
        }
      }

      "have a second paragraph when signing up for next tax year" when {

        "user is mandated" in {
          enable(PrePopulate)
          docNoSoftwareAndNTYMandated.selectNth("p", 2).text mustBe PrePopEnabled.ParaTwo.must(2025)
          docHasSoftwareAndNTYMandated.selectNth("p", 2).text mustBe PrePopEnabled.ParaTwo.must(2025)
        }

        "user is not mandated" in {
          enable(PrePopulate)
          docNoSoftwareAndNTY.selectNth("p", 2).text mustBe PrePopEnabled.ParaTwo.should(2025)
          docHasSoftwareAndNTY.selectNth("p", 2).text mustBe PrePopEnabled.ParaTwo.should(2025)
        }
      }

      "have a bullet list" which {
        enable(PrePopulate)

        def hasSoftwareScenarios: List[Element] = List(docHasSoftwareAndCTY,
          docHasSoftwareAndNTY,
          docHasSoftwareAndCTYMandated,
          docHasSoftwareAndNTYMandated)

        def noSoftwareScenarios: List[Element] = List(docNoSoftwareAndCTY,
          docNoSoftwareAndNTY,
          docNoSoftwareAndCTYMandated,
          docNoSoftwareAndNTYMandated)

        "has a first point" when {
          "user has compatible software" in {
            enable(PrePopulate)
            hasSoftwareScenarios.foreach(_.selectHead("ol.govuk-list--bullet")
              .selectNth("li", 1).text mustBe PrePopEnabled.BulletOne.hasSoftware)
          }

          "user has no compatible software with link" in {
            enable(PrePopulate)
            noSoftwareScenarios.foreach(_.selectHead("ol.govuk-list--bullet")
              .selectNth("li", 1).text mustBe PrePopEnabled.BulletOne.noSoftwareText)

            noSoftwareScenarios.foreach(_.selectHead("ol.govuk-list--bullet").selectNth("li", 1)
              .selectHead("a").attr("href") mustBe PrePopEnabled.BulletOne.noSoftwareLink)
          }
        }

        "has a second point" in {
          enable(PrePopulate)
          allScenarios.foreach(_.selectHead("ol.govuk-list--bullet").selectNth("li", 2).text mustBe PrePopEnabled.bulletTwo)
        }

        "has a third point" in {
          enable(PrePopulate)
          allScenarios.foreach(_.selectHead("ol.govuk-list--bullet").selectNth("li", 3).text mustBe PrePopEnabled.bulletThree)
        }

        "has a fourth point" when {
          "user is signing up for current tax year" in {
            enable(PrePopulate)
            currentTaxYearScenarios.foreach(_.selectHead("ol.govuk-list--bullet").selectNth("li", 4).text mustBe PrePopEnabled.bulletFourCTYOnly)

          }
          "user is signing up for next tax year" in {
            enable(PrePopulate)
            nextTaxYearScenarios.foreach(_.selectHead("ol.govuk-list--bullet").selectNth("li", 4).text mustBe PrePopEnabled.bulletFive)
          }
        }

        "has a fifth point when user is signing up for current tax year" in {
          enable(PrePopulate)
          currentTaxYearScenarios.foreach(_.selectHead("ol.govuk-list--bullet").selectNth("li", 5).text mustBe PrePopEnabled.bulletFive)
        }
      }

      "have a third paragraph" in {
        enable(PrePopulate)
        currentTaxYearScenarios.foreach(_.selectNth("p", 2).text mustBe PrePopEnabled.paraThree)
        currentTaxYearScenarios.foreach(_.selectNth("p", 2).selectHead("a").attr("href") mustBe PrePopEnabled.paraThreeLinkHref)

        nextTaxYearScenarios.foreach(_.selectNth("p", 3).text mustBe PrePopEnabled.paraThree)
        nextTaxYearScenarios.foreach(_.selectNth("p", 3).selectHead("a").attr("href") mustBe PrePopEnabled.paraThreeLinkHref)

      }

      "have a fourth paragraph when user is signing up voluntarily" in {
        enable(PrePopulate)
        docHasSoftwareAndCTY.selectNth("p", 3).text mustBe PrePopEnabled.paraFour
        docHasSoftwareAndNTY.selectNth("p", 4).text mustBe PrePopEnabled.paraFour

      }

      "have a sub heading" in {
        enable(PrePopulate)
        allScenarios.foreach(_.selectHead("h2").text mustBe PrePopEnabled.subHeading)
      }

      "have a fifth paragraph" when {
        "user is signing up for current tax year" in {
          enable(PrePopulate)
          docHasSoftwareAndCTY.selectNth("p", 4).text mustBe PrePopEnabled.paraFive
          docNoSoftwareAndCTY.selectNth("p", 4).text mustBe PrePopEnabled.paraFive
        }
        "user is signing up for next tax year" in {
          enable(PrePopulate)
          docHasSoftwareAndNTY.selectNth("p", 5).text mustBe PrePopEnabled.paraFive
          docNoSoftwareAndNTY.selectNth("p", 5).text mustBe PrePopEnabled.paraFive
        }

      }

      "have a sixth paragraph" when {
        "user is signing up for current tax year" in {
          enable(PrePopulate)
          docHasSoftwareAndCTY.selectNth("p", 5).text mustBe PrePopEnabled.paraSix
          docNoSoftwareAndCTY.selectNth("p", 5).text mustBe PrePopEnabled.paraSix
        }
        "user is signing up for next tax year" in {
          enable(PrePopulate)
          docHasSoftwareAndNTY.selectNth("p", 6).text mustBe PrePopEnabled.paraSix
          docNoSoftwareAndNTY.selectNth("p", 6).text mustBe PrePopEnabled.paraSix
        }

      }

      "have a form" which {

        "has the correct attributes" in {
          allScenarios.foreach(_.selectHead("form").attr("method") mustBe testCall.method)
          allScenarios.foreach(_.selectHead("form").attr("action") mustBe testCall.url)
        }

        "has an accept and continue button to submit the form" in {
          allScenarios.foreach(_.selectHead("form").selectHead("button").text mustBe WhatYouNeedToDoMessages.acceptAndContinue)
        }
      }

    }
  }

  def page(eligibleNextYearOnly: Boolean,
           mandatedCurrentYear: Boolean,
           mandatedNextYear: Boolean,
           taxYearSelectionIsCurrent: Boolean,
           usingSoftwareStatus: Boolean,
           clientName: String,
           clientNino: String
          ): HtmlFormat.Appendable = {
    whatYouNeedToDo(
      postAction = testCall,
      eligibleNextYearOnly = eligibleNextYearOnly,
      mandatedCurrentYear = mandatedCurrentYear,
      mandatedNextYear = mandatedNextYear,
      taxYearSelectionIsCurrent = taxYearSelectionIsCurrent,
      usingSoftwareStatus = usingSoftwareStatus,
      clientName = clientName,
      clientNino = clientNino,
      backUrl = Some(testBackUrl)
    )
  }

  def document(eligibleNextYearOnly: Boolean, mandatedCurrentYear: Boolean, mandatedNextYear: Boolean, taxYearSelectionIsCurrent: Boolean,
               usingSoftwareStatus: Boolean, clientName: String, clientNino: String): Document = {
    Jsoup.parse(page(eligibleNextYearOnly, mandatedCurrentYear, mandatedNextYear, taxYearSelectionIsCurrent, usingSoftwareStatus, clientName, clientNino).body)
  }

  object WhatYouNeedToDoMessages {
    val title: String = "What you need to do"
    val heading: String = "What you need to do"
    val acceptAndContinue: String = "Accept and continue"

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

      val paraThree: String = "You’re also agreeing that our new penalties (opens in new tab) will apply to your client if they miss deadlines for submitting their tax return or paying their bill."

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
      val paraTwo: String = "You’re also agreeing that our new penalties (opens in new tab) will apply to your client if they miss deadlines for submitting their tax return or paying their bill."
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

    object PrePopEnabled {
      val heading: String = "What you are agreeing to"

      object ParaOne {
        val currentYear: String = "If you continue to sign up this client, you and your client are agreeing to meet their tax obligations using Making Tax Digital for Income Tax. You or your client will need to:"
        val nextYear: String = "If you continue to sign up this client, you need to submit their Self Assessment tax return as normal for the current tax year."
      }

      object ParaTwo {
        def should(taxYear: Int): String = s"From 6 April $taxYear, you or your client should:"

        def must(taxYear: Int): String = s"From 6 April $taxYear, you or your client must:"
      }

      object BulletOne {
        val noSoftwareText: String = "find and use software that works with Making Tax Digital for Income Tax (opens in new tab)"
        val noSoftwareLink: String = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
        val hasSoftware: String = "use software that works with Making Tax Digital for Income Tax"
      }

      val bulletTwo: String = "keep digital records of your client’s business income and expenses"
      val bulletThree: String = "use software to send us quarterly updates"
      val bulletFourCTYOnly: String = "send any missed quarterly updates for the current tax year"
      val bulletFive: String = "make their final declaration by 31 January after the end of each tax year"
      val paraThree: String = "You’re also agreeing that our new penalties (opens in new tab) will apply to your client if they miss deadlines for submitting their tax return or paying their bill."
      val paraThreeLinkText: String = "our new penalties (opens in new tab)"
      val paraThreeLinkHref: String = "https://www.gov.uk/guidance/penalties-for-income-tax-self-assessment-volunteers"
      val paraFour: String = "While you and your client are taking part voluntarily, your client will not get penalties for missed quarterly updates."

      val subHeading: String = "Opting out"

      val paraFive: String = "Making Tax Digital for Income Tax is voluntary until 6 April 2026. Your client can opt out of sending quarterly updates. But if we’ve told your client that our new penalties apply to them, that will continue."
      val paraSix: String = "From 6 April 2026, some people will need to use Making Tax Digital for Income Tax. They will not be able to opt out. We’ll write to your client if this applies to them."
    }
  }

}
