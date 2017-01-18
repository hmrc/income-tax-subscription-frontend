/*
 * Copyright 2017 HM Revenue & Customs
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

package views
import assets.MessageLookup
import assets.MessageLookup.{Summary => messages}
import models._
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Matchers._
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.Html
import utils.UnitTestTrait
import views.html.helpers.SummaryIdConstants._

class SummaryPageViewSpec extends UnitTestTrait {

  val testAccountingPeriod = AccountingPeriodModel(DateModel("1", "4", "2017"), DateModel("1", "4", "2018"))
  val testBusinessName = BusinessNameModel("test business name")
  val testIncomeType = IncomeTypeModel("Cash")
  val testContactEmail = EmailModel("test@example.com")
  val testTerms = TermModel(true)
  val testSummary = SummaryModel(
    accountingPeriod = testAccountingPeriod,
    businessName = testBusinessName,
    incomeType = testIncomeType,
    contactEmail = testContactEmail,
    terms = testTerms
  )

  lazy val postAction: Call = controllers.routes.SummaryController.submitSummary()

  lazy val page: Html = views.html.summary_page(
    summaryModel = testSummary,
    postAction = postAction
  )(FakeRequest(), applicationMessages)

  lazy val document: Document = page.doc

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

  "Summary page view" should {

    s"have the title '${messages.title}'" in {
      document.title() mustBe messages.title
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.select("h1").text() mustBe messages.heading
    }

    "has a form" which {

      "has a submit button" in {
        val submit = document.getElementById("continue-button")
        submit.isEmpty mustBe false
        submit.text shouldBe MessageLookup.Base.submit
      }

      s"has a post action to '${postAction.url}'" in {
        document.select("form").attr("action") mustBe postAction.url
        document.select("form").attr("method") mustBe "POST"
      }

    }

    def sectionTest(sectionId: String, expectedQuestion: String, expectedAnswer: String, expectedEditLink: String) = {
      val accountingPeriod = document.getElementById(sectionId)
      val question = document.getElementById(questionId(sectionId))
      val answer = document.getElementById(answerId(sectionId))
      val editLink = document.getElementById(editLinkId(sectionId))

      questionStyleCorrectness(question)
      answerStyleCorrectness(answer)
      editLinkStyleCorrectness(editLink)

      question.text() shouldBe expectedQuestion
      answer.text() shouldBe expectedAnswer
      editLink.attr("href") shouldBe expectedEditLink
      editLink.text() shouldBe MessageLookup.Base.chage
    }

    "display the correct info for the accounting period" in {
      val sectionId = AccountingPeriodId
      val expectedQuestion = messages.accounting_period
      val periodInMonth = testAccountingPeriod.startDate.diffInMonth(testAccountingPeriod.endDate)
      val expectedAnswer = s"${testAccountingPeriod.startDate.toOutputDateFormat} to ${testAccountingPeriod.endDate.toOutputDateFormat} ${messages.accounting_period_month(periodInMonth)}"
      val expectedEditLink = controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for the business name" in {
      val sectionId = BusinessNameId
      val expectedQuestion = messages.business_name
      val expectedAnswer = testBusinessName.businessName
      val expectedEditLink = controllers.business.routes.BusinessNameController.showBusinessName().url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for the income type" in {
      val sectionId = IncomeTypeId
      val expectedQuestion = messages.income_type
      val expectedAnswer = testIncomeType.incomeType
      val expectedEditLink = controllers.business.routes.BusinessIncomeTypeController.showBusinessIncomeType().url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for the contact email" in {
      val sectionId = ContactEmailId
      val expectedQuestion = messages.contact_email
      val expectedAnswer = testContactEmail.emailAddress
      val expectedEditLink = controllers.routes.ContactEmailController.showContactEmail().url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for the terms" in {
      val sectionId = TermsId
      val expectedQuestion = messages.terms
      val expectedAnswer = messages.terms_agreed
      val expectedEditLink = controllers.routes.TermsController.showTerms().url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

  }

}
