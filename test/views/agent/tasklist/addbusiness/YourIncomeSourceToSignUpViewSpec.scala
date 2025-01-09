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

package views.agent.tasklist.addbusiness

import config.featureswitch.FeatureSwitch.PrePopulate
import models.common.business._
import models.common.{IncomeSources, OverseasPropertyModel, PropertyModel}
import models.{Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.tasklist.addbusiness.YourIncomeSourceToSignUp

//scalastyle:off
class YourIncomeSourceToSignUpViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(PrePopulate)
  }

  "YourIncomeSourceToSignUp" should {
    "display the template correctly" when {
      "there are no income sources added" in new TemplateViewTest(
        view = view(),
        title = AgentIncomeSource.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "there are incomplete income sources added" in new TemplateViewTest(
        view = view(IncomeSources(incompleteSelfEmployments, None, incompleteUKProperty, incompleteForeignProperty)),
        title = AgentIncomeSource.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "there are complete income sources added" in new TemplateViewTest(
        view = view(IncomeSources(completeSelfEmployments, Some(Cash), completeUKProperty, completeForeignProperty)),
        title = AgentIncomeSource.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "there are complete and confirmed income sources added" in new TemplateViewTest(
        view = view(IncomeSources(completeAndConfirmedSelfEmployments, Some(Cash), completeAndConfirmedUKProperty, completeAndConfirmedForeignProperty)),
        title = AgentIncomeSource.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
    }
  }

  "YourIncomeSourceToSignUp" must {
    "have a heading and caption" in new ViewTest {
      document.mainContent.mustHaveHeadingAndCaption(
        heading = AgentIncomeSource.heading,
        caption = s"${clientDetails.name} | ${clientDetails.formattedNino}",
        isSection = false
      )
    }

    "have a lead paragraph" which {
      "summarises the page" when {
        "the pre-pop feature switch is disabled" in new ViewTest {
          document.mainContent.selectNth("p", 1).text mustBe AgentIncomeSource.lead
        }
      }
      "summarises the page and tells the user to check sources" when {
        "the pre-pop feature switch is enabled" in new ViewTest {
          enable(PrePopulate)
          document.mainContent.selectNth("p", 1).text mustBe AgentIncomeSource.leadPrepop
        }
      }
    }

    "have a final paragraph" when {
      "the income sources were prepopulated and the income sources have not been confirmed" in new ViewTest(
        incomeSources = completeIncomeSources,
        prepopulated = true
      ) {
        document.mainContent.selectNth("p", 3).text mustBe AgentIncomeSource.beforeYouContinue
      }
      "the income sources were prepopulated and the income sources are missing data items" in new ViewTest(
        incomeSources = incompleteIncomeSources,
        prepopulated = true
      ) {
        document.mainContent.selectNth("p", 3).text mustBe AgentIncomeSource.beforeYouContinue
      }
    }
    "have no final paragraph" when {
      "the income sources were prepopulated, and they were completed and confirmed" in new ViewTest(
        incomeSources = completeAndConfirmedIncomeSources,
        prepopulated = true
      ) {
        document.mainContent.selectOptionalNth("p", 3) mustBe None
      }
      "the income sources were prepopulated, but they were subsequently removed" in new ViewTest(
        incomeSources = noIncomeSources,
        prepopulated = true
      ) {
        document.mainContent.selectOptionalNth("p", 5) mustBe None
      }
      "the income sources weren't prepopulated" in new ViewTest(
        incomeSources = completeIncomeSources,
        prepopulated = false
      ) {
        document.mainContent.selectOptionalNth("p", 3) mustBe None
      }
    }
  }

  "YourIncomeSourceToSignUp" when {
    "there are no income sources added" should {
      "have a sole trader section" which {
        "has a heading" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe AgentIncomeSource.soleTrader
        }
        "has an add business link" in new ViewTest(noIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
          link.text mustBe AgentIncomeSource.soleTraderLinkText
          link.attr("href") mustBe AgentIncomeSource.soleTraderLink
        }
      }
      "have a property section" which {
        "has a heading" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("h2", 2).text mustBe AgentIncomeSource.incomeFromPropertyHeading
        }

        "has an add uk property link" in new ViewTest(noIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-uk-property").selectHead("a")
          link.text mustBe AgentIncomeSource.ukPropertyLinkText
          link.attr("href") mustBe AgentIncomeSource.ukPropertyLink
        }

        "has an add foreign property link" in new ViewTest(noIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-foreign-property").selectHead("a")
          link.text mustBe AgentIncomeSource.foreignPropertyLinkText
          link.attr("href") mustBe AgentIncomeSource.foreignPropertyLink
        }
      }

      "have a form" which {
        def form(document: Document): Element = document.mainContent.getForm

        "has the correct attributes" in new ViewTest(noIncomeSources) {
          form(document).attr("method") mustBe testCall.method
          form(document).attr("action") mustBe testCall.url
        }
        "has no save and come back later button" in new ViewTest(noIncomeSources) {
          val button = form(document).getGovukSubmitButton
          button.text mustBe AgentIncomeSource.saveAndComeBackLater
          button.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(Some("income-sources")).url
        }
      }
    }
    "there are incomplete income sources added" should {
      "have a sole trader section" which {
        "has a heading" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe AgentIncomeSource.soleTrader
        }
        "has a first business card" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(1))(
            title = "business trade",
            cardActions = Seq(
              SummaryListActionValues(
                href = AgentIncomeSource.soleTraderChangeLinkOne,
                text = s"${AgentIncomeSource.addDetails} business name (business trade)",
                visuallyHidden = s"business name (business trade)"
              ),
              SummaryListActionValues(
                href = controllers.agent.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idOne").url,
                text = s"${AgentIncomeSource.remove} business name (business trade)",
                visuallyHidden = s"business name (business trade)"
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = AgentIncomeSource.soleTraderBusinessNameKey,
                value = Some("business name"),
                actions = Seq.empty
              )
            )
          )
        }

        "has a second business card" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(2))(
            title = "Business 2",
            cardActions = Seq(
              SummaryListActionValues(
                href = AgentIncomeSource.soleTraderChangeLinkTwo,
                text = s"${AgentIncomeSource.addDetails} business name (Business 2)",
                visuallyHidden = s"business name (Business 2)"
              ),
              SummaryListActionValues(
                href = controllers.agent.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idTwo").url,
                text = s"${AgentIncomeSource.remove} business name (Business 2)",
                visuallyHidden = s"business name (Business 2)"
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = AgentIncomeSource.soleTraderBusinessNameKey,
                value = Some("business name"),
                actions = Seq.empty
              )
            )
          )
        }

        "has a third business card" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(3))(
            title = "business trade",
            cardActions = Seq(
              SummaryListActionValues(
                href = AgentIncomeSource.soleTraderChangeLinkThree,
                text = s"${AgentIncomeSource.addDetails} (business trade)",
                visuallyHidden = s"(business trade)"
              ),
              SummaryListActionValues(
                href = controllers.agent.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idThree").url,
                text = s"${AgentIncomeSource.remove} (business trade)",
                visuallyHidden = s"(business trade)"
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = AgentIncomeSource.soleTraderBusinessNameKey,
                value = Some("Business 3"),
                actions = Seq.empty
              )
            )
          )
        }

        "has a forth business card" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(4))(
            title = "Business 4",
            cardActions = Seq(
              SummaryListActionValues(
                href = AgentIncomeSource.soleTraderChangeLinkFour,
                text = s"${AgentIncomeSource.addDetails} (Business 4)",
                visuallyHidden = s"(Business 4)"
              ),
              SummaryListActionValues(
                href = controllers.agent.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idFour").url,
                text = s"${AgentIncomeSource.remove} (Business 4)",
                visuallyHidden = s"(Business 4)"
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = AgentIncomeSource.soleTraderBusinessNameKey,
                value = Some("Business 4"),
                actions = Seq.empty
              )
            )
          )
        }

        "has an add business link" in new ViewTest(incompleteIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
          link.text mustBe AgentIncomeSource.soleTraderLinkText
          link.attr("href") mustBe AgentIncomeSource.soleTraderLink
        }
      }
      "have a income from property section" which {
        "has a heading" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.selectNth("h2", 6).text mustBe AgentIncomeSource.incomeFromPropertyHeading
        }
        "has a uk property summary card" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.mustHaveSummaryCard("div.govuk-summary-card", Some(5))(
            title = AgentIncomeSource.ukPropertyTitle,
            cardActions = Seq(
              SummaryListActionValues(
                href = AgentIncomeSource.ukPropertyChangeLink,
                text = s"${AgentIncomeSource.addDetails} ${AgentIncomeSource.ukPropertyChange}",
                visuallyHidden = AgentIncomeSource.ukPropertyChange
              ),
              SummaryListActionValues(
                href = AgentIncomeSource.ukPropertyRemoveLink,
                text = s"${AgentIncomeSource.remove} ${AgentIncomeSource.ukPropertyRemove}",
                visuallyHidden = AgentIncomeSource.ukPropertyRemove
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = AgentIncomeSource.ukPropertyStartDate,
                value = Some("1 January 1981"),
                actions = Seq.empty
              )
            )
          )
        }
        "has a foreign property summary card" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(6))(
            title = AgentIncomeSource.foreignPropertyTitle,
            cardActions = Seq(
              SummaryListActionValues(
                href = AgentIncomeSource.foreignPropertyChangeLink,
                text = s"${AgentIncomeSource.addDetails} ${AgentIncomeSource.foreignPropertyChange}",
                visuallyHidden = AgentIncomeSource.foreignPropertyChange
              ),
              SummaryListActionValues(
                href = AgentIncomeSource.foreignPropertyRemoveLink,
                text = s"${AgentIncomeSource.remove} ${AgentIncomeSource.foreignPropertyRemove}",
                visuallyHidden = AgentIncomeSource.foreignPropertyRemove
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = AgentIncomeSource.foreignPropertyStartDate,
                value = Some("2 February 1982"),
                actions = Seq.empty
              )
            )
          )
        }
      }

      "have a form" which {
        def form(document: Document): Element = document.mainContent.getForm

        "has the correct attributes" in new ViewTest(incompleteIncomeSources) {
          form(document).attr("method") mustBe testCall.method
          form(document).attr("action") mustBe testCall.url
        }

        "has a save and come back later button" in new ViewTest(incompleteIncomeSources) {
          val button = form(document).getGovukSubmitButton
          button.text mustBe AgentIncomeSource.saveAndComeBackLater
          button.attr("href") mustBe AgentIncomeSource.progressSavedLink
        }
      }
    }
    "there are fully complete income sources added" should {
      "have a sole trader section" which {
        "has a heading" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe AgentIncomeSource.soleTrader
        }
        "has a summary card with change link" when {
          "all details are present and confirmed and an accounting method is present" in new ViewTest(completeIncomeSources) {
            document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(1))(
              title = "business trade",
              cardActions = Seq(
                SummaryListActionValues(
                  href = AgentIncomeSource.soleTraderChangeLinkOne,
                  text = s"${AgentIncomeSource.check} business name (business trade)",
                  visuallyHidden = s"business name (business trade)"
                ),
                SummaryListActionValues(
                  href = controllers.agent.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idOne").url,
                  text = s"${AgentIncomeSource.remove} business name (business trade)",
                  visuallyHidden = s"business name (business trade)"
                )
              ),
              rows = Seq(
                SummaryListRowValues(
                  key = AgentIncomeSource.soleTraderBusinessNameKey,
                  value = Some("business name"),
                  actions = Seq.empty
                )
              )
            )
          }
        }
        "has a summary card with add detail link" when {
          "there is no accounting method present in the income sources" in new ViewTest(completeIncomeSources.copy(selfEmploymentAccountingMethod = None)) {
            document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(1))(
              title = "business trade",
              cardActions = Seq(
                SummaryListActionValues(
                  href = AgentIncomeSource.soleTraderChangeLinkOne,
                  text = s"${AgentIncomeSource.addDetails} business name (business trade)",
                  visuallyHidden = s"business name (business trade)"
                ),
                SummaryListActionValues(
                  href = controllers.agent.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idOne").url,
                  text = s"${AgentIncomeSource.remove} business name (business trade)",
                  visuallyHidden = s"business name (business trade)"
                )
              ),
              rows = Seq(
                SummaryListRowValues(
                  key = AgentIncomeSource.soleTraderBusinessNameKey,
                  value = Some("business name"),
                  actions = Seq.empty
                )
              )
            )
          }
        }
        "has an add business link" in new ViewTest(completeIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
          link.text mustBe AgentIncomeSource.soleTraderLinkText
          link.attr("href") mustBe AgentIncomeSource.soleTraderLink
        }
      }
      "have an income from property section" which {
        "has a heading" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("h2", 3).text mustBe AgentIncomeSource.incomeFromPropertyHeading
        }
        "has a uk property summary card" in new ViewTest(completeIncomeSources) {
          document.mainContent.mustHaveSummaryCard("div.govuk-summary-card", Some(2))(
            title = AgentIncomeSource.ukPropertyTitle,
            cardActions = Seq(
              SummaryListActionValues(
                href = AgentIncomeSource.ukPropertyChangeLink,
                text = s"${AgentIncomeSource.check} ${AgentIncomeSource.ukPropertyChange}",
                visuallyHidden = AgentIncomeSource.ukPropertyChange
              ),
              SummaryListActionValues(
                href = AgentIncomeSource.ukPropertyRemoveLink,
                text = s"${AgentIncomeSource.remove} ${AgentIncomeSource.ukPropertyRemove}",
                visuallyHidden = AgentIncomeSource.ukPropertyRemove
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = AgentIncomeSource.ukPropertyStartDate,
                value = Some("1 January 1981"),
                actions = Seq.empty
              )
            )
          )
        }
        "has a foreign property summary card" in new ViewTest(completeIncomeSources) {
          document.mainContent.mustHaveSummaryCard("div.govuk-summary-card", Some(3))(
            title = AgentIncomeSource.foreignPropertyTitle,
            cardActions = Seq(
              SummaryListActionValues(
                href = AgentIncomeSource.foreignPropertyChangeLink,
                text = s"${AgentIncomeSource.check} ${AgentIncomeSource.foreignPropertyChange}",
                visuallyHidden = AgentIncomeSource.foreignPropertyChange
              ),
              SummaryListActionValues(
                href = AgentIncomeSource.foreignPropertyRemoveLink,
                text = s"${AgentIncomeSource.remove} ${AgentIncomeSource.foreignPropertyRemove}",
                visuallyHidden = AgentIncomeSource.foreignPropertyRemove
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = AgentIncomeSource.foreignPropertyStartDate,
                value = Some("2 February 1982"),
                actions = Seq.empty
              )
            )
          )
        }
      }

      "have a form" which {
        def form(document: Document): Element = document.mainContent.getForm

        "has the correct attributes" in new ViewTest(completeIncomeSources) {
          form(document).attr("method") mustBe testCall.method
          form(document).attr("action") mustBe testCall.url
        }

        "has a save and come back later button" in new ViewTest(completeIncomeSources) {
          val button = form(document).getGovukSubmitButton
          button.text mustBe AgentIncomeSource.saveAndComeBackLater
          button.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(Some("income-sources")).url
        }
      }
    }
    "there are fully complete and confirmed income sources added" should {
      def completeAndConfirmedIncomeSources: IncomeSources = IncomeSources(completeAndConfirmedSelfEmployments, Some(Cash), completeAndConfirmedUKProperty, completeAndConfirmedForeignProperty)

      "have a heading and caption" in new ViewTest(completeAndConfirmedIncomeSources) {
        document.mainContent.mustHaveHeadingAndCaption(
          heading = AgentIncomeSource.heading,
          caption = s"${clientDetails.name} | ${clientDetails.formattedNino}",
          isSection = false
        )
      }

      "have a sole trader section" which {
        "has a heading" in new ViewTest(completeAndConfirmedIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe AgentIncomeSource.soleTrader
        }
        "has a summary card" in new ViewTest(completeAndConfirmedIncomeSources) {
          document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(1))(
            title = "business trade",
            cardActions = Seq(
              SummaryListActionValues(
                href = AgentIncomeSource.soleTraderChangeLinkOne,
                text = s"${AgentIncomeSource.change} business name (business trade)",
                visuallyHidden = s"business name (business trade)"
              ),
              SummaryListActionValues(
                href = controllers.agent.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idOne").url,
                text = s"${AgentIncomeSource.remove} business name (business trade)",
                visuallyHidden = s"business name (business trade)"
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = AgentIncomeSource.soleTraderBusinessNameKey,
                value = Some("business name"),
                actions = Seq.empty
              )
            )
          )
        }
        "has an add business link" in new ViewTest(completeAndConfirmedIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
          link.text mustBe AgentIncomeSource.soleTraderLinkText
          link.attr("href") mustBe AgentIncomeSource.soleTraderLink
        }
      }
      "have an income source from property section" which {
        "has a heading" in new ViewTest(completeAndConfirmedIncomeSources) {
          document.mainContent.selectNth("h2", 3).text mustBe AgentIncomeSource.incomeFromPropertyHeading
        }
        "has a uk property summary card" in new ViewTest(completeAndConfirmedIncomeSources) {
          document.mainContent.mustHaveSummaryCard("div.govuk-summary-card", Some(2))(
            title = AgentIncomeSource.ukPropertyTitle,
            cardActions = Seq(
              SummaryListActionValues(
                href = AgentIncomeSource.ukPropertyChangeLink,
                text = s"${AgentIncomeSource.change} ${AgentIncomeSource.ukPropertyChange}",
                visuallyHidden = AgentIncomeSource.ukPropertyChange
              ),
              SummaryListActionValues(
                href = AgentIncomeSource.ukPropertyRemoveLink,
                text = s"${AgentIncomeSource.remove} ${AgentIncomeSource.ukPropertyRemove}",
                visuallyHidden = AgentIncomeSource.ukPropertyRemove
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = AgentIncomeSource.ukPropertyStartDate,
                value = Some("1 January 1981"),
                actions = Seq.empty
              )
            )
          )
        }
        "has a foreign property summary card" in new ViewTest(completeAndConfirmedIncomeSources) {
          document.mainContent.mustHaveSummaryCard("div.govuk-summary-card", Some(3))(
            title = AgentIncomeSource.foreignPropertyTitle,
            cardActions = Seq(
              SummaryListActionValues(
                href = AgentIncomeSource.foreignPropertyChangeLink,
                text = s"${AgentIncomeSource.change} ${AgentIncomeSource.foreignPropertyChange}",
                visuallyHidden = AgentIncomeSource.foreignPropertyChange
              ),
              SummaryListActionValues(
                href = AgentIncomeSource.foreignPropertyRemoveLink,
                text = s"${AgentIncomeSource.remove} ${AgentIncomeSource.foreignPropertyRemove}",
                visuallyHidden = AgentIncomeSource.foreignPropertyRemove
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = AgentIncomeSource.foreignPropertyStartDate,
                value = Some("2 February 1982"),
                actions = Seq.empty
              )
            )
          )
        }
      }

      "have a form" which {
        def form(document: Document): Element = document.mainContent.getForm

        "has the correct attributes" in new ViewTest(completeAndConfirmedIncomeSources) {
          form(document).attr("method") mustBe testCall.method
          form(document).attr("action") mustBe testCall.url
        }
        "has a continue button" in new ViewTest(completeAndConfirmedIncomeSources) {
          form(document).getGovukSubmitButton.text mustBe AgentIncomeSource.continue
        }
        "has a save and come back later button" in new ViewTest(completeAndConfirmedIncomeSources) {
          val button: Element = form(document).selectHead(".govuk-button--secondary")
          button.text mustBe AgentIncomeSource.saveAndComeBackLater
          button.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(Some("income-sources")).url
        }
      }
    }
  }

  object AgentIncomeSource {
    val heading = "Your client’s income sources"
    val lead = "Add all of these sources that your client gets income from."
    val leadPrepop = s"Add all of these sources that your client gets income from. Check, change or add details to any that were started previously. Remove any that ceased before 6 April ${AccountingPeriodUtil.getCurrentTaxEndYear - 1}."
    val paragraph1: String = "If your client is self-employed, you must add all of their sole trader businesses if they have more than one. " +
      "If they have income from property you must add it, but this is limited to one UK property business."
    val paragraph1Overseas: String = "Your client can have up to 50 sole trader businesses. " +
      "However, they can have only one UK property business and one overseas property."
    val paragraph2 = "Renting out a property includes using a letting agency."
    val soleTrader = "Sole trader businesses"
    val soleTraderLinkText = "Add a sole trader business"
    val soleTraderBusinessNameKey = "Business name"
    val soleTraderLink: String = appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl
    val soleTraderChangeLinkOne: String = s"${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=idOne&isEditMode=true"
    val soleTraderChangeLinkTwo: String = s"${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=idTwo&isEditMode=true"
    val soleTraderChangeLinkThree: String = s"${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=idThree&isEditMode=true"
    val soleTraderChangeLinkFour: String = s"${appConfig.agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=idFour&isEditMode=true"
    val soleTraderRemoveLinkOne: String = controllers.agent.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idOne").url
    val soleTraderRemoveLinkTwo: String = controllers.agent.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idTwo").url
    val soleTraderRemoveLinkThree: String = controllers.agent.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idThree").url
    val soleTraderRemoveLinkFour: String = controllers.agent.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idFour").url
    val incomeFromPropertyHeading = "Income from property"
    val ukPropertyTitle = "UK property"
    val ukPropertyStartDate = "Start date"
    val ukPropertyLinkText = "Add UK property"
    val ukPropertyLink: String = controllers.agent.tasklist.ukproperty.routes.PropertyStartDateController.show().url
    val ukPropertyChangeLink: String = controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(editMode = true).url
    val ukPropertyRemoveLink: String = controllers.agent.tasklist.ukproperty.routes.RemoveUkPropertyController.show.url
    val foreignPropertyTitle = "Foreign property"
    val foreignPropertyStartDate = "Start date"
    val foreignPropertyLinkText = "Add foreign property"
    val foreignPropertyLink: String = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show().url
    val foreignPropertyChangeLink: String = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
    val foreignPropertyRemoveLink: String = controllers.agent.tasklist.overseasproperty.routes.RemoveOverseasPropertyController.show.url
    val progressSavedLink: String = controllers.agent.tasklist.routes.ProgressSavedController.show(Some("income-sources")).url

    val continue = "Save and continue"
    val saveAndComeBackLater = "Save and come back later"

    val addDetails: String = "Add details"
    val check: String = "Check"

    val change: String = "Change"
    val remove: String = "Remove"

    def selfEmploymentChange(name: String) = s"$name"

    def selfEmploymentRemove(name: String) = s"$name"

    val ukPropertyChange = "(UK property)"
    val ukPropertyRemove = "(UK property)"

    val foreignPropertyChange = "(Foreign property)"
    val foreignPropertyRemove = "(Foreign property)"

    val beforeYouContinue = "Before you continue, make sure you have checked any income sources we added for you."
  }

  private lazy val incomeSourceView = app.injector.instanceOf[YourIncomeSourceToSignUp]

  private lazy val clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")

  lazy val completeSelfEmployments: Seq[SelfEmploymentData] = Seq(
    SelfEmploymentData(
      id = "idOne",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name")),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel(Address(Seq("1 Long Road"), Some("ZZ1 1ZZ")))))
  )
  lazy val completeUKProperty: Option[PropertyModel] = Some(PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("1", "1", "1981"))))

  lazy val completeForeignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("2", "2", "1982"))))

  lazy val completeAndConfirmedSelfEmployments: Seq[SelfEmploymentData] = Seq(
    SelfEmploymentData(
      id = "idOne",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name")),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel(Address(Seq("1 Long Road"), Some("ZZ1 1ZZ")))),
      confirmed = true
    )
  )
  lazy val completeAndConfirmedUKProperty: Option[PropertyModel] = Some(PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("1", "1", "1981")),
    confirmed = true
  ))
  lazy val completeAndConfirmedForeignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("2", "2", "1982")),
    confirmed = true
  ))

  lazy val incompleteSelfEmployments: Seq[SelfEmploymentData] = Seq(
    SelfEmploymentData(id = "idOne", businessName = Some(BusinessNameModel("business name")), businessTradeName = Some(BusinessTradeNameModel("business trade"))),
    SelfEmploymentData(id = "idTwo", businessName = Some(BusinessNameModel("business name"))),
    SelfEmploymentData(id = "idThree", businessTradeName = Some(BusinessTradeNameModel("business trade"))),
    SelfEmploymentData(id = "idFour")
  )
  lazy val incompleteUKProperty: Option[PropertyModel] = Some(PropertyModel(startDate = Some(DateModel("1", "1", "1981"))))
  lazy val incompleteForeignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel(startDate = Some(DateModel("2", "2", "1982"))))

  def view(incomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None, None), prepopulated: Boolean = false): Html = {
    incomeSourceView(
      postAction = testCall,
      backUrl = testBackUrl,
      clientDetails = clientDetails,
      incomeSources = incomeSources,
      prepopulated = prepopulated
    )
  }

  class ViewTest(incomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None, None), prepopulated: Boolean = false) {
    def document: Document = Jsoup.parse(view(incomeSources, prepopulated).body)
  }

  lazy val noIncomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None, None)
  lazy val incompleteIncomeSources: IncomeSources = IncomeSources(incompleteSelfEmployments, Some(Cash), incompleteUKProperty, incompleteForeignProperty)
  lazy val completeIncomeSources: IncomeSources = IncomeSources(completeSelfEmployments, Some(Cash), completeUKProperty, completeForeignProperty)
  lazy val completeAndConfirmedIncomeSources: IncomeSources = IncomeSources(completeAndConfirmedSelfEmployments, Some(Cash), completeAndConfirmedUKProperty, completeAndConfirmedForeignProperty)


}
