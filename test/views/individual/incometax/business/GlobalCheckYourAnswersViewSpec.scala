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

package views.individual.incometax.business

import forms.individual.business.GlobalCheckYourAnswersForm
import forms.submapping.YesNoMapping
import models._
import models.common.business.Address
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import services.GetCompleteDetailsService._
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.incometax.business.GlobalCheckYourAnswers

import java.time.LocalDate

class GlobalCheckYourAnswersViewSpec extends ViewSpec {

  val formError: FormError = FormError(GlobalCheckYourAnswersForm.fieldName, "individual.global-check-your-answers.form.error.empty")

  "GlobalCheckYourAnswers" must {

    "use the correct template" when {
      "there is no error on the page" in new TemplateViewTest(
        view = page(
          form = GlobalCheckYourAnswersForm.form,
          completeDetails = completeDetails()
        ),
        title = GlobalCheckYourAnswersMessages.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true,
        error = None
      )
      "there is an error on the page" in new TemplateViewTest(
        view = page(
          form = GlobalCheckYourAnswersForm.form.withError(
            formError
          ),
          completeDetails = completeDetails()
        ),
        title = GlobalCheckYourAnswersMessages.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true,
        error = Some(formError)
      )
    }

    "have a heading" in {
      document().mainContent.getH1Element.text mustBe GlobalCheckYourAnswersMessages.heading
    }

    "have an income sources heading" in {
      document().mainContent.selectNth("h2", 1).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.heading
    }

    "have the correct income sources displayed" when {
      "there is only a single self employment business" must {
        def mainContent(accountingMethod: AccountingMethod = Cash): Element = {
          document(details = minDetails(soleTraderBusinesses = Some(selfEmploymentIncomeSource(accountingMethod = accountingMethod))))
            .mainContent
        }

        "have a list of their sole trader income source details" which {
          def summaryList(accountingMethod: AccountingMethod = Cash): Element = {
            mainContent(accountingMethod).selectNth(".govuk-summary-list", 1)
          }

          "has a sole trader business heading detailing their trade" in {
            mainContent().selectNth("h3", 1).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading("Plumbing-1")
          }
          "has a row with the business name" in {
            val row: Element = summaryList().selectNth(".govuk-summary-list__row", 1)
            row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.name
            row.selectHead("dd").text mustBe "ABC-1"
          }
          "has a row with the trading start date" in {
            val row: Element = summaryList().selectNth(".govuk-summary-list__row", 2)
            row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.startDate
            row.selectHead("dd").text mustBe "1 January 1980"
          }
          "has a row with the address" in {
            val row: Element = summaryList().selectNth(".govuk-summary-list__row", 3)
            row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.address
            row.selectHead("dd").text mustBe Address(
              lines = Seq(
                "1 Long Road",
                "Lonely City"
              ),
              postcode = Some("ZZ11ZZ")
            ).toString
          }
          "has a row with the accounting method" when {
            "the accounting method is Cash" in {
              val row: Element = summaryList().selectNth(".govuk-summary-list__row", 4)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.accountingMethod
              row.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.cash
            }
            "the accounting method is Accruals" in {
              val row: Element = summaryList(accountingMethod = Accruals).selectNth(".govuk-summary-list__row", 4)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.accountingMethod
              row.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.accruals
            }
          }
        }
        "have no other income sources displayed" in {
          mainContent()
            .selectNth(".govuk-summary-list", 2)
            .selectHead(".govuk-summary-list__row")
            .selectHead(".govuk-summary-list__key")
            .text mustBe GlobalCheckYourAnswersMessages.SelectedTaxYear.key
        }
      }
      "there are only multiple self employment businesses" must {
        def mainContent: Element = {
          document(details = minDetails(soleTraderBusinesses = Some(selfEmploymentIncomeSource(accountingMethod = Cash, count = 2))))
            .mainContent
        }

        "have a first sole trader business displayed" which {
          def summaryList: Element = {
            mainContent.selectNth(".govuk-summary-list", 1)
          }

          "has a sole trader business heading detailing their trade" in {
            mainContent.selectNth("h3", 1).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading("Plumbing-1")
          }
          "has a row with the business name" in {
            val row: Element = summaryList.selectNth(".govuk-summary-list__row", 1)
            row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.name
            row.selectHead("dd").text mustBe "ABC-1"
          }
          "has a row with the trading start date" in {
            val row: Element = summaryList.selectNth(".govuk-summary-list__row", 2)
            row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.startDate
            row.selectHead("dd").text mustBe "1 January 1980"
          }
          "has a row with the address" in {
            val row: Element = summaryList.selectNth(".govuk-summary-list__row", 3)
            row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.address
            row.selectHead("dd").text mustBe Address(
              lines = Seq(
                "1 Long Road",
                "Lonely City"
              ),
              postcode = Some("ZZ11ZZ")
            ).toString
          }
          "have no row for accounting method" in {
            summaryList.selectOptionalNth(".govuk-summary-list__row", 4) mustBe None
          }
        }

        "have a second sole trader business displayed" which {
          def summaryList: Element = {
            mainContent.selectNth(".govuk-summary-list", 2)
          }

          "has a sole trader business heading detailing their trade" in {
            mainContent.selectNth("h3", 2).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading("Plumbing-2")
          }
          "has a row with the business name" in {
            val row: Element = summaryList.selectNth(".govuk-summary-list__row", 1)
            row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.name
            row.selectHead("dd").text mustBe "ABC-2"
          }
          "has a row with the trading start date" in {
            val row: Element = summaryList.selectNth(".govuk-summary-list__row", 2)
            row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.startDate
            row.selectHead("dd").text mustBe "2 February 1980"
          }
          "has a row with the address" in {
            val row: Element = summaryList.selectNth(".govuk-summary-list__row", 3)
            row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.address
            row.selectHead("dd").text mustBe Address(
              lines = Seq(
                "2 Long Road",
                "Lonely City"
              ),
              postcode = Some("ZZ22ZZ")
            ).toString
          }
          "have no row for accounting method" in {
            summaryList.selectOptionalNth(".govuk-summary-list__row", 4) mustBe None
          }
        }

        "have a separate accounting method section displayed" which {
          "has an accounting method heading" in {
            mainContent.selectNth("h3", 3).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.accountingMethodHeading
          }
          "has the accounting method chosen displayed" in {
            val summaryListRow = mainContent.selectNth(".govuk-summary-list", 3).selectHead(".govuk-summary-list__row")
            summaryListRow.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.accountingMethod
            summaryListRow.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.cash
          }
        }
      }
      "there is only a single uk property business" must {
        def mainContent(accountingMethod: AccountingMethod = Cash): Element = {
          document(details = minDetails(ukProperty = Some(ukPropertyIncomeSource(accountingMethod)))).mainContent
        }

        "have a heading for the uk property business" in {
          mainContent().selectNth("h3", 1).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.heading
        }

        "have the uk property business details listed" which {
          def summaryList(accountingMethod: AccountingMethod = Cash): Element = {
            mainContent(accountingMethod).selectNth(".govuk-summary-list", 1)
          }

          "has a start date" in {
            val row: Element = summaryList().selectNth(".govuk-summary-list__row", 1)
            row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.startDate
            row.selectHead("dd").text mustBe "2 January 1980"
          }
          "has an accounting method" when {
            "the accounting method is Cash" in {
              val row: Element = summaryList().selectNth(".govuk-summary-list__row", 2)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.accountingMethod
              row.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.cash
            }
            "the accounting method is Accruals" in {
              val row: Element = summaryList(accountingMethod = Accruals).selectNth(".govuk-summary-list__row", 2)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.accountingMethod
              row.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.accruals
            }
          }
        }
      }
      "there is only a single foreign property business" must {
        def mainContent(accountingMethod: AccountingMethod): Element = {
          document(details = minDetails(foreignProperty = Some(foreignPropertyIncomeSource(accountingMethod))))
            .mainContent
        }

        "have a heading for the foreign property business" in {
          mainContent(Cash).selectNth("h3", 1).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.heading
        }

        "have the foreign property business details listed" which {
          def summaryList(accountingMethod: AccountingMethod = Cash): Element = {
            mainContent(accountingMethod).selectNth(".govuk-summary-list", 1)
          }

          "has a start date" in {
            val row: Element = summaryList().selectNth(".govuk-summary-list__row", 1)
            row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.startDate
            row.selectHead("dd").text mustBe "3 January 1980"
          }
          "has an accounting method" when {
            "the accounting method is Cash" in {
              val row: Element = summaryList().selectNth(".govuk-summary-list__row", 2)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.accountingMethod
              row.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.cash
            }
            "the accounting method is Accruals" in {
              val row: Element = summaryList(accountingMethod = Accruals).selectNth(".govuk-summary-list__row", 2)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.accountingMethod
              row.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.accruals
            }
          }
        }
      }
      "there is all income sources" must {
        def mainContent: Element = {
          document().mainContent
        }

        "have a sole trader business" which {
          "has a heading" in {
            mainContent.selectNth("h3", 1).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading("Plumbing-1")
          }
          "has the business details" which {
            def summaryList: Element = mainContent.selectNth(".govuk-summary-list", 1)

            "has a business name row" in {
              val row: Element = summaryList.selectNth(".govuk-summary-list__row", 1)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.name
              row.selectHead("dd").text mustBe "ABC-1"
            }
            "has a start date row" in {
              val row: Element = summaryList.selectNth(".govuk-summary-list__row", 2)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.startDate
              row.selectHead("dd").text mustBe "1 January 1980"
            }
            "has an address row" in {
              val row: Element = summaryList.selectNth(".govuk-summary-list__row", 3)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.address
              row.selectHead("dd").text mustBe Address(
                lines = Seq(
                  "1 Long Road",
                  "Lonely City"
                ),
                postcode = Some("ZZ11ZZ")
              ).toString
            }
            "has an accounting method row" in {
              val row: Element = summaryList.selectNth(".govuk-summary-list__row", 4)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.accountingMethod
              row.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.cash
            }
          }
        }
        "have a uk property business" which {
          "has a heading" in {
            mainContent.selectNth("h3", 2).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.heading
          }
          "has the uk property details" which {
            def summaryList: Element = mainContent.selectNth(".govuk-summary-list", 2)

            "has a start date row" in {
              val row: Element = summaryList.selectNth(".govuk-summary-list__row", 1)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.startDate
              row.selectHead("dd").text mustBe "2 January 1980"
            }
            "has an accounting method row" in {
              val row: Element = summaryList.selectNth(".govuk-summary-list__row", 2)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.accountingMethod
              row.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.cash
            }
          }
        }
        "have a foreign property business" which {
          "has a heading" in {
            mainContent.selectNth("h3", 3).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.heading
          }
          "has the foreign property details" which {
            def summaryList: Element = mainContent.selectNth(".govuk-summary-list", 3)

            "has a start date row" in {
              val row: Element = summaryList.selectNth(".govuk-summary-list__row", 1)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.startDate
              row.selectHead("dd").text mustBe "3 January 1980"
            }
            "has an accounting method row" in {
              val row: Element = summaryList.selectNth(".govuk-summary-list__row", 2)
              row.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.accountingMethod
              row.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.cash
            }
          }
        }
      }
    }

    "have a selected tax year heading" in {
      document().mainContent.selectNth("h2", 2).text mustBe GlobalCheckYourAnswersMessages.SelectedTaxYear.heading
    }

    "have a summary list with the tax year selected" when {
      "the selected tax year is Current" in {
        val summaryList = document(details = completeDetails(taxYear = Current)).mainContent
          .selectNth(".govuk-summary-list", 4)
        val selectedTaxYearRow = summaryList.selectHead(".govuk-summary-list__row")
        selectedTaxYearRow.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.SelectedTaxYear.key
        selectedTaxYearRow.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.SelectedTaxYear.current(AccountingPeriodUtil.getCurrentTaxEndYear)
      }
      "the selected tax year is Next" in {
        val summaryList = document(details = completeDetails(taxYear = Next)).mainContent
          .selectNth(".govuk-summary-list", 4)
        val selectedTaxYearRow = summaryList.selectHead(".govuk-summary-list__row")
        selectedTaxYearRow.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.SelectedTaxYear.key
        selectedTaxYearRow.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.SelectedTaxYear.next(AccountingPeriodUtil.getNextTaxEndYear)
      }
    }

    "have a software heading" in {
      document().mainContent.selectNth("h2", 3).text mustBe GlobalCheckYourAnswersMessages.Software.heading
    }

    "have a summary list with the compatible software selection" when {
      "the compatible software choice was Yes" in {
        val summaryList = document().mainContent
          .selectNth(".govuk-summary-list", 5)
        val softwareRow = summaryList.selectHead(".govuk-summary-list__row")
        softwareRow.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.Software.key
        softwareRow.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.Software.yes
      }
      "the compatible software choice was No" in {
        val summaryList = document(details = completeDetails(hasSoftware = false)).mainContent
          .selectNth(".govuk-summary-list", 5)
        val softwareRow = summaryList.selectHead(".govuk-summary-list__row")
        softwareRow.selectHead("dt").text mustBe GlobalCheckYourAnswersMessages.Software.key
        softwareRow.selectHead("dd").text mustBe GlobalCheckYourAnswersMessages.Software.no
      }
    }

    "have a form" which {
      def form: Element = document().mainContent.selectHead("form")

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a radio button selection" which {
        "has a heading" in {
          form.selectHead("fieldset").selectHead("legend").text() mustBe GlobalCheckYourAnswersMessages.Form.heading
        }
        "has a yes option" in {
          val radio: Element = form.selectHead("fieldset").selectHead(".govuk-radios").selectNth(".govuk-radios__item", 1)
          val input: Element = radio.selectHead("input")
          input.id mustBe GlobalCheckYourAnswersForm.fieldName
          input.attr("name") mustBe GlobalCheckYourAnswersForm.fieldName
          input.attr("type") mustBe "radio"
          input.attr("value") mustBe YesNoMapping.option_yes

          val label: Element = radio.selectHead("label")
          label.text mustBe GlobalCheckYourAnswersMessages.Form.yes
          label.attr("for") mustBe input.id
        }
        "has a no option" in {
          val radio: Element = form.selectHead("fieldset").selectHead(".govuk-radios").selectNth(".govuk-radios__item", 2)
          val input: Element = radio.selectHead("input")
          input.id mustBe s"${GlobalCheckYourAnswersForm.fieldName}-2"
          input.attr("name") mustBe GlobalCheckYourAnswersForm.fieldName
          input.attr("type") mustBe "radio"
          input.attr("value") mustBe YesNoMapping.option_no

          val label: Element = radio.selectHead("label")
          label.text mustBe GlobalCheckYourAnswersMessages.Form.no
          label.attr("for") mustBe input.id
        }
      }

      "has a continue button to submit" in {
        form.selectHead("button").text mustBe GlobalCheckYourAnswersMessages.continue
      }
    }

  }

  val globalCheckYourAnswers: GlobalCheckYourAnswers = app.injector.instanceOf[GlobalCheckYourAnswers]

  def page(form: Form[YesNo], completeDetails: CompleteDetails): Html = globalCheckYourAnswers(
    globalCheckYourAnswersForm = form,
    postAction = testCall,
    backUrl = testBackUrl,
    completeDetails = completeDetails
  )

  def document(details: CompleteDetails = completeDetails()): Document = Jsoup.parse(page(
    form = GlobalCheckYourAnswersForm.form,
    completeDetails = details
  ).body)

  object GlobalCheckYourAnswersMessages {
    val heading: String = "Check your answers before signing up"

    object IncomeSources {
      val heading: String = "1. Income sources"

      object SoleTrader {
        def heading(trade: String): String = s"Trade: $trade"

        val name: String = "Business name"
        val startDate: String = "Trading start date"
        val address: String = "Address"
        val accountingMethod: String = "Accounting method"
        val accountingMethodHeading: String = "Sole trader businesses accounting method"
        val cash: String = "Cash basis accounting"
        val accruals: String = "Traditional accounting"
      }

      object UKProperty {
        val heading: String = "UK property business"
        val startDate: String = "Start date"
        val accountingMethod: String = "Accounting method"
        val cash: String = "Cash basis accounting"
        val accruals: String = "Traditional accounting"
      }

      object ForeignProperty {
        val heading: String = "Foreign property business"
        val startDate: String = "Start date"
        val accountingMethod: String = "Accounting method"
        val cash: String = "Cash basis accounting"
        val accruals: String = "Traditional accounting"
      }

    }

    object SelectedTaxYear {
      val heading: String = "2. Selected tax year"
      val key: String = "Tax year"

      def current(year: Int): String = s"Current tax year (6 April ${year - 1} to 5 April $year)"

      def next(year: Int): String = s"Next tax year (6 April ${year - 1} to 5 April $year)"
    }

    object Software {
      val heading: String = "3. Software for Making Tax Digital for Income Tax"
      val key: String = "Compatible software"
      val yes: String = "Yes"
      val no: String = "No"
    }

    object Form {
      val heading: String = "Is this information correct?"
      val yes: String = "Yes, I’m ready to sign up"
      val no: String = "No, I need to change something"
      val error: String = "Select ‘Yes’ if you’re ready to sign up"
    }

    val continue: String = "Continue"

  }

  def selfEmploymentIncomeSource(accountingMethod: AccountingMethod, count: Int = 1): SoleTraderBusinesses = SoleTraderBusinesses(
    accountingMethod = accountingMethod,
    businesses = (1 to count) map { index =>
      SoleTraderBusiness(
        id = "id",
        name = s"ABC-$index",
        trade = s"Plumbing-$index",
        startDate = LocalDate.of(1980, index, index),
        address = Address(
          lines = Seq(
            s"$index Long Road",
            "Lonely City"
          ),
          postcode = Some(s"ZZ$index${index}ZZ")
        )
      )
    }
  )

  def ukPropertyIncomeSource(accountingMethod: AccountingMethod): UKProperty = UKProperty(
    startDate = LocalDate.of(1980, 1, 2),
    accountingMethod = accountingMethod
  )

  def foreignPropertyIncomeSource(accountingMethod: AccountingMethod): ForeignProperty = ForeignProperty(
    startDate = LocalDate.of(1980, 1, 3),
    accountingMethod = accountingMethod
  )

  def completeDetails(
                       soleTraderBusinesses: Option[SoleTraderBusinesses] = Some(selfEmploymentIncomeSource(Cash)),
                       ukProperty: Option[UKProperty] = Some(ukPropertyIncomeSource(Cash)),
                       foreignProperty: Option[ForeignProperty] = Some(foreignPropertyIncomeSource(Cash)),
                       taxYear: AccountingYear = Current,
                       hasSoftware: Boolean = true
                     ): CompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = soleTraderBusinesses,
      ukProperty = ukProperty,
      foreignProperty = foreignProperty
    ),
    taxYear = taxYear,
    hasSoftware = hasSoftware
  )

  def minDetails(
                  soleTraderBusinesses: Option[SoleTraderBusinesses] = None,
                  ukProperty: Option[UKProperty] = None,
                  foreignProperty: Option[ForeignProperty] = None,
                  taxYear: AccountingYear = Current,
                  hasSoftware: Boolean = true
                ): CompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = soleTraderBusinesses,
      ukProperty = ukProperty,
      foreignProperty = foreignProperty
    ),
    taxYear = taxYear,
    hasSoftware = hasSoftware
  )

}