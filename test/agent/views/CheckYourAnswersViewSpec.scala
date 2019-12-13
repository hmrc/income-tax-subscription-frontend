/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.views

import _root_.agent.models.enums.{AccountingPeriodViewType, CurrentAccountingPeriodView}
import _root_.agent.views.html.helpers.SummaryIdConstants._
import _root_.core.utils.{TestModels, UnitTestTrait}
import agent.assets.MessageLookup
import agent.assets.MessageLookup.{Summary => messages}
import core.models._
import core.utils.TestModels.{testAccountingPeriod, testAgentSummaryData, testBusinessName}
import incometax.business.models.address.Address
import incometax.business.models._
import incometax.incomesource.models.{AreYouSelfEmployedModel, RentUkPropertyModel}
import incometax.subscription.models.{AgentSummary, IncomeSourceType}
import incometax.util.AccountingPeriodUtil
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Matchers._
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.Html


class CheckYourAnswersViewSpec extends UnitTestTrait {

  lazy val postAction: Call = _root_.agent.controllers.routes.CheckYourAnswersController.submit()
  lazy val backUrl: String = _root_.agent.controllers.routes.TermsController.show().url

  val testBusinessPhoneNumber: BusinessPhoneNumberModel = TestModels.testBusinessPhoneNumber
  val testBusinessStartDate: BusinessStartDateModel = TestModels.testBusinessStartDate
  val testBusinessAddress: Address = TestModels.testAddress
  val testSelectedTaxYear: AccountingYearModel = TestModels.testSelectedTaxYearNext
  val testAccountingMethod: AccountingMethodModel = TestModels.testAccountingMethod
  val testAccountingPropertyModel: AccountingMethodPropertyModel = TestModels.testAccountingMethodProperty
  val testIncomeSource: IncomeSourceType = TestModels.testIncomeSourceBoth
  val testRentUkProperty: RentUkPropertyModel = TestModels.testRentUkProperty_property_and_other
  val testAreYouSelfEmployed: AreYouSelfEmployedModel = TestModels.testAreYouSelfEmployed_yes
  val testOtherIncome: YesNo = No
  val testSummary = customTestSummary()

  def customTestSummary(matchTaxYear: Option[MatchTaxYearModel] = TestModels.testMatchTaxYearNo,
                        accountingPeriod: Option[AccountingPeriodModel] = testAccountingPeriod,
                        selectedTaxYear: Option[AccountingYearModel] = testSelectedTaxYear,
                        accountingMethodProperty: Option[AccountingMethodPropertyModel] = None) = AgentSummary(
    otherIncome = testOtherIncome,
    matchTaxYear = matchTaxYear,
    accountingPeriodDate = accountingPeriod,
    businessName = testBusinessName,
    businessAddress = testBusinessAddress,
    businessStartDate = testBusinessStartDate,
    businessPhoneNumber = testBusinessPhoneNumber,
    selectedTaxYear = selectedTaxYear,
    accountingMethod = testAccountingMethod,
    accountingMethodProperty = accountingMethodProperty
  )

  def page(testSummaryModel: AgentSummary): Html = _root_.agent.views.html.check_your_answers(
    summaryModel = testSummaryModel,
    postAction = postAction,
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)

  def document(testSummaryModel: AgentSummary = testAgentSummaryData): Document
    = page(testSummaryModel).doc

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

    s"have a back button pointed to $backUrl" in {
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


    def sectionTest(sectionId: String, expectedQuestion: String, expectedAnswer: String, expectedEditLink: Option[String])(setupData: AgentSummary = testAgentSummaryData) = {
      val accountingPeriod = document(setupData).getElementById(sectionId)
      val question = document(setupData).getElementById(questionId(sectionId))
      val answer = document(setupData).getElementById(answerId(sectionId))
      val editLink = document(setupData).getElementById(editLinkId(sectionId))

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

    "display the correct info for the accounting period date" when {

      "do not display if the user chooses yes to match tax year" in {
        val sectionId = AccountingPeriodDateId
        val doc = document(testSummaryModel = customTestSummary(matchTaxYear = Some(TestModels.testMatchTaxYearYes), accountingPeriod = None))
        doc.getElementById(sectionId) mustBe null

        val doc2 = document(testSummaryModel = customTestSummary(matchTaxYear = Some(TestModels.testMatchTaxYearYes), accountingPeriod = Some(testAccountingPeriod)))
        doc2.getElementById(sectionId) mustBe null
      }
      "the user chooses no to match tax year" in {
        val sectionId = AccountingPeriodDateId
        val expectedQuestion = messages.accounting_period
        val periodInMonth = testAccountingPeriod.startDate.diffInMonth(testAccountingPeriod.endDate)
        val expectedAnswer = s"${testAccountingPeriod.startDate.toOutputDateFormat} to ${testAccountingPeriod.endDate.toOutputDateFormat}"
        val expectedEditLink = _root_.agent.controllers.business.routes.BusinessAccountingPeriodDateController.show(editMode = true).url

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = expectedEditLink
        )()

      }
    }

    "display the correct info for the match tax year" in {
      val sectionId = MatchTaxYearId
      val expectedQuestion = messages.match_tax_year
      val expectedAnswer = MessageLookup.Business.MatchTaxYear.no
      val expectedEditLink = _root_.agent.controllers.business.routes.MatchTaxYearController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )()
    }

    "display the correct info for the select tax year" when {

      "selected current tax year" in {
        val currentTaxYear: AccountingPeriodModel = AccountingPeriodUtil.getCurrentTaxYear

        val sectionId = SelectedTaxYearId
        val expectedQuestion = messages.selected_tax_year
        val expectedAnswer = MessageLookup.Business.WhatYearToSignUp.option1(currentTaxYear.startDate.year, currentTaxYear.endDate.year)
        val expectedEditLink = _root_.agent.controllers.business.routes.WhatYearToSignUpController.show(editMode = true).url

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = expectedEditLink
        )(setupData = customTestSummary(matchTaxYear = Some(MatchTaxYearModel(Yes)),
          selectedTaxYear = Some(AccountingYearModel(Current))))
      }

      "selected next tax year" in {
        val nextTaxYear = AccountingPeriodUtil.getNextTaxYear

        val sectionId = SelectedTaxYearId
        val expectedQuestion = messages.selected_tax_year
        val expectedAnswer = MessageLookup.Business.WhatYearToSignUp.option2(nextTaxYear.startDate.year, nextTaxYear.endDate.year)
        val expectedEditLink = _root_.agent.controllers.business.routes.WhatYearToSignUpController.show(editMode = true).url

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = expectedEditLink
        )(setupData = customTestSummary(matchTaxYear = Some(MatchTaxYearModel(Yes)),
          selectedTaxYear = Some(AccountingYearModel(Next))))
      }
    }

    "display the correct info for the income source" in {
      val sectionId = IncomeSourceId
      val expectedQuestion = messages.income_source
      val expectedAnswer = MessageLookup.Summary.IncomeSource.both
      val expectedEditLink = _root_.agent.controllers.routes.IncomeSourceController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )()
    }

    "display the correct info for other income" in {
      val sectionId = OtherIncomeId
      val expectedQuestion = messages.other_income
      val expectedAnswer = MessageLookup.OtherIncome.no
      val expectedEditLink = _root_.agent.controllers.routes.OtherIncomeController.show(editMode = true).url

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
      val expectedEditLink = _root_.agent.controllers.business.routes.BusinessNameController.show(editMode = true).url

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
      val expectedEditLink = _root_.agent.controllers.business.routes.BusinessAccountingMethodController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )()
    }

    "display the correct info for the property accounting method" in {
      val sectionId = AccountingMethodPropertyId
      val expectedQuestion = messages.income_type_property
      val expectedAnswer = messages.AccountingMethodProperty.cash
      val expectedEditLink = _root_.agent.controllers.business.routes.PropertyAccountingMethodController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )()
    }

  }

}
