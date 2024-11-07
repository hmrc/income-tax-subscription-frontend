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

import config.featureswitch.FeatureSwitch.PrePopulate
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.WhatYouNeedToDo

import java.time.format.DateTimeFormatter

class WhatYouNeedToDoSpec extends ViewSpec {
  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(PrePopulate)
  }

  def whatYouNeedToDo: WhatYouNeedToDo = app.injector.instanceOf[WhatYouNeedToDo]

  def page(onlyNextYear: Boolean): HtmlFormat.Appendable = whatYouNeedToDo(testCall, onlyNextYear, mandatedCurrentYear = false, mandatedNextYear = false, isUsingSoftware = true, signUpNextTaxYear = false, backUrl = "backUrl")

  def document(onlyNextYear: Boolean): Document = Jsoup.parse(page(onlyNextYear).body)

  def pageCurrentMandated(currentYearMandated: Boolean): HtmlFormat.Appendable = whatYouNeedToDo(testCall, onlyNextYear = false, mandatedCurrentYear = currentYearMandated, mandatedNextYear = false, isUsingSoftware = true, signUpNextTaxYear = false, backUrl = "backUrl")

  def pageNextYearOnlyAndMandated(nextYearMandated: Boolean): HtmlFormat.Appendable = whatYouNeedToDo(testCall, onlyNextYear = true, mandatedCurrentYear = false, mandatedNextYear = nextYearMandated, isUsingSoftware = true, signUpNextTaxYear = false, backUrl = "backUrl")

  def pageVoluntaryNextYear(onlyNextYear: Boolean): HtmlFormat.Appendable = whatYouNeedToDo(testCall, onlyNextYear = true, mandatedCurrentYear = false, mandatedNextYear = false, isUsingSoftware = true, signUpNextTaxYear = false, backUrl = "backUrl")

  def pagePrePop(isUsingSoftware: Boolean, signUpNextTaxYear:Boolean, currentYearMandated: Boolean, nextYearMandated: Boolean): HtmlFormat.Appendable = whatYouNeedToDo(testCall, onlyNextYear = false, mandatedCurrentYear = currentYearMandated, mandatedNextYear = nextYearMandated, isUsingSoftware = isUsingSoftware, signUpNextTaxYear = signUpNextTaxYear, backUrl = "backUrl")

  def documentCurrentMandated(currentYearMandated: Boolean): Document = Jsoup.parse(pageCurrentMandated(currentYearMandated).body)

  def documentNextYearOnlyAndMandated(nextYearMandated: Boolean): Document = Jsoup.parse(pageNextYearOnlyAndMandated(nextYearMandated).body)

  def documentVoluntaryNextYear(onlyNextYear: Boolean): Document = Jsoup.parse(page(onlyNextYear).body)

  def documentPrePop(isUsingSoftware: Boolean, signUpNextTaxYear:Boolean, currentYearMandated: Boolean = false, nextYearMandated: Boolean = false): Document = Jsoup.parse(pagePrePop(isUsingSoftware, signUpNextTaxYear, currentYearMandated, nextYearMandated).body)


  object WhatYouNeedToDoMessages {
    val title: String = "What you are agreeing to"
    val heading: String = "What you are agreeing to"
    val paraOne: String = "If you continue to sign up, you’re agreeing to meet your tax obligations using Making Tax Digital for Income Tax. These include:"
    val bulletOne: String = "using software that works with Making Tax Digital for Income Tax"
    val bulletTwo: String = "keeping digital records of your business income and expenses"
    val bulletThree: String = "using compatible software to send us quarterly updates"
    val bulletFour: String = "sending any missed quarterly updates - if you’re signing up part way through the current tax year"
    val bulletFive: String = "making your final declaration by 31 January after the end of the tax year"

    val paraTwo: String = "You’re also agreeing that our new penalties (opens in new tab) will apply if you miss deadlines for submitting your tax return or paying your bill. We’ll write to you when you’re liable for these penalties."

    val subHeading: String = "Opting out"

    val paraThree: String = "Making Tax Digital for Income Tax is voluntary until 6 April 2026. You can opt out of sending quarterly updates. But if we’ve told you that you’re liable for our new penalties, you’ll continue to be liable for them."
    val paraFour: String = "From 6 April 2026, some people will need to use Making Tax Digital for Income Tax. They will not be able to opt out. We’ll write to you if this applies to you."
  }

  object NextYearOnlyWhatYouNeedToDoMessages {
    val heading: String = "What you are agreeing to"
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
    val heading: String = "What you are agreeing to"
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
    val heading: String = "What you are agreeing to"
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

  object VoluntaryNextYearOnly {
    val title: String = "What you are agreeing to"
    val heading: String = "What you are agreeing to"

    val paraOne: String = "If you continue to sign up, you need to submit your Self Assessment tax returns as normal for the current tax year."
    val paraTwo: String = "From the tax year starting on 6 April 2025, you’re agreeing to:"
    val bulletOne: String = "use software that works with Making Tax Digital for Income Tax"
    val bulletTwo: String = "keep digital records of your business income and expenses"
    val bulletThree: String = "use compatible software to send us quarterly updates"
    val bulletFour: String = "make your final declaration by 31 January after the end of the tax year"

    val paraThree: String = "You’re also agreeing that our new penalties (opens in new tab) will apply if you miss deadlines for submitting your tax return or paying your bill. We’ll write to you when you’re liable for these penalties."

    val subHeading: String = "Opting out"
    val paraFour: String = "Making Tax Digital for Income Tax is voluntary until 6 April 2026. You can opt out of sending quarterly updates. But if we’ve told you that you’re liable for our new penalties, you’ll continue to be liable for them."
    val paraFive: String = "From 6 April 2026, some people will need to use Making Tax Digital for Income Tax. They will not be able to opt out. We’ll write to you if this applies to you."
  }

  object PrePopMessages {
    val title: String = "What you are agreeing to"
    val heading: String = "What you are agreeing to"

    object Para1 {
      val currentTaxYear: String = "If you continue to sign up, you’re agreeing to meet your tax obligations using Making Tax Digital for Income Tax. You’ll need to:"
      val nextTaxYear: String = "If you continue to sign up, you need to submit your Self Assessment tax return as normal for the current tax year."
    }
    object Para2 {
      val nextTaxYearStartYear: Int = AccountingPeriodUtil.getCurrentTaxEndYear
      val should: String = s"From 6 April $nextTaxYearStartYear, you should:"
      val must: String = s"From 6 April $nextTaxYearStartYear, you must:"
    }

    object BulletOne {
      val hasSoftware: String = "use software that works with Making Tax Digital for Income Tax"
      val noSoftware: String = "find and use software that works with Making Tax Digital for Income Tax (opens in new tab)"
      val noSoftwareHref: String = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
    }

    val bullet2: String = "keep digital records of your business income and expenses"
    val bullet3: String = "use software to send us quarterly updates"
    val bullet4CTY: String = "send any missed quarterly updates for the current tax year"
    val bullet5: String = "make your final declaration by 31 January after the end of each tax year"

    val para3: String = "You’re also agreeing that our new penalties (opens in new tab) will apply if you miss deadlines for submitting your tax return or paying your bill."
    val para3Href: String = "https://www.gov.uk/guidance/penalties-for-income-tax-self-assessment-volunteers"

    val para4: String = "While you’re taking part voluntarily, you will not get penalties for missed quarterly updates."


    val subHeading: String = "Opting out"
    val para5: String = "Making Tax Digital for Income Tax is voluntary until 6 April 2026. You can opt out of sending quarterly updates. But if we’ve told you that our new penalties apply to you, that will continue."
    val para6: String = "From 6 April 2026, some people will need to use Making Tax Digital for Income Tax. They will not be able to opt out. We’ll write to you if this applies to you."

    val acceptAndContinue: String = "Accept and continue"
  }


  object PrePopScenarios{
    def docHasSoftwareAndCTY: Document = documentPrePop(isUsingSoftware = true, signUpNextTaxYear = false)
    def docHasSoftwareAndNTY: Document = documentPrePop(isUsingSoftware = true, signUpNextTaxYear = true)

    def docHasSoftwareAndCTYMandated: Document = documentPrePop(isUsingSoftware = true, signUpNextTaxYear = false, currentYearMandated = true)
    def docHasSoftwareAndNTYMandated: Document = documentPrePop(isUsingSoftware = true, signUpNextTaxYear = true, nextYearMandated = true)

    def docNoSoftwareAndCTY: Document = documentPrePop(isUsingSoftware = false, signUpNextTaxYear = false)
    def docNoSoftwareAndNTY: Document = documentPrePop(isUsingSoftware = false, signUpNextTaxYear = true)

    def docNoSoftwareAndCTYMandated: Document = documentPrePop(isUsingSoftware = false, signUpNextTaxYear = false, currentYearMandated = true)
    def docNoSoftwareAndNTYMandated: Document = documentPrePop(isUsingSoftware = false, signUpNextTaxYear = true, nextYearMandated = true)

    def allScenarios: List[Document] = List(
      docHasSoftwareAndCTY,
      docHasSoftwareAndNTY,
      docHasSoftwareAndCTYMandated,
      docHasSoftwareAndNTYMandated,
      docNoSoftwareAndCTY,
      docNoSoftwareAndNTY,
      docNoSoftwareAndCTYMandated,
      docNoSoftwareAndNTYMandated)

    def hasSoftwareScenarios: List[Document] = List(
      docHasSoftwareAndCTY,
      docHasSoftwareAndNTY,
      docHasSoftwareAndCTYMandated,
      docHasSoftwareAndNTYMandated)

    def noSoftwareScenarios: List[Document] = List(
      docNoSoftwareAndCTY,
      docNoSoftwareAndNTY,
      docNoSoftwareAndCTYMandated,
      docNoSoftwareAndNTYMandated)

    def allCTYScenarios: List[Document] = List(
      docHasSoftwareAndCTY,
      docHasSoftwareAndCTYMandated,
      docNoSoftwareAndCTY,
      docNoSoftwareAndCTYMandated)

    def allNTYScenarios: List[Document] = List(
      docHasSoftwareAndNTY,
      docHasSoftwareAndNTYMandated,
      docNoSoftwareAndNTY,
      docNoSoftwareAndNTYMandated)

    def voluntaryCTYScenarios: List[Document] = List(docHasSoftwareAndCTY, docNoSoftwareAndCTY)

    def voluntaryNTYScenarios: List[Document] = List(docHasSoftwareAndNTY, docNoSoftwareAndNTY)

    def mandatedCTYScenarios: List[Document] = List(docHasSoftwareAndCTYMandated, docNoSoftwareAndCTYMandated)

    def mandatedNTYScenarios: List[Document] = List(docHasSoftwareAndNTYMandated, docNoSoftwareAndNTYMandated)
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

    "has a sub heading" in {
      document(false).mainContent.selectHead("h2").text mustBe WhatYouNeedToDoMessages.subHeading
    }

    "have a third paragraph" in {
      document(false).mainContent.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.paraThree
    }

    "have a fourth paragraph" in {
      document(false).mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.paraFour
    }
  }

  "WhatYouNeedToDoPrePop" must {

    "use the correct template details" when {
      "the user has compatible software and signs up for the current tax year" in new TemplateViewTest(
        view = pagePrePop(isUsingSoftware = true, signUpNextTaxYear = false, currentYearMandated = false, nextYearMandated = false),
        title = WhatYouNeedToDoMessages.title,
        isAgent = false,
        backLink = None,
        hasSignOutLink = true
      )
      "the user has compatible software and signs up for the next tax year" in new TemplateViewTest(
        view = pagePrePop(isUsingSoftware = true, signUpNextTaxYear = true, currentYearMandated = false, nextYearMandated = false),
        title = WhatYouNeedToDoMessages.title,
        isAgent = false,
        backLink = None,
        hasSignOutLink = true
      )
      "the user does not have compatible software and signs up for the current tax year" in new TemplateViewTest(
        view = pagePrePop(isUsingSoftware = false, signUpNextTaxYear = false, currentYearMandated = false, nextYearMandated = false),
        title = WhatYouNeedToDoMessages.title,
        isAgent = false,
        backLink = None,
        hasSignOutLink = true
      )
      "the user does not have compatible software and signs up for the next tax year" in new TemplateViewTest(
        view = pagePrePop(isUsingSoftware = false, signUpNextTaxYear = true, currentYearMandated = false, nextYearMandated = false),
        title = WhatYouNeedToDoMessages.title,
        isAgent = false,
        backLink = None,
        hasSignOutLink = true
      )
    }

    "have a page heading" in {
      enable(PrePopulate)
      PrePopScenarios.allScenarios.foreach(_.mainContent.selectHead("h1").text mustBe PrePopMessages.heading)
    }

    "have the correct fist paragraph" when {
      "the user signs up for the current tax year" in {
        enable(PrePopulate)
        PrePopScenarios.allCTYScenarios.foreach(_.mainContent.selectNth("p", 1).text mustBe PrePopMessages.Para1.currentTaxYear)
      }
      "the user signs up for the next tax year" in {
        enable(PrePopulate)
        PrePopScenarios.allNTYScenarios.foreach(_.mainContent.selectNth("p",1).text mustBe PrePopMessages.Para1.nextTaxYear)
      }
    }

    "have a second paragraph" when {
      "the user signs up for the next tax year voluntarily" in {
        enable(PrePopulate)
        PrePopScenarios.voluntaryNTYScenarios.foreach(_.mainContent.selectNth("p", 2).text mustBe PrePopMessages.Para2.should)
      }
      "the user signs up for the next tax year mandated" in {
        enable(PrePopulate)
        PrePopScenarios.mandatedNTYScenarios.foreach(_.mainContent.selectNth("p", 2).text mustBe PrePopMessages.Para2.must)
      }
    }

    "have a bullet list" which {
      "has the correct first point" when {
        "the user has compatible software" in {
          enable(PrePopulate)
          PrePopScenarios.hasSoftwareScenarios.foreach(_.mainContent.selectNth("li", 1).text mustBe PrePopMessages.BulletOne.hasSoftware)
        }
        "the user does not have compatible software" in {
          enable(PrePopulate)
          PrePopScenarios.noSoftwareScenarios foreach { doc =>
            doc.mainContent.selectNth("li", 1).text mustBe PrePopMessages.BulletOne.noSoftware
            doc.mainContent.selectNth("li", 1).selectHead("a").attr("href") mustBe PrePopMessages.BulletOne.noSoftwareHref
          }
        }
      }
      "has a second point" in {
        enable(PrePopulate)
        PrePopScenarios.allScenarios.foreach(_.mainContent.selectNth("li", 2).text mustBe PrePopMessages.bullet2)
      }
      "has a third point" in {
        enable(PrePopulate)
        PrePopScenarios.allScenarios.foreach(_.mainContent.selectNth("li", 3).text mustBe PrePopMessages.bullet3)
      }
      "has a fourth point" when {
        "the user signs up for the current tax year" in {
          enable(PrePopulate)
          PrePopScenarios.allCTYScenarios.foreach(_.mainContent.selectNth("li", 4).text mustBe PrePopMessages.bullet4CTY)
        }
        "the user signs up for the next tax year" in {
          enable(PrePopulate)
          PrePopScenarios.allNTYScenarios.foreach(_.mainContent.selectNth("li", 4).text mustBe PrePopMessages.bullet5)
        }
      }
      "has a fifth point" when {
        "the user signs up for the current tax year" in {
          enable(PrePopulate)
          PrePopScenarios.allCTYScenarios.foreach(_.mainContent.selectNth("li", 5).text mustBe PrePopMessages.bullet5)
        }
      }
    }

    "have the correct third paragraph" in {
      enable(PrePopulate)
      PrePopScenarios.allCTYScenarios foreach { doc =>
        doc.mainContent.selectNth("p", 2).text mustBe PrePopMessages.para3
        doc.mainContent.selectNth("p", 2).selectHead("a").attr("href") mustBe PrePopMessages.para3Href
      }
      PrePopScenarios.allNTYScenarios foreach { doc =>
        doc.mainContent.selectNth("p", 3).text mustBe PrePopMessages.para3
        doc.mainContent.selectNth("p", 3).selectHead("a").attr("href") mustBe PrePopMessages.para3Href
      }
    }

    "have a fourth paragraph" when {
      "the user signs up voluntary" in {
        enable(PrePopulate)
        PrePopScenarios.voluntaryCTYScenarios.foreach(_.mainContent.selectNth("p", 3).text mustBe PrePopMessages.para4)
        PrePopScenarios.voluntaryNTYScenarios.foreach(_.mainContent.selectNth("p", 4).text mustBe PrePopMessages.para4)
      }
    }

    "have a subheading" in {
      enable(PrePopulate)
      PrePopScenarios.allScenarios.foreach(_.mainContent.selectNth("h2", 1).text mustBe PrePopMessages.subHeading)
    }

    "have a fifth paragraph" when {
      "the user signs up for voluntary tax year" in {
        enable(PrePopulate)
        PrePopScenarios.voluntaryCTYScenarios.foreach(_.mainContent.selectNth("p", 4).text mustBe PrePopMessages.para5)
        PrePopScenarios.voluntaryNTYScenarios.foreach(_.mainContent.selectNth("p", 5).text mustBe PrePopMessages.para5)
      }
      "the user signs up for mandated tax year" in {
        enable(PrePopulate)
        PrePopScenarios.mandatedCTYScenarios.foreach(_.mainContent.selectNth("p",3).text mustBe PrePopMessages.para5)
        PrePopScenarios.mandatedNTYScenarios.foreach(_.mainContent.selectNth("p",4).text mustBe PrePopMessages.para5)
      }
    }

    "have a sixth paragraph" when {
      "the user signs up for voluntary tax year" in {
        enable(PrePopulate)
        PrePopScenarios.voluntaryCTYScenarios.foreach(_.mainContent.selectNth("p", 5).text mustBe PrePopMessages.para6)
        PrePopScenarios.voluntaryNTYScenarios.foreach(_.mainContent.selectNth("p", 6).text mustBe PrePopMessages.para6)
      }
      "the user signs up for mandated tax year" in {
        enable(PrePopulate)
        PrePopScenarios.mandatedCTYScenarios.foreach(_.mainContent.selectNth("p", 4).text mustBe PrePopMessages.para6)
        PrePopScenarios.mandatedNTYScenarios.foreach(_.mainContent.selectNth("p", 5).text mustBe PrePopMessages.para6)
      }
    }

    "have a form" which {
      "has the correct attributes" in {
        enable(PrePopulate)
        PrePopScenarios.allScenarios.foreach(_.selectHead("form").attr("method") mustBe testCall.method)
        PrePopScenarios.allScenarios.foreach(_.selectHead("form").attr("action") mustBe testCall.url)
      }
      "has an accept and continue button to submit the form" in {
        enable(PrePopulate)
        PrePopScenarios.allScenarios.foreach(_.selectHead("form").selectHead("button").text mustBe PrePopMessages.acceptAndContinue)
      }
    }
  }
  "WhatYouNeedToDoVoluntaryNextYear" must {
    "use the correct template details" in new TemplateViewTest(
      view = pageVoluntaryNextYear(true),
      title = VoluntaryNextYearOnly.title,
      isAgent = false,
      backLink = None,
      hasSignOutLink = true
    )

    "have a page heading" in {
      documentVoluntaryNextYear(true).mainContent.selectHead("h1").text mustBe VoluntaryNextYearOnly.heading
    }

    "have a first paragraph" in {
      documentVoluntaryNextYear(true).mainContent.selectNth("p", 1).text mustBe VoluntaryNextYearOnly.paraOne
    }

    "have a second paragraph" in {
      documentVoluntaryNextYear(true).mainContent.selectNth("p", 2).text mustBe VoluntaryNextYearOnly.paraTwo
    }

    "has a numbered list" which {
      def numberedList: Element = documentVoluntaryNextYear(true).mainContent.selectHead("ol.govuk-list--bullet")

      "has a first point" in {
        numberedList.selectNth("li", 1).text mustBe VoluntaryNextYearOnly.bulletOne
      }

      "has a second point" in {
        numberedList.selectNth("li", 2).text mustBe VoluntaryNextYearOnly.bulletTwo
      }

      "has a third point" in {
        numberedList.selectNth("li", 3).text mustBe VoluntaryNextYearOnly.bulletThree
      }

      "has a fourth point" in {
        numberedList.selectNth("li", 4).text mustBe VoluntaryNextYearOnly.bulletFour
      }
    }

    "have a third paragraph" in {
      documentVoluntaryNextYear(true).mainContent.selectNth("p", 3).text mustBe VoluntaryNextYearOnly.paraThree
    }

    "has a sub heading" in {
      documentVoluntaryNextYear(true).mainContent.selectHead("h2").text mustBe VoluntaryNextYearOnly.subHeading
    }

    "have a fourth paragraph" in {
      documentVoluntaryNextYear(true).mainContent.selectNth("p", 4).text mustBe VoluntaryNextYearOnly.paraFour
    }

    "have a fifth paragraph" in {
      documentVoluntaryNextYear(true).mainContent.selectNth("p", 5).text mustBe VoluntaryNextYearOnly.paraFive
    }
  }

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
