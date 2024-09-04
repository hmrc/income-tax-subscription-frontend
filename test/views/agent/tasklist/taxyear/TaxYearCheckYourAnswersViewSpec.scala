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

package views.agent.tasklist.taxyear

import messagelookup.individual.MessageLookup.TaxYearCheckYourAnswers
import messagelookup.individual.MessageLookup.TaxYearCheckYourAnswers._
import models.common.AccountingYearModel
import models.{AccountingYear, Current, Next}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import services.AccountingPeriodService
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.tasklist.taxyear.TaxYearCheckYourAnswers

class TaxYearCheckYourAnswersViewSpec extends ViewSpec {

  val taxYearCheckYourAnswersView: TaxYearCheckYourAnswers = app.injector.instanceOf[TaxYearCheckYourAnswers]
  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  private val postAction = controllers.agent.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.submit()
  private val fullName = "FirstName LastName"
  private val nino = "ZZ 11 11 11 Z"

  "TaxYearCheckYourAnswers" must {
      "have the correct template" in new TemplateViewTest(
        view = page(
          Current,
          fullName,
          nino
        ),
        title = TaxYearCheckYourAnswers.agentTitle,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )

    "have a heading" in {
      document().mainContent.getH1Element.text mustBe heading
    }

    "have a caption" in {
      document().selectHead(".govuk-caption-l").text mustBe TaxYearCheckYourAnswers.agentCaption
    }

    "have a summary of the answers" when {
      "current tax year selected" in {
        document().mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = TaxYearCheckYourAnswers.individualQuestion,
            value = Some(TaxYearCheckYourAnswers.current(AccountingPeriodUtil.getCurrentTaxEndYear - 1, AccountingPeriodUtil.getCurrentTaxEndYear)),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show(editMode = true).url,
                text = s"Change ${TaxYearCheckYourAnswers.individualQuestion}",
                visuallyHidden = TaxYearCheckYourAnswers.individualQuestion
              )
            )
          )
        ))
      }
      "next tax year selected" in {
        document(Next).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = TaxYearCheckYourAnswers.individualQuestion,
            value = Some(TaxYearCheckYourAnswers.next(AccountingPeriodUtil.getCurrentTaxEndYear, AccountingPeriodUtil.getCurrentTaxEndYear + 1)),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show(editMode = true).url,
                text = s"Change ${TaxYearCheckYourAnswers.individualQuestion}",
                visuallyHidden = TaxYearCheckYourAnswers.individualQuestion
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
        form.attr("action") mustBe controllers.agent.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.submit().url
      }

      "has a confirm and continue button" in {
        form.selectNth(".govuk-button", 1).text mustBe TaxYearCheckYourAnswers.confirmAndContinue
      }

      "has a save and come back later button" in {
        val saveAndComeBackLater = form.selectNth(".govuk-button", 2)
        saveAndComeBackLater.text mustBe TaxYearCheckYourAnswers.saveAndComeBack
        saveAndComeBackLater.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(location = Some("tax-year-check-your-answers")).url
      }


    }
  }

  private def page(accountingYear: AccountingYear, clientName: String, clientNino: String) =
    taxYearCheckYourAnswersView(
      postAction = postAction,
      viewModel = Some(AccountingYearModel(accountingYear)),
      clientName,
      clientNino,
      backUrl = testBackUrl
    )

  private def document(accountingYear: AccountingYear = Current) = Jsoup.parse(page(accountingYear, clientName = fullName, clientNino= nino).body)
}
