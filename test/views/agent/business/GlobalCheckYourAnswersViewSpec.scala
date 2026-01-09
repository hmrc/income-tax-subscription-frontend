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
import models.common.AccountingYearModel
import models.common.business.Address
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import services.GetCompleteDetailsService._
import utilities.UserMatchingSessionUtil.ClientDetails
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

    "have a summary of answers" when {
      "display the yes for using software" in {
        def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 1)

        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.UsingSoftwareSection.key,
            value = Some(""),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.routes.UsingSoftwareController.show(editMode = true).url,
                text = s"${GlobalCheckYourAnswersMessages.SelectedTaxYear.change} ${GlobalCheckYourAnswersMessages.UsingSoftwareSection.key}",
                visuallyHidden = GlobalCheckYourAnswersMessages.UsingSoftwareSection.key
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
                href = controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show(editMode = true).url,
                text = s"${GlobalCheckYourAnswersMessages.SelectedTaxYear.change} ${GlobalCheckYourAnswersMessages.SelectedTaxYear.key}",
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
                href = controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show(editMode = true).url,
                text = s"${GlobalCheckYourAnswersMessages.SelectedTaxYear.change} ${GlobalCheckYourAnswersMessages.SelectedTaxYear.key}",
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

    "display the correct income source headings" when {
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
        mainContent.selectOptionalNth("h2", 4) mustBe None
        mainContent.selectOptionalNth(".govuk-summary-list", 3) mustBe None
      }

      "all income sources are present" should {

        "display the sole trader income sources heading" in {
          document().mainContent.getSubHeading("h2", 2).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading
        }

        "display the first sole trader business" when {
          "there is a table present present" in {
            def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 3)

            summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
              SummaryListRowValues(
                key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade,
                value = Some("Plumbing-1"),
                actions = Seq(
                  SummaryListActionValues(
                    href = GlobalCheckYourAnswersMessages.IncomeSources.firstIncome,
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
                    href = GlobalCheckYourAnswersMessages.IncomeSources.firstIncome,
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
                  href = GlobalCheckYourAnswersMessages.IncomeSources.nextIncome,
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
          document().mainContent.getSubHeading("h2", 3).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.Property.heading
        }

        "display the uk property income" when {
          "there is a stored start date" in {
            def summaryList: Element = document().mainContent.selectNth(".govuk-summary-list", 5)

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
              )
            ))
          }
          "there is no stored start date" in {
            def summaryList: Element = document(
              details = completeDetails(ukProperty = Some(ukPropertyIncomeSource(startDate = None)))
            ).mainContent.selectNth(".govuk-summary-list", 5)

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
              )
            ))
          }
          "there is no stored start date" in {
            def summaryList: Element = document(
              details = completeDetails(foreignProperty = Some(foreignPropertyIncomeSource(startDate = None)))
            ).mainContent.selectNth(".govuk-summary-list", 6)

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
                value = Some(GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.beforeStartDateLimit),
                actions = Seq.empty
              )
            ))
          }
        }
      }
    }

    "have a second paragraph" in {
      document().mainContent.selectNth("p", 3).text mustBe GlobalCheckYourAnswersMessages.para3
    }

    "have a form" which {
      def form: Element = document().mainContent.selectHead("form")

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a confirm and continue button" in {
        form.selectNth(".govuk-button", 1).text mustBe GlobalCheckYourAnswersMessages.confirmAndSend
      }

      "has a save and comeback later button" in {
        val saveAndComeBackLater = form.selectNth(".govuk-button", 2)
        saveAndComeBackLater.text mustBe GlobalCheckYourAnswersMessages.saveAndComeback
        saveAndComeBackLater.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(location = Some("global-check-your-answers")).url
      }
    }

  }

  val globalCheckYourAnswers: GlobalCheckYourAnswers = app.injector.instanceOf[GlobalCheckYourAnswers]

  def page(completeDetails: CompleteDetails, clientDetails: ClientDetails): Html = globalCheckYourAnswers(
    postAction = testCall,
    backUrl = testBackUrl,
    completeDetails = completeDetails,
    clientDetails = clientDetails,
    softwareStatus = None
  )

  def document(
                details: CompleteDetails = completeDetails()
              )(implicit clientDetails: ClientDetails): Document = Jsoup.parse(page(
    completeDetails = details,
    clientDetails = clientDetails
  ).body)


  object GlobalCheckYourAnswersMessages {
    val heading: String = "Check your answers before signing up your client"
    val caption = "FirstName LastName – ZZ 11 11 11 Z"
    val para1: String = "Before your client is signed up to Making Tax Digital for Income Tax you need to check the information you have given us and confirm it is correct. You can change any incorrect information."
    val printLink = "Print this page"
    val subheading = "Check your answers before signing up"

    object UsingSoftwareSection {
      val key: String = "Software works with Making Tax Digital for Income Tax"
      val value: String = "Yes"
    }

    object SelectedTaxYear {
      val key: String = "Selected tax year"
      val current: String = "2025 to 2026"
      val next: String = "2026 to 2027"
      val change: String = "Change"
    }

    object IncomeSources {
      val heading: String = "Sole trader businesses"
      val propertyHeading: String = "Income from property"
      val propertyKey: String = "Property"
      val firstIncome: String = s"${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id-1&isEditMode=true&isGlobalEdit=true"
      val nextIncome: String = s"${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=id-2&isEditMode=true&isGlobalEdit=true"

      object SoleTrader {
        val heading: String = "Sole trader businesses"
        val trade: String = "Trade"
        val name: String = "Business name"
        val startDate: String = "Start date"
        val address: String = "Address"
        val beforeStartDateLimit = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
        val change: String = "Change details"
        val details: String = "of sole trader business"
      }

      object Property {
        val heading: String = "Properties"
      }

      object UKProperty {
        val value: String = "UK property"
        val startDate: String = "Start date"
        val beforeStartDateLimit: String = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
        val change: String = "Change"
        val link: String = controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(isGlobalEdit = true).url
      }

      object ForeignProperty {
        val value: String = "Foreign property"
        val startDate: String = "Start date"
        val beforeStartDateLimit: String = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
        val change: String = "Change"
        val link: String = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(isGlobalEdit = true).url
      }

    }

    val para2: String = "By continuing, you’re confirming that the information you have given is correct to the best of your knowledge."
    val para3: String = "By confirming, you will be signed up to Making Tax Digital for Income Tax."
    val confirmAndSend: String = "Confirm and send"
    val saveAndComeback: String = "Save and come back later"

  }

  private implicit lazy val clientDetails: ClientDetails = ClientDetails(
    name = "FirstName LastName",
    nino = "ZZ 11 11 11 Z"
  )

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
