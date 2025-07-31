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

package views.individual.tasklist.addbusiness

import config.featureswitch.FeatureSwitch.RemoveAccountingMethod
import models.common.business._
import models.common.{IncomeSources, OverseasPropertyModel, PropertyModel}
import models.{Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.ViewSpecTrait
import views.html.individual.tasklist.addbusiness.YourIncomeSourceToSignUp

import java.time.format.DateTimeFormatter

class YourIncomeSourceToSignUpViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(RemoveAccountingMethod)
  }

  object IndividualIncomeSource {
    val title = "Your income sources"
    val heading: String = title
    val currentTaxYearStart: Int = AccountingPeriodUtil.getCurrentTaxEndYear - 1
    val incomeSourcesPara1: String = s"Add all of these sources that you get income from. Check, change or add details to any that were started previously. Remove any that ceased before 6 April $currentTaxYearStart."
    val incomeSourcesPara2: String = "Before you continue, make sure you have checked any income sources we added for you."

    val addDetails: String = "Add details"
    val checkDetails: String = "Check details"
    val change: String = "Change"
    val remove: String = "Remove"
    val statusTagKey = "Status"
    val incompleteTag: String = "Incomplete"
    val completedTag: String = "Completed"

    val ukPropertyHiddenText = "(UK property)"
    val foreignPropertyHiddenText = "(Foreign property)"

    val selfEmploymentHeading = "Sole trader businesses"
    val selfEmploymentPara: String = "You’re a sole trader if you run your own business as an individual and work for yourself. " +
      "This is also known as being self-employed. You’re not a sole trader if your only business income is from a limited company."
    val addSelfEmploymentLinkText = "Add a sole trader business"
    val soleTraderBusinessNameKey = "Business name"
    val soleTraderLink: String = appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl
    val soleTraderChangeLinkOne: String = s"${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=idOne&isEditMode=true"
    val soleTraderChangeLinkTwo: String = s"${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=idTwo&isEditMode=true"
    val soleTraderChangeLinkThree: String = s"${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=idThree&isEditMode=true"
    val soleTraderChangeLinkFour: String = s"${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=idFour&isEditMode=true"
    val soleTraderRemoveLinkOne: String = controllers.individual.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show(id = "idOne").url
    val soleTraderRemoveLinkTwo: String = controllers.individual.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show(id = "idTwo").url
    val soleTraderRemoveLinkThree: String = controllers.individual.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show(id = "idThree").url
    val soleTraderRemoveLinkFour: String = controllers.individual.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show(id = "idFour").url

    val incomeFromPropertiesHeading: String = "Income from property"
    val incomeFromPropertiesPara: String = "Tell us about any income you get from property. For example, letting houses, " +
      "flats or holiday homes either on a long or short term basis. " +
      "If you have more than one property, treat them as one income source."

    val propertyStartDate: String = "Start date"
    val ukPropertyCardTitle: String = "UK property"
    val addUKPropertyLink: String = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateController.show().url
    val addUKPropertyLinkFSEnabled: String = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateBeforeLimitController.show().url
    val addUkPropertyLinkText: String = "Add UK property"
    val ukPropertyChangeLink: String = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(editMode = true).url
    val ukPropertyRemoveLink: String = controllers.individual.tasklist.ukproperty.routes.RemoveUkPropertyController.show.url
    val foreignPropertyCardTitle: String = "Foreign property"
    val addForeignPropertyLink: String = controllers.individual.tasklist.overseasproperty.routes.ForeignPropertyStartDateBeforeLimitController.show().url
    val addForeignPropertyLinkText: String = "Add foreign property"
    val foreignPropertyChangeLink: String = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
    val foreignPropertyRemoveLink: String = controllers.individual.tasklist.overseasproperty.routes.RemoveOverseasPropertyController.show.url

    val propertyDateBeforeLimit = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"

    val continue: String = "Save and continue"
    val saveAndComeBackLater: String = "Save and come back later"
  }

  val backUrl: String = ViewSpecTrait.testBackUrl

  val action: Call = ViewSpecTrait.testCall

  val incomeSource: YourIncomeSourceToSignUp = app.injector.instanceOf[YourIncomeSourceToSignUp]

  val completeAndConfirmedSelfEmployments: Seq[SelfEmploymentData] = Seq(
    SelfEmploymentData(
      id = "idOne",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name")),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel(Address(Seq("1 Long Road"), Some("ZZ1 1ZZ")))),
      confirmed = true
    )
  )
  val completeAndConfirmedUKProperty: Option[PropertyModel] = Some(PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("1", "1", "1981")),
    confirmed = true
  ))
  val completeAndConfirmedForeignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("1", "1", "1982")),
    confirmed = true
  ))

  val completeSelfEmployments: Seq[SelfEmploymentData] = Seq(
    SelfEmploymentData(
      id = "idOne",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name")),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel(Address(Seq("1 Long Road"), Some("ZZ1 1ZZ")))))
  )
  val completeUKProperty: Option[PropertyModel] = Some(PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("1", "1", "1981"))))

  val completeForeignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("1", "1", "1982"))))

  val ukProperty: Option[PropertyModel] = Some(PropertyModel())
  val foreignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel())

  val incompleteSelfEmployments: Seq[SelfEmploymentData] = Seq(SelfEmploymentData(id = "idTwo", businessName = Some(BusinessNameModel("business name"))),
    SelfEmploymentData(id = "idThree", businessTradeName = Some(BusinessTradeNameModel("business trade"))),
    SelfEmploymentData(id = "idFour")
  )

  val incompleteUKProperty: Option[PropertyModel] = Some(PropertyModel())
  val incompleteForeignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel())

  val olderThanLimitDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit.minusDays(1))
  val limitDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

  def view(incomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None, None), isPrePopulated: Boolean = false): Html = {
    incomeSource(
      postAction = testCall,
      backUrl = testBackUrl,
      incomeSources = incomeSources,
      isPrePopulated = isPrePopulated
    )
  }

  class ViewTest(incomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None, None), isPrePopulated: Boolean = false) {
    def document: Document = Jsoup.parse(view(incomeSources, isPrePopulated).body)
  }

  "YourIncomeSourceToSignUp" should {
    "display the template correctly" when {
      "there are no income sources added" in new TemplateViewTest(
        view = view(),
        title = IndividualIncomeSource.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "there are incomplete income sources added" in new TemplateViewTest(
        view = view(IncomeSources(incompleteSelfEmployments, None, incompleteUKProperty, incompleteForeignProperty)),
        title = IndividualIncomeSource.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "there are complete income sources added" in new TemplateViewTest(
        view = view(IncomeSources(completeSelfEmployments, Some(Cash), completeUKProperty, completeForeignProperty)),
        title = IndividualIncomeSource.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "there are complete and confirmed income sources added" in new TemplateViewTest(
        view = view(IncomeSources(completeAndConfirmedSelfEmployments, Some(Cash), completeAndConfirmedUKProperty, completeAndConfirmedForeignProperty)),
        title = IndividualIncomeSource.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
    }
    "have a form" which {
      def form(document: Document): Element = document.mainContent.getForm

      "has the correct attributes" in new ViewTest() {
        form(document).attr("method") mustBe testCall.method
        form(document).attr("action") mustBe testCall.url
      }
      "has a continue button" in new ViewTest() {
        form(document).getGovukSubmitButton.text mustBe IndividualIncomeSource.continue
      }
      "has a save and come back later button" in new ViewTest() {
        val button: Element = form(document).selectHead(".govuk-button--secondary")
        button.text mustBe IndividualIncomeSource.saveAndComeBackLater
        button.attr("href") mustBe controllers.individual.tasklist.routes.ProgressSavedController.show(Some("income-sources")).url
      }
    }
  }

  "YourIncomeSourceToSignUp" when {

    "there are no income sources added" should {
      def noIncomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None, None)

      "have a heading for the page" in new ViewTest(noIncomeSources) {
        document.mainContent.getH1Element.text mustBe IndividualIncomeSource.heading
      }

      "have the correct lead paragraph" in new ViewTest(noIncomeSources) {
        document.mainContent.selectNth("p", 1).text mustBe IndividualIncomeSource.incomeSourcesPara1
      }

      "have a sole trader section" which {
        "has a heading" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe IndividualIncomeSource.selfEmploymentHeading
        }
        "has a paragraph" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("p", 2).text mustBe IndividualIncomeSource.selfEmploymentPara
        }

        "has an add business link" in new ViewTest(noIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
          link.text mustBe IndividualIncomeSource.addSelfEmploymentLinkText
          link.attr("href") mustBe IndividualIncomeSource.soleTraderLink
        }
      }
      "have a income from properties section" which {
        "has a heading" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("h2", 2).text mustBe IndividualIncomeSource.incomeFromPropertiesHeading
        }

        "has a paragraph" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("p", 4).text mustBe IndividualIncomeSource.incomeFromPropertiesPara
        }

        "has an add UK property link" which {
          "goes to the property start date before limit page" in new ViewTest(noIncomeSources) {
            val link: Element = document.mainContent.getElementById("add-uk-property").selectHead("a")
            link.text mustBe IndividualIncomeSource.addUkPropertyLinkText
            link.attr("href") mustBe IndividualIncomeSource.addUKPropertyLinkFSEnabled
          }
        }
      }

      "has an add Foreign property link" in new ViewTest(noIncomeSources) {
        val link: Element = document.mainContent.getElementById("add-foreign-property").selectHead("a")
        link.text mustBe IndividualIncomeSource.addForeignPropertyLinkText
        link.attr("href") mustBe IndividualIncomeSource.addForeignPropertyLink
      }

      "not have a second paragraph" when {
        "data has been pre-populated" in new ViewTest(noIncomeSources, isPrePopulated = true) {
          document.mainContent.selectOptionalNth("p", 7) mustBe None
        }
      }
    }

    "there are incomplete set of income sources added" should {

      def incompleteIncomeSources: IncomeSources = IncomeSources(incompleteSelfEmployments, None, incompleteUKProperty, incompleteForeignProperty)

      "have a heading for the page" in new ViewTest(incompleteIncomeSources) {
        document.mainContent.getH1Element.text mustBe IndividualIncomeSource.heading
      }

      "have the correct lead paragraph" in new ViewTest(incompleteIncomeSources) {
        document.mainContent.selectNth("p", 1).text mustBe IndividualIncomeSource.incomeSourcesPara1
      }

      "have a section for sole trader income sources" which {

        "has a heading" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe IndividualIncomeSource.selfEmploymentHeading
        }

        "has a paragraph" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.selectNth("p", 2).text mustBe IndividualIncomeSource.selfEmploymentPara
        }

        "has a first sole trader business card" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(1))(
            title = "Business 1",
            cardActions = Seq(
              SummaryListActionValues(
                href = IndividualIncomeSource.soleTraderRemoveLinkTwo,
                text = s"${IndividualIncomeSource.remove} business name (Business 1)",
                visuallyHidden = s"business name (Business 1)"
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = IndividualIncomeSource.soleTraderBusinessNameKey,
                value = Some("business name"),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = IndividualIncomeSource.statusTagKey,
                value = Some(IndividualIncomeSource.incompleteTag),
                actions = Seq(SummaryListActionValues(
                  href = IndividualIncomeSource.soleTraderChangeLinkTwo,
                  text = s"${IndividualIncomeSource.addDetails} business name (Business 1)",
                  visuallyHidden = s"business name (Business 1)"
                ))
              )
            )
          )
        }

        "has a second sole trader business card" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(2))(
            title = "business trade",
            cardActions = Seq(
              SummaryListActionValues(
                href = IndividualIncomeSource.soleTraderRemoveLinkThree,
                text = s"${IndividualIncomeSource.remove} (business trade)",
                visuallyHidden = s"(business trade)"
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = IndividualIncomeSource.soleTraderBusinessNameKey,
                value = Some("Business 2"),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = IndividualIncomeSource.statusTagKey,
                value = Some(IndividualIncomeSource.incompleteTag),
                actions = Seq(SummaryListActionValues(
                  href = IndividualIncomeSource.soleTraderChangeLinkThree,
                  text = s"${IndividualIncomeSource.addDetails} (business trade)",
                  visuallyHidden = s"(business trade)"
                ))
              )
            )
          )
        }

        "has a third sole trader business card" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(3))(
            title = "Business 3",
            cardActions = Seq(
              SummaryListActionValues(
                href = IndividualIncomeSource.soleTraderRemoveLinkFour,
                text = s"${IndividualIncomeSource.remove} (Business 3)",
                visuallyHidden = s"(Business 3)"
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = IndividualIncomeSource.soleTraderBusinessNameKey,
                value = Some("Business 3"),
                actions = Seq.empty
              ),
              SummaryListRowValues(
                key = IndividualIncomeSource.statusTagKey,
                value = Some(IndividualIncomeSource.incompleteTag),
                actions = Seq(SummaryListActionValues(
                  href = IndividualIncomeSource.soleTraderChangeLinkFour,
                  text = s"${IndividualIncomeSource.addDetails} (Business 3)",
                  visuallyHidden = "(Business 3)"
                ))
              )
            )
          )
        }

        "has an add business link" in new ViewTest(incompleteIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
          link.text mustBe IndividualIncomeSource.addSelfEmploymentLinkText
          link.attr("href") mustBe IndividualIncomeSource.soleTraderLink
        }

        "have a income from properties section" which {

          "has a heading" in new ViewTest(incompleteIncomeSources) {
            document.mainContent.selectNth("h2", 5).text mustBe IndividualIncomeSource.incomeFromPropertiesHeading
          }

          "has a paragraph" in new ViewTest(incompleteIncomeSources) {
            document.mainContent.selectNth("p", 4).text mustBe IndividualIncomeSource.incomeFromPropertiesPara
          }

          "has a UK property card" in new ViewTest(incompleteIncomeSources) {
            document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(4))(
              title = IndividualIncomeSource.ukPropertyCardTitle,
              cardActions = Seq(
                SummaryListActionValues(
                  href = IndividualIncomeSource.ukPropertyRemoveLink,
                  text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.ukPropertyHiddenText}",
                  visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                )
              ),
              rows = Seq(
                SummaryListRowValues(
                  key = IndividualIncomeSource.propertyStartDate,
                  value = Some(""),
                  actions = Seq.empty
                ),
                SummaryListRowValues(
                  key = IndividualIncomeSource.statusTagKey,
                  value = Some(IndividualIncomeSource.incompleteTag),
                  actions = Seq(SummaryListActionValues(
                    href = IndividualIncomeSource.ukPropertyChangeLink,
                    text = s"${IndividualIncomeSource.addDetails} ${IndividualIncomeSource.ukPropertyHiddenText}",
                    visuallyHidden = IndividualIncomeSource.ukPropertyHiddenText
                  ))
                )
              )
            )
          }

          "has a foreign property card" in new ViewTest(incompleteIncomeSources) {
            document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(5))(
              title = IndividualIncomeSource.foreignPropertyCardTitle,
              cardActions = Seq(
                SummaryListActionValues(
                  href = IndividualIncomeSource.foreignPropertyRemoveLink,
                  text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.foreignPropertyHiddenText}",
                  visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                )
              ),
              rows = Seq(
                SummaryListRowValues(
                  key = IndividualIncomeSource.propertyStartDate,
                  value = Some(""),
                  actions = Seq.empty
                ),
                SummaryListRowValues(
                  key = IndividualIncomeSource.statusTagKey,
                  value = Some(IndividualIncomeSource.incompleteTag),
                  actions = Seq(SummaryListActionValues(
                    href = IndividualIncomeSource.foreignPropertyChangeLink,
                    text = s"${IndividualIncomeSource.addDetails} ${IndividualIncomeSource.foreignPropertyHiddenText}",
                    visuallyHidden = IndividualIncomeSource.foreignPropertyHiddenText
                  ))
                )
              )
            )
          }
        }
      }

      "have a second paragraph" when {
        "data has been pre-populated and not checked by the user" in new ViewTest(incompleteIncomeSources, isPrePopulated = true) {
          document.mainContent.selectNth("p", 5).text mustBe IndividualIncomeSource.incomeSourcesPara2
        }
      }

      "not have a second paragraph" when {
        "data has not been pre-populated" in new ViewTest(incompleteIncomeSources, isPrePopulated = false) {
          document.mainContent.selectOptionalNth("p", 5) mustBe None
        }
      }
    }

    "there are complete set of income sources added" should {
      def completeIncomeSources: IncomeSources = IncomeSources(completeSelfEmployments, Some(Cash), completeUKProperty, completeForeignProperty)

      "have a heading for the page" in new ViewTest(completeIncomeSources) {
        document.mainContent.getH1Element.text mustBe IndividualIncomeSource.heading
      }

      "have the correct lead paragraph" in new ViewTest(completeIncomeSources) {
        document.mainContent.selectNth("p", 1).text mustBe IndividualIncomeSource.incomeSourcesPara1
      }

      "have a section for sole trader income sources" which {

        "has a heading" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe IndividualIncomeSource.selfEmploymentHeading
        }

        "has a paragraph" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("p", 2).text mustBe IndividualIncomeSource.selfEmploymentPara
        }

        "has a summary card with change link" when {
          "all details are present and confirmed and an accounting method is present" in new ViewTest(completeIncomeSources) {
            document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(1))(
              title = "business trade",
              cardActions = Seq(
                SummaryListActionValues(
                  href = controllers.individual.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idOne").url,
                  text = s"${IndividualIncomeSource.remove} business name (business trade)",
                  visuallyHidden = s"business name (business trade)"
                )
              ),
              rows = Seq(
                SummaryListRowValues(
                  key = IndividualIncomeSource.soleTraderBusinessNameKey,
                  value = Some("business name"),
                  actions = Seq.empty
                ),
                SummaryListRowValues(
                  key = IndividualIncomeSource.statusTagKey,
                  value = Some(IndividualIncomeSource.incompleteTag),
                  actions = Seq(SummaryListActionValues(
                    href = IndividualIncomeSource.soleTraderChangeLinkOne,
                    text = s"${IndividualIncomeSource.checkDetails} business name (business trade)",
                    visuallyHidden = "business name (business trade)"
                  ))
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
                  href = controllers.individual.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show("idOne").url,
                  text = s"${IndividualIncomeSource.remove} business name (business trade)",
                  visuallyHidden = s"business name (business trade)"
                )
              ),
              rows = Seq(
                SummaryListRowValues(
                  key = IndividualIncomeSource.soleTraderBusinessNameKey,
                  value = Some("business name"),
                  actions = Seq.empty
                ),
                SummaryListRowValues(
                  key = IndividualIncomeSource.statusTagKey,
                  value = Some(IndividualIncomeSource.incompleteTag),
                  actions = Seq(SummaryListActionValues(
                    href = IndividualIncomeSource.soleTraderChangeLinkOne,
                    text = s"${IndividualIncomeSource.addDetails} business name (business trade)",
                    visuallyHidden = "business name (business trade)"
                  ))
                )
              )
            )
          }
        }

        "has an add business link" in new ViewTest(completeIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
          link.text mustBe IndividualIncomeSource.addSelfEmploymentLinkText
          link.attr("href") mustBe IndividualIncomeSource.soleTraderLink
        }
      }

      "has a income from properties section" which {

        "has a heading" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("h2", 3).text mustBe IndividualIncomeSource.incomeFromPropertiesHeading
        }

        "has a paragraph" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("p", 4).text mustBe IndividualIncomeSource.incomeFromPropertiesPara
        }

        "has a UK property card" which {
          "displays a start date" when {
            "the start date is not before the start date limit" in new ViewTest(
              incomeSources = completeIncomeSources.copy(
                ukProperty = completeUKProperty.map(_.copy(startDate = Some(limitDate)))
              )
            ) {
              document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(2))(
                title = IndividualIncomeSource.ukPropertyCardTitle,
                cardActions = Seq(
                  SummaryListActionValues(
                    href = IndividualIncomeSource.ukPropertyRemoveLink,
                    text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.ukPropertyHiddenText}",
                    visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                  )
                ),
                rows = Seq(
                  SummaryListRowValues(
                    key = IndividualIncomeSource.propertyStartDate,
                    value = Some(limitDate.toLocalDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))),
                    actions = Seq.empty
                  ),
                  SummaryListRowValues(
                    key = IndividualIncomeSource.statusTagKey,
                    value = Some(IndividualIncomeSource.incompleteTag),
                    actions = Seq(SummaryListActionValues(
                      href = IndividualIncomeSource.ukPropertyChangeLink,
                      text = s"${IndividualIncomeSource.checkDetails} ${IndividualIncomeSource.ukPropertyHiddenText}",
                      visuallyHidden = IndividualIncomeSource.ukPropertyHiddenText
                    ))
                  )
                )
              )
            }
          }
          "display before the limit" when {
            "the start date is before the start date limit" in new ViewTest(
              incomeSources = completeIncomeSources.copy(
                ukProperty = completeUKProperty.map(_.copy(startDate = Some(olderThanLimitDate)))
              )
            ) {
              document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(2))(
                title = IndividualIncomeSource.ukPropertyCardTitle,
                cardActions = Seq(
                  SummaryListActionValues(
                    href = IndividualIncomeSource.ukPropertyRemoveLink,
                    text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.ukPropertyHiddenText}",
                    visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                  )
                ),
                rows = Seq(
                  SummaryListRowValues(
                    key = IndividualIncomeSource.propertyStartDate,
                    value = Some(IndividualIncomeSource.propertyDateBeforeLimit),
                    actions = Seq.empty
                  ),
                  SummaryListRowValues(
                    key = IndividualIncomeSource.statusTagKey,
                    value = Some(IndividualIncomeSource.incompleteTag),
                    actions = Seq(SummaryListActionValues(
                      href = IndividualIncomeSource.ukPropertyChangeLink,
                      text = s"${IndividualIncomeSource.checkDetails} ${IndividualIncomeSource.ukPropertyHiddenText}",
                      visuallyHidden = IndividualIncomeSource.ukPropertyHiddenText
                    ))
                  )
                )
              )
            }
            "the start date before limit was answered 'Yes'" in new ViewTest(
              incomeSources = completeIncomeSources.copy(
                ukProperty = completeUKProperty.map(_.copy(startDateBeforeLimit = Some(true), startDate = Some(limitDate)))
              )
            ) {
              document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(2))(
                title = IndividualIncomeSource.ukPropertyCardTitle,
                cardActions = Seq(
                  SummaryListActionValues(
                    href = IndividualIncomeSource.ukPropertyRemoveLink,
                    text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.ukPropertyHiddenText}",
                    visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                  )
                ),
                rows = Seq(
                  SummaryListRowValues(
                    key = IndividualIncomeSource.propertyStartDate,
                    value = Some(IndividualIncomeSource.propertyDateBeforeLimit),
                    actions = Seq.empty
                  ),
                  SummaryListRowValues(
                    key = IndividualIncomeSource.statusTagKey,
                    value = Some(IndividualIncomeSource.incompleteTag),
                    actions = Seq(SummaryListActionValues(
                      href = IndividualIncomeSource.ukPropertyChangeLink,
                      text = s"${IndividualIncomeSource.checkDetails} ${IndividualIncomeSource.ukPropertyHiddenText}",
                      visuallyHidden = IndividualIncomeSource.ukPropertyHiddenText
                    ))
                  )
                )
              )
            }
          }
        }
        "has a foreign property card" which {
          "displays a start date" when {
            "the start date is not before the start date limit" in new ViewTest(
              incomeSources = completeIncomeSources.copy(
                foreignProperty = completeForeignProperty.map(_.copy(startDate = Some(limitDate)))
              )
            ) {
              document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(3))(
                title = IndividualIncomeSource.foreignPropertyCardTitle,
                cardActions = Seq(
                  SummaryListActionValues(
                    href = IndividualIncomeSource.foreignPropertyRemoveLink,
                    text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.foreignPropertyHiddenText}",
                    visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                  )
                ),
                rows = Seq(
                  SummaryListRowValues(
                    key = IndividualIncomeSource.propertyStartDate,
                    value = Some(limitDate.toLocalDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))),
                    actions = Seq.empty
                  ),
                  SummaryListRowValues(
                    key = IndividualIncomeSource.statusTagKey,
                    value = Some(IndividualIncomeSource.incompleteTag),
                    actions = Seq(SummaryListActionValues(
                      href = IndividualIncomeSource.foreignPropertyChangeLink,
                      text = s"${IndividualIncomeSource.checkDetails} ${IndividualIncomeSource.foreignPropertyHiddenText}",
                      visuallyHidden = IndividualIncomeSource.foreignPropertyHiddenText
                    ))
                  )
                )
              )
            }
          }
          "display before the limit" when {
            "the start date is before the start date limit" in new ViewTest(
              incomeSources = completeIncomeSources.copy(
                foreignProperty = completeForeignProperty.map(_.copy(startDate = Some(olderThanLimitDate)))
              )
            ) {
              document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(3))(
                title = IndividualIncomeSource.foreignPropertyCardTitle,
                cardActions = Seq(
                  SummaryListActionValues(
                    href = IndividualIncomeSource.foreignPropertyRemoveLink,
                    text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.foreignPropertyHiddenText}",
                    visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                  )
                ),
                rows = Seq(
                  SummaryListRowValues(
                    key = IndividualIncomeSource.propertyStartDate,
                    value = Some(IndividualIncomeSource.propertyDateBeforeLimit),
                    actions = Seq.empty
                  ),
                  SummaryListRowValues(
                    key = IndividualIncomeSource.statusTagKey,
                    value = Some(IndividualIncomeSource.incompleteTag),
                    actions = Seq(SummaryListActionValues(
                      href = IndividualIncomeSource.foreignPropertyChangeLink,
                      text = s"${IndividualIncomeSource.checkDetails} ${IndividualIncomeSource.foreignPropertyHiddenText}",
                      visuallyHidden = IndividualIncomeSource.foreignPropertyHiddenText
                    ))
                  )
                )
              )
            }
            "the start date before limit was answered 'Yes'" in new ViewTest(
              incomeSources = completeIncomeSources.copy(
                foreignProperty = completeForeignProperty.map(_.copy(startDateBeforeLimit = Some(true), startDate = Some(limitDate)))
              )
            ) {
              document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(3))(
                title = IndividualIncomeSource.foreignPropertyCardTitle,
                cardActions = Seq(
                  SummaryListActionValues(
                    href = IndividualIncomeSource.foreignPropertyRemoveLink,
                    text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.foreignPropertyHiddenText}",
                    visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                  )
                ),
                rows = Seq(
                  SummaryListRowValues(
                    key = IndividualIncomeSource.propertyStartDate,
                    value = Some(IndividualIncomeSource.propertyDateBeforeLimit),
                    actions = Seq.empty
                  ),
                  SummaryListRowValues(
                    key = IndividualIncomeSource.statusTagKey,
                    value = Some(IndividualIncomeSource.incompleteTag),
                    actions = Seq(SummaryListActionValues(
                      href = IndividualIncomeSource.foreignPropertyChangeLink,
                      text = s"${IndividualIncomeSource.checkDetails} ${IndividualIncomeSource.foreignPropertyHiddenText}",
                      visuallyHidden = IndividualIncomeSource.foreignPropertyHiddenText
                    ))
                  )
                )
              )
            }
          }
        }

      }

      "have a second paragraph" when {
        "data has been pre-populated" in new ViewTest(completeIncomeSources, isPrePopulated = true) {
          document.mainContent.selectNth("p", 5).text mustBe IndividualIncomeSource.incomeSourcesPara2
        }
      }
      "not have a second paragraph" when {
        "data has not been pre-populated" in new ViewTest(completeIncomeSources, isPrePopulated = false) {
          document.mainContent.selectOptionalNth("p", 5) mustBe None
        }
      }
    }

    "there are complete and confirmed set of income sources added" should {

      def completeAndConfirmedIncomeSources: IncomeSources = IncomeSources(completeAndConfirmedSelfEmployments, Some(Cash), completeAndConfirmedUKProperty, completeAndConfirmedForeignProperty)
      def completeAndConfirmedIncomeSourcesNoAccMethod: IncomeSources = IncomeSources(
        completeAndConfirmedSelfEmployments, Some(Cash),
        completeAndConfirmedUKProperty.map(_.copy(accountingMethod = None)),
        completeAndConfirmedForeignProperty.map(_.copy(accountingMethod = None)))

      "have a heading for the page" in new ViewTest(completeAndConfirmedIncomeSources) {
        document.mainContent.getH1Element.text mustBe IndividualIncomeSource.heading
      }

      "have the correct lead paragraph" when {
        "have the correct lead paragraph" in new ViewTest(completeAndConfirmedIncomeSources) {
          document.mainContent.selectNth("p", 1).text mustBe IndividualIncomeSource.incomeSourcesPara1
        }
      }

      "have a section for sole trader income sources" which {

        "has a heading" in new ViewTest(completeAndConfirmedIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe IndividualIncomeSource.selfEmploymentHeading
        }

        "has a paragraph" in new ViewTest(completeAndConfirmedIncomeSources) {
          document.mainContent.selectNth("p", 2).text mustBe IndividualIncomeSource.selfEmploymentPara
        }

        "has a sole trader business card" in new ViewTest(completeAndConfirmedIncomeSources) {
          document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(1))(
            title = "business trade",
            cardActions = Seq(
              SummaryListActionValues(
                href = IndividualIncomeSource.soleTraderChangeLinkOne,
                text = s"${IndividualIncomeSource.change} business name (business trade)",
                visuallyHidden = s"business name (business trade)"
              ),
              SummaryListActionValues(
                href = IndividualIncomeSource.soleTraderRemoveLinkOne,
                text = s"${IndividualIncomeSource.remove} business name (business trade)",
                visuallyHidden = s"business name (business trade)"
              )
            ),
            rows = Seq(
              SummaryListRowValues(
                key = IndividualIncomeSource.soleTraderBusinessNameKey,
                value = Some("business name"),
                actions = Seq.empty
              )
            )
          )
        }

        "has an add business link" in new ViewTest(completeAndConfirmedIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
          link.text mustBe IndividualIncomeSource.addSelfEmploymentLinkText
          link.attr("href") mustBe IndividualIncomeSource.soleTraderLink
        }
      }

      "has a income from properties section" which {

        "has a heading" in new ViewTest(completeAndConfirmedIncomeSources) {
          document.mainContent.selectNth("h2", 3).text mustBe IndividualIncomeSource.incomeFromPropertiesHeading
        }

        "has a paragraph" in new ViewTest(completeAndConfirmedIncomeSources) {
          document.mainContent.selectNth("p", 4).text mustBe IndividualIncomeSource.incomeFromPropertiesPara
        }

        "when remove accounting method feature switch disabled" should {
          "has a UK property card" in new ViewTest(completeAndConfirmedIncomeSources) {
            document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(2))(
              title = IndividualIncomeSource.ukPropertyCardTitle,
              cardActions = Seq(
                SummaryListActionValues(
                  href = IndividualIncomeSource.ukPropertyChangeLink,
                  text = s"${IndividualIncomeSource.change} ${IndividualIncomeSource.ukPropertyHiddenText}",
                  visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                ),
                SummaryListActionValues(
                  href = IndividualIncomeSource.ukPropertyRemoveLink,
                  text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.ukPropertyHiddenText}",
                  visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                )
              ),
              rows = Seq(
                SummaryListRowValues(
                  key = IndividualIncomeSource.propertyStartDate,
                  value = Some(IndividualIncomeSource.propertyDateBeforeLimit),
                  actions = Seq.empty
                ),
                SummaryListRowValues(
                  key = IndividualIncomeSource.statusTagKey,
                  value = Some(IndividualIncomeSource.completedTag),
                  actions = Seq.empty
                )
              )
            )
          }

          "has a foreign property card" in new ViewTest(completeAndConfirmedIncomeSources) {
            document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(3))(
              title = IndividualIncomeSource.foreignPropertyCardTitle,
              cardActions = Seq(
                SummaryListActionValues(
                  href = IndividualIncomeSource.foreignPropertyChangeLink,
                  text = s"${IndividualIncomeSource.change} ${IndividualIncomeSource.foreignPropertyHiddenText}",
                  visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                ),
                SummaryListActionValues(
                  href = IndividualIncomeSource.foreignPropertyRemoveLink,
                  text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.foreignPropertyHiddenText}",
                  visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                )
              ),
              rows = Seq(
                SummaryListRowValues(
                  key = IndividualIncomeSource.propertyStartDate,
                  value = Some(IndividualIncomeSource.propertyDateBeforeLimit),
                  actions = Seq.empty
                ),
                SummaryListRowValues(
                  key = IndividualIncomeSource.statusTagKey,
                  value = Some(IndividualIncomeSource.completedTag),
                  actions = Seq.empty
                )
              )
            )
          }
        }
        "when remove accounting method feature switch enabled" should {
          "has a UK property card" in new ViewTest(completeAndConfirmedIncomeSourcesNoAccMethod) {
            enable(RemoveAccountingMethod)
            document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(2))(
              title = IndividualIncomeSource.ukPropertyCardTitle,
              cardActions = Seq(
                SummaryListActionValues(
                  href = IndividualIncomeSource.ukPropertyChangeLink,
                  text = s"${IndividualIncomeSource.change} ${IndividualIncomeSource.ukPropertyHiddenText}",
                  visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                ),
                SummaryListActionValues(
                  href = IndividualIncomeSource.ukPropertyRemoveLink,
                  text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.ukPropertyHiddenText}",
                  visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                )
              ),
              rows = Seq(
                SummaryListRowValues(
                  key = IndividualIncomeSource.propertyStartDate,
                  value = Some(IndividualIncomeSource.propertyDateBeforeLimit),
                  actions = Seq.empty
                ),
                SummaryListRowValues(
                  key = IndividualIncomeSource.statusTagKey,
                  value = Some(IndividualIncomeSource.completedTag),
                  actions = Seq.empty
                )
              )
            )
          }

          "has a foreign property card" in new ViewTest(completeAndConfirmedIncomeSourcesNoAccMethod) {
            enable(RemoveAccountingMethod)
            document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(3))(
              title = IndividualIncomeSource.foreignPropertyCardTitle,
              cardActions = Seq(
                SummaryListActionValues(
                  href = IndividualIncomeSource.foreignPropertyChangeLink,
                  text = s"${IndividualIncomeSource.change} ${IndividualIncomeSource.foreignPropertyHiddenText}",
                  visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                ),
                SummaryListActionValues(
                  href = IndividualIncomeSource.foreignPropertyRemoveLink,
                  text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.foreignPropertyHiddenText}",
                  visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                )
              ),
              rows = Seq(
                SummaryListRowValues(
                  key = IndividualIncomeSource.propertyStartDate,
                  value = Some(IndividualIncomeSource.propertyDateBeforeLimit),
                  actions = Seq.empty
                ),
                SummaryListRowValues(
                  key = IndividualIncomeSource.statusTagKey,
                  value = Some(IndividualIncomeSource.completedTag),
                  actions = Seq.empty
                )
              )
            )
          }
        }
      }

      "not have a second paragraph" when {
        "data has been pre-populated" in new ViewTest(completeAndConfirmedIncomeSources, isPrePopulated = true) {
          document.mainContent.selectOptionally("p:nth-of-type(5)") mustBe None
        }
      }
    }
  }
}





