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

    "have a summary of answers" when {
      "display the yes for using software" in {
        def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 1)

        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.CompatibleSoftware.key,
            value = Some(GlobalCheckYourAnswersMessages.CompatibleSoftware.yes),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.individual.routes.UsingSoftwareController.show(editMode = true).url,
                text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.CompatibleSoftware.key}",
                visuallyHidden = GlobalCheckYourAnswersMessages.CompatibleSoftware.key
              )
            )
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

    "have the correct income source headings" when {
      "only sole trader businesses are present" in {
        val mainContent: Element = document(details = minDetails(soleTraderBusinesses = Some(selfEmploymentIncomeSource()))).mainContent
        mainContent.selectHead("#sole-trader-business-heading").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading
        mainContent.selectOptionally("#property-business-heading") mustBe None
      }

      "only property income sources are present" in {
        val mainContent: Element = document(details = minDetails(ukProperty = Some(ukPropertyIncomeSource()))).mainContent
        mainContent.selectOptionally("#sole-trader-business-heading") mustBe None
        mainContent.selectHead("#property-business-heading").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.Property.heading
      }
    }

    "have the correct income sources displayed" when {

      "no income sources are present" in {
        val mainContent: Element = document(details = minDetails()).mainContent
        mainContent.selectOptionally("#sole-trader-business-heading") mustBe None
        mainContent.selectOptionally("#property-business-heading") mustBe None
        mainContent.selectOptionalNth(".govuk-summary-list", 3) mustBe None
      }

      "all income sources are present" should {
        "display the sole trader income sources heading" in {
          document().mainContent.selectHead("#sole-trader-business-heading").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading
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
                    text = s"${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.change} ${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.details} Plumbing-1, ABC-1",
                    visuallyHidden = s"${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.details} Plumbing-1, ABC-1"
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
          "there is no start date present" in {
            def summaryList: Element = document(
              details = completeDetails(soleTraderBusinesses = Some(selfEmploymentIncomeSource(startDate = None)))
            ).mainContent.selectNth(".govuk-summary-list", 3)

            summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade,
                value = Some("Plumbing-1"),
                actions = Seq(
                  SummaryListActionValues(
                    href = s"${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id-1&isEditMode=true&isGlobalEdit=true",
                    text = s"${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.change} ${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.details} Plumbing-1, ABC-1",
                    visuallyHidden = s"${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.details} Plumbing-1, ABC-1"
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
              )
            ))
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
                  text = s"${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.change} ${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.details} Plumbing-2, ABC-2",
                  visuallyHidden = s"${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.details} Plumbing-2, ABC-2"
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
            )
          ))
        }

        "display the property income sources heading" in {
          document().mainContent.selectHead("#property-business-heading").text mustBe GlobalCheckYourAnswersMessages.IncomeSources.Property.heading
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
              )
            ))
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
              )
            ))
          }
        }
      }
    }

    "have a second subheading" in {
      document().mainContent.selectNth("h2", 3).text mustBe GlobalCheckYourAnswersMessages.subheading
    }

    "have a second paragraph" in {
      document().mainContent.selectNth("p", 2).text mustBe GlobalCheckYourAnswersMessages.paraTwo
    }

    "have a third paragraph" in {
      document().mainContent.selectNth("p", 3).text mustBe GlobalCheckYourAnswersMessages.paraThree
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
    completeDetails = completeDetails,
    maybeAccountingPeriod = None
  )

  def document(details: CompleteDetails = completeDetails()): Document = Jsoup.parse(page(
    completeDetails = details
  ).body)

  object GlobalCheckYourAnswersMessages {

    val heading: String = "Check your answers before signing up"

    val paraOne: String = "Before you are signed up to Making Tax Digital for Income Tax, you need to check the information you have given us and confirm it is correct. You can change any incorrect data."

    val printLink = "Print this page"

    val beforeSigningUpHeading: String = "Information we hold for you"

    object CompatibleSoftware {
      val key: String = "Your chosen software works with Making Tax Digital for Income Tax"
      val yes: String = "Yes"
    }

    object SelectedTaxYear {
      val key: String = "Selected tax year"
      val current: String = "2025 to 2026"
      val next: String = "2026 to 2027"
    }

    object IncomeSources {

      object SoleTrader {
        val heading: String = "Sole trader businesses"
        val change: String = "Change details"
        val details: String = "of sole trader business"
        val trade: String = "Trade"
        val name: String = "Business name"
        val startDate: String = "Business start date"
        val address: String = "Business address"
        val beforeStartDateLimit: String = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
      }

      object Property {
        val heading: String = "Property income"
      }

      object UKProperty {
        val key: String = "Property income type"
        val value: String = "UK property"
        val startDate: String = "Tax start date"
        val beforeStartDateLimit: String = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
      }

      object ForeignProperty {
        val key: String = "Property income type"
        val value: String = "Foreign property"
        val startDate: String = "Tax start date"
        val beforeStartDateLimit: String = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
      }
    }

    val subheading: String = "Declaration"
    val paraTwo: String = "The information you have given must be correct to the best of your knowledge."
    val paraThree: String = "By confirming, you will be signed up to Making Tax Digital for Income Tax."

    object Form {
      val confirmAndContinue: String = "Confirm and continue"
      val saveAndComeBack: String = "Save and come back later"
    }

    object Common {
      val change: String = "Change"
    }
  }

  def selfEmploymentIncomeSource(count: Int = 1, startDate: Option[LocalDate] = Some(LocalDate.of(1980, 1, 1))): SoleTraderBusinesses = SoleTraderBusinesses(
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

  def ukPropertyIncomeSource(startDate: Option[LocalDate] = Some(LocalDate.of(1980, 1, 2))): UKProperty = UKProperty(
    startDate = startDate
  )

  def foreignPropertyIncomeSource(startDate: Option[LocalDate] = Some(LocalDate.of(1980, 1, 3))): ForeignProperty = ForeignProperty(
    startDate = startDate
  )

  def completeDetails(
                       soleTraderBusinesses: Option[SoleTraderBusinesses] = Some(selfEmploymentIncomeSource(2)),
                       ukProperty: Option[UKProperty] = Some(ukPropertyIncomeSource()),
                       foreignProperty: Option[ForeignProperty] = Some(foreignPropertyIncomeSource()),
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
