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

package views.individual

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.WhatYouNeedToDo

import java.time.format.DateTimeFormatter

class WhatYouNeedToDoSpec extends ViewSpec {

  val whatYouNeedToDo: WhatYouNeedToDo = app.injector.instanceOf[WhatYouNeedToDo]

  def page(onlyNextYear: Boolean): HtmlFormat.Appendable = whatYouNeedToDo(testCall, onlyNextYear, mandatedCurrentYear = false, mandatedNextYear = false)

  def document(onlyNextYear: Boolean): Document = Jsoup.parse(page(onlyNextYear).body)


  def pageCurrentMandated(currentYearMandated: Boolean): HtmlFormat.Appendable = whatYouNeedToDo(testCall, onlyNextYear = false, mandatedCurrentYear = currentYearMandated, mandatedNextYear = false)

  def pageNextYearOnlyAndMandated(nextYearMandated: Boolean): HtmlFormat.Appendable = whatYouNeedToDo(testCall, onlyNextYear = true, mandatedCurrentYear = false, mandatedNextYear = nextYearMandated)

  def documentCurrentMandated(currentYearMandated: Boolean): Document = Jsoup.parse(pageCurrentMandated(currentYearMandated).body)

  def documentNextYearOnlyAndMandated(nextYearMandated: Boolean): Document = Jsoup.parse(pageNextYearOnlyAndMandated(nextYearMandated).body)

  object WhatYouNeedToDoMessages {
    val title: String = "What you need to do"
    val heading: String = "What you are agreeing to"
    val paraOne: String = "If you continue to sign up, you’re agreeing to meet your tax obligations using Making Tax Digital for Income Tax. These include:"
    val bulletOne: String = "using software that works with Making Tax Digital for Income Tax"
    val bulletTwo: String = "keeping digital records of your business income and expenses"
    val bulletThree: String = "using compatible software to send us quarterly updates"
    val bulletFour: String = "sending any missed quarterly updates - if you’re signing up part way through the current tax year"
    val bulletFive: String = "making your final declaration by 31 January after the end of the tax year"

    val paraTwo: String = "You’re also agreeing that our new penalties will apply if you miss deadlines for:"
    val bulletSix: String = "submitting your tax return"
    val bulletSeven: String = "paying your bill"

    val paraThree: String = "We’ll write to you when you’re liable for these penalties."

    val subHeading: String = "Opting out"

    val paraFour: String = "Making Tax Digital for Income Tax is voluntary until 6 April 2026. You can opt out of sending quarterly updates. But if we’ve told you that you’re liable for our new penalties, you’ll continue to be liable for them."
    val paraFive: String = "From 6 April 2026, some people will need to use Making Tax Digital for Income Tax. They will not be able to opt out. We’ll write to you if this applies to you."
  }

  object NextYearOnlyWhatYouNeedToDoMessages {
    val heading: String = "What you need to do"
    val paraOne: String = s"You can sign up to use Making Tax Digital for Income Tax from 6 April ${AccountingPeriodUtil.getCurrentTaxEndYear}."
    val paraTwo: String = "By taking part you agree to:"

    object NotificationBanner {
      val heading: String = "Important"
      val bulletOne: String = s"Get compatible software to record your income and expenses from 6 April ${AccountingPeriodUtil.getCurrentTaxEndYear}."
      val bulletTwo: String = "Use your software to send us quarterly updates."
      val bulletThree: String = {
        val date = AccountingPeriodUtil.getFinalDeclarationDate(true).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
        s"Send an end of period statement using your software and send your final declaration by $date."
      }
      val bulletFour: String = "Tell HMRC if you stop trading or start a new business."
    }

    object InsetText {
      val para: String = "You must continue to submit your Self Assessment tax returns until the year you have signed up."
    }

  }


  object WhatYouNeedToDoMandatedCurrent {
    val heading: String = "What you need to do"
    val paraOne: String = "Based on your previous returns, you need to sign up for Making Tax Digital for Income Tax."
    val paraTwo: String = "By signing up you agree to:"

    object NotificationBanner {
      val heading: String = "Important"
      val bulletOne: String = "Get compatible software to record your income and expenses."
      val bulletTwo: String = "Use your software to send us quarterly updates."
      val bulletThree: String = {
        val date = AccountingPeriodUtil.getFinalDeclarationDate(false).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
        s"Send an end of period statement and submit your final declaration by $date."
      }
    }

    object WarningText {
      val para: String = "Warning You may be penalised if you do not use Making Tax Digital for Income Tax."
    }

  }

  object WhatYouNeedToDoMandatedAndEligibleForNextYearOnly {
    val heading: String = "What you need to do"
    val currentTaxYearEndDate: Int = AccountingPeriodUtil.getCurrentTaxEndYear
    val paraOne = s"You can sign up to use Making Tax Digital for Income Tax from 6 April $currentTaxYearEndDate."
    val paraTwo = "By signing up you agree to:"

    object NotificationBanner {
      val finalDeclarationDate: String = AccountingPeriodUtil.getFinalDeclarationDate(true).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
      val heading: String = "Important"
      val bulletOne: String = "Get compatible software to record your income and expenses."
      val bulletTwo: String = "Use your compatible software to send us quarterly updates."
      val bulletThree: String = s"Send an end of period statement and submit your final declaration by $finalDeclarationDate."
    }
  }

  "WhatYouNeedToDo" must {

    "use the correct template details" in new TemplateViewTest(
      view = page(false),
      title = WhatYouNeedToDoMessages.title,
      isAgent = false,
      backLink = None,
      hasSignOutLink = true
    )

    "have a page heading" in {
      document(false).mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMessages.heading
    }

    "have a first paragraph" in {
      document(false).mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.paraOne
    }

    "has a numbered list" which {
      def numberedList: Element = document(false).mainContent.selectHead("ol.govuk-list--bullet")

      "has a first point" in {
        numberedList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.bulletOne
      }

      "has a second point" in {
        numberedList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.bulletTwo
      }

      "has a third point" in {
        numberedList.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.bulletThree
      }

      "has a forth point" in {
        numberedList.selectNth("li", 4).text mustBe WhatYouNeedToDoMessages.bulletFour
      }
    }

    "have a second paragraph" in {
      document(false).mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.paraTwo
    }

    "has a second numbered list" which {
      def numberedList: Element = document(onlyNextYear = false).mainContent.selectNth("ol", 2)

      "has a fifth point" in {
        numberedList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.bulletSix
      }

      "has a sixth point" in {
        numberedList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.bulletSeven
      }
    }

    "have a third paragraph" in {
      document(false).mainContent.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.paraThree
    }

    "has a sub heading" in {
      document(false).mainContent.selectHead("h2").text mustBe WhatYouNeedToDoMessages.subHeading
    }

    "have a fourth paragraph" in {
      document(false).mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.paraFour
    }

    "have a fifth paragraph" in {
      document(false).mainContent.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.paraFive
    }
  }

  // Non Mandated next year only test to be incuded here

  "WhatYouNeedToDoMandatedCurrent" must {

    "use the correct template details" in new TemplateViewTest(
      view = pageCurrentMandated(true),
      title = WhatYouNeedToDoMandatedCurrent.heading,
      isAgent = false,
      backLink = None,
      hasSignOutLink = true
    )

    "have a page heading" in {
      documentCurrentMandated(true).mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMandatedCurrent.heading
    }

    "have a first paragraph" in {
      documentCurrentMandated(true).mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMandatedCurrent.paraOne
    }

    "have a second paragraph" in {
      documentCurrentMandated(true).mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMandatedCurrent.paraTwo
    }

    "has a numbered list" which {
      def numberedList: Element = documentCurrentMandated(true).mainContent.selectHead("ol.govuk-list--number")

      "has a first point" in {
        numberedList.selectNth("li", 1).text mustBe WhatYouNeedToDoMandatedCurrent.NotificationBanner.bulletOne
      }

      "has a second point" in {
        numberedList.selectNth("li", 2).text mustBe WhatYouNeedToDoMandatedCurrent.NotificationBanner.bulletTwo
      }

      "has a third point" in {
        numberedList.selectNth("li", 3).text mustBe WhatYouNeedToDoMandatedCurrent.NotificationBanner.bulletThree
      }
    }
  }

  "has an warning text" in {
    documentCurrentMandated(true).selectHead(".govuk-warning-text__text").text mustBe WhatYouNeedToDoMandatedCurrent.WarningText.para
  }

  "WhatYouNeedToDoMandatedAndNextYearOnly" must {

    "use the correct template details" in new TemplateViewTest(
      view = pageNextYearOnlyAndMandated(true),
      title = WhatYouNeedToDoMandatedAndEligibleForNextYearOnly.heading,
      isAgent = false,
      backLink = None,
      hasSignOutLink = true
    )

    "have a page heading" in {
      documentNextYearOnlyAndMandated(true).mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMandatedAndEligibleForNextYearOnly.heading
    }

    "have a first paragraph" in {
      documentNextYearOnlyAndMandated(true).mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMandatedAndEligibleForNextYearOnly.paraOne
    }

    "have a second paragraph" in {
      documentNextYearOnlyAndMandated(true).mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMandatedAndEligibleForNextYearOnly.paraTwo
    }

    "has a numbered list" which {
      def numberedList: Element = documentNextYearOnlyAndMandated(true).mainContent.selectHead("ol.govuk-list--number")

      "has a first point" in {
        numberedList.selectNth("li", 1).text mustBe WhatYouNeedToDoMandatedAndEligibleForNextYearOnly.NotificationBanner.bulletOne
      }

      "has a second point" in {
        numberedList.selectNth("li", 2).text mustBe WhatYouNeedToDoMandatedAndEligibleForNextYearOnly.NotificationBanner.bulletTwo
      }

      "has a third point" in {
        numberedList.selectNth("li", 3).text mustBe WhatYouNeedToDoMandatedAndEligibleForNextYearOnly.NotificationBanner.bulletThree
      }
    }

  }


}
