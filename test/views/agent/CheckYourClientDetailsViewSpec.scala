/*
 * Copyright 2020 HM Revenue & Customs
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

import agent.assets.MessageLookup
import agent.assets.MessageLookup.{Base => common, ConfirmClient => messages}
import agent.utils.TestConstants
import core.utils.{TestModels, UnitTestTrait}
import models.DateModel
import models.usermatching.UserDetailsModel
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Matchers._
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.agent.helpers.ConfirmClientIdConstants._

class CheckYourClientDetailsViewSpec extends UnitTestTrait {

  val testFirstName = "Test"
  val testLastName = "User"
  val testNino: String = TestConstants.testNino
  val testDob: DateModel = TestModels.testStartDate
  val testClientDetails = UserDetailsModel(
    testFirstName,
    testLastName,
    testNino,
    testDob)

  lazy val postAction: Call = controllers.agent.matching.routes.ConfirmClientController.submit()
  lazy val backUrl: String = "testBackUrl"

  def page(): HtmlFormat.Appendable = views.html.agent.check_your_client_details(
    userDetailsModel = testClientDetails,
    postAction = postAction,
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)

  def document(): Document = page().doc

  val questionId: String => String = (sectionId: String) => s"$sectionId-question"
  val answerId: String => String = (sectionId: String) => s"$sectionId-answer"
  val editLinkId: String => String = (sectionId: String) => s"$sectionId-edit"

  def questionStyleCorrectness(section: Element): Unit = {
    section.attr("class") shouldBe "tabular-data__heading tabular-data__heading--label"
  }

  def answerStyleCorrectness(section: Element): Unit = {
    section.attr("class") shouldBe "tabular-data__data-1"
  }

  def editLinkStyleCorrectness(section: Element): Unit = {
    section.attr("class") shouldBe "tabular-data__data-2"
  }

  "Confirm Client page view" should {

    s"have a back buttong pointed to $backUrl" in {
      val backLink = document().select("#back")
      backLink.isEmpty shouldBe false
      backLink.attr("href") shouldBe backUrl
    }

    s"have the title '${messages.title}'" in {
      document().title() mustBe messages.title
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document().select("h1").text() must include(messages.heading)
    }

    s"have visually hidden text as part of the (H1) '${messages.heading_hidden}'" in {
      document().select("h1 span").text() must include(messages.heading_hidden)
    }

    s"have the secondary heading (H2) '${messages.h2}'" in {
      document().select("h2").text() must include(messages.h2)
    }

    "has a form" which {

      "has a submit button" in {
        val submit = document().getElementById("continue-button")
        submit.isEmpty mustBe false
        submit.text shouldBe common.continue
      }

      s"has a post action to '${postAction.url}'" in {
        document().select("form").attr("action") mustBe postAction.url
        document().select("form").attr("method") mustBe "POST"
      }

    }

    def sectionTest(sectionId: String, expectedQuestion: String, expectedAnswer: String, expectedEditLink: Option[String]): Unit = {
      val accountingPeriod = document().getElementById(sectionId)
      val question = document().getElementById(questionId(sectionId))
      val answer = document().getElementById(answerId(sectionId))
      val editLink = document().getElementById(editLinkId(sectionId))

      questionStyleCorrectness(question)
      answerStyleCorrectness(answer)
      if (expectedEditLink.nonEmpty) editLinkStyleCorrectness(editLink)

      question.text() shouldBe expectedQuestion
      answer.text() shouldBe expectedAnswer
      if (expectedEditLink.nonEmpty) {
        editLink.attr("href") shouldBe expectedEditLink.get
        editLink.text() should include(MessageLookup.Base.change)
        editLink.select("span").text() shouldBe expectedQuestion
        editLink.select("span").hasClass("visuallyhidden") shouldBe true
      }
    }

    "display the correct info for firstName" in {
      val sectionId = FirstNameId
      val expectedQuestion = messages.firstName
      val expectedAnswer = testFirstName
      val expectedEditLink = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for lastName" in {
      val sectionId = LastNameId
      val expectedQuestion = messages.lastName
      val expectedAnswer = testLastName
      val expectedEditLink = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for nino" in {
      val sectionId = NinoId
      val expectedQuestion = messages.nino
      val expectedAnswer = testNino.toNinoDisplayFormat
      val expectedEditLink = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for dob" in {
      val sectionId = DobId
      val expectedQuestion = messages.dob
      val expectedAnswer = testDob.toCheckYourAnswersDateFormat
      val expectedEditLink = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

  }

}
