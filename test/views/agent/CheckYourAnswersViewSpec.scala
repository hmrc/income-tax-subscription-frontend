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
import agent.assets.MessageLookup.{Summary => messages}
import models.common._
import models.{AgentSummary, Current, Next}
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Matchers._
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.TestModels.{testAgentSummaryData, testBusinessName}
import utilities.{AccountingPeriodUtil, ImplicitDateFormatter, ImplicitDateFormatterImpl, TestModels, UnitTestTrait}
import views.agent.helpers.SummaryIdConstants._

class CheckYourAnswersViewSpec extends UnitTestTrait with ImplicitDateFormatter {

  override val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]

  lazy val postAction: Call = controllers.agent.routes.CheckYourAnswersController.submit()
  lazy val backUrl: String = controllers.agent.routes.IncomeSourceController.show().url

  val testSelectedTaxYear: AccountingYearModel = TestModels.testSelectedTaxYearNext
  val testAccountingMethod: AccountingMethodModel = TestModels.testAccountingMethod
  val testAccountingPropertyModel: AccountingMethodPropertyModel = TestModels.testAccountingMethodProperty
  val testIncomeSource: IncomeSourceModel = TestModels.testAgentIncomeSourceBoth
  val testPropertyCommencementDate: PropertyCommencementDateModel = TestModels.testPropertyCommencementDateModel
  val testSummary: AgentSummary = customTestSummary()
  val dateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  def customTestSummary(selectedTaxYear: Option[AccountingYearModel] = testSelectedTaxYear,
                        accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                        propertyCommencementDate: Option[PropertyCommencementDateModel] = testPropertyCommencementDate): AgentSummary = AgentSummary(
    businessName = testBusinessName,
    selectedTaxYear = selectedTaxYear,
    accountingMethod = testAccountingMethod,
    propertyCommencementDate = testPropertyCommencementDate,
    accountingMethodProperty = accountingMethodProperty
  )

  def page(testSummaryModel: AgentSummary): HtmlFormat.Appendable = views.html.agent.check_your_answers(
    summaryModel = testSummaryModel,
    postAction = postAction,
    backUrl = backUrl,
    dateFormatter
  )(FakeRequest(), implicitly, appConfig)

  def document(testSummaryModel: AgentSummary = testAgentSummaryData): Document
  = page(testSummaryModel).doc

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

  "Summary page view" should {

    s"have a back button pointed to $backUrl" in {
      val backLink = document().select("#back")
      backLink.isEmpty shouldBe false
      backLink.attr("href") shouldBe backUrl
    }

    s"have the title '${messages.title}'" in {
      val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
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
        submit.text shouldBe MessageLookup.Summary.confirm_and_sign_up
      }

      s"has a post action to '${postAction.url}'" in {
        document().select("form").attr("action") mustBe postAction.url
        document().select("form").attr("method") mustBe "POST"
      }
    }


    def sectionTest(sectionId: String, expectedQuestion: String, expectedAnswer: String, expectedEditLink: Option[String],
                    testSummaryModel: AgentSummary = testSummary)(
      setupData: AgentSummary = testAgentSummaryData): Unit = {
      val question = document(setupData).getElementById(questionId(sectionId))
      val answer = document(setupData).getElementById(answerId(sectionId))
      val editLink = document(setupData).getElementById(editLinkId(sectionId))

      questionStyleCorrectness(question)
      answerStyleCorrectness(answer)
      if (expectedEditLink.nonEmpty) editLinkStyleCorrectness(editLink)

      question.text() shouldBe expectedQuestion
      answer.text() shouldBe expectedAnswer
      if (expectedEditLink.nonEmpty) {
        val link = editLink.select("a")
        link.attr("href") shouldBe expectedEditLink.get
        link.text() should include(MessageLookup.Base.change)
        link.select("span").text() shouldBe expectedQuestion
        link.select("span").hasClass("visuallyhidden") shouldBe true
      }
    }

    "display the correct info for the select tax year" when {

      "selected current tax year" in {
        val currentTaxYear: AccountingPeriodModel = AccountingPeriodUtil.getCurrentTaxYear
        val sectionId = SelectedTaxYearId
        val expectedQuestion = messages.selected_tax_year
        val expectedAnswer = messages.option1(currentTaxYear.startDate.year, currentTaxYear.endDate.year)
        val expectedEditLink = controllers.agent.business.routes.WhatYearToSignUpController.show(editMode = true).url

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = expectedEditLink
        )(setupData = customTestSummary(selectedTaxYear = Some(AccountingYearModel(Current))))
      }

      "selected next tax year" in {
        val nextTaxYear = AccountingPeriodUtil.getNextTaxYear

        val sectionId = SelectedTaxYearId
        val expectedQuestion = messages.selected_tax_year
        val expectedAnswer = messages.option2(nextTaxYear.startDate.year, nextTaxYear.endDate.year)
        val expectedEditLink = controllers.agent.business.routes.WhatYearToSignUpController.show(editMode = true).url

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = expectedEditLink
        )(setupData = customTestSummary(selectedTaxYear = Some(AccountingYearModel(Next))))
      }
    }

    "display the correct info for the income source" in {
      val sectionId = IncomeSourceId
      val expectedQuestion = messages.income_source
      val expectedAnswer = MessageLookup.Summary.IncomeSource.both
      val expectedEditLink = controllers.agent.routes.IncomeSourceController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )()
    }

    "display the correct info for the business name" in {
      val sectionId = BusinessNameId
      val expectedQuestion = messages.business_name
      val expectedAnswer = testBusinessName.businessName
      val expectedEditLink = controllers.agent.business.routes.BusinessNameController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )()
    }

    "display the correct info for the accounting method" in {
      val sectionId = AccountingMethodId
      val expectedQuestion = messages.income_type
      val expectedAnswer = messages.AccountingMethod.cash
      val expectedEditLink = controllers.agent.business.routes.BusinessAccountingMethodController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )()
    }

    "display the correct info for the property commencement date" in {
      val sectionId = PropertyCommencementDateId
      val expectedQuestion = messages.propertyCommencementDate
      val expectedAnswer = testPropertyCommencementDate.startDate.toLocalDate.toLongDate
      val expectedEditLink = controllers.agent.business.routes.PropertyCommencementDateController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink,
        testSummaryModel = customTestSummary(propertyCommencementDate = testPropertyCommencementDate)
      )()
    }

    "display the correct info for the property accounting method" in {
      val sectionId = AccountingMethodPropertyId
      val expectedQuestion = messages.income_type_property
      val expectedAnswer = messages.AccountingMethodProperty.cash
      val expectedEditLink = controllers.agent.business.routes.PropertyAccountingMethodController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink,
        testSummaryModel = customTestSummary(accountingMethodProperty = testAccountingPropertyModel)
      )()
    }
  }
}
