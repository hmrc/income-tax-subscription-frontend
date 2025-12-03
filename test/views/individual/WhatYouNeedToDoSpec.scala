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
import utilities.ViewSpec
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
    val title = "What penalties apply to you in Making Tax Digital for Income Tax"
    val heading = "What penalties apply to you in Making Tax Digital for Income Tax"
    val h2_1 = "If you are signing up voluntarily"
    val p1_1 = "You are agreeing that our new penalties"
    val p1_2 = "will apply if you are late sending your tax return or paying your tax bill."
    val p2 = "Whilst you are a volunteer, penalties will not apply for submitting quarterly updates late."
    val p3 = "You can opt out of Making Tax Digital For Income Tax at any time. If they do this, the new penalties will still apply."
    val h2_2 = "If you are required to use Making Tax Digital for Income Tax"
    val p4 = "The new penalties will apply to you if you are late sending a quarterly update, your tax return or you pay after the due date."
    val acceptAndContinue: String = "Accept and continue"
  }

  object PrePopScenarios{
    private def docHasSoftwareAndCTY: Document = documentPrePop(isUsingSoftware = true, signUpNextTaxYear = false)
    private def docHasSoftwareAndNTY: Document = documentPrePop(isUsingSoftware = true, signUpNextTaxYear = true)

    private def docHasSoftwareAndCTYMandated: Document = documentPrePop(isUsingSoftware = true, signUpNextTaxYear = false, currentYearMandated = true)
    private def docHasSoftwareAndNTYMandated: Document = documentPrePop(isUsingSoftware = true, signUpNextTaxYear = true, nextYearMandated = true)

    private def docNoSoftwareAndCTY: Document = documentPrePop(isUsingSoftware = false, signUpNextTaxYear = false)
    private def docNoSoftwareAndNTY: Document = documentPrePop(isUsingSoftware = false, signUpNextTaxYear = true)

    private def docNoSoftwareAndCTYMandated: Document = documentPrePop(isUsingSoftware = false, signUpNextTaxYear = false, currentYearMandated = true)
    private def docNoSoftwareAndNTYMandated: Document = documentPrePop(isUsingSoftware = false, signUpNextTaxYear = true, nextYearMandated = true)

    def allScenarios: List[Document] = List(
      docHasSoftwareAndCTY,
      docHasSoftwareAndNTY,
      docHasSoftwareAndCTYMandated,
      docHasSoftwareAndNTYMandated,
      docNoSoftwareAndCTY,
      docNoSoftwareAndNTY,
      docNoSoftwareAndCTYMandated,
      docNoSoftwareAndNTYMandated)
  }

  "WhatYouNeedToDo" must {
    "have a page heading" in {
      PrePopScenarios.allScenarios.foreach(_.mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMessages.heading)
    }

    "have a page first sub-heading" in {
      PrePopScenarios.allScenarios.foreach(_.mainContent.selectNth("h2", 1).text mustBe WhatYouNeedToDoMessages.h2_1)
    }

    "have the correct first paragraph" in {
      PrePopScenarios.allScenarios.foreach { d =>
        d.mainContent.selectNth("a", 1).text mustBe WhatYouNeedToDoMessages.p1_1
        d.mainContent.selectNth("p", 1).text mustBe Seq(WhatYouNeedToDoMessages.p1_1, WhatYouNeedToDoMessages.p1_2).mkString(" ")
      }
    }

    "have a second paragraph" when {
      PrePopScenarios.allScenarios.foreach(_.mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.p2)
    }

    "have the correct third paragraph" in {
      PrePopScenarios.allScenarios.foreach(_.mainContent.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.p3)
    }

    "have a page second sub-heading" in {
      PrePopScenarios.allScenarios.foreach(_.mainContent.selectNth("h2", 2).text mustBe WhatYouNeedToDoMessages.h2_2)
    }

    "have the correct fourth paragraph" in {
      PrePopScenarios.allScenarios.foreach(_.mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.p4)
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
