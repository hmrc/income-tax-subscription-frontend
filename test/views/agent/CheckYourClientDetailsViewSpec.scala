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

import agent.assets.MessageLookup
import agent.assets.MessageLookup.{Base => common}
import models.DateModel
import models.usermatching.UserDetailsModel
import models.usermatching.UserDetailsModel._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.agent.TestConstants
import utilities.{TestModels, ViewSpec}
import views.agent.helpers.ConfirmClientIdConstants._
import views.html.agent.CheckYourClientDetails

class CheckYourClientDetailsViewSpec extends ViewSpec {

  val testFirstName = "Test"
  val testLastName = "User"
  val testNino: String = TestConstants.testNino
  val testDob: DateModel = TestModels.testStartDateThisYear
  val testClientDetails = UserDetailsModel(
    testFirstName,
    testLastName,
    testNino,
    testDob)

  lazy val postAction: Call = controllers.agent.matching.routes.ConfirmClientController.submit()

  private val checkYourClientDetails = app.injector.instanceOf[CheckYourClientDetails]

  def page(): HtmlFormat.Appendable = checkYourClientDetails(
    userDetailsModel = testClientDetails,
    postAction = postAction
  )(FakeRequest(), implicitly, appConfig)

  def document(): Document = Jsoup.parse(page().body)

  val questionId: String => String = (sectionId: String) => s"$sectionId-question"
  val answerId: String => String = (sectionId: String) => s"$sectionId-answer"
  val editLinkId: String => String = (sectionId: String) => s"$sectionId-edit"

  def questionStyleCorrectness(section: Element): Unit = {
    section.attr("class") mustBe "govuk-summary-list__key"
  }

  def answerStyleCorrectness(section: Element): Unit = {
    section.attr("class") mustBe "govuk-summary-list__value"
  }

  def editLinkStyleCorrectness(section: Element): Unit = {
    section.attr("class") mustBe "govuk-summary-list__actions"
  }

  "Confirm Client page view" should {

    s"have the title '${ConfirmClient.title}'" in {
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      document().title() mustBe ConfirmClient.title + serviceNameGovUk
    }

    s"have the heading (H1) '${ConfirmClient.heading}'" in {
      document().select("h1").text() must include(ConfirmClient.heading)
    }

    "have a caption" in {
      document().select(".hmrc-page-heading p").text mustBe ConfirmClient.caption
    }

    "has a form" which {

      "has a submit button" in {
        val submit = Option(document().getElementById("continue-button"))
        submit.isEmpty mustBe false
        submit.get.text mustBe common.continue
      }

      s"has a post action to '${postAction.url}'" in {
        document().select("form").attr("action") mustBe postAction.url
        document().select("form").attr("method") mustBe "POST"
      }

    }

    "display the correct info for firstName" in {
      val sectionId = FirstNameId
      val expectedQuestion = ConfirmClient.firstName
      val expectedAnswer = testFirstName
      val expectedEditLink = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url
      val expectedHiddenContent = ConfirmClient.firstNameChangeLink

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink,
        expectedHiddenContent = expectedHiddenContent
      )
    }

    "display the correct info for lastName" in {
      val sectionId = LastNameId
      val expectedQuestion = ConfirmClient.lastName
      val expectedAnswer = testLastName
      val expectedEditLink = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url
      val expectedHiddenContent = ConfirmClient.lastNameChangeLink
      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink,
        expectedHiddenContent = expectedHiddenContent
      )
    }

    "display the correct info for nino" in {
      val sectionId = NinoId
      val expectedQuestion = ConfirmClient.nino
      val expectedAnswer = testNino.toNinoDisplayFormat
      val expectedEditLink = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url
      val expectedHiddenContent = ConfirmClient.ninoChangeLink
      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink,
        expectedHiddenContent = expectedHiddenContent
      )
    }

    "display the correct info for dob" in {
      val sectionId = DobId
      val expectedQuestion = ConfirmClient.dob
      val expectedAnswer = testDob.toCheckYourAnswersDateFormat
      val expectedEditLink = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url
      val expectedHiddenContent = ConfirmClient.dobChangeLink
      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink,
        expectedHiddenContent = expectedHiddenContent
      )
    }
  }

  private def sectionTest(sectionId: String,
                  expectedQuestion: String,
                  expectedAnswer: String,
                  expectedEditLink: String,
                  expectedHiddenContent: String): Unit = {
    val question = document().getElementById(questionId(sectionId))
    val answer = document().getElementById(answerId(sectionId))
    val editLink = document().getElementById(editLinkId(sectionId))

    questionStyleCorrectness(question)
    answerStyleCorrectness(answer)
    if (expectedEditLink.nonEmpty) editLinkStyleCorrectness(editLink)

    question.text() mustBe expectedQuestion
    answer.text() mustBe expectedAnswer
    val link = editLink.select(".govuk-link")
    link.attr("href") mustBe expectedEditLink
    link.text() must include(MessageLookup.Base.change)
    link.select(".govuk-visually-hidden").get(0).text() mustBe expectedHiddenContent
  }

  object ConfirmClient {
    val title = "Check your answers - client’s details"
    val heading = "Check your answers"
    val caption = "This section is Details you are signing up your client with"
    val firstName = "First name"
    val firstNameChangeLink = "Change first name"
    val lastName = "Last name"
    val lastNameChangeLink = "Change last name"
    val nino = "National Insurance number"
    val ninoChangeLink = "Change National Insurance number"
    val dob = "Date of birth"
    val dobChangeLink = "Change Date of Birth"
  }
}
