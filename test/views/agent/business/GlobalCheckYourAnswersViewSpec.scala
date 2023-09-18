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

import forms.submapping.YesNoMapping
import models._
import models.common.business.Address
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import services.GetCompleteDetailsService._
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.business.GlobalCheckYourAnswers

import java.time.LocalDate

class GlobalCheckYourAnswersViewSpec extends ViewSpec {

  "GlobalCheckYourAnswers" must {

    "use the correct template" in new TemplateViewTest (
        view = page(
          completeDetails = completeDetails()
        ),
        title = GlobalCheckYourAnswersMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true,
        error = None
      )


    "have a heading" in {
      document().mainContent.getH1Element.text mustBe GlobalCheckYourAnswersMessages.heading
    }

    "have an income sources heading" in {
      document().mainContent.selectNth("h2", 1).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading("Plumbing-1")
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
            mainContent().selectNth("h2", 1).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading("Plumbing-1")
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
            mainContent.selectNth("h2", 1).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading("Plumbing-1")
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
            mainContent.selectNth("h2", 2).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading("Plumbing-2")
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
            mainContent.selectNth("h2", 3).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.accountingMethodHeading
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
          mainContent().selectNth("h2", 1).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.heading
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
          mainContent(Cash).selectNth("h2", 1).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.heading
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
            mainContent.selectNth("h2", 1).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading("Plumbing-1")
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
            mainContent.selectNth("h2", 2).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.heading
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
            mainContent.selectNth("h2", 3).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.heading
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
      document().mainContent.selectNth("h2", 4).text mustBe GlobalCheckYourAnswersMessages.SelectedTaxYear.heading
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

    "have a correct client infromation heading" in {
      document().mainContent.selectNth("h2", 5).text mustBe GlobalCheckYourAnswersMessages.correctClientInfo.clientInfoHeading
    }

    "have a client information paragraph" in {
      document().mainContent.selectNth("p", 7).text mustBe GlobalCheckYourAnswersMessages.correctClientInfo.clientInfoPara
    }

    "have a form" which {
      def form: Element = document().mainContent.selectHead("form")

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a Sign up my client button to submit" in {
        form.selectHead("button").text mustBe GlobalCheckYourAnswersMessages.signUpMyClient
      }

      "has a I need to change something hyper link and have redirection link to Tasklist" in {
        form.selectHead("a").text mustBe GlobalCheckYourAnswersMessages.needToChange
        form.selectHead("a").attr("href").matches(controllers.agent.routes.TaskListController.show().url)
      }
    }

  }

  val globalCheckYourAnswers: GlobalCheckYourAnswers = app.injector.instanceOf[GlobalCheckYourAnswers]

  def page(completeDetails: CompleteDetails): Html = globalCheckYourAnswers(
    postAction = testCall,
    backUrl = testBackUrl,
    completeDetails = completeDetails
  )

  def document(details: CompleteDetails = completeDetails()): Document = Jsoup.parse(page(
    completeDetails = details
  ).body)

  object GlobalCheckYourAnswersMessages {
    val heading: String = "Check your answers before signing up your client"

    object IncomeSources {
      val heading: String = "Sole trader income source: $trade"

      object SoleTrader {
        def heading(trade: String): String = s"Sole trader income source : $trade"

        val name: String = "Name"
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
      val heading: String = "Selected tax year"
      val key: String = "Tax year"

      def current(year: Int): String = s"Current tax year (6 April ${year - 1} to 5 April $year)"

      def next(year: Int): String = s"Next tax year (6 April ${year - 1} to 5 April $year)"
    }

    object correctClientInfo {
      val clientInfoHeading: String = "Is your client’s information correct?"
      val clientInfoPara: String = "By submitting this application you are confirming that, to the best of your knowledge, the details you are providing are correct."
    }

    val signUpMyClient: String = "Sign up my client"
    val needToChange: String = "No, I need to change something"

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
