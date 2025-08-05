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

package views.individual

import models._
import models.common.AccountingYearModel
import models.common.business.Address
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import services.GetCompleteDetailsService._
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.GlobalCheckYourAnswers
import config.featureswitch.FeatureSwitch.RemoveAccountingMethod

import java.time.LocalDate

class GlobalCheckYourAnswersViewSpec extends ViewSpec {

  "GlobalCheckYourAnswers" must {

    "use the correct template" in new TemplateViewTest(
      view = page(
        completeDetails = completeDetails()
      ),
      title = GlobalCheckYourAnswersMessages.heading,
      isAgent = false,
      backLink = Some(testBackUrl),
      hasSignOutLink = true,
      error = None
    )

    "have a heading" in {
      document().mainContent.getH1Element.text mustBe GlobalCheckYourAnswersMessages.heading
    }

    "have a first paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe GlobalCheckYourAnswersMessages.paraOne
    }

    "have a print information link" in {
      val link = document().mainContent.selectHead("p > a.govuk-link")
      link.text mustBe GlobalCheckYourAnswersMessages.printLink
      link.attr("data-module") mustBe "hmrc-print-link"
      link.attr("href") mustBe "#"
    }

    "have a before signing up subheading" in {
      document().mainContent.selectNth("h2", 1).text mustBe GlobalCheckYourAnswersMessages.beforeSigningUpHeading
    }

    "have a summary of answers" when {
      "display the yes for using software" in {
        def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 1)

        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.CompatibleSoftware.key,
            value = Some(GlobalCheckYourAnswersMessages.CompatibleSoftware.yes),
            actions = Seq.empty
          )
        ))
      }

      "display the tax year when the client has selected current tax year" in {
        def summaryList: Element = document(details = completeDetails(taxYear = Current)).mainContent.selectNth(".govuk-summary-list", 2)

        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.SelectedTaxYear.key,
            value = Some(GlobalCheckYourAnswersMessages.SelectedTaxYear.current),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show(editMode = true).url,
                text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.SelectedTaxYear.key}",
                visuallyHidden = GlobalCheckYourAnswersMessages.SelectedTaxYear.key
              )
            )
          )
        ))
      }

      "display the tax year when the client has selected next tax year" in {
        def summaryList: Element = document(details = completeDetails(taxYear = Next)).mainContent.selectNth(".govuk-summary-list", 2)

        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.SelectedTaxYear.key,
            value = Some(GlobalCheckYourAnswersMessages.SelectedTaxYear.next),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show(editMode = true).url,
                text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.SelectedTaxYear.key}",
                visuallyHidden = GlobalCheckYourAnswersMessages.SelectedTaxYear.key
              )
            )
          )
        ))
      }

      "display the tax year with no change link when the user did not have a tax year choice" in {
        def summaryList: Element = document(details = minDetails(taxYear = Next)).mainContent.selectNth(".govuk-summary-list", 2)

        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.SelectedTaxYear.key,
            value = Some(GlobalCheckYourAnswersMessages.SelectedTaxYear.next),
            actions = Seq.empty
          )
        ))
      }
    }

    "have the correct income sources displayed" when {

      "no income sources are present" in {
        val mainContent: Element = document(details = minDetails()).mainContent
        mainContent.selectOptionalNth("h2", 2) mustBe None
        mainContent.selectOptionalNth(".govuk-summary-list", 3) mustBe None
      }

      "all income sources are present" should {
        "display the sole trader income sources heading" in {
          document().mainContent.selectNth("h2", 2).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading
        }

        "display the first sole trader business" when {
          "there is a start date present" in {
            def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 3)

            summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade,
                value = Some("Plumbing-1"),
                actions = Seq(
                  SummaryListActionValues(
                    href = s"${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id-1&isEditMode=true&isGlobalEdit=true",
                    text = s"${GlobalCheckYourAnswersMessages.Common.change} Plumbing-1 - ABC-1",
                    visuallyHidden = "Plumbing-1 - ABC-1"
                  )
                )
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.name,
                value = Some("ABC-1"),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.startDate,
                value = Some("1 January 1980"),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.address,
                value = Some("1 Long Road, Lonely City, ZZ11ZZ"),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.accountingMethod,
                value = Some(GlobalCheckYourAnswersMessages.Common.cash),
                actions = Seq.empty
              )
            ))
          }
          "there is no start date present" in {
            def summaryList: Element = document(
              details = completeDetails(soleTraderBusinesses = Some(selfEmploymentIncomeSource(accountingMethod = Some(Cash), startDate = None)))
            ).mainContent.selectNth(".govuk-summary-list", 3)

            summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade,
                value = Some("Plumbing-1"),
                actions = Seq(
                  SummaryListActionValues(
                    href = s"${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id-1&isEditMode=true&isGlobalEdit=true",
                    text = s"${GlobalCheckYourAnswersMessages.Common.change} Plumbing-1 - ABC-1",
                    visuallyHidden = "Plumbing-1 - ABC-1"
                  )
                )
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.name,
                value = Some("ABC-1"),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.startDate,
                value = Some(GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.beforeStartDateLimit),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.address,
                value = Some("1 Long Road, Lonely City, ZZ11ZZ"),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.accountingMethod,
                value = Some(GlobalCheckYourAnswersMessages.Common.cash),
                actions = Seq.empty
              )
            ))
          }
          "there is no accounting method present" in {
            enable(RemoveAccountingMethod)
            def summaryList: Element = document(
              details = completeDetails(soleTraderBusinesses = Some(selfEmploymentIncomeSource(accountingMethod = None, startDate = None)))
            ).mainContent.selectNth(".govuk-summary-list", 3)

            summaryList.mustNotHaveSummaryListRow(key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.accountingMethod)
            disable(RemoveAccountingMethod)
          }
        }

        "display the next sole trader business" in {

          def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 4)

          summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
            SummaryListRowValues(
              key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade,
              value = Some("Plumbing-2"),
              actions = Seq(
                SummaryListActionValues(
                  href = s"${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id-2&isEditMode=true&isGlobalEdit=true",
                  text = s"${GlobalCheckYourAnswersMessages.Common.change} Plumbing-2 - ABC-2",
                  visuallyHidden = "Plumbing-2 - ABC-2"
                )
              )
            ),
            SummaryListRowValues(
              key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.name,
              value = Some("ABC-2"),
              actions = Seq.empty
            ),
            SummaryListRowValues(
              key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.startDate,
              value = Some("1 February 1980"),
              actions = Seq.empty
            ),
            SummaryListRowValues(
              key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.address,
              value = Some("2 Long Road, Lonely City, ZZ22ZZ"),
              actions = Seq.empty
            ),
            SummaryListRowValues(
              key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.accountingMethod,
              value = Some(GlobalCheckYourAnswersMessages.Common.cash),
              actions = Seq.empty
            )
          ))
        }

        "display the property income sources heading" in {
          document().mainContent.selectNth("h2", 3).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.Property.heading
        }

        "display the uk property income" when {
          "there is a stored start date" in {
            def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 5)

            summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.key,
                value = Some(GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.value),
                actions = Seq(
                  SummaryListActionValues(
                    href = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(isGlobalEdit = true).url,
                    text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.value}",
                    visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.value
                  )
                )
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.startDate,
                value = Some("2 January 1980"),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.accountingMethod,
                value = Some(GlobalCheckYourAnswersMessages.Common.cash),
                actions = Seq.empty
              )
            ))
          }
          "there is no stored start date" in {
            def summaryList: Element = document(
              details = completeDetails(ukProperty = Some(ukPropertyIncomeSource(startDate = None)))
            ).mainContent.selectNth(".govuk-summary-list", 5)

            summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.key,
                value = Some(GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.value),
                actions = Seq(
                  SummaryListActionValues(
                    href = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(isGlobalEdit = true).url,
                    text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.value}",
                    visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.value
                  )
                )
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.startDate,
                value = Some(GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.beforeStartDateLimit),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.accountingMethod,
                value = Some(GlobalCheckYourAnswersMessages.Common.cash),
                actions = Seq.empty
              )
            ))
          }
          "there is no accounting method present" in {
            enable(RemoveAccountingMethod)
            def summaryList: Element = document(
              details = completeDetails(ukProperty = Some(ukPropertyIncomeSource(accountingMethod = None, startDate = None)))
            ).mainContent.selectNth(".govuk-summary-list", 5)

            summaryList.mustNotHaveSummaryListRow(key = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.accountingMethod)
            disable(RemoveAccountingMethod)
          }
        }

        "display the foreign property income" when {
          "there is a stored start date" in {
            def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 6)

            summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.key,
                value = Some(GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.value),
                actions = Seq(
                  SummaryListActionValues(
                    href = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(isGlobalEdit = true).url,
                    text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.value}",
                    visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.value
                  )
                )
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.startDate,
                value = Some("3 January 1980"),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.accountingMethod,
                value = Some(GlobalCheckYourAnswersMessages.Common.cash),
                actions = Seq.empty
              )
            ))
          }
          "there is no stored start date" in {
            def summaryList: Element = document(
              details = completeDetails(foreignProperty = Some(foreignPropertyIncomeSource(startDate = None)))
            ).mainContent.selectNth(".govuk-summary-list", 6)

            summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.key,
                value = Some(GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.value),
                actions = Seq(
                  SummaryListActionValues(
                    href = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(isGlobalEdit = true).url,
                    text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.value}",
                    visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.value
                  )
                )
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.startDate,
                value = Some(GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.beforeStartDateLimit),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.accountingMethod,
                value = Some(GlobalCheckYourAnswersMessages.Common.cash),
                actions = Seq.empty
              )
            ))
          }
          "there is no accounting method present" in {
            enable(RemoveAccountingMethod)
            def summaryList: Element = document(
              details = completeDetails(foreignProperty = Some(foreignPropertyIncomeSource(accountingMethod = None, startDate = None)))
            ).mainContent.selectNth(".govuk-summary-list", 6)

            summaryList.mustNotHaveSummaryListRow(key = GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.accountingMethod)
            disable(RemoveAccountingMethod)
          }
        }
      }
    }

    "have a second paragraph" in {
      document().mainContent.selectNth("p", 3).text mustBe GlobalCheckYourAnswersMessages.paraTwo
    }

    "have a third paragraph" in {
      document().mainContent.selectNth("p", 4).text mustBe GlobalCheckYourAnswersMessages.paraThree
    }

    "have a form" which {
      def form: Element = document().mainContent.selectHead("form")

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a confirm and continue button" in {
        form.selectNth(".govuk-button", 1).text mustBe GlobalCheckYourAnswersMessages.Form.confirmAndContinue
      }

      "has a save and comeback later button" in {
        val saveAndComeBackLater = form.selectNth(".govuk-button", 2)
        saveAndComeBackLater.text mustBe GlobalCheckYourAnswersMessages.Form.saveAndComeBack
        saveAndComeBackLater.attr("href") mustBe controllers.individual.tasklist.routes.ProgressSavedController.show(location = Some("global-check-your-answers")).url
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

    val heading: String = "Declaration"

    val paraOne: String = "This is the information you have given to us."

    val printLink = "Print this page"

    val beforeSigningUpHeading: String = "Check your answers before signing up"

    object CompatibleSoftware {
      val key: String = "Software works with Making Tax Digital for Income Tax"
      val yes: String = "Yes"
    }

    object SelectedTaxYear {
      val key: String = "When you’re signing up from"
      val current: String = "Current tax year"
      val next: String = "Next tax year"
    }

    object IncomeSources {

      object SoleTrader {
        val heading: String = "Sole trader businesses"
        val trade: String = "Trade"
        val name: String = "Business name"
        val startDate: String = "Start date"
        val address: String = "Address"
        val accountingMethod: String = "Accounting method"
        val beforeStartDateLimit: String = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
      }

      object Property {
        val heading: String = "Income from property"
      }

      object UKProperty {
        val key: String = "Property"
        val value: String = "UK property"
        val startDate: String = "Start date"
        val accountingMethod: String = "Accounting method"
        val beforeStartDateLimit: String = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
      }

      object ForeignProperty {
        val key: String = "Property"
        val value: String = "Foreign property"
        val startDate: String = "Start date"
        val accountingMethod: String = "Accounting method"
        val beforeStartDateLimit: String = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
      }

    }

    val paraTwo: String = "By continuing, you’re confirming that the information you have given is correct to the best of your knowledge."
    val paraThree: String = "When you continue, we’ll sign you up. This may take a few seconds."

    object Form {
      val confirmAndContinue: String = "Confirm and continue"
      val saveAndComeBack: String = "Save and come back later"
    }

    object Common {
      val change: String = "Change"
      val cash: String = "Cash basis accounting"
      val accruals: String = "Traditional accounting"
    }


  }

  def selfEmploymentIncomeSource(accountingMethod: Option[AccountingMethod], count: Int = 1, startDate: Option[LocalDate] = Some(LocalDate.of(1980, 1, 1))): SoleTraderBusinesses = SoleTraderBusinesses(
    accountingMethod = accountingMethod,
    businesses = (1 to count) map { index =>
      SoleTraderBusiness(
        id = s"id-$index",
        name = s"ABC-$index",
        trade = s"Plumbing-$index",
        startDate = startDate.map(_.plusMonths(index - 1)),
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

  def ukPropertyIncomeSource(accountingMethod: Option[AccountingMethod] = Some(Cash), startDate: Option[LocalDate] = Some(LocalDate.of(1980, 1, 2))): UKProperty = UKProperty(
    startDate = startDate,
    accountingMethod = accountingMethod
  )

  def foreignPropertyIncomeSource(accountingMethod: Option[AccountingMethod] = Some(Cash), startDate: Option[LocalDate] = Some(LocalDate.of(1980, 1, 3))): ForeignProperty = ForeignProperty(
    startDate = startDate,
    accountingMethod = accountingMethod
  )

  def completeDetails(
                       soleTraderBusinesses: Option[SoleTraderBusinesses] = Some(selfEmploymentIncomeSource(Some(Cash), 2)),
                       ukProperty: Option[UKProperty] = Some(ukPropertyIncomeSource(Some(Cash))),
                       foreignProperty: Option[ForeignProperty] = Some(foreignPropertyIncomeSource(Some(Cash))),
                       taxYear: AccountingYear = Current
                     ): CompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = soleTraderBusinesses,
      ukProperty = ukProperty,
      foreignProperty = foreignProperty
    ),
    taxYear = AccountingYearModel(taxYear)
  )

  def minDetails(
                  soleTraderBusinesses: Option[SoleTraderBusinesses] = None,
                  ukProperty: Option[UKProperty] = None,
                  foreignProperty: Option[ForeignProperty] = None,
                  taxYear: AccountingYear = Current,
                ): CompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = soleTraderBusinesses,
      ukProperty = ukProperty,
      foreignProperty = foreignProperty
    ),
    taxYear = AccountingYearModel(taxYear, editable = false)
  )

}
