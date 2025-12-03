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
import utilities.ViewSpec
import views.html.agent.WhatYouNeedToDo

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

      "have a page heading" in {
        allScenarios.foreach(_.mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMessages.heading)
      }

      "have a page first sub-heading" in {
        allScenarios.foreach(_.mainContent.selectNth("h2", 2).text mustBe WhatYouNeedToDoMessages.h2_1)
      }

      "have the correct first paragraph" in {
        allScenarios.foreach { d =>
          d.mainContent.selectNth("a", 1).text mustBe WhatYouNeedToDoMessages.p1_1
          d.mainContent.selectNth("p", 1).text mustBe Seq(WhatYouNeedToDoMessages.p1_1, WhatYouNeedToDoMessages.p1_2).mkString(" ")
        }
      }

      "have a second paragraph" when {
        allScenarios.foreach(_.mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.p2)
      }

      "have the correct third paragraph" in {
        allScenarios.foreach(_.mainContent.selectNth("p", 3).text mustBe WhatYouNeedToDoMessages.p3)
      }

      "have a page second sub-heading" in {
        allScenarios.foreach(_.mainContent.selectNth("h2", 3).text mustBe WhatYouNeedToDoMessages.h2_2)
      }

      "have the correct fourth paragraph" in {
        allScenarios.foreach(_.mainContent.selectNth("p", 4).text mustBe WhatYouNeedToDoMessages.p4)
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
    val title = "What penalties apply in Making Tax Digital for Income Tax"
    val heading = "What penalties apply in Making Tax Digital for Income Tax"
    val h2_1 = "If your client is signing up voluntarily"
    val p1_1 = "You and your client are agreeing that our new penalties"
    val p1_2 = "will apply if your client’s tax return is sent late, or their tax bill is paid late."
    val p2 = "Whilst your client is a volunteer, penalties will not apply for submitting quarterly updates late."
    val p3 = "Your client can opt out of Making Tax Digital For Income Tax at any time. If they do this, the new penalties will still apply."
    val h2_2 = "If your client is required to use Making Tax Digital for Income Tax"
    val p4 = "The new penalties will apply to your client if their quarterly update or tax return is sent late, or payment is made after the due date."
    val acceptAndContinue: String = "Accept and continue"
  }
}
