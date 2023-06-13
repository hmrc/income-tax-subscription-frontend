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

package views.agent.business

import assets.MessageLookup.TaxYearCheckYourAnswers._
import models.common.AccountingYearModel
import models.{AccountingYear, Current, Next}
import org.jsoup.Jsoup
import services.AccountingPeriodService
import utilities.AccountingPeriodUtil.getCurrentTaxEndYear
import utilities.ViewSpec
import views.html.agent.business.TaxYearCheckYourAnswers

class TaxYearCheckYourAnswersViewSpec extends ViewSpec {

  val taxYearCheckYourAnswersView: TaxYearCheckYourAnswers = app.injector.instanceOf[TaxYearCheckYourAnswers]
  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  private val postAction = controllers.individual.business.routes.TaxYearCheckYourAnswersController.submit()
  private val sectionId = "#tax-year"
  private val fullName = "FirstName LastName"
  private val nino = "ZZ 11 11 11 Z"

  "business task list view" must {
    "have a title" in {
      document().title mustBe agentTitle
    }

    "have a heading" in {
      document().select("h1").text mustBe heading
    }

    "have client details as caption" in {
      document().select(".govuk-caption-xl").text mustBe agentCaption
    }

    "display the tax year section question" in {
      document()
        .mainContent
        .selectHead(s"$sectionId-question")
        .text() mustBe question
    }

    "display the current in the tax year section answer" in {
      document()
        .mainContent
        .selectHead(s"$sectionId-answer")
        .text() mustBe current(getCurrentTaxEndYear - 1, getCurrentTaxEndYear)
    }

    "display the next in the tax year section answer" in {
      document(Next)
        .mainContent
        .selectHead(s"$sectionId-answer")
        .text() mustBe next(getCurrentTaxEndYear, getCurrentTaxEndYear + 1)
    }

    "display the tax year section edit link" in {
      document()
        .mainContent
        .selectHead(s"$sectionId-edit a")
        .attr("href") mustBe controllers.agent.routes.WhatYearToSignUpController.show(editMode = true).url
    }

    "display the tax year section hidden content" in {
      document()
        .mainContent
        .selectHead(s"$sectionId-row .govuk-visually-hidden")
        .text() mustBe hiddenQuestion
    }

    "The back url" should {
      "have the value provided to the view" in {
        val link = document().selectHead(".govuk-back-link")
        link.text mustBe "Back"
        link.attr("href") mustBe "/testUrl"
      }
    }
  }

  private def page(accountingYear: AccountingYear, clientName: String, clientNino: String) =
    taxYearCheckYourAnswersView(
      postAction = postAction,
      viewModel = Some(AccountingYearModel(accountingYear)),
      clientName,
      clientNino,
      backUrl = "/testUrl"
    )

  private def document(accountingYear: AccountingYear = Current) = Jsoup.parse(page(accountingYear, clientName = fullName, clientNino= nino).body)
}
