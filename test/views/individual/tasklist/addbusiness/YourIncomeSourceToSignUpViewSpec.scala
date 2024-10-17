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

import models.common.business._
import models.common.{IncomeSources, OverseasPropertyModel, PropertyModel}
import models.{Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.individual.tasklist.addbusiness.YourIncomeSourceToSignUp

class YourIncomeSourceToSignUpViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  object IndividualIncomeSource {
    val title = "Your income sources"
    val heading: String = title
    val incomeSourcesPara: String = "Add all of these sources that you get income from."

    val addDetails: String = "Add details"
    val check: String = "Check"
    val change: String = "Change"
    val remove: String = "Remove"

    val ukPropertyChange = "(UK property)"
    val ukPropertyRemove = "(UK property)"

    val foreignPropertyChange = "(Foreign property)"
    val foreignPropertyRemove = "(Foreign property)"

    val selfEmploymentHeading = "Sole trader businesses"
    val selfEmploymentPara = "You’re a sole trader if you run your own business as an individual and work for yourself. " +
      "This is also known as being self-employed. You’re not a sole trader if your only business income is from a limited company."
    val addSelfEmploymentLinkText = "Add a sole trader business"
    val soleTraderBusinessNameKey = "Business name"
    val soleTraderLink: String = appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl
    val soleTraderChangeLinkOne: String = s"${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=idOne&isEditMode=true"
    val soleTraderChangeLinkTwo: String = s"${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=idTwo&isEditMode=true"
    val soleTraderChangeLinkThree: String = s"${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=idThree&isEditMode=true"
    val soleTraderChangeLinkFour: String = s"${appConfig.incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl}?id=idFour&isEditMode=true"
    val soleTraderRemoveLinkOne: String = controllers.individual.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show(id="idOne").url
    val soleTraderRemoveLinkTwo: String = controllers.individual.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show(id="idTwo").url
    val soleTraderRemoveLinkThree: String = controllers.individual.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show(id="idThree").url
    val soleTraderRemoveLinkFour: String = controllers.individual.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.show(id="idFour").url

    val incomeFromPropertiesHeading: String = "Income from property"
    val incomeFromPropertiesPara: String = "Tell us about any income you get from property. For example, letting houses, " +
      "flats or holiday homes either on a long or short term basis. " +
      "If you have more than one property, treat them as one income source."

    val propertyStartDate : String = "Start date"
    val ukPropertyCardTitle : String = "UK property"
    val addUKPropertyLink : String = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateController.show().url
    val addUkPropertyLinkText : String = "Add UK property"
    val ukPropertyChangeLink : String = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(editMode = true).url
    val ukPropertyRemoveLink : String = controllers.individual.tasklist.ukproperty.routes.RemoveUkPropertyController.show.url
    val foreignPropertyCardTitle : String = "Foreign property"
    val addForeignPropertyLink : String = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show().url
    val addForeignPropertyLinkText : String = "Add foreign property"
    val foreignPropertyChangeLink : String = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
    val foreignPropertyRemoveLink : String = controllers.individual.tasklist.overseasproperty.routes.RemoveOverseasPropertyController.show.url

    val continue : String = "Continue"
    val saveAndComeBackLater : String = "Save and come back later"

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

  val incompleteSelfEmployments: Seq[SelfEmploymentData] = Seq(SelfEmploymentData("idTwo", None, Some(BusinessNameModel("business name"))),
    SelfEmploymentData("idThree", None, None, businessTradeName = Some(BusinessTradeNameModel("business trade"))),
    SelfEmploymentData("idFour")
  )

  val incompleteUKProperty: Option[PropertyModel] = Some(PropertyModel())
  val incompleteForeignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel())

  def view(incomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None)): Html = {
    incomeSource(
      postAction = testCall,
      backUrl = testBackUrl,
      incomeSources = incomeSources
    )
  }

  class ViewTest(incomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None)) {
    val document: Document = Jsoup.parse(view(incomeSources).body)

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
        view = view(IncomeSources(incompleteSelfEmployments, incompleteUKProperty, incompleteForeignProperty)),
        title = IndividualIncomeSource.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "there are complete income sources added" in new TemplateViewTest(
        view = view(IncomeSources(completeSelfEmployments, completeUKProperty, completeForeignProperty)),
        title = IndividualIncomeSource.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "there are complete and confirmed income sources added" in new TemplateViewTest(
        view = view(IncomeSources(completeAndConfirmedSelfEmployments, completeAndConfirmedUKProperty, completeAndConfirmedForeignProperty)),
        title = IndividualIncomeSource.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
    }
  }

  "YourIncomeSourceToSignUp" when {
    "there are no income sources added" should {
      def noIncomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None)

      "have a heading for the page" in new ViewTest(noIncomeSources) {
        document.mainContent.getH1Element.text mustBe IndividualIncomeSource.heading
      }
      "have a lead paragraph" in new ViewTest(noIncomeSources) {
        document.mainContent.selectNth("p", 1).text mustBe IndividualIncomeSource.incomeSourcesPara
      }
      "have a sole trader section" which {
        "has a heading" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe IndividualIncomeSource.selfEmploymentHeading
        }

        " has a paragraph" in new ViewTest(noIncomeSources) {
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
          document.mainContent.selectNth("p",4).text mustBe IndividualIncomeSource.incomeFromPropertiesPara
        }

        "has an add UK property link" in new ViewTest(noIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-uk-property").selectHead("a")
          link.text mustBe IndividualIncomeSource.addUkPropertyLinkText
          link.attr("href") mustBe IndividualIncomeSource.addUKPropertyLink
        }

        "has an add Foreign property link" in new ViewTest(noIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-foreign-property").selectHead("a")
          link.text mustBe IndividualIncomeSource.addForeignPropertyLinkText
          link.attr("href") mustBe IndividualIncomeSource.addForeignPropertyLink
        }

        "have a form" which {
          def form(document: Document): Element = document.mainContent.getForm

          "has the correct attributes" in new ViewTest(noIncomeSources) {
            form(document).attr("method") mustBe testCall.method
            form(document).attr("action") mustBe testCall.url
          }
          "has a continue button" in new ViewTest(noIncomeSources) {
            form(document).getGovukSubmitButton.text mustBe IndividualIncomeSource.continue
          }
          "has no save and come back later button" in new ViewTest(noIncomeSources) {
            form(document).selectOptionally(".govuk-button--secondary") mustBe None
          }
        }

        "there are complete and confirmed set of income sources added" should {

          def completeAndConfirmedIncomeSources: IncomeSources = IncomeSources(completeAndConfirmedSelfEmployments, completeAndConfirmedUKProperty, completeAndConfirmedForeignProperty)

          "have a heading for the page" in new ViewTest(completeAndConfirmedIncomeSources) {
            document.mainContent.getH1Element.text mustBe IndividualIncomeSource.heading
          }

          "have a lead paragraph" in new ViewTest(completeAndConfirmedIncomeSources) {
            document.mainContent.selectNth("p", 1).text mustBe IndividualIncomeSource.incomeSourcesPara
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

            "has a income from properties section" which {

              "has a heading" in new ViewTest(completeAndConfirmedIncomeSources) {
                document.mainContent.selectNth("h2", 3).text mustBe IndividualIncomeSource.incomeFromPropertiesHeading
              }

              "has a paragraph" in new ViewTest(noIncomeSources) {
                document.mainContent.selectNth("p", 4).text mustBe IndividualIncomeSource.incomeFromPropertiesPara
              }

              "has a UK property card" in new ViewTest(completeAndConfirmedIncomeSources) {
                document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(2))(
                  title = IndividualIncomeSource.ukPropertyCardTitle,
                  cardActions = Seq(
                    SummaryListActionValues(
                      href = IndividualIncomeSource.ukPropertyChangeLink,
                      text = s"${IndividualIncomeSource.change} ${IndividualIncomeSource.ukPropertyChange}",
                      visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                    ),
                    SummaryListActionValues(
                      href = IndividualIncomeSource.ukPropertyRemoveLink,
                      text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.ukPropertyRemove}",
                      visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                    )
                  ),
                  rows = Seq(
                    SummaryListRowValues(
                      key = IndividualIncomeSource.propertyStartDate,
                      value = Some("1 January 1981"),
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
                      text = s"${IndividualIncomeSource.change} ${IndividualIncomeSource.foreignPropertyChange}",
                      visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                    ),
                    SummaryListActionValues(
                      href = IndividualIncomeSource.foreignPropertyRemoveLink,
                      text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.foreignPropertyRemove}",
                      visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                    )
                  ),
                  rows = Seq(
                    SummaryListRowValues(
                      key = IndividualIncomeSource.propertyStartDate,
                      value = Some("1 January 1982"),
                      actions = Seq.empty
                    )
                  )
                )
              }
            }

            "have a form" which {
              def form(document: Document): Element = document.mainContent.getForm

              "has the correct attributes" in new ViewTest(noIncomeSources) {
                form(document).attr("method") mustBe testCall.method
                form(document).attr("action") mustBe testCall.url
              }
              "has a continue button" in new ViewTest(noIncomeSources) {
                form(document).getGovukSubmitButton.text mustBe IndividualIncomeSource.continue
              }
              "has no save and come back later button" in new ViewTest(noIncomeSources) {
                form(document).selectOptionally(".govuk-button--secondary") mustBe None
              }
            }
          }
        }


        "there are complete set of income sources added" should {

          def completeIncomeSources: IncomeSources = IncomeSources(completeSelfEmployments, completeUKProperty, completeForeignProperty)

          "have a heading for the page" in new ViewTest(completeIncomeSources) {
            document.mainContent.getH1Element.text mustBe IndividualIncomeSource.heading
          }

          "have a lead paragraph" in new ViewTest(completeIncomeSources) {
            document.mainContent.selectNth("p", 1).text mustBe IndividualIncomeSource.incomeSourcesPara
          }

          "have a section for sole trader income sources" which {

            "has a heading" in new ViewTest(completeIncomeSources) {
              document.mainContent.selectNth("h2", 1).text mustBe IndividualIncomeSource.selfEmploymentHeading
            }

            "has a paragraph" in new ViewTest(completeIncomeSources) {
              document.mainContent.selectNth("p", 2).text mustBe IndividualIncomeSource.selfEmploymentPara
            }

            "has a sole trader business card" in new ViewTest(completeIncomeSources) {
              document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(1))(
                title = "business trade",
                cardActions = Seq(
                  SummaryListActionValues(
                    href = IndividualIncomeSource.soleTraderChangeLinkOne,
                    text = s"${IndividualIncomeSource.check} business name (business trade)",
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

            "has an add business link" in new ViewTest(completeIncomeSources) {
              val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
              link.text mustBe IndividualIncomeSource.addSelfEmploymentLinkText
              link.attr("href") mustBe IndividualIncomeSource.soleTraderLink
            }

            "has a income from properties section" which {

              "has a heading" in new ViewTest(completeIncomeSources) {
                document.mainContent.selectNth("h2", 3).text mustBe IndividualIncomeSource.incomeFromPropertiesHeading
              }

              "has a paragraph" in new ViewTest(noIncomeSources) {
                document.mainContent.selectNth("p", 4).text mustBe IndividualIncomeSource.incomeFromPropertiesPara
              }

              "has a UK property card" in new ViewTest(completeIncomeSources) {
                document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(2))(
                  title = IndividualIncomeSource.ukPropertyCardTitle,
                  cardActions = Seq(
                    SummaryListActionValues(
                      href = IndividualIncomeSource.ukPropertyChangeLink,
                      text = s"${IndividualIncomeSource.check} ${IndividualIncomeSource.ukPropertyChange}",
                      visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                    ),
                    SummaryListActionValues(
                      href = IndividualIncomeSource.ukPropertyRemoveLink,
                      text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.ukPropertyRemove}",
                      visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                    )
                  ),
                  rows = Seq(
                    SummaryListRowValues(
                      key = IndividualIncomeSource.propertyStartDate,
                      value = Some("1 January 1981"),
                      actions = Seq.empty
                    )
                  )
                )
              }

              "has a foreign property card" in new ViewTest(completeIncomeSources) {
                document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(3))(
                  title = IndividualIncomeSource.foreignPropertyCardTitle,
                  cardActions = Seq(
                    SummaryListActionValues(
                      href = IndividualIncomeSource.foreignPropertyChangeLink,
                      text = s"${IndividualIncomeSource.check} ${IndividualIncomeSource.foreignPropertyChange}",
                      visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                    ),
                    SummaryListActionValues(
                      href = IndividualIncomeSource.foreignPropertyRemoveLink,
                      text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.foreignPropertyRemove}",
                      visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                    )
                  ),
                  rows = Seq(
                    SummaryListRowValues(
                      key = IndividualIncomeSource.propertyStartDate,
                      value = Some("1 January 1982"),
                      actions = Seq.empty
                    )
                  )
                )
              }
            }

            "have a form" which {
              def form(document: Document): Element = document.mainContent.getForm

              "has the correct attributes" in new ViewTest(noIncomeSources) {
                form(document).attr("method") mustBe testCall.method
                form(document).attr("action") mustBe testCall.url
              }
              "has a continue button" in new ViewTest(noIncomeSources) {
                form(document).getGovukSubmitButton.text mustBe IndividualIncomeSource.continue
              }
              "has no save and come back later button" in new ViewTest(noIncomeSources) {
                form(document).selectOptionally(".govuk-button--secondary") mustBe None
              }
            }
          }
        }

        "there are incomplete set of income sources added" should {

          def incompleteIncomeSources: IncomeSources = IncomeSources(incompleteSelfEmployments, incompleteUKProperty, incompleteForeignProperty)

          "have a heading for the page" in new ViewTest(incompleteIncomeSources) {
            document.mainContent.getH1Element.text mustBe IndividualIncomeSource.heading
          }

          "have a lead paragraph" in new ViewTest(incompleteIncomeSources) {
            document.mainContent.selectNth("p", 1).text mustBe IndividualIncomeSource.incomeSourcesPara
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
                    href = IndividualIncomeSource.soleTraderChangeLinkTwo,
                    text = s"${IndividualIncomeSource.addDetails} business name (Business 1)",
                    visuallyHidden = s"business name (Business 1)"
                  ),
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
                  )
                )
              )
            }

            "has a second sole trader business card" in new ViewTest(incompleteIncomeSources) {
              document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(2))(
                title = "business trade",
                cardActions = Seq(
                  SummaryListActionValues(
                    href = IndividualIncomeSource.soleTraderChangeLinkThree,
                    text = s"${IndividualIncomeSource.addDetails} (business trade)",
                    visuallyHidden = s"(business trade)"
                  ),
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
                  )
                )
              )
            }

            "has a third sole trader business card" in new ViewTest(incompleteIncomeSources) {
              document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(3))(
                title = "Business 3",
                cardActions = Seq(
                  SummaryListActionValues(
                    href = IndividualIncomeSource.soleTraderChangeLinkFour,
                    text = s"${IndividualIncomeSource.addDetails} (Business 3)",
                    visuallyHidden = s"(Business 3)"
                  ),
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
                  )
                )
              )
            }

            "has an add business link" in new ViewTest(incompleteIncomeSources) {
              val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
              link.text mustBe IndividualIncomeSource.addSelfEmploymentLinkText
              link.attr("href") mustBe IndividualIncomeSource.soleTraderLink
            }

            "has a income from properties section" which {

              "has a heading" in new ViewTest(incompleteIncomeSources) {
                document.mainContent.selectNth("h2", 5).text mustBe IndividualIncomeSource.incomeFromPropertiesHeading
              }

              "has a paragraph" in new ViewTest(noIncomeSources) {
                document.mainContent.selectNth("p", 4).text mustBe IndividualIncomeSource.incomeFromPropertiesPara
              }

              "has a UK property card" in new ViewTest(incompleteIncomeSources) {
                document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(4))(
                  title = IndividualIncomeSource.ukPropertyCardTitle,
                  cardActions = Seq(
                    SummaryListActionValues(
                      href = IndividualIncomeSource.ukPropertyChangeLink,
                      text = s"${IndividualIncomeSource.addDetails} ${IndividualIncomeSource.ukPropertyChange}",
                      visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                    ),
                    SummaryListActionValues(
                      href = IndividualIncomeSource.ukPropertyRemoveLink,
                      text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.ukPropertyRemove}",
                      visuallyHidden = s"(${IndividualIncomeSource.ukPropertyCardTitle})"
                    )
                  ),
                  rows = Seq(
                    SummaryListRowValues(
                      key = IndividualIncomeSource.propertyStartDate,
                      value = Some(""),
                      actions = Seq.empty
                    )
                  )
                )
              }

              "has a foreign property card" in new ViewTest(incompleteIncomeSources) {
                document.mainContent.mustHaveSummaryCard(".govuk-summary-card", Some(5))(
                  title = IndividualIncomeSource.foreignPropertyCardTitle,
                  cardActions = Seq(
                    SummaryListActionValues(
                      href = IndividualIncomeSource.foreignPropertyChangeLink,
                      text = s"${IndividualIncomeSource.addDetails} ${IndividualIncomeSource.foreignPropertyChange}",
                      visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                    ),
                    SummaryListActionValues(
                      href = IndividualIncomeSource.foreignPropertyRemoveLink,
                      text = s"${IndividualIncomeSource.remove} ${IndividualIncomeSource.foreignPropertyRemove}",
                      visuallyHidden = s"(${IndividualIncomeSource.foreignPropertyCardTitle})"
                    )
                  ),
                  rows = Seq(
                    SummaryListRowValues(
                      key = IndividualIncomeSource.propertyStartDate,
                      value = Some(""),
                      actions = Seq.empty
                    )
                  )
                )
              }
            }

            "have a form" which {
              def form(document: Document): Element = document.mainContent.getForm

              "has the correct attributes" in new ViewTest(noIncomeSources) {
                form(document).attr("method") mustBe testCall.method
                form(document).attr("action") mustBe testCall.url
              }
              "has a continue button" in new ViewTest(noIncomeSources) {
                form(document).getGovukSubmitButton.text mustBe IndividualIncomeSource.continue
              }
              "has no save and come back later button" in new ViewTest(noIncomeSources) {
                form(document).selectOptionally(".govuk-button--secondary") mustBe None
              }
            }
          }
        }
      }
    }
  }

}

