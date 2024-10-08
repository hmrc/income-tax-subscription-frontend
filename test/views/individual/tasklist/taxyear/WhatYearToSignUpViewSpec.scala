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

import forms.individual.business.AccountingYearForm
import messagelookup.agent.MessageLookup
import models.{Current, Next}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.data.FormError
import play.twirl.api.Html
import services.AccountingPeriodService
import uk.gov.hmrc.govukfrontend.views.Aliases.{Hint, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utilities.ViewSpec
import views.html.individual.tasklist.taxyear.WhatYearToSignUp

class WhatYearToSignUpViewSpec extends ViewSpec {

  private val accountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  val taxYearEnd: Int = accountingPeriodService.currentTaxYear
  val taxYearPrevious: Int = taxYearEnd - 1
  val taxYearNext: Int = taxYearEnd + 1

  val whatYearToSignUp: WhatYearToSignUp = app.injector.instanceOf[WhatYearToSignUp]

  val testFormError: FormError = FormError(AccountingYearForm.accountingYear, "error.business.what-year-to-sign-up.empty")

  "what year to sign up" must {
    "have the correct template details" when {
      "the page has no back link" in new TemplateViewTest(
        view = page(editMode = false, hasBackLink = false),
        title = WhatYearToSignUp.heading,
      )
      "the page has a back link + error" in new TemplateViewTest(
        view = page(editMode = false, hasBackLink = true, hasError = true),
        title = WhatYearToSignUp.heading,
        backLink = Some(testBackUrl),
        error = Some(testFormError)
      )
    }

    "have a heading" in {
      document().select("h1").text mustBe WhatYearToSignUp.heading
    }

    "have a body" which {
      "has paragraph" in {
        document().mainContent.selectHead("p").text mustBe WhatYearToSignUp.paragraph

      }
      "has a warning text" in {
        document().mainContent.selectHead(".govuk-warning-text").selectHead("strong").text mustBe WhatYearToSignUp.warningText
      }
    }


    "has a caption for the table" in {
      document()
        .selectHead(".govuk-table")
        .selectHead(".govuk-table__caption").text() mustBe WhatYearToSignUp.returnTableCaption
    }

    "have a return quarterly example table filing and deadline dates" which {
      val tableRows = document().select(".govuk-table__row")

      "has the filing and deadline heading" in {
        val tableRows =
          document()
            .select(".govuk-table__row")

        tableRows.get(0).select(".govuk-table__header").get(0).text() mustBe WhatYearToSignUp.updatesHeader
        tableRows.get(0).select(".govuk-table__header").get(1).text() mustBe WhatYearToSignUp.deadlineHeader
      }

      "has the filing and deadline dates 1" in {
        val tableCellsHeader =
          tableRows
            .get(1)
            .select(".govuk-table__header")

        val tableCells =
          tableRows
            .get(1)
            .select(".govuk-table__cell")

        tableCellsHeader.get(0).text() mustBe WhatYearToSignUp.filingDateOne
        tableCells.get(0).text() mustBe WhatYearToSignUp.deadlineDateOne
      }

      "has the filing and deadline dates 2" in {
        val tableCellsHeader =
          tableRows
            .get(2)
            .select(".govuk-table__header")

        val tableCells =
          tableRows
            .get(2)
            .select(".govuk-table__cell")

        tableCellsHeader.get(0).text() mustBe WhatYearToSignUp.filingDateTwo
        tableCells.get(0).text() mustBe WhatYearToSignUp.deadlineDateTwo
      }

      "has the filing and deadline dates 3" in {
        val tableCellsHeader =
          tableRows
            .get(3)
            .select(".govuk-table__header")

        val tableCells =
          tableRows
            .get(3)
            .select(".govuk-table__cell")

        tableCellsHeader.get(0).text() mustBe WhatYearToSignUp.filingDateThree
        tableCells.get(0).text() mustBe WhatYearToSignUp.deadlineDateThree
      }

      "has the filing and deadline dates 4" in {
        val tableCellsHeader =
          tableRows
            .get(4)
            .select(".govuk-table__header")

        val tableCells =
          tableRows
            .get(4)
            .select(".govuk-table__cell")

        tableCellsHeader.get(0).text() mustBe WhatYearToSignUp.filingDateFour
        tableCells.get(0).text() mustBe WhatYearToSignUp.deadlineDateFour
      }
    }


    "have a form" which {
      "has correct attributes" in {
        val form: Elements = document().select("form")
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has the correct radio inputs" in {
        document().mustHaveRadioInput(selector = "fieldset")(
          name = AccountingYearForm.accountingYear,
          legend = WhatYearToSignUp.heading,
          isHeading = false,
          isLegendHidden = true,
          hint = None,
          errorMessage = None,
          radioContents = Seq(
            RadioItem(
              content = Text(Seq(
                WhatYearToSignUp.currentYearOption((taxYearEnd - 1).toString, taxYearEnd.toString)
              ).mkString(" ")),
              value = Some(Current.toString),
              hint = Some(Hint(content = Text(WhatYearToSignUp.currentYearOptionHint))),
            ),
            RadioItem(
              content = Text(Seq(
                WhatYearToSignUp.nextYearOption(taxYearEnd.toString, (taxYearEnd + 1).toString)
              ).mkString(" ")),
              value = Some(Next.toString),
              hint = Some(Hint(content = Text(WhatYearToSignUp.nextYearOptionHint))),
            )
          )
        )
      }

      "has a continue button" that {
        s"displays ${MessageLookup.Base.continue} when not in edit mode" in {
          document().select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
        }

        s"displays ${MessageLookup.Base.update} when in edit mode" in {
          document(editMode = true).select("button[id=continue-button]").text mustBe MessageLookup.Base.update
        }
      }
    }
  }

  private def page(editMode: Boolean, hasBackLink: Boolean, hasError: Boolean = false): Html =
    whatYearToSignUp(
      if (hasError) AccountingYearForm.accountingYearForm.withError(testFormError) else AccountingYearForm.accountingYearForm,
      postAction = testCall,
      backUrl = if (hasBackLink) Some(testBackUrl) else None,
      endYearOfCurrentTaxPeriod = taxYearEnd,
      isEditMode = editMode,
    )

  private def document(editMode: Boolean = false, hasBackLink: Boolean = false, hasError: Boolean = false): Document =
    Jsoup.parse(page(editMode = editMode, hasBackLink = hasBackLink, hasError).body)

  object WhatYearToSignUp {
    val heading = "Choose when you want to start using Making Tax Digital for Income Tax"
    val returnTableCaption = "Submit quarterly updates by the deadline"
    val updatesHeader = "Quarterly update"
    val deadlineHeader = "Deadline"
    val paragraph = s"You can start sending quarterly updates during the current tax year (6 April ${taxYearPrevious} to 5 April ${taxYearEnd}) or the next tax year (6 April ${taxYearEnd} to 5 April ${taxYearNext})."
    val warningText = "Warning You will not be penalised if you start sending updates mid-way through the tax year. However, you will need to make updates for the quarters you’ve missed."

    val currentYearOptionHint = s"Send a final declaration by the 31 January ${(taxYearEnd + 1).toString}."

    val nextYearOptionHint: String =
      s"Send a final declaration by 31 January ${(taxYearEnd + 2).toString} and " +
        "complete a Self Assessment return for the current tax year as normal."

    def currentYearOption(fromYear: String, toYear: String): String = s"Current tax year (6 April $fromYear to 5 April $toYear)"

    def nextYearOption(fromYear: String, toYear: String): String = s"Next tax year (6 April $fromYear to 5 April $toYear)"

    def filingDateOne: String = s"6 April to 5 July"

    def deadlineDateOne: String = s"5 August"

    def filingDateTwo: String = s"6 July to 5 October"

    def deadlineDateTwo: String = s"5 November"

    def filingDateThree: String = s"6 October to 5 January"

    def deadlineDateThree: String = s"5 February"

    def filingDateFour: String = s"6 January to 5 April"

    def deadlineDateFour: String = s"5 May"
  }
}
