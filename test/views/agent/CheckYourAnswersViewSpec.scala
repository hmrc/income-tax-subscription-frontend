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

package views.agent

import agent.assets.MessageLookup
import agent.assets.MessageLookup.{Summary => messages}
import models.common._
import models.common.business._
import models.{AgentSummary, Current, DateModel, Next}
import org.jsoup.nodes.{Document, Element}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.TestModels.{testAgentSummaryData, testBusinessName}
import utilities._
import views.agent.helpers.SummaryIdConstants._
import views.html.agent.CheckYourAnswers

class CheckYourAnswersViewSpec extends UnitTestTrait with ImplicitDateFormatter {

  override val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]

  val checkYourAnswers: CheckYourAnswers = app.injector.instanceOf[CheckYourAnswers]

  lazy val postAction: Call = controllers.agent.routes.CheckYourAnswersController.submit
  lazy val backUrl: String = controllers.agent.routes.IncomeSourceController.show().url

  def selfEmploymentData(id: String): SelfEmploymentData = SelfEmploymentData(
    id = id,
    businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "2018"))),
    businessName = Some(BusinessNameModel(s"ABC Limited $id")),
    businessTradeName = Some(BusinessTradeNameModel(s"Plumbing $id")),
    businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), "TF2 1PF")))
  )

  val testSelectedTaxYear: AccountingYearModel = TestModels.testSelectedTaxYearNext
  val testAccountingMethod: AccountingMethodModel = TestModels.testAccountingMethod
  val testAccountingPropertyModel: AccountingMethodPropertyModel = TestModels.testAccountingMethodProperty
  val testIncomeSource: IncomeSourceModel = TestModels.testAgentIncomeSourceBusinessProperty
  val testPropertyStartDate: PropertyStartDateModel = TestModels.testPropertyStartDateModel
  val testOverseasPropertyStartDate: OverseasPropertyStartDateModel = TestModels.testOverseasPropertyStartDateModel
  val testSummary: AgentSummary = customTestSummary()
  val dateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  val testSelfEmployments: Seq[SelfEmploymentData] = Seq(selfEmploymentData("1"), selfEmploymentData("2"))

  def customTestSummary(selectedTaxYear: Option[AccountingYearModel] = Some(testSelectedTaxYear),
                        accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                        propertyStartDate: Option[PropertyStartDateModel] = Some(testPropertyStartDate),
                        overseasAccountingMethodProperty: Option[OverseasAccountingMethodPropertyModel] = None,
                        overseasPropertyStartDate: Option[OverseasPropertyStartDateModel] = Some(testOverseasPropertyStartDate)): AgentSummary = AgentSummary(
    businessName = Some(testBusinessName),
    selectedTaxYear = selectedTaxYear,
    selfEmployments = Some(testSelfEmployments),
    accountingMethod = Some(testAccountingMethod),
    propertyStartDate = propertyStartDate,
    accountingMethodProperty = accountingMethodProperty,
    overseasPropertyStartDate = overseasPropertyStartDate,
    overseasAccountingMethodProperty = overseasAccountingMethodProperty
  )


  def page(testSummaryModel: AgentSummary): HtmlFormat.Appendable = checkYourAnswers(
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
    section.attr("class") mustBe "govuk-summary-list__key"
  }

  def answerStyleCorrectness(section: Element): Unit = {
    section.attr("class") mustBe "govuk-summary-list__value"
  }

  def editLinkStyleCorrectness(section: Element): Unit = {
    section.attr("class") mustBe "govuk-summary-list__actions"
  }

  "Summary page view" should {

    s"have a back button pointed to $backUrl" in {
      val backLink = document().select(".govuk-back-link")
      backLink.isEmpty mustBe false
      backLink.attr("href") mustBe backUrl
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
        val submit = Option(document().select("button").last())
        submit.isEmpty mustBe false
        submit.get.text mustBe MessageLookup.Summary.confirm_and_sign_up
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
                    expectedHiddenContent: Option[String],
                    testSummaryModel: AgentSummary = testSummary)(
                     setupData: AgentSummary = testAgentSummaryData): Unit = {
      val question = document(setupData).getElementById(questionId(sectionId))
      val answer = document(setupData).getElementById(answerId(sectionId))
      val editLink = document(setupData).getElementById(editLinkId(sectionId))
      val hiddenContent = document(setupData).getElementsByClass("govuk-visually-hidden").get(rowNo + 1).text()
      questionStyleCorrectness(question)
      answerStyleCorrectness(answer)
      if (expectedEditLink.nonEmpty) editLinkStyleCorrectness(editLink)

      question.text() mustBe expectedQuestion
      answer.text() mustBe expectedAnswer
      if (expectedEditLink.nonEmpty) {
        val link = editLink.select("a")
        link.attr("href") mustBe expectedEditLink.get
        link.text() must include(MessageLookup.Base.change)
        link.select(".govuk-visually-hidden").get(0).text() mustBe hiddenContent
      }
    }

    "display the correct info for the select tax year" when {

      "selected current tax year" in {
        val currentTaxYear: AccountingPeriodModel = AccountingPeriodUtil.getCurrentTaxYear
        val sectionId = SelectedTaxYearId
        val expectedQuestion = messages.selected_tax_year
        val expectedAnswer = messages.option1(currentTaxYear.startDate.year, currentTaxYear.endDate.year)
        val expectedEditLink = controllers.agent.routes.WhatYearToSignUpController.show(editMode = true).url
        val expectedHiddenContent = "Change" + messages.selected_tax_year

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = Some(expectedEditLink),
          rowNo = 1,
          expectedHiddenContent = Some(expectedHiddenContent)
        )(setupData = customTestSummary(selectedTaxYear = Some(AccountingYearModel(Current))))
      }

      "selected next tax year" in {
        val nextTaxYear = AccountingPeriodUtil.getNextTaxYear

        val sectionId = SelectedTaxYearId
        val expectedQuestion = messages.selected_tax_year
        val expectedAnswer = messages.option2(nextTaxYear.startDate.year, nextTaxYear.endDate.year)
        val expectedEditLink = controllers.agent.routes.WhatYearToSignUpController.show(editMode = true).url
        val expectedHiddenContent = "Change" + messages.selected_tax_year

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = Some(expectedEditLink),
          rowNo = 1,
          expectedHiddenContent = Some(expectedHiddenContent)
        )(setupData = customTestSummary(selectedTaxYear = Some(AccountingYearModel(Next))))
      }
    }

    "display the correct info for the income sources" in {
      import MessageLookup.Summary.IncomeSource

      val sectionId = IncomeSourceId
      val expectedQuestion = messages.income_source
      val expectedAnswer = s"${IncomeSource.business} ${IncomeSource.property} ${IncomeSource.overseas_property}"
      val expectedEditLink = controllers.agent.routes.IncomeSourceController.show(editMode = true).url
      val expectedHiddenContent = "Change" + messages.income_source

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = Some(expectedEditLink),
        rowNo = 1,
        expectedHiddenContent = Some(expectedHiddenContent)
      )()
    }

    "display the correct info for the number of businesses and edit link" in {
      val sectionId = SelfEmploymentsId
      val expectedQuestion = messages.number_of_businesses
      val expectedAnswer = testSelfEmployments.size.toString
      val expectedEditLink = appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/client/details/business-list"
      val expectedHiddenContent = "Change" + messages.number_of_businesses

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = Some(expectedEditLink),
        rowNo = 2,
        expectedHiddenContent = Some(expectedHiddenContent)
      )(setupData = customTestSummary())
    }

    "display the correct info for the accounting method" in {
      val sectionId = AccountingMethodId
      val expectedQuestion = messages.business_accountingmethod
      val expectedAnswer = messages.AccountingMethod.cash
      val expectedEditLink = appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/client/details/business-accounting-method?isEditMode=true"
      val expectedHiddenContent = "Change" + messages.business_accountingmethod

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = Some(expectedEditLink),
        rowNo = 2,
        expectedHiddenContent = Some(expectedHiddenContent)
      )()
    }

    "display the correct info for the property commencement date" in {
      val sectionId = PropertyStartDateId
      val expectedQuestion = messages.propertyStartDate
      val expectedAnswer = testPropertyStartDate.startDate.toLocalDate.toLongDate
      val expectedEditLink = controllers.agent.business.routes.PropertyStartDateController.show(editMode = true).url
      val expectedHiddenContent = "Change" + messages.propertyStartDate
      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = Some(expectedEditLink),
        rowNo = 3,
        expectedHiddenContent = Some(expectedHiddenContent),
        testSummaryModel = customTestSummary(propertyStartDate = Some(testPropertyStartDate))
      )()
    }

    "display the correct info for the property accounting method" in {
      val sectionId = AccountingMethodPropertyId
      val expectedQuestion = messages.ukproperty__accountingmethod
      val expectedAnswer = messages.AccountingMethodProperty.cash
      val expectedEditLink = controllers.agent.business.routes.PropertyAccountingMethodController.show(editMode = true).url
      val expectedHiddenContent = "Change" + messages.ukproperty__accountingmethod

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = Some(expectedEditLink),
        rowNo = 4,
        expectedHiddenContent = Some(expectedHiddenContent),
        testSummaryModel = customTestSummary(accountingMethodProperty = Some(testAccountingPropertyModel))
      )()
    }

    "display the correct info for the Overseas property commencement date" in {
      val sectionId = OverseasPropertyStartDateId
      val expectedQuestion = messages.overseasPropertyStartDate
      val expectedAnswer = testOverseasPropertyStartDate.startDate.toLocalDate.toLongDate
      val expectedEditLink = controllers.agent.business.routes.OverseasPropertyStartDateController.show(editMode = true).url
      val expectedHiddenContent = "Change" + messages.overseasPropertyStartDate

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = Some(expectedEditLink),
        rowNo = 5,
        expectedHiddenContent = Some(expectedHiddenContent)
      )()
    }

    "display the correct info for the Overseas property accounting method" in {
      val sectionId = OverseasAccountingMethodPropertyId
      val expectedQuestion = messages.overseasproperty_accountingmethod
      val expectedAnswer = messages.AccountingMethodOverseasProperty.cash
      val expectedEditLink = controllers.agent.business.routes.OverseasPropertyAccountingMethodController.show(editMode = true).url
      val expectedHiddenContent = "Change" + messages.overseasproperty_accountingmethod

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = Some(expectedEditLink),
        rowNo = 6,
        expectedHiddenContent = Some(expectedHiddenContent)
      )()
    }
  }
}
