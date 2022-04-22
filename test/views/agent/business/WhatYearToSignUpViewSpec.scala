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

package views.agent.business

import agent.assets.MessageLookup
import forms.agent.AccountingYearForm
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.data.FormError
import play.twirl.api.Html
import services.AccountingPeriodService
import utilities.ViewSpec
import views.html.agent.business.WhatYearToSignUp

class WhatYearToSignUpViewSpec extends ViewSpec {

  private val accountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  val taxYearEnd: Int = accountingPeriodService.currentTaxYear

  private val whatYearToSignUp: WhatYearToSignUp = app.injector.instanceOf[WhatYearToSignUp]

  val testFormError: FormError = FormError(AccountingYearForm.accountingYear, "test error")

  "what year to sign up" must {
    "have the correct template details" when {
      "the page has no back link" in new TemplateViewTest(
        view = page(editMode = false, hasBackLink = false),
        isAgent = true,
        title = WhatYearToSignUp.heading,
      )
      "the page has a back link + error" in new TemplateViewTest(
        view = page(editMode = false, hasBackLink = true, hasError = true),
        isAgent = true,
        title = WhatYearToSignUp.heading,
        backLink = Some(testBackUrl),
        error = Some(testFormError)
      )
    }

    "have a heading" in {
      document().getH1Element.text mustBe WhatYearToSignUp.heading
    }

    "have a body" which {
      val paragraphs: Elements = document().select(".govuk-body").select("p")

      "has paragraph 1" in {
        paragraphs.get(0).text() mustBe WhatYearToSignUp.paragraph1
      }

      "has paragraph 2" in {
        paragraphs.get(1).text() mustBe WhatYearToSignUp.paragraph2
      }

      "has paragraph 3" in {
        paragraphs.get(2).text() mustBe WhatYearToSignUp.paragraph3
      }
    }

    "have the current tax year hint paragraphs" which {
      val hintParagraphs =
        document()
          .select(".govuk-radios__item")
          .get(0)
          .select(".govuk-hint")
          .select("p")

      "has the paragraph 1" in {
        hintParagraphs.get(0).text() mustBe WhatYearToSignUp.currentYearOptionHintParagraph1
      }

      "has the paragraph 2" in {
        hintParagraphs.get(1).text() mustBe WhatYearToSignUp.currentYearOptionHintParagraph2
      }
    }

    "have the current tax year hint filing and deadline dates" which {
      val tableRows =
        document()
          .select(".govuk-radios__item")
          .get(0)
          .select(".govuk-hint")
          .select(".govuk-table")
          .select(".govuk-table__row")

      "has the filing and deadline headers" in {
        val tableHeaders =
          tableRows
            .get(0)
            .select(".govuk-table__header")

        tableHeaders.get(0).text() mustBe WhatYearToSignUp.updatesHeader
        tableHeaders.get(1).text() mustBe WhatYearToSignUp.deadlineHeader
      }

      "has the filing and deadline dates 1" in {
        val tableCells =
          tableRows
            .get(1)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.filingDateOne((taxYearEnd - 1).toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateOne((taxYearEnd - 1).toString)
      }

      "has the filing and deadline dates 2" in {
        val tableCells =
          tableRows
            .get(2)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.filingDateTwo((taxYearEnd - 1).toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateTwo((taxYearEnd - 1).toString)
      }

      "has the filing and deadline dates 3" in {
        val tableCells =
          tableRows
            .get(3)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.filingDateThree((taxYearEnd - 1).toString, taxYearEnd.toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateThree(taxYearEnd.toString)
      }

      "has the filing and deadline dates 4" in {
        val tableCells =
          tableRows
            .get(4)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.filingDateFour(taxYearEnd.toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateFour(taxYearEnd.toString)
      }
    }

    "have the next tax year hint paragraphs" which {
      val hintParagraphs =
        document()
          .select(".govuk-radios__item")
          .get(1)
          .select(".govuk-hint")
          .select("p")

      "has the paragraph 1" in {
        hintParagraphs.get(0).text() mustBe WhatYearToSignUp.nextYearOptionHintParagraph1
      }

      "has the paragraph 2" in {
        hintParagraphs.get(1).text() mustBe WhatYearToSignUp.nextYearOptionHintParagraph2
      }
    }

    "have the next tax year hint filing and deadline dates" which {
      val tableRows =
        document()
          .select(".govuk-radios__item")
          .get(1)
          .select(".govuk-hint")
          .select(".govuk-table")
          .select(".govuk-table__row")

      "has the filing and deadline headers" in {
        val tableHeaders =
          tableRows
            .get(0)
            .select(".govuk-table__header")

        tableHeaders.get(0).text() mustBe WhatYearToSignUp.updatesHeader
        tableHeaders.get(1).text() mustBe WhatYearToSignUp.deadlineHeader
      }

      "has the filing and deadline dates 1" in {
        val tableCells =
          tableRows
            .get(1)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.filingDateOne(taxYearEnd.toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateOne(taxYearEnd.toString)
      }

      "has the filing and deadline dates 2" in {
        val tableCells =
          tableRows
            .get(2)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.filingDateTwo(taxYearEnd.toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateTwo(taxYearEnd.toString)
      }

      "has the filing and deadline dates 3" in {
        val tableCells =
          tableRows
            .get(3)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.filingDateThree(taxYearEnd.toString, (taxYearEnd + 1).toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateThree((taxYearEnd + 1).toString)
      }

      "has the filing and deadline dates 4" in {
        val tableCells =
          tableRows
            .get(4)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.filingDateFour((taxYearEnd + 1).toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateFour((taxYearEnd + 1).toString)
      }
    }

    "have a form" which {
      "has correct attributes" in {
        val form: Elements = document().select("form")
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a current tax year radio button" in {
        val radio: Element = document().select(".govuk-radios__item").get(0)
        radio.select("input[id=accountingYear]").`val` mustBe "CurrentYear"
        radio.select("label[for=accountingYear]").text mustBe Seq(
          WhatYearToSignUp.currentYearOption((taxYearEnd - 1).toString, taxYearEnd.toString)
        ).mkString(" ")
      }

      "has a next tax year radio button" in {
        val radio: Element = document().select(".govuk-radios__item").get(1)
        radio.select("input[id=accountingYear-2]").`val` mustBe "NextYear"
        radio.select("label[for=accountingYear-2]").text mustBe Seq(
          WhatYearToSignUp.nextYearOption(taxYearEnd.toString, (taxYearEnd + 1).toString)
        ).mkString(" ")
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

  private object WhatYearToSignUp {
    val heading = "Which tax year do you want your client to start filing income tax updates for?"
    val updatesHeader = "Quarterly update"
    val deadlineHeader = "Deadline"
    val paragraph1 = "You can start sending income tax updates during the current tax year or the next tax year. It will not affect the amount you pay."
    val paragraph2 = "There is no penalty if you start making updates mid-way through the current tax year."
    val paragraph3 = "You or your client will need to add all business income and expenses into your software from 6 April:"

    val currentYearOptionHintParagraph1: String =
      "You or your client will need to add all business income and expenses into your software " +
        "from the start of the current tax year. You or your client need to send a quarterly update for:"

    val currentYearOptionHintParagraph2 = s"You or your client will need to submit a final declaration by the 31 January ${(taxYearEnd + 1).toString}."

    val nextYearOptionHintParagraph1: String = "You or your client need to send a quarterly update for:"

    val nextYearOptionHintParagraph2: String =
      s"You or your client will need to submit a final declaration by 31 January ${(taxYearEnd + 2).toString} and " +
        "will need to complete a Self Assessment return for the current tax year as normal."

    def currentYearOption(fromYear: String, toYear: String): String = s"Current tax year (6 April $fromYear to 5 April $toYear)"

    def nextYearOption(fromYear: String, toYear: String): String = s"Next tax year (6 April $fromYear to 5 April $toYear)"

    def filingDateOne(year: String): String = s"6 April $year - 5 July $year"

    def deadlineDateOne(year: String): String = s"5 August $year"

    def filingDateTwo(year: String): String = s"6 July $year - 5 October $year"

    def deadlineDateTwo(year: String): String = s"5 November $year"

    def filingDateThree(yearFrom: String, yearTo: String): String = s"6 October $yearFrom - 5 January $yearTo"

    def deadlineDateThree(year: String): String = s"5 February $year"

    def filingDateFour(year: String): String = s"6 January $year - 5 April $year"

    def deadlineDateFour(year: String): String = s"5 May $year"
  }

}
