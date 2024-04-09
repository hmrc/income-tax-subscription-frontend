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

import forms.agent.AccountingYearForm
import messagelookup.agent.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.data.FormError
import play.twirl.api.Html
import services.AccountingPeriodService
import utilities.ViewSpec
import views.html.agent.tasklist.taxyear.WhatYearToSignUp

class WhatYearToSignUpViewSpec extends ViewSpec {

  private val accountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  val taxYearEnd: Int = accountingPeriodService.currentTaxYear
  val taxYearPrevious: Int = taxYearEnd - 1
  val taxYearNext: Int = taxYearEnd + 1

  private val whatYearToSignUp: WhatYearToSignUp = app.injector.instanceOf[WhatYearToSignUp]
  private val fullName = "FirstName LastName"
  private val nino = "ZZ 11 11 11 Z"

  val testFormError: FormError = FormError(AccountingYearForm.accountingYear, "agent.error.business.what-year-to-sign-up.empty")

  "what year to sign up" must {
    "have the correct template details" when {
      "the page has no back link" in new TemplateViewTest(
        view = page(editMode = false, hasBackLink = false, clientName = fullName, clientNino = nino),
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

    "have client details as caption" in {
      document().selectHead(".govuk-caption-l").text mustBe WhatYearToSignUp.agentCaption
    }

    "have a heading" in {
      document().getH1Element.text mustBe WhatYearToSignUp.heading
    }

    "have a body" which {
      val paragraphs: Elements = document().select(".govuk-body").select("p")

      "has paragraph 1" in {
        paragraphs.get(0).text() mustBe WhatYearToSignUp.paragraph1
      }

      "has warning text" in {
        document().selectHead(".govuk-warning-text").text() mustBe WhatYearToSignUp.warningText
      }

      "has details section" which {

        "have a table for quarterly update example with filing and deadline dates" which {
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
      }
      "has details para1" in {
        document().getElementById("para1").text() mustBe WhatYearToSignUp.detailsPara1
      }

      "has details para2" in {
        document().getElementById("para2").text() mustBe WhatYearToSignUp.detailsPara2
      }
    }

    "has the current year option hint" in {
      document().getElementById("accountingYear-item-hint").text() mustBe WhatYearToSignUp.currentYearOptionHint
    }

    "has the next year option hint" in {
      document().getElementById("accountingYear-2-item-hint").text() mustBe WhatYearToSignUp.nextYearOptionHint
    }

    "has a caption for the table" in {
      document()
        .selectHead(".govuk-table")
        .selectHead(".govuk-table__caption").text() mustBe WhatYearToSignUp.returnTableCaption
    }

    "have a form" which {
      "has correct attributes" in {
        val form: Elements = document().select("form")
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a small size legend for radio options heading" in {
        document().selectHead(".govuk-fieldset__legend").text() mustBe WhatYearToSignUp.radioSectionHeading

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

  private def page(editMode: Boolean, clientName: String = fullName, clientNino: String = nino, hasBackLink: Boolean, hasError: Boolean = false): Html =
    whatYearToSignUp(
      if (hasError) AccountingYearForm.accountingYearForm.withError(testFormError) else AccountingYearForm.accountingYearForm,
      postAction = testCall,
      clientName,
      clientNino,
      backUrl = if (hasBackLink) Some(testBackUrl) else None,
      endYearOfCurrentTaxPeriod = taxYearEnd,
      isEditMode = editMode,
    )

  private def document(editMode: Boolean = false, clientName: String = fullName, clientNino: String = nino, hasBackLink: Boolean = false, hasError: Boolean = false): Document =
    Jsoup.parse(page(editMode = editMode, clientName, clientNino, hasBackLink = hasBackLink, hasError).body)

  private object WhatYearToSignUp {
    val heading = "Select when your client will start using Making Tax Digital for Income Tax"
    val agentCaption = fullName + " | " + nino
    val returnTableCaption = "Submit quarterly updates by the deadline"
    val detailsPara1 = "You can choose to send your client’s updates by calendar quarterly period dates. This must be selected in the compatible software before the first update is made."
    val detailsPara2 = "The deadline for both quarterly period dates are the same."
    val updatesHeader = "Quarterly update"
    val deadlineHeader = "Deadline"
    val paragraph1 = s"You or your client can start sending quarterly updates during the current tax year 6 April ${taxYearPrevious.toString} to 5 April ${taxYearEnd.toString} or the next tax year 6 April ${taxYearEnd.toString} to 5 April ${taxYearNext.toString}."
    val warningText = "! Warning Your client will not be penalised if updates are sent mid-way through the tax year. However, updates need to be made for any missed quarters."
    val paragraph2 = "Submit quarterly updates by the deadline"
    val radioSectionHeading: String = "Select tax year"
    val currentYearOptionHint = s"You or your client will need to submit a final declaration by the 31 January ${(taxYearEnd + 1).toString}."
    val nextYearOptionHint: String =
      s"You or your client will need to submit a final declaration by 31 January ${(taxYearEnd + 2).toString} and " +
        "will need to complete a Self Assessment return for the current tax year as normal."

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
