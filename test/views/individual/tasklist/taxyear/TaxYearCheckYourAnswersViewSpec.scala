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
import org.jsoup.nodes.Element
import services.AccountingPeriodService
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.tasklist.taxyear.TaxYearCheckYourAnswers

class TaxYearCheckYourAnswersViewSpec extends ViewSpec {

  val taxYearCheckYourAnswersView: TaxYearCheckYourAnswers = app.injector.instanceOf[TaxYearCheckYourAnswers]
  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  private val postAction = controllers.individual.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.submit()
  private val sectionId = "#tax-year"

  "TaxYearCheckYourAnswers" must {
    "have the correct template" in new TemplateViewTest(
      page(Current),
      title = messages.title,
      isAgent = false,
      backLink = Some(testBackUrl),
      hasSignOutLink = true
    )

    "have a heading and caption" in {
      document().mainContent.mustHaveHeadingAndCaption(
        heading = messages.heading,
        caption = messages.caption,
        isSection = true
      )
    }

    "have a summary of the answers" when {
      "current tax year selected" in {
        document().mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = messages.individualQuestion,
            value = Some(messages.current(AccountingPeriodUtil.getCurrentTaxEndYear - 1, AccountingPeriodUtil.getCurrentTaxEndYear)),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show(editMode = true).url,
                text = s"Change ${messages.individualQuestion}",
                visuallyHidden = messages.individualQuestion
              )
            )
          )
        ))
      }
      "next tax year selected" in {
        document(Next).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = messages.individualQuestion,
            value = Some(messages.next(AccountingPeriodUtil.getCurrentTaxEndYear, AccountingPeriodUtil.getCurrentTaxEndYear + 1)),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show(editMode = true).url,
                text = s"Change ${messages.individualQuestion}",
                visuallyHidden = messages.individualQuestion
              )
            )
          )
        ))
      }
    }

    "have a form" which {
      def form: Element = document().mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe "POST"
        form.attr("action") mustBe controllers.individual.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.submit().url
      }
      "has a confirm and continue button" in {
        form.selectNth(".govuk-button", 1).text mustBe messages.confirmAndContinue
      }
      "has a save and come back later button" in {
        val saveAndComeBackLater = form.selectNth(".govuk-button", 2)
        saveAndComeBackLater.text mustBe messages.saveAndComeBack
        saveAndComeBackLater.attr("href") mustBe controllers.individual.tasklist.routes.ProgressSavedController.show(location = Some("tax-year-check-your-answers")).url
      }
    }

  }

  private def page(accountingYear: AccountingYear) =
    taxYearCheckYourAnswersView(
      postAction = postAction,
      viewModel = Some(AccountingYearModel(accountingYear)),
      backUrl = testBackUrl
    )

  private def document(accountingYear: AccountingYear = Current) = Jsoup.parse(page(accountingYear).body)

}
