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

package views.individual.incometax.subscription

import assets.MessageLookup
import assets.MessageLookup.{Summary => messages}
import models.common.business._
import models.common._
import models.{DateModel, IndividualSummary}
import org.jsoup.nodes.{Document, Element}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.AccountingPeriodUtil.getCurrentTaxEndYear
import utilities.{ImplicitDateFormatter, ImplicitDateFormatterImpl, TestModels, UnitTestTrait}
import views.html.individual.incometax.subscription.CheckYourAnswers
import views.individual.helpers.SummaryIdConstants._

class CheckYourAnswersViewSpec extends UnitTestTrait with ImplicitDateFormatter  {

  override val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]

  val checkYourAnswers: CheckYourAnswers = app.injector.instanceOf[CheckYourAnswers]

  def selfEmploymentData(id: String): SelfEmploymentData = SelfEmploymentData(
    id = id,
    businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "2018"))),
    businessName = Some(BusinessNameModel(s"ABC Limited $id")),
    businessTradeName = Some(BusinessTradeNameModel(s"Plumbing $id")),
    businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), Some("TF2 1PF"))))
  )

  val testBusinessName: BusinessNameModel = BusinessNameModel("test business name")
  val testSelfEmployments: Seq[SelfEmploymentData] = Seq(selfEmploymentData("1"), selfEmploymentData("2"))
  val testSelectedTaxYear: AccountingYearModel = TestModels.testSelectedTaxYearNext
  val testAccountingMethod: AccountingMethodModel = TestModels.testAccountingMethod
  val testAccountingPropertyModel: AccountingMethodPropertyModel = TestModels.testAccountingMethodProperty
  val testOverseasAccountingPropertyModel: OverseasAccountingMethodPropertyModel = TestModels.testOverseasAccountingMethodProperty
  val testIncomeSourceBoth: IncomeSourceModel = TestModels.testIncomeSourceBoth
  val testPropertyStart: PropertyStartDateModel = TestModels.testPropertyStartDateModel
  val testOverseasPropertyStart: OverseasPropertyStartDateModel = TestModels.testOverseasPropertyStartDateModel
  val testSummary: IndividualSummary = customTestSummary()
  val dateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  def customTestSummary(
                         incomeSource: Option[IncomeSourceModel] = Some(testIncomeSourceBoth),
                         businessName: Option[BusinessNameModel] = Some(testBusinessName),
                         selfEmployments: Seq[SelfEmploymentData] = testSelfEmployments,
                         selectedTaxYear: Option[AccountingYearModel] = Some(testSelectedTaxYear),
                         accountingMethod: Option[AccountingMethodModel] = Some(testAccountingMethod),
                         propertyStartDate: Option[PropertyStartDateModel] = Some(testPropertyStart),
                         accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                         overseasPropertyStartDate: Option[OverseasPropertyStartDateModel] = Some(testOverseasPropertyStart),
                         overseasAccountingMethodProperty: Option[OverseasAccountingMethodPropertyModel] = Some(testOverseasAccountingPropertyModel)
                       ): IndividualSummary = IndividualSummary(
    incomeSource = incomeSource,
    businessName = businessName,
    selfEmployments = selfEmployments,
    selectedTaxYear = selectedTaxYear,
    accountingMethod = accountingMethod,
    accountingMethodProperty = accountingMethodProperty,
    propertyStartDate = propertyStartDate,
    overseasAccountingMethodProperty = overseasAccountingMethodProperty,
    overseasPropertyStartDate = overseasPropertyStartDate
  )

  lazy val postAction: Call = controllers.individual.subscription.routes.CheckYourAnswersController.submit
  lazy val backUrl: String = controllers.individual.subscription.routes.CheckYourAnswersController.show.url

  def page(testSummaryModel: IndividualSummary): HtmlFormat.Appendable =
    checkYourAnswers(
      summaryModel = testSummaryModel,
      postAction = postAction,
      backUrl = backUrl,
      dateFormatter
    )(FakeRequest(), implicitly, appConfig)

  def document(testSummaryModel: IndividualSummary = testSummary): Document =
    page(testSummaryModel).doc

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
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
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
                    testSummaryModel: IndividualSummary = testSummary): Unit = {
      val doc = document(testSummaryModel)
      val question = doc.getElementById(questionId(sectionId))
      val answer = doc.getElementById(answerId(sectionId))
      val editLink = doc.getElementById(editLinkId(sectionId))
      val hiddenContent = doc.getElementsByClass("govuk-visually-hidden").get(rowNo + 1).text()

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

    "display the correct info for the income source" in {
      val sectionId = IncomeSourceId
      val expectedQuestion = messages.income_source
      val expectedAnswer = messages.selfEmployment + " " + messages.ukProperty
      val expectedEditLink = controllers.individual.incomesource.routes.IncomeSourceController.show(editMode = true).url
      val expectedHiddenContent = "Change" + messages.income_source
      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = Some(expectedEditLink),
        rowNo = 2,
        expectedHiddenContent = Some(expectedHiddenContent)
      )
    }

    "display the correct info" should {
      "Self Employments is displayed" in {
        val sectionId = SelfEmploymentsId
        val expectedQuestion = messages.selfEmployments
        val expectedAnswer = "2"
        val expectedEditLink = appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/details/business-list"
        val expectedHiddenContent = "Change" + messages.selfEmployments

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = Some(expectedEditLink),
          rowNo = 3,
          expectedHiddenContent = Some(expectedHiddenContent)
        )
      }


      "display the correct info for the Selected Year" when {
        "selected year is current" in {
          val sectionId = SelectedTaxYearId
          val expectedQuestion = messages.selected_tax_year
          val expectedAnswer = messages.SelectedTaxYear.current(getCurrentTaxEndYear - 1, getCurrentTaxEndYear)
          val expectedEditLink = controllers.individual.business.routes.WhatYearToSignUpController.show(editMode = true).url
          val expectedHiddenContent = "Change" + messages.selected_tax_year

          sectionTest(
            sectionId = sectionId,
            expectedQuestion = expectedQuestion,
            expectedAnswer = expectedAnswer,
            expectedEditLink = Some(expectedEditLink),
            rowNo = 1,
            expectedHiddenContent = Some(expectedHiddenContent),
            testSummaryModel = customTestSummary(
              incomeSource = Some(TestModels.testIncomeSourceBusiness),
              selectedTaxYear = Some(TestModels.testSelectedTaxYearCurrent)
            )
          )
        }
        "selected year is next" in {
          val sectionId = SelectedTaxYearId
          val expectedQuestion = messages.selected_tax_year
          val expectedAnswer = messages.SelectedTaxYear.next(getCurrentTaxEndYear, getCurrentTaxEndYear + 1)
          val expectedEditLink = controllers.individual.business.routes.WhatYearToSignUpController.show(editMode = true).url
          val expectedHiddenContent = "Change" + messages.selected_tax_year
          sectionTest(
            sectionId = sectionId,
            expectedQuestion = expectedQuestion,
            expectedAnswer = expectedAnswer,
            expectedEditLink = Some(expectedEditLink),
            rowNo = 1,
            expectedHiddenContent = Some(expectedHiddenContent),
            testSummaryModel = customTestSummary(
              incomeSource = Some(TestModels.testIncomeSourceBusiness),
              selectedTaxYear = Some(TestModels.testSelectedTaxYearNext)
            )
          )
        }
      }

      "display the correct info for the accounting method" in {
        val sectionId = AccountingMethodId
        val expectedQuestion = messages.business_accountingmethod
        val expectedAnswer = messages.AccountingMethod.cash
        val expectedEditLink = appConfig.incomeTaxSelfEmploymentsFrontendBusinessAccountingMethodUrl + "?isEditMode=true"
        val expectedHiddenContent = "Change" + messages.business_accountingmethod

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = Some(expectedEditLink),
          rowNo = 4,
          expectedHiddenContent = Some(expectedHiddenContent)
        )
      }


      "display the correct info for the accounting method Property " in {
        val sectionId = AccountingMethodPropertyId
        val expectedQuestion = messages.accountingMethodProperty
        val expectedAnswer = messages.AccountingMethod.cash
        val expectedEditLink = controllers.individual.business.routes.PropertyAccountingMethodController.show(editMode = true).url
        val expectedHiddenContent = "Change" + messages.accountingMethodProperty
        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = Some(expectedEditLink),
          rowNo = 6,
          expectedHiddenContent = Some(expectedHiddenContent),
          testSummaryModel = customTestSummary(accountingMethodProperty = Some(testAccountingPropertyModel))
        )
      }


      "display the correct info for the Property Start Date" in {
        val sectionId = PropertyStartId
        val expectedQuestion = messages.propertyStart
        val expectedAnswer = testPropertyStart.startDate.toLocalDate.toLongDate
        val expectedEditLink = controllers.individual.business.routes.PropertyStartDateController.show(editMode = true).url
        val expectedHiddenContent = "Change" + messages.propertyStart

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = Some(expectedEditLink),
          rowNo = 5,
          expectedHiddenContent = Some(expectedHiddenContent),
          testSummaryModel = customTestSummary(propertyStartDate = Some(testPropertyStart))
        )
      }

      "display the correct info for the Overseas Property Business Start" in {
        val sectionId = OverseasPropertyStartId
        val expectedQuestion = messages.overseasPropertyStartDate
        val expectedAnswer = testPropertyStart.startDate.toLocalDate.toLongDate
        val expectedEditLink = controllers.individual.business.routes.OverseasPropertyStartDateController.show(editMode = true).url
        val expectedHiddenContent = "Change" + messages.overseasPropertyStartDate

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = Some(expectedEditLink),
          rowNo = 6,
          expectedHiddenContent = Some(expectedHiddenContent),
          testSummaryModel = customTestSummary(propertyStartDate = Some(testPropertyStart))
        )
      }

      "display the correct info for the accounting method Foreign Property " in {
        val sectionId = AccountingMethodForeignPropertyId
        val expectedQuestion = messages.accountingMethodForeignProperty
        val expectedAnswer = messages.AccountingMethod.cash
        val expectedEditLink = controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show(editMode = true).url
        val expectedHiddenContent = "Change" + messages.accountingMethodForeignProperty

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = Some(expectedEditLink),
          expectedHiddenContent = Some(expectedHiddenContent),
          rowNo = 8,
          testSummaryModel = customTestSummary(accountingMethodProperty = Some(testAccountingPropertyModel))
        )
      }
    }
  }
}
