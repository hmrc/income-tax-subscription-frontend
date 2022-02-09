/*
 * Copyright 2022 HM Revenue & Customs
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

package views.individual.usermatching

import assets.MessageLookup
import assets.MessageLookup.{ConfirmUser => messages}
import models.DateModel
import models.usermatching.UserDetailsModel
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Matchers._
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.individual.TestConstants
import utilities.{TestModels, UnitTestTrait}
import views.html.individual.usermatching.CheckYourUserDetails
import views.individual.helpers.ConfirmUserIdConstants._

class CheckYourUserDetailsViewSpec extends UnitTestTrait {

  val testFirstName = "Test"
  val testLastName = "User"
  val testNino: String = TestConstants.testNino
  val testDob: DateModel = TestModels.testStartDate
  val testUserDetails: UserDetailsModel = UserDetailsModel(
    testFirstName,
    testLastName,
    testNino,
    testDob)
  val checkYourUserDetails: CheckYourUserDetails = app.injector.instanceOf[CheckYourUserDetails]
  implicit val request: Request[_] = FakeRequest()

  lazy val postAction: Call = controllers.usermatching.routes.ConfirmUserController.submit()
  lazy val backUrl: String = controllers.usermatching.routes.ConfirmUserController.show().url
  val expectedEditLink: String = controllers.usermatching.routes.UserDetailsController.show(editMode = true).url

  def page(): HtmlFormat.Appendable = checkYourUserDetails(
    userDetailsModel = testUserDetails,
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

  "Confirm User page view" should {

    s"have a back button pointed to $backUrl" in {
      val backLink = document().select(".govuk-back-link")
      backLink.isEmpty shouldBe false
      backLink.attr("href") shouldBe backUrl
    }
    s"have the title '${messages.title}'" in {
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      document().title() mustBe messages.title + serviceNameGovUk
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document().select("h1").text() mustBe messages.heading
    }

    "has a form" which {

      "has a submit button" in {
        val submit = document().getElementsByClass("govuk-button")
        submit.isEmpty mustBe false
        submit.text shouldBe MessageLookup.Base.continue
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
      val question = document().getElementById(questionId(sectionId))
      val answer = document().getElementById(answerId(sectionId))
      val editLink = document().getElementById(editLinkId(sectionId))
      val hiddenContent = document.getElementsByClass("govuk-visually-hidden").get(rowNo).text()

      questionStyleCorrectness(question)
      answerStyleCorrectness(answer)
      if (expectedEditLink.nonEmpty) editLinkStyleCorrectness(editLink)

      question.text() shouldBe expectedQuestion
      answer.text() shouldBe expectedAnswer
      if (expectedEditLink.nonEmpty) {
        val link = editLink.select("a")
        link.attr("href") should include(expectedEditLink.get)
        link.text() should include(MessageLookup.Base.change)
        link.select(".govuk-visually-hidden").get(0).text() shouldBe hiddenContent
      }
    }

    "display the correct info for firstName" in {
      val sectionId = FirstNameId
      val expectedQuestion = messages.firstName
      val expectedAnswer = testFirstName
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
