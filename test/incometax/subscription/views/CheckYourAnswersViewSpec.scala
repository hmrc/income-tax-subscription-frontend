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

package incometax.subscription.views

import assets.MessageLookup
import assets.MessageLookup.{Summary => messages}
import core.models.DateModel
import incometax.incomesource.models.{IncomeSourceModel, OtherIncomeModel}
import incometax.subscription.models.SummaryModel
import models._
import incometax.business.models.enums.{AccountingPeriodViewType, CurrentAccountingPeriodView}
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Matchers._
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.Html
import core.utils.{TestModels, UnitTestTrait}
import core.views.html.SummaryIdConstants._
import incometax.business.models._
import incometax.business.models.address.Address

class CheckYourAnswersViewSpec extends UnitTestTrait {

  val testAccountingPeriod = AccountingPeriodModel(DateModel("1", "4", "2017"), DateModel("1", "4", "2018"))
  val testBusinessName = BusinessNameModel("test business name")
  val testBusinessPhoneNumber: BusinessPhoneNumberModel = TestModels.testBusinessPhoneNumber
  val testBusinessStartDate: BusinessStartDateModel = TestModels.testBusinessStartDate
  val testBusinessAddress: Address = TestModels.testAddress
  val testAccountingMethod: AccountingMethodModel = TestModels.testAccountingMethod
  val testIncomeSource: IncomeSourceModel = TestModels.testIncomeSourceBoth
  val testOtherIncome: OtherIncomeModel = TestModels.testOtherIncomeNo
  val testSummary = SummaryModel(
    incomeSource = testIncomeSource,
    otherIncome = testOtherIncome,
    accountingPeriod = testAccountingPeriod,
    businessName = testBusinessName,
    businessAddress = testBusinessAddress,
    businessStartDate = testBusinessStartDate,
    businessPhoneNumber = testBusinessPhoneNumber,
    accountingMethod = testAccountingMethod
  )

  lazy val postAction: Call = incometax.subscription.controllers.routes.CheckYourAnswersController.submit()
  lazy val backUrl: String = incometax.subscription.controllers.routes.TermsController.showTerms().url

  def page(accountingPeriodViewType: AccountingPeriodViewType = CurrentAccountingPeriodView, isRegistration: Boolean): Html =
    incometax.subscription.views.html.check_your_answers(
      summaryModel = testSummary,
      isRegistration = isRegistration,
      postAction = postAction,
      backUrl = backUrl
    )(FakeRequest(), applicationMessages, appConfig)

  def document(accountingPeriodViewType: AccountingPeriodViewType = CurrentAccountingPeriodView, isRegistration: Boolean = false): Document =
    page(accountingPeriodViewType, isRegistration = isRegistration).doc

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
        submit.text shouldBe MessageLookup.Summary.confirm_and_sign_up
      }

      s"has a post action to '${postAction.url}'" in {
        document().select("form").attr("action") mustBe postAction.url
        document().select("form").attr("method") mustBe "POST"
      }

    }

    def sectionTest(sectionId: String, expectedQuestion: String, expectedAnswer: String, expectedEditLink: Option[String],
                    accountingPeriodViewType: AccountingPeriodViewType = CurrentAccountingPeriodView, isRegistration: Boolean = false): Unit = {
      val doc = document(accountingPeriodViewType, isRegistration)
      val accountingPeriod = doc.getElementById(sectionId)
      val question = doc.getElementById(questionId(sectionId))
      val answer = doc.getElementById(answerId(sectionId))
      val editLink = doc.getElementById(editLinkId(sectionId))

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

    "display the correct info for the accounting period date" when {
      "the user is on the sign up journey" in {
        val sectionId = AccountingPeriodDateId
        val expectedQuestion = messages.accounting_period
        val periodInMonth = testAccountingPeriod.startDate.diffInMonth(testAccountingPeriod.endDate)
        val expectedAnswer = s"${testAccountingPeriod.startDate.toOutputDateFormat} to ${testAccountingPeriod.endDate.toOutputDateFormat}"
        val expectedEditLink = incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true).url

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = expectedEditLink
        )
      }

      "the user is on the registration journey" in {
        val sectionId = AccountingPeriodDateId
        val expectedQuestion = messages.accounting_period_registration
        val periodInMonth = testAccountingPeriod.startDate.diffInMonth(testAccountingPeriod.endDate)
        val expectedAnswer = s"${testAccountingPeriod.startDate.toOutputDateFormat} to ${testAccountingPeriod.endDate.toOutputDateFormat}"
        val expectedEditLink = incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true).url

        sectionTest(
          sectionId = sectionId,
          expectedQuestion = expectedQuestion,
          expectedAnswer = expectedAnswer,
          expectedEditLink = expectedEditLink,
          isRegistration = true
        )
      }
    }

    "display the correct info for the income source" in {
      val sectionId = IncomeSourceId
      val expectedQuestion = messages.income_source
      val expectedAnswer = MessageLookup.Summary.IncomeSource.both
      val expectedEditLink = incometax.incomesource.controllers.routes.IncomeSourceController.showIncomeSource(editMode = true).url

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
      val expectedEditLink = incometax.incomesource.controllers.routes.OtherIncomeController.showOtherIncome(editMode = true).url

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
      val expectedEditLink = incometax.business.controllers.routes.BusinessNameController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for the business telephone" in {
      val sectionId = BusinessPhoneNumberId
      val expectedQuestion = messages.business_phone_number
      val expectedAnswer = testBusinessPhoneNumber.phoneNumber
      val expectedEditLink = incometax.business.controllers.routes.BusinessPhoneNumberController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for the business address" in {
      val sectionId = BusinessAddressId
      val expectedQuestion = messages.business_address
      val expectedAnswer = testBusinessAddress.lines.get
        .:+(testBusinessAddress.postcode.get)
        .:+(testBusinessAddress.country.map(_.name).get)
        .mkString(" ")
      val expectedEditLink = incometax.business.controllers.routes.BusinessAddressController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

    "display the correct info for the business start date" in {
      val sectionId = BusinessStartDateId
      val expectedQuestion = messages.business_start_date
      val expectedAnswer = testBusinessStartDate.startDate.toCheckYourAnswersDateFormat
      val expectedEditLink = incometax.business.controllers.routes.BusinessStartDateController.show(editMode = true).url

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
      val expectedEditLink = incometax.business.controllers.routes.BusinessAccountingMethodController.show(editMode = true).url

      sectionTest(
        sectionId = sectionId,
        expectedQuestion = expectedQuestion,
        expectedAnswer = expectedAnswer,
        expectedEditLink = expectedEditLink
      )
    }

  }

}
