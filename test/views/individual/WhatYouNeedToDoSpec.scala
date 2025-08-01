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
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.WhatYouNeedToDo

class WhatYouNeedToDoSpec extends ViewSpec {

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
    val bullet5: String = "make your tax return by 31 January after the end of each tax year"

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
      PrePopScenarios.allScenarios.foreach(_.mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMessages.heading)
    }

    "have the correct fist paragraph" when {
      "the user signs up for the current tax year" in {
        PrePopScenarios.allCTYScenarios.foreach(_.mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.Para1.currentTaxYear)
      }
      "the user signs up for the next tax year" in {
        PrePopScenarios.allNTYScenarios.foreach(_.mainContent.selectNth("p",1).text mustBe WhatYouNeedToDoMessages.Para1.nextTaxYear)
      }
    }

    "have a second paragraph" when {
      "the user signs up for the next tax year voluntarily" in {
        PrePopScenarios.voluntaryNTYScenarios.foreach(_.mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.Para2.should)
      }
      "the user signs up for the next tax year mandated" in {
        PrePopScenarios.mandatedNTYScenarios.foreach(_.mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.Para2.must)
      }
    }

    "have a bullet list" which {
      "has the correct first point" when {
        "the user has compatible software" in {
          PrePopScenarios.hasSoftwareScenarios.foreach(_.mainContent.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.BulletOne.hasSoftware)
        }
        "the user does not have compatible software" in {
          PrePopScenarios.noSoftwareScenarios foreach { doc =>
            doc.mainContent.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.BulletOne.noSoftware
            doc.mainContent.selectNth("li", 1).selectHead("a").attr("href") mustBe WhatYouNeedToDoMessages.BulletOne.noSoftwareHref
          }
        }
      }
      "has a second point" in {
        PrePopScenarios.allScenarios.foreach(_.mainContent.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.bullet2)
      }
      "has a third point" in {
        PrePopScenarios.allScenarios.foreach(_.mainContent.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.bullet3)
      }
      "has a fourth point" when {
        "the user signs up for the current tax year" in {
          PrePopScenarios.allCTYScenarios.foreach(_.mainContent.selectNth("li", 4).text mustBe WhatYouNeedToDoMessages.bullet4CTY)
        }
        "the user signs up for the next tax year" in {
          PrePopScenarios.allNTYScenarios.foreach(_.mainContent.selectNth("li", 4).text mustBe WhatYouNeedToDoMessages.bullet5)
        }
      }
      "has a fifth point" when {
        "the user signs up for the current tax year" in {
          PrePopScenarios.allCTYScenarios.foreach(_.mainContent.selectNth("li", 5).text mustBe WhatYouNeedToDoMessages.bullet5)
        }
      }
    }

    "have the correct third paragraph" in {
      PrePopScenarios.allCTYScenarios foreach { doc =>
        doc.mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.para3
        doc.mainContent.selectNth("p", 2).selectHead("a").attr("href") mustBe WhatYouNeedToDoMessages.para3Href
      }
      PrePopScenarios.allNTYScenarios foreach { doc =>
        doc.mainContent.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.para3
        doc.mainContent.selectNth("p", 3).selectHead("a").attr("href") mustBe WhatYouNeedToDoMessages.para3Href
      }
    }

    "have a fourth paragraph" when {
      "the user signs up voluntary" in {
        PrePopScenarios.voluntaryCTYScenarios.foreach(_.mainContent.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.para4)
        PrePopScenarios.voluntaryNTYScenarios.foreach(_.mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.para4)
      }
    }

    "have a subheading" in {
      PrePopScenarios.allScenarios.foreach(_.mainContent.selectNth("h2", 1).text mustBe WhatYouNeedToDoMessages.subHeading)
    }

    "have a fifth paragraph" when {
      "the user signs up for voluntary tax year" in {
        PrePopScenarios.voluntaryCTYScenarios.foreach(_.mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.para5)
        PrePopScenarios.voluntaryNTYScenarios.foreach(_.mainContent.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.para5)
      }
      "the user signs up for mandated tax year" in {
        PrePopScenarios.mandatedCTYScenarios.foreach(_.mainContent.selectNth("p",3).text mustBe WhatYouNeedToDoMessages.para5)
        PrePopScenarios.mandatedNTYScenarios.foreach(_.mainContent.selectNth("p",4).text mustBe WhatYouNeedToDoMessages.para5)
      }
    }

    "have a sixth paragraph" when {
      "the user signs up for voluntary tax year" in {
        PrePopScenarios.voluntaryCTYScenarios.foreach(_.mainContent.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.para6)
        PrePopScenarios.voluntaryNTYScenarios.foreach(_.mainContent.selectNth("p", 6).text mustBe WhatYouNeedToDoMessages.para6)
      }
      "the user signs up for mandated tax year" in {
        PrePopScenarios.mandatedCTYScenarios.foreach(_.mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.para6)
        PrePopScenarios.mandatedNTYScenarios.foreach(_.mainContent.selectNth("p", 5).text mustBe WhatYouNeedToDoMessages.para6)
      }
    }

    "have a form" which {
      "has the correct attributes" in {
        PrePopScenarios.allScenarios.foreach(_.selectHead("form").attr("method") mustBe testCall.method)
        PrePopScenarios.allScenarios.foreach(_.selectHead("form").attr("action") mustBe testCall.url)
      }
      "has an accept and continue button to submit the form" in {
        PrePopScenarios.allScenarios.foreach(_.selectHead("form").selectHead("button").text mustBe WhatYouNeedToDoMessages.acceptAndContinue)
      }
    }
  }
}


