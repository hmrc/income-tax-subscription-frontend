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
    "the body of the content" should {

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
        allScenarios.foreach(_.selectHead(".govuk-caption-l").text contains clientName)
        allScenarios.foreach(_.selectHead(".govuk-caption-l").text contains clientNino)
      }

      "have a first paragraph" when {

        "user is signing up for current tax year" in {
          currentTaxYearScenarios.foreach(_.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.ParaOne.currentYear)
        }

        "user is signing up for next tax year" in {
          nextTaxYearScenarios.foreach(_.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.ParaOne.nextYear)
        }
      }

      "have a second paragraph when signing up for next tax year" when {

        "user is mandated" in {
          docNoSoftwareAndNTYMandated.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.ParaTwo.must(AccountingPeriodUtil.getNextTaxEndYear - 1)
          docHasSoftwareAndNTYMandated.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.ParaTwo.must(AccountingPeriodUtil.getNextTaxEndYear - 1)
        }

        "user is not mandated" in {
          docNoSoftwareAndNTY.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.ParaTwo.should(AccountingPeriodUtil.getNextTaxEndYear - 1)
          docHasSoftwareAndNTY.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.ParaTwo.should(AccountingPeriodUtil.getNextTaxEndYear - 1)
        }
      }

      "have a bullet list" which {

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
            hasSoftwareScenarios.foreach(_.selectHead("ol.govuk-list--bullet")
              .selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.BulletOne.hasSoftware)
          }

          "user has no compatible software with link" in {
            noSoftwareScenarios.foreach(_.selectHead("ol.govuk-list--bullet")
              .selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.BulletOne.noSoftwareText)

            noSoftwareScenarios.foreach(_.selectHead("ol.govuk-list--bullet").selectNth("li", 1)
              .selectHead("a").attr("href") mustBe WhatYouNeedToDoMessages.BulletOne.noSoftwareLink)
          }
        }

        "has a second point" in {
          allScenarios.foreach(_.selectHead("ol.govuk-list--bullet").selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.bulletTwo)
        }

        "has a third point" in {
          allScenarios.foreach(_.selectHead("ol.govuk-list--bullet").selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.bulletThree)
        }

        "has a fourth point" when {
          "user is signing up for current tax year" in {
            currentTaxYearScenarios.foreach(_.selectHead("ol.govuk-list--bullet").selectNth("li", 4).text mustBe WhatYouNeedToDoMessages.bulletFourCTYOnly)

          }
          "user is signing up for next tax year" in {
            nextTaxYearScenarios.foreach(_.selectHead("ol.govuk-list--bullet").selectNth("li", 4).text mustBe WhatYouNeedToDoMessages.bulletFive)
          }
        }

        "has a fifth point when user is signing up for current tax year" in {
          currentTaxYearScenarios.foreach(_.selectHead("ol.govuk-list--bullet").selectNth("li", 5).text mustBe WhatYouNeedToDoMessages.bulletFive)
        }
      }

      "have a third paragraph" in {
        currentTaxYearScenarios.foreach(_.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.paraThree)
        currentTaxYearScenarios.foreach(_.selectNth("p", 2).selectHead("a").attr("href") mustBe WhatYouNeedToDoMessages.paraThreeLinkHref)

        nextTaxYearScenarios.foreach(_.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.paraThree)
        nextTaxYearScenarios.foreach(_.selectNth("p", 3).selectHead("a").attr("href") mustBe WhatYouNeedToDoMessages.paraThreeLinkHref)

      }

      "have a fourth paragraph when user is signing up voluntarily" in {
        docHasSoftwareAndCTY.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.paraFour
        docHasSoftwareAndNTY.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.paraFour

      }

      "have a sub heading" in {
        allScenarios.foreach(_.getSubHeading("h2", 1).text mustBe WhatYouNeedToDoMessages.subHeading)
      }

      "have a fifth paragraph" when {
        "user is signing up for current tax year" in {
          docHasSoftwareAndCTY.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.paraFive
          docNoSoftwareAndCTY.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.paraFive
        }
        "user is signing up for next tax year" in {
          docHasSoftwareAndNTY.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.paraFive
          docNoSoftwareAndNTY.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.paraFive
        }

      }

      "have a sixth paragraph" when {
        "user is signing up for current tax year" in {
          docHasSoftwareAndCTY.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.paraSix
          docNoSoftwareAndCTY.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.paraSix
        }
        "user is signing up for next tax year" in {
          docHasSoftwareAndNTY.selectNth("p", 6).text mustBe WhatYouNeedToDoMessages.paraSix
          docNoSoftwareAndNTY.selectNth("p", 6).text mustBe WhatYouNeedToDoMessages.paraSix
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
      val bulletFive: String = "make their tax return by 31 January after the end of each tax year"
      val paraThree: String = "You’re also agreeing that our new penalties (opens in new tab) will apply to your client if they miss deadlines for submitting their tax return or paying their bill."
      val paraThreeLinkText: String = "our new penalties (opens in new tab)"
      val paraThreeLinkHref: String = "https://www.gov.uk/guidance/penalties-for-income-tax-self-assessment-volunteers"
      val paraFour: String = "While you and your client are taking part voluntarily, your client will not get penalties for missed quarterly updates."

      val subHeading: String = "Opting out"

      val paraFive: String = "Making Tax Digital for Income Tax is voluntary until 6 April 2026. Your client can opt out of sending quarterly updates. But if we’ve told your client that our new penalties apply to them, that will continue."
      val paraSix: String = "From 6 April 2026, some people will need to use Making Tax Digital for Income Tax. They will not be able to opt out. We’ll write to your client if this applies to them."
    }
}
