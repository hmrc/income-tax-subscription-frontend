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
import play.api.mvc.Call
import play.twirl.api.Html
import services.AccountingPeriodService
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.agent.business.WhatYearToSignUp

class WhatYearToSignUpViewSpec extends ViewSpec {
  private val accountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  val backUrl: String = ViewSpecTrait.testBackUrl
  val action: Call = ViewSpecTrait.testCall
  val taxYearEnd: Int = accountingPeriodService.currentTaxYear

  private val whatYearToSignUp: WhatYearToSignUp = app.injector.instanceOf[WhatYearToSignUp]

  "what year to sign up" must {
    "have a title" in {
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      document().title mustBe WhatYearToSignUp.heading + serviceNameGovUk
    }

    "have a heading" in {
      document().select("h1").text mustBe WhatYearToSignUp.heading
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

    "have the current tax year hint filling and deadline dates" which {
      val tableRows =
        document()
          .select(".govuk-radios__item")
          .get(0)
          .select(".govuk-hint")
          .select(".govuk-table")
          .select(".govuk-table__row")

      "has the filling and deadline dates 1" in {
        val tableCells =
          tableRows
            .get(1)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.fillingDateOne((taxYearEnd - 1).toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateOne((taxYearEnd - 1).toString)
      }

      "has the filling and deadline dates 2" in {
        val tableCells =
          tableRows
            .get(2)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.fillingDateTwo((taxYearEnd - 1).toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateTwo((taxYearEnd - 1).toString)
      }

      "has the filling and deadline dates 3" in {
        val tableCells =
          tableRows
            .get(3)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.fillingDateThree(taxYearEnd.toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateThree(taxYearEnd.toString)
      }

      "has the filling and deadline dates 4" in {
        val tableCells =
          tableRows
            .get(4)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.fillingDateFour(taxYearEnd.toString)
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

    "have the next tax year hint filling and deadline dates" which {
      val tableRows =
        document()
          .select(".govuk-radios__item")
          .get(1)
          .select(".govuk-hint")
          .select(".govuk-table")
          .select(".govuk-table__row")

      "has the filling and deadline dates 1" in {
        val tableCells =
          tableRows
            .get(1)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.fillingDateOne(taxYearEnd.toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateOne(taxYearEnd.toString)
      }

      "has the filling and deadline dates 2" in {
        val tableCells =
          tableRows
            .get(2)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.fillingDateTwo(taxYearEnd.toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateTwo(taxYearEnd.toString)
      }

      "has the filling and deadline dates 3" in {
        val tableCells =
          tableRows
            .get(3)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.fillingDateThree((taxYearEnd + 1).toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateThree((taxYearEnd + 1).toString)
      }

      "has the filling and deadline dates 4" in {
        val tableCells =
          tableRows
            .get(4)
            .select(".govuk-table__cell")

        tableCells.get(0).text() mustBe WhatYearToSignUp.fillingDateFour((taxYearEnd + 1).toString)
        tableCells.get(1).text() mustBe WhatYearToSignUp.deadlineDateFour((taxYearEnd + 1).toString)
      }
    }

    "have a form" which {
      "has correct attributes" in {
        val form: Elements = document().select("form")
        form.attr("method") mustBe action.method
        form.attr("action") mustBe action.url
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

    "have a back button" when {
      "in edit mode" in {
        val backButton: Elements = document(editMode = true).select(".govuk-back-link")
        backButton.attr("href") mustBe backUrl
        backButton.text mustBe MessageLookup.Base.back
      }
    }

    "not have a back button" when {
      "not in edit mode" in {
        Option(document().selectFirst(".link-back")) mustBe None
      }
    }
  }

  private def page(editMode: Boolean): Html =
    whatYearToSignUp(
      AccountingYearForm.accountingYearForm,
      postAction = testCall,
      backUrl = testBackUrl,
      endYearOfCurrentTaxPeriod = taxYearEnd,
      isEditMode = editMode,
    )

  private def document(editMode: Boolean = false): Document =
    Jsoup.parse(page(editMode = editMode).body)

  private object WhatYearToSignUp {
    val heading = "Which tax year do you want your client to start filing income tax updates for?"
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

    def fillingDateOne(year: String): String = s"5 July $year"
    def deadlineDateOne(year: String): String = s"5 August $year"

    def fillingDateTwo(year: String): String = s"5 October $year"
    def deadlineDateTwo(year: String): String = s"5 November $year"

    def fillingDateThree(year: String): String = s"5 January $year"
    def deadlineDateThree(year: String): String = s"5 February $year"

    def fillingDateFour(year: String): String = s"5 April $year"
    def deadlineDateFour(year: String): String = s"5 May $year"
  }
}
