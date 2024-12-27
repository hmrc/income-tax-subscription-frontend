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

import models._
import models.common.business.Address
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import services.GetCompleteDetailsService._
import utilities.UserMatchingSessionUtil.{ClientDetails, nino}
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.GlobalCheckYourAnswers

import java.time.LocalDate

class GlobalCheckYourAnswersViewSpec extends ViewSpec {

  "GlobalCheckYourAnswers" must {

    "use the correct template" in new TemplateViewTest(
      view = page(
        completeDetails = completeDetails(),
        clientDetails = clientDetails
      ),
      title = GlobalCheckYourAnswersMessages.heading,
      isAgent = true,
      backLink = Some(testBackUrl),
      hasSignOutLink = true,
      error = None
    )

    "have a heading" in {
      document().mustHaveHeadingAndCaption(
        GlobalCheckYourAnswersMessages.heading,
        GlobalCheckYourAnswersMessages.caption,
        isSection = false
      )
    }

    "have a first paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe GlobalCheckYourAnswersMessages.para1
    }

    "have a print information link" in {
      val link = document().mainContent.selectHead("div > p > a.govuk-link")
      link.text mustBe GlobalCheckYourAnswersMessages.printLink
      link.attr("data-module") mustBe "hmrc-print-link"
      link.attr("href") mustBe "#"
    }

    "have an income sources subheading" in {
      document().mainContent.selectNth("h2", 1).text mustBe GlobalCheckYourAnswersMessages.subheading
    }

    "have a summary of answers" when {
      "display the yes for using software" in {
        def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 1)
        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.UsingSoftwareSection.key,
            value = Some(GlobalCheckYourAnswersMessages.UsingSoftwareSection.value),
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
                href = controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show(editMode = true).url,
                text = s"${GlobalCheckYourAnswersMessages.SelectedTaxYear.change} ${GlobalCheckYourAnswersMessages.SelectedTaxYear.visuallyHidden}",
                visuallyHidden = GlobalCheckYourAnswersMessages.SelectedTaxYear.visuallyHidden
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
                href = controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show(editMode = true).url,
                text = s"${GlobalCheckYourAnswersMessages.SelectedTaxYear.change} ${GlobalCheckYourAnswersMessages.SelectedTaxYear.visuallyHidden}",
                visuallyHidden = GlobalCheckYourAnswersMessages.SelectedTaxYear.visuallyHidden
              )
            )
          )
        ))
      }
    }

    "have the correct income sources displayed" when {
      "display the accounting method when cash is selected" in {

        val soleTraderBusinesses = SoleTraderBusinesses(
          accountingMethod = Cash,
          businesses = Seq(SoleTraderBusiness(
            id = "id-1",
            name = "ABC-1",
            trade = "Plumbing",
            startDate = LocalDate.of(1980, 1, 1),
            address = Address(
              lines = Seq("1 Long Road", "Lonely City"),
              postcode = Some("ZZ11ZZ")
            )
          ))
        )

        def summaryList: Element = document(details = completeDetails(soleTraderBusinesses = Some(soleTraderBusinesses))).mainContent.selectNth(".govuk-summary-list", 3)
        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.AccountingMethod.key,
            value = Some(GlobalCheckYourAnswersMessages.IncomeSources.AccountingMethod.cash),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.firstIncome,
                text = s"${GlobalCheckYourAnswersMessages.IncomeSources.AccountingMethod.change} ${GlobalCheckYourAnswersMessages.IncomeSources.AccountingMethod.key}",
                visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.AccountingMethod.key
              )
            )
          )
        ))
      }

      "display the accounting method when accruals is selected" in {

        val soleTraderBusinesses = SoleTraderBusinesses(
          accountingMethod = Accruals,
          businesses = Seq(SoleTraderBusiness(
            id = "id-1",
            name = "ABC-1",
            trade = "Plumbing",
            startDate = LocalDate.of(1980, 1, 1),
            address = Address(
              lines = Seq("1 Long Road", "Lonely City"),
              postcode = Some("ZZ11ZZ")
            )
          ))
        )

        def summaryList: Element = document(details = completeDetails(soleTraderBusinesses = Some(soleTraderBusinesses))).mainContent.selectNth(".govuk-summary-list", 3)
        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.AccountingMethod.key,
            value = Some(GlobalCheckYourAnswersMessages.IncomeSources.AccountingMethod.accruals),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.firstIncome,
                text = s"${GlobalCheckYourAnswersMessages.IncomeSources.AccountingMethod.change} ${GlobalCheckYourAnswersMessages.IncomeSources.AccountingMethod.key}",
                visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.AccountingMethod.key
              )
            )
          )
        ))
      }

      "display the first sole trader business" in {
        def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 4)
        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade,
            value = Some("Plumbing-1"),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.firstIncome,
                text = s"${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.change} ${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade}",
                visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade
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
          )
        ))
      }

      "display the next sole trader business" in {

        def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 5)
        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade,
            value = Some("Plumbing-2"),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.nextIncome,
                text = s"${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.change} ${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade}",
                visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade
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
            value = Some("2 February 1980"),
            actions = Seq.empty
          ),
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.address,
            value = Some("2 Long Road, Lonely City, ZZ22ZZ"),
            actions = Seq.empty
          )
        ))
      }

      "display the uk property income" in {
        def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 6)
        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.propertyKey,
            value = Some(GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.value),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.link,
                text = s"${GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.change} ${GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.value}",
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
            value = Some(GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.cash),
            actions = Seq.empty
          )
        ))
      }

      "display the foreign property income" in {
        def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 7)
        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.propertyKey,
            value = Some(GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.value),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.link,
                text = s"${GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.change} ${GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.value}",
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
            value = Some(GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.cash),
            actions = Seq.empty
          )
        ))
      }
    }

    "have a second paragraph" in {
      document().mainContent.selectNth("p", 3).text mustBe GlobalCheckYourAnswersMessages.para2
    }

    "have a third paragraph" in {
      document().mainContent.selectNth("p", 4).text mustBe GlobalCheckYourAnswersMessages.para3
    }

    "have a form" which {
      def form: Element = document().mainContent.selectHead("form")

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a confirm and continue button" in {
        form.selectNth(".govuk-button", 1).text mustBe GlobalCheckYourAnswersMessages.confirmAndContinue
      }

      "has a save and comeback later button" in {
        val saveAndComeBackLater = form.selectNth(".govuk-button", 2)
        saveAndComeBackLater.text mustBe GlobalCheckYourAnswersMessages.saveAndComeback
        saveAndComeBackLater.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(location = Some("agent.global-check-your-answers")).url
      }
    }

  }

  val globalCheckYourAnswers: GlobalCheckYourAnswers = app.injector.instanceOf[GlobalCheckYourAnswers]

  def page(completeDetails: CompleteDetails, clientDetails: ClientDetails): Html = globalCheckYourAnswers(
    postAction = testCall,
    backUrl = testBackUrl,
    completeDetails = completeDetails,
    clientDetails = clientDetails
  )

  def document(
                details: CompleteDetails = completeDetails()
              )(implicit clientDetails: ClientDetails): Document = Jsoup.parse(page(
    completeDetails = details,
    clientDetails = clientDetails
  ).body)


  object GlobalCheckYourAnswersMessages {
    val heading: String = "Declaration"
    val caption = "FirstName LastName | ZZ 11 11 11 Z"
    val para1: String = "This is the information you have given to us."
    val printLink = "Print this information"
    val subheading = "Check your answers before signing up"

    object UsingSoftwareSection {
      val key: String = "Software works with Making Tax Digital for Income Tax"
      val value: String = "Yes"
    }
    object SelectedTaxYear {
      val key: String = "When you’re signing up from"
      val current: String = "Current tax year"
      val next: String = "Next tax year"
      val change: String = "Change"
      val visuallyHidden: String = "Tax year"
    }

    object IncomeSources {
      val heading: String = "Sole trader businesses"
      val propertyHeading: String = "Income from property"
      val propertyKey: String = "Property"
      val firstIncome: String = s"${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id-1&isEditMode=true&isGlobalEdit=true"
      val nextIncome: String = s"${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id-2&isEditMode=true&isGlobalEdit=true"

      object AccountingMethod {
        val key: String = "Accounting method"
        val cash: String = "Cash basis accounting"
        val accruals: String = "Traditional accounting"
        val change: String = "Change"
      }
      object SoleTrader {
        val trade: String = "Trade"
        val name: String = "Business name"
        val startDate: String = "Start date"
        val address: String = "Address"
        val change: String = "Change"
      }

      object UKProperty {
        val value: String = "UK property"
        val startDate: String = "Start date"
        val accountingMethod: String = "Accounting method"
        val cash: String = "Cash basis accounting"
        val accruals: String = "Traditional accounting"
        val change: String = "Change"
        val link: String = controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(isGlobalEdit=true).url
      }

      object ForeignProperty {
        val value: String = "Foreign property"
        val startDate: String = "Start date"
        val accountingMethod: String = "Accounting method"
        val cash: String = "Cash basis accounting"
        val accruals: String = "Traditional accounting"
        val change: String = "Change"
        val link: String = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(isGlobalEdit=true).url
      }

    }

    val para2: String = "By continuing, you’re confirming that the information you have given is correct to the best of your knowledge."
    val para3: String = "When you continue, we’ll sign up your client. This may take a few seconds."
    val confirmAndContinue: String = "Confirm and continue"
    val saveAndComeback: String = "Save and come back later"

  }

  private implicit val clientDetails: ClientDetails = ClientDetails(
    name = "FirstName LastName",
    nino = "ZZ 11 11 11 Z"
  )

  def selfEmploymentIncomeSource(accountingMethod: AccountingMethod, count: Int = 1): SoleTraderBusinesses = SoleTraderBusinesses(
    accountingMethod = accountingMethod,
    businesses = (1 to count) map { index =>
      SoleTraderBusiness(
        id = s"id-$index",
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
                       soleTraderBusinesses: Option[SoleTraderBusinesses] = Some(selfEmploymentIncomeSource(Cash, 2)),
                       ukProperty: Option[UKProperty] = Some(ukPropertyIncomeSource(Cash)),
                       foreignProperty: Option[ForeignProperty] = Some(foreignPropertyIncomeSource(Cash)),
                       taxYear: AccountingYear = Current
                     ): CompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = soleTraderBusinesses,
      ukProperty = ukProperty,
      foreignProperty = foreignProperty
    ),
    taxYear = taxYear
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
    taxYear = taxYear
  )

}
