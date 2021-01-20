/*
 * Copyright 2021 HM Revenue & Customs
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
import models.DateModel
import models.usermatching.UserDetailsModel
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Matchers._
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.agent.TestConstants
import utilities.{TestModels, UnitTestTrait}
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
  )(FakeRequest(), implicitly, appConfig)

  def document(): Document = page().doc

  val questionId: String => String = (sectionId: String) => s"$sectionId-question"
  val answerId: String => String = (sectionId: String) => s"$sectionId-answer"
  val editLinkId: String => String = (sectionId: String) => s"$sectionId-edit"

  def questionStyleCorrectness(section: Element): Unit = {
    section.attr("class") shouldBe "govuk-summary-list__key"
  }

  def answerStyleCorrectness(section: Element): Unit = {
    section.attr("class") shouldBe "govuk-summary-list__value"
  }

  def editLinkStyleCorrectness(section: Element): Unit = {
    section.attr("class") shouldBe "govuk-summary-list__actions"
  }

  "Confirm Client page view" should {

    s"have a back buttong pointed to $backUrl" in {
      val backLink = document().select("#back")
      backLink.isEmpty shouldBe false
      backLink.attr("href") shouldBe backUrl
    }

    s"have the title '${messages.title}'" in {
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      document().title() mustBe messages.title + serviceNameGovUk
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document().select("h1").text() must include(messages.heading)
    }

    s"have visually hidden text as part of the (H1) '${messages.heading_hidden}'" in {
      document().select("h1 span").text() must include(messages.heading_hidden)
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

    def sectionTest(sectionId: String,
                    expectedQuestion: String,
                    expectedAnswer: String,
                    expectedEditLink: Option[String],
                    rowNo: Int,
                    expectedHiddenContent: Option[String]): Unit = {
      val accountingPeriod = document().getElementById(sectionId)
      val question = document().getElementById(questionId(sectionId))
      val answer = document().getElementById(answerId(sectionId))
      val editLink = document().getElementById(editLinkId(sectionId))
      val hiddenContent = document.getElementsByClass("visuallyhidden").get(rowNo).text()

      questionStyleCorrectness(question)
      answerStyleCorrectness(answer)
      if (expectedEditLink.nonEmpty) editLinkStyleCorrectness(editLink)

      question.text() shouldBe expectedQuestion
      answer.text() shouldBe expectedAnswer
      if (expectedEditLink.nonEmpty) {
        val link = editLink.select("a")
        link.attr("href") shouldBe expectedEditLink.get
        link.text() should include(MessageLookup.Base.change)
        link.select(".visuallyhidden").get(0).text() shouldBe hiddenContent
      }
    }

    "display the correct info for firstName" in {
      val sectionId = FirstNameId
      val expectedQuestion = messages.firstName
      val expectedAnswer = testFirstName
      val expectedEditLink = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url
      val expectedHiddenContent = "Change" + messages.firstName

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink,
        rowNo = 1,
        expectedHiddenContent = expectedHiddenContent
      )
    }

    "display the correct info for lastName" in {
      val sectionId = LastNameId
      val expectedQuestion = messages.lastName
      val expectedAnswer = testLastName
      val expectedEditLink = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url
      val expectedHiddenContent = "Change" + messages.lastName
      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink,
        rowNo = 2,
        expectedHiddenContent = expectedHiddenContent
      )
    }

    "display the correct info for nino" in {
      val sectionId = NinoId
      val expectedQuestion = messages.nino
      val expectedAnswer = testNino.toNinoDisplayFormat
      val expectedEditLink = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url
      val expectedHiddenContent = "Change" + messages.nino
      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink,
        rowNo = 3,
        expectedHiddenContent = expectedHiddenContent
      )
    }

    "display the correct info for dob" in {
      val sectionId = DobId
      val expectedQuestion = messages.dob
      val expectedAnswer = testDob.toCheckYourAnswersDateFormat
      val expectedEditLink = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url
      val expectedHiddenContent = "Change" + messages.dob
      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink,
        rowNo = 4,
        expectedHiddenContent = expectedHiddenContent
      )
    }

  }

}
