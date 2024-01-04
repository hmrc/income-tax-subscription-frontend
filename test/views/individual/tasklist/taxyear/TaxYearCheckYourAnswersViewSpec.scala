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

package views.individual.tasklist.taxyear

import messagelookup.individual.MessageLookup.{TaxYearCheckYourAnswers => messages}
import models.common.AccountingYearModel
import models.{AccountingYear, Current, Next}
import org.jsoup.Jsoup
import services.AccountingPeriodService
import utilities.AccountingPeriodUtil.getCurrentTaxEndYear
import utilities.ViewSpec
import views.html.individual.tasklist.taxyear.TaxYearCheckYourAnswers

class TaxYearCheckYourAnswersViewSpec extends ViewSpec {

  val taxYearCheckYourAnswersView: TaxYearCheckYourAnswers = app.injector.instanceOf[TaxYearCheckYourAnswers]
  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  private val postAction = controllers.individual.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.submit()
  private val sectionId = "#tax-year"

  "business task list view" must {
    "have a title" in {
      document().title mustBe messages.title
    }

    "have a heading" in {
      document().select("h1").text mustBe messages.heading
    }

    "have a caption" in {
      document().select(".hmrc-page-heading p").text mustBe messages.caption
    }

    "display the tax year section question" in {
      document()
        .mainContent
        .selectHead(s"$sectionId-question")
        .text() mustBe messages.question
    }

    "display the current in the tax year section answer" in {
      document()
        .mainContent
        .selectHead(s"$sectionId-answer")
        .text() mustBe messages.current(getCurrentTaxEndYear - 1, getCurrentTaxEndYear)
    }

    "display the next in the tax year section answer" in {
      document(Next)
        .mainContent
        .selectHead(s"$sectionId-answer")
        .text() mustBe messages.next(getCurrentTaxEndYear, getCurrentTaxEndYear + 1)
    }

    "display the tax year section edit link" in {
      document()
        .mainContent
        .selectHead(s"$sectionId-edit a")
        .attr("href") mustBe controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show(editMode = true).url
    }

    "display the tax year section hidden content" in {
      document()
        .mainContent
        .selectHead(s"$sectionId-row .govuk-visually-hidden")
        .text() mustBe messages.hiddenQuestion
    }

    "The back url" should {
      "have the value provided to the view" in {
        val link = document().selectHead(".govuk-back-link")
        link.text mustBe "Back"
        link.attr("href") mustBe "/testUrl"
      }
    }
  }

  private def page(accountingYear: AccountingYear) =
    taxYearCheckYourAnswersView(
      postAction = postAction,
      viewModel = Some(AccountingYearModel(accountingYear)),
      accountingPeriodService = accountingPeriodService,
      backUrl = "/testUrl"
    )

  private def document(accountingYear: AccountingYear = Current) = Jsoup.parse(page(accountingYear).body)
}
