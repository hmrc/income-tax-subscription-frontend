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
import models.enums.{AccountingPeriodViewType, CurrentAccountingPeriodView}
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Matchers._
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.Html
import utils.{TestModels, UnitTestTrait}
import views.html.helpers.SummaryIdConstants._

class CheckYourAnswersViewSpec extends UnitTestTrait {

  val testAccountingPeriod = AccountingPeriodModel(DateModel("1", "4", "2017"), DateModel("1", "4", "2018"))
  val testBusinessName = BusinessNameModel("test business name")
  val testAccountingMethod: AccountingMethodModel = TestModels.testAccountingMethod
  val testTerms = TermModel(true)
  val testIncomeSource: IncomeSourceModel = TestModels.testIncomeSourceBoth
  val testOtherIncome: OtherIncomeModel = TestModels.testOtherIncomeNo
  val testSummary = SummaryModel(
    incomeSource = testIncomeSource,
    otherIncome = testOtherIncome,
    accountingPeriod = testAccountingPeriod,
    businessName = testBusinessName,
    accountingMethod = testAccountingMethod,
    terms = testTerms
  )

  lazy val postAction: Call = controllers.routes.CheckYourAnswersController.submit()
  lazy val backUrl: String = controllers.routes.TermsController.showTerms().url

  def page(accountingPeriodViewType: AccountingPeriodViewType = CurrentAccountingPeriodView): Html = views.html.check_your_answers(
    summaryModel = testSummary,
    postAction = postAction,
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)

  def document(accountingPeriodViewType: AccountingPeriodViewType = CurrentAccountingPeriodView): Document = page(accountingPeriodViewType).doc

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

    s"have a back buttong pointed to $backUrl" in {
      val backLink = document().select("#back")
      backLink.isEmpty shouldBe false
      backLink.attr("href") shouldBe backUrl
    }

    s"have the title '${messages.title}'" in {
      document().title() mustBe messages.title
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document().select("h1").text() must include (messages.heading)
    }

    s"have visually hidden text as part of the (H1) '${messages.heading_hidden}'" in {
      document().select("h1 span").text() must include (messages.heading_hidden)
    }

    s"have the secondary heading (H2) '${messages.h2}'" in {
      document().select("h2").text() must include(messages.h2)
    }

    "has a form" which {

      "has a submit button" in {
        val submit = document().getElementById("continue-button")
        submit.isEmpty mustBe false
        submit.text shouldBe MessageLookup.Summary.confirm_and_sign_up
      }

      s"has a post action to '${postAction.url}'" in {
        document().select("form").attr("action") mustBe postAction.url
        document().select("form").attr("method") mustBe "POST"
      }

    }

    def sectionTest(sectionId: String, expectedQuestion: String, expectedAnswer: String, expectedEditLink: Option[String]) = {
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
        editLink.text() should include (MessageLookup.Base.change)
        editLink.select("span").text() shouldBe expectedQuestion
        editLink.select("span").hasClass("visuallyhidden") shouldBe true
      }
    }

    "display the correct info for the accounting period date" in {
      val sectionId = AccountingPeriodDateId
      val expectedQuestion = messages.accounting_period
      val periodInMonth = testAccountingPeriod.startDate.diffInMonth(testAccountingPeriod.endDate)
      val expectedAnswer = s"${testAccountingPeriod.startDate.toOutputDateFormat} to ${testAccountingPeriod.endDate.toOutputDateFormat}"
      val expectedEditLink = controllers.business.routes.BusinessAccountingPeriodDateController.showAccountingPeriod(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for the income source" in {
      val sectionId = IncomeSourceId
      val expectedQuestion = messages.income_source
      val expectedAnswer = MessageLookup.Summary.IncomeSource.both
      val expectedEditLink = controllers.routes.IncomeSourceController.showIncomeSource(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for other income" in {
      val sectionId = OtherIncomeId
      val expectedQuestion = messages.other_income
      val expectedAnswer = MessageLookup.OtherIncome.no
      val expectedEditLink = controllers.routes.OtherIncomeController.showOtherIncome(editMode = true).url

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
      val expectedEditLink = controllers.business.routes.BusinessNameController.showBusinessName(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for the accounting method" in {
      val sectionId = AccountingMethodId
      val expectedQuestion = messages.income_type
      val expectedAnswer = messages.AccountingMethod.cash
      val expectedEditLink = controllers.business.routes.BusinessAccountingMethodController.show(editMode = true).url

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

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = None
      )
    }

  }

}
