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

import models.*
import models.common.AccountingYearModel
import models.common.business.{Address, Country}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import services.GetCompleteDetailsService.*
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
      hasSignOutLink = true,
      error = None
    )

    "have a heading with a caption" in {
      document().mustHaveHeadingAndCaption(
        GlobalCheckYourAnswersMessages.heading,
        GlobalCheckYourAnswersMessages.caption,
        isSection = false
      )
    }

    "have a print link with a paragraph wrapper" in {
      val wrapper = document().mainContent.selectNth("p", 1)
      val link = wrapper.selectHead("a")

      wrapper.hasClass("govuk-!-display-none-print") mustBe true
      wrapper.hasClass("hmrc-!-js-visible") mustBe true

      link.text mustBe GlobalCheckYourAnswersMessages.printLink
      link.attr("href") mustBe "#"
      link.attr("data-module") mustBe "hmrc-print-link"
    }

    "have the introduction paragraphs" in {
      val mainContent = document().mainContent
      mainContent.selectNth("p", 2).text mustBe GlobalCheckYourAnswersMessages.paraOne
      mainContent.selectNth("p", 3).text mustBe GlobalCheckYourAnswersMessages.paraTwo
    }

    "have an inset note" in {
      document().mainContent.selectHead(".govuk-inset-text").text mustBe GlobalCheckYourAnswersMessages.note
    }

    "have a summary of answers" when {

      "display the tax year with no change link when the user did not have a tax year choice" in {
        def summaryList: Element = document(details = minDetails(taxYear = Next)).mainContent.selectNth(".govuk-summary-list", 1)

        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.SelectedTaxYear.key,
            value = Some(GlobalCheckYourAnswersMessages.SelectedTaxYear.next),
            actions = Seq.empty
          )
        ))
      }

      "display the tax year when the WhenDoYouWantToStart feature switch is enabled and the user is voluntary for next year" in {

        def summaryList: Element = document(details = completeDetails(taxYear = Current)).mainContent.selectNth(".govuk-summary-list", 1)

        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.SelectedTaxYear.key,
            value = Some(GlobalCheckYourAnswersMessages.SelectedTaxYear.current),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.taxyear.routes.WhenDoYouWantToStartController.show(editMode = true).url,
                text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.SelectedTaxYear.change}",
                visuallyHidden = GlobalCheckYourAnswersMessages.SelectedTaxYear.change
              )
            )
          )
        ))
      }

      "display the tax year when the WhenDoYouWantToStart feature switch is enabled and the user is mandated for next year" in {

        def summaryList: Element = document(
          details = completeDetails(taxYear = Current),
          isMandatedNextYear = true
        ).mainContent.selectNth(".govuk-summary-list", 1)

        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.SelectedTaxYear.key,
            value = Some(GlobalCheckYourAnswersMessages.SelectedTaxYear.current),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.taxyear.routes.NextYearMandatorySignUpController.show(editMode = true).url,
                text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.SelectedTaxYear.change}",
                visuallyHidden = GlobalCheckYourAnswersMessages.SelectedTaxYear.change
              )
            )
          )
        ))
      }
    }

    "render sole trader summaries" when {
      "sole trader businesses are present" in {
        val mainContent = document().mainContent
        mainContent.selectNth("h2.govuk-heading-m", 2).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading(1)
        mainContent.selectNth("h2.govuk-heading-m", 3).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.heading(2)

        val firstSoleTraderList = mainContent.selectNth(".govuk-summary-list", 2)
        firstSoleTraderList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.detailsKey,
            value = Some(""),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.detailsLink(1),
                text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.detailsHiddenText("Plumbing-1", "ABC-1")}",
                visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.detailsHiddenText("Plumbing-1", "ABC-1")
              )
            )
          ),
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade,
            value = Some("Plumbing-1"),
            actions = Seq.empty
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
            value = Some("1 Long Road Lonely City ZZ11ZZ United Kingdom"),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.addressLink(1),
                text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.addressHiddenText("Plumbing-1", "ABC-1")}",
                visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.addressHiddenText("Plumbing-1", "ABC-1")
              )
            )
          )
        ))
      }

      "a sole trader start date is not present" in {
        val summaryList = document(
          details = completeDetails(soleTraderBusinesses = Some(selfEmploymentIncomeSource(startDate = None)))
        ).mainContent.selectNth(".govuk-summary-list", 2)

        summaryList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.detailsKey,
            value = Some(""),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.detailsLink(1),
                text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.detailsHiddenText("Plumbing-1", "ABC-1")}",
                visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.detailsHiddenText("Plumbing-1", "ABC-1")
              )
            )
          ),
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.trade,
            value = Some("Plumbing-1"),
            actions = Seq.empty
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
            value = Some("1 Long Road Lonely City ZZ11ZZ United Kingdom"),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.addressLink(1),
                text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.addressHiddenText("Plumbing-1", "ABC-1")}",
                visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.SoleTrader.addressHiddenText("Plumbing-1", "ABC-1")
              )
            )
          )
        ))
      }
    }

    "render property summaries" when {
      "all income sources are present" in {
        val mainContent = document().mainContent
        mainContent.selectNth("h2.govuk-heading-m", 4).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.heading
        mainContent.selectNth("h2.govuk-heading-m", 5).text mustBe GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.heading

        val ukPropertyList = mainContent.selectNth(".govuk-summary-list", 4)
        ukPropertyList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.StartDate.key,
            value = Some("2 January 1980"),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.link,
                text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.changeHiddenText}",
                visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.changeHiddenText
              )
            )
          )
        ))

        val foreignPropertyList = mainContent.selectNth(".govuk-summary-list", 5)
        foreignPropertyList.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.StartDate.key,
            value = Some("3 January 1980"),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.link,
                text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.changeHiddenText}",
                visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.changeHiddenText
              )
            )
          )
        ))
      }

      "only UK property is present" in {
        val mainContent = document(details = minDetails(ukProperty = Some(ukPropertyIncomeSource()))).mainContent
        mainContent.selectOptionally(s"h2:contains(${GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.heading})") must not be None
        mainContent.selectOptionally(s"h2:contains(${GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.heading})") mustBe None
      }

      "only foreign property is present" in {
        val mainContent = document(details = minDetails(foreignProperty = Some(foreignPropertyIncomeSource()))).mainContent
        mainContent.selectOptionally(s"h2:contains(${GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.heading})") mustBe None
        mainContent.selectOptionally(s"h2:contains(${GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.heading})") must not be None
      }

      "a property start date is not present" in {
        val ukPropertySummary = document(
          details = completeDetails(ukProperty = Some(ukPropertyIncomeSource(startDate = None)), foreignProperty = None)
        ).mainContent.selectNth(".govuk-summary-list", 4)

        ukPropertySummary.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = GlobalCheckYourAnswersMessages.IncomeSources.StartDate.key,
            value = Some(GlobalCheckYourAnswersMessages.IncomeSources.beforeStartDateLimit),
            actions = Seq(
              SummaryListActionValues(
                href = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.link,
                text = s"${GlobalCheckYourAnswersMessages.Common.change} ${GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.changeHiddenText}",
                visuallyHidden = GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.changeHiddenText
              )
            )
          )
        ))
      }
    }

    "not render income source sections when there are no income sources" in {
      val mainContent = document(details = minDetails()).mainContent
      mainContent.select(".govuk-summary-list").size() mustBe 1
      mainContent.selectOptionally(s"h2:contains(${GlobalCheckYourAnswersMessages.IncomeSources.UKProperty.heading})") mustBe None
      mainContent.selectOptionally(s"h2:contains(${GlobalCheckYourAnswersMessages.IncomeSources.ForeignProperty.heading})") mustBe None
      mainContent.selectOptionally("h2:contains(Sole trader business)") mustBe None
    }

    "have a declaration section" in {
      document().mainContent.selectNth("h2.govuk-heading-m", 6).text mustBe GlobalCheckYourAnswersMessages.declarationHeading
    }

    "have declaration paragraphs" in {
      val mainContent = document().mainContent
      mainContent.selectNth("p", 4).text mustBe GlobalCheckYourAnswersMessages.declarationParaOne
      mainContent.selectNth("p", 5).text mustBe GlobalCheckYourAnswersMessages.declarationParaTwo
    }

    "have a form" which {
      def form: Element = document().mainContent.selectHead("form")

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a confirm and submit button" in {
        form.selectNth(".govuk-button", 1).text mustBe GlobalCheckYourAnswersMessages.confirmAndSubmit
      }

      "has a save and comeback later button" in {
        val saveAndComeBackLater = form.selectNth(".govuk-button", 2)
        saveAndComeBackLater.text mustBe GlobalCheckYourAnswersMessages.saveAndComeBack
        saveAndComeBackLater.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(location = Some("global-check-your-answers")).url
      }
    }

  }

  val globalCheckYourAnswers: GlobalCheckYourAnswers = app.injector.instanceOf[GlobalCheckYourAnswers]

  def page(completeDetails: CompleteDetails, clientDetails: ClientDetails, isMandatedNextYear: Boolean = false): Html = globalCheckYourAnswers(
    postAction = testCall,
    completeDetails = completeDetails,
    clientDetails = clientDetails,
    isMandatedNextYear = isMandatedNextYear
  )

  def document(
                details: CompleteDetails = completeDetails(),
                isMandatedNextYear: Boolean = false
              )(implicit clientDetails: ClientDetails): Document = Jsoup.parse(page(
    completeDetails = details,
    clientDetails = clientDetails,
    isMandatedNextYear = isMandatedNextYear
  ).body)


  object GlobalCheckYourAnswersMessages {
    val heading: String = "Check your answers before signing up your client"
    val caption: String = "FirstName LastName – ZZ 11 11 11 Z"
    val paraOne: String = "Before your client is signed up to Making Tax Digital for Income Tax you need to check the information you have given us and confirm it is correct."
    val paraTwo: String = "You can change any incorrect information."
    val note = "Make sure that you have not added limited companies or business partnerships here."
    val printLink = "Print this page"

    object SelectedTaxYear {
      val key: String = "Selected tax year"
      val current: String = s"6 April ${AccountingPeriodUtil.getCurrentTaxStartYear} to 5 April ${AccountingPeriodUtil.getCurrentTaxEndYear}"
      val next: String = s"6 April ${AccountingPeriodUtil.getNextTaxStartYear} to 5 April ${AccountingPeriodUtil.getNextTaxEndYear}"
      val change: String = "tax year"
    }

    object IncomeSources {
      object SoleTrader {
        def heading(index: Int): String = s"Sole trader business $index"

        val detailsKey: String = "Business details"
        val trade: String = "Trade"
        val name: String = "Business name"
        val startDate: String = "Business start date"
        val address: String = "Business address"
        val beforeStartDateLimit = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"

        def detailsHiddenText(tradeName: String, businessName: String): String = s"details of sole trader business $tradeName, $businessName"

        def addressHiddenText(tradeName: String, businessName: String): String = s"business address of sole trader business $tradeName, $businessName"

        def detailsLink(index: Int): String = s"${appConfig.agentIncomeTaxSelfEmploymentsFrontendFullIncomeSourceUrl}?id=id-$index&isEditMode=true&isGlobalEdit=true"

        def addressLink(index: Int): String = s"${appConfig.agentIncomeTaxSelfEmploymentsFrontendAddressStartUrl}?id=id-$index&isEditMode=true&isGlobalEdit=true"
      }

      object UKProperty {
        val heading: String = "UK property"
        val value: String = "UK property"
        val link: String = controllers.agent.tasklist.ukproperty.routes.PropertyStartDateBeforeLimitController.show(editMode = true, isGlobalEdit = true).url
        val changeHiddenText: String = "details of UK property"
      }

      object ForeignProperty {
        val heading: String = "Foreign property"
        val value: String = "Foreign property"
        val link: String = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyStartDateBeforeLimitController.show(editMode = true, isGlobalEdit = true).url
        val changeHiddenText: String = "details of foreign property"
      }

      object StartDate {
        val key: String = "Start date"
      }

      val beforeStartDateLimit: String = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"

    }

    val declarationHeading: String = "Declaration"
    val declarationParaOne: String = "The information you have given must be correct to the best of your knowledge."
    val declarationParaTwo: String = "By confirming, your client will be signed up to Making Tax Digital for Income Tax."
    val confirmAndSubmit: String = "Confirm and submit"
    val saveAndComeBack: String = "Save and come back later"

    object Common {
      val change: String = "Change"
    }

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
          postcode = Some(s"ZZ$index${index}ZZ"),
          country = Some(Country(
            code = "GB",
            name = "United Kingdom"
          ))
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
