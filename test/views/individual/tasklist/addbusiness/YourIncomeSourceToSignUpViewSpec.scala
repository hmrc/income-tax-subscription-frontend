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

import config.featureswitch.FeatureSwitch.ForeignProperty
import forms.individual.incomesource.HaveYouCompletedThisSectionForm
import models.common.business._
import models.common.{IncomeSources, OverseasPropertyModel, PropertyModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.individual.tasklist.addbusiness.YourIncomeSourceToSignUp
import models.{Cash, DateModel}

class YourIncomeSourceToSignUpViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ForeignProperty)
  }

  object IndividualIncomeSource {
    val title = "Your income sources"
    val heading: String = title
    val paragraph1: String = "You only need to add the income sources that apply to you."
    val selfEmploymentHeading = "Sole trader (self-employed)"
    val selfEmploymentDescription = "You’re self-employed if you run your own business as an individual and work for yourself. This is also known as being a ‘sole trader’. If you work through a limited company, you’re not a sole trader."
    val addSelfEmploymentLinkText = "Add sole trader income source"
    val soleTraderLink: String = appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl

    def selfEmploymentChange(name: String) = s"Change $name"

    def selfEmploymentRemove(name: String) = s"Remove $name"

    val addAnotherSelfEmploymentLinkText = "Add another sole trader income source"
    val ukPropertyHeading = "UK property business"
    val ukPropertyDescription = "A UK property business is when you get income from land or buildings in the UK. For example, letting houses, flats or holiday homes either on a long or short term basis."
    val ukPropertyLabel = "UK property income source"
    val ukPropertyLinkText = "Add UK property income source"
    val ukPropertyChange = "Change UK property income source"
    val ukPropertyRemove = "Remove UK property income source"
    val foreignPropertyHeading = "Foreign property business"
    val foreignPropertyDescription = "If you get income from property in another country, you have a foreign property business. For example, letting houses, flats or holiday homes on a long or short term basis."
    val foreignPropertyLabel = "Foreign property income source"
    val foreignPropertyLinkText = "Add foreign property business"
    val foreignPropertyChange = "Change foreign property income source"
    val foreignPropertyRemove = "Remove foreign property income source"
    val completedThisSectionFormHeading = "Have you added all your income source information?"
    val completedSectionYes = "Yes, I have completed the information"
    val completedSectionNo = "No, I’ll come back to it later"

    val continue = "Continue"
    val saveAndComeBackLater = "Save and come back later"

    val change: String = "Change"
    val remove: String = "Remove"
  }

  val backUrl: String = ViewSpecTrait.testBackUrl

  val action: Call = ViewSpecTrait.testCall

  val incomeSource: YourIncomeSourceToSignUp = app.injector.instanceOf[YourIncomeSourceToSignUp]

  val completeSelfEmployments: Seq[SelfEmploymentData] = Seq(
    SelfEmploymentData(
      id = "idOne",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name")),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel(Address(Seq("1 Long Road"), Some("ZZ1 1ZZ")))),
      confirmed = true
    )
  )
  val completeUKProperty: Option[PropertyModel] = Some(PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("1", "1", "1981")),
    confirmed = true
  ))
  val completeForeignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("1", "1", "1982")),
    confirmed = true
  ))
  val ukProperty: Option[PropertyModel] = Some(PropertyModel())
  val foreignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel())

  val incompleteSelfEmployments: Seq[SelfEmploymentData] = Seq( SelfEmploymentData("idTwo", None, Some(BusinessNameModel("business name"))),
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

  class ViewTest(incomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None),
                 foreignPropertyEnabled: Boolean = true) {

    if (foreignPropertyEnabled) enable(ForeignProperty)

    val document: Document = Jsoup.parse(view(incomeSources).body)

  }

  "YourIncomeSourceToSignUp" should {
    "display the template correctly" when {
      "the are no income sources added" in new TemplateViewTest(
        view = view(),
        title = IndividualIncomeSource.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "the are incomplete income sources added" in new TemplateViewTest(
        view = view(IncomeSources(incompleteSelfEmployments, incompleteUKProperty, incompleteForeignProperty)),
        title = IndividualIncomeSource.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "the are complete income sources added" in new TemplateViewTest(
        view = view(IncomeSources(completeSelfEmployments, completeUKProperty, completeForeignProperty)),
        title = IndividualIncomeSource.heading,
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
        document.mainContent.selectHead("p.govuk-body-l").text mustBe IndividualIncomeSource.paragraph1
      }
      "have a sole trader section" which {
        "has a heading" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe IndividualIncomeSource.selfEmploymentHeading
        }
        "has an add business link" in new ViewTest(noIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
          link.text mustBe IndividualIncomeSource.addSelfEmploymentLinkText
          link.attr("href") mustBe IndividualIncomeSource.soleTraderLink
        }
      }
      "have a uk property section" which {
        "has a heading" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("h2", 2).text mustBe IndividualIncomeSource.ukPropertyHeading
        }

        "has an add business link" in new ViewTest(noIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-uk-property").selectHead("a")
          link.text mustBe IndividualIncomeSource.ukPropertyLinkText
          link.attr("href") mustBe controllers.individual.tasklist.ukproperty.routes.PropertyStartDateController.show().url
      }
      "have a foreign property section" when {
        "the foreign property feature switch is enabled" which {
          "has a heading" in new ViewTest(noIncomeSources) {
            document.mainContent.selectNth("h2", 3).text mustBe IndividualIncomeSource.foreignPropertyHeading
          }
          "has an add business link" in new ViewTest(noIncomeSources) {
            val link: Element = document.mainContent.getElementById("add-foreign-property").selectHead("a")
            link.text mustBe IndividualIncomeSource.foreignPropertyLinkText
            link.attr("href") mustBe controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show().url
          }
        }
      }
      "have no foreign property section" when {
        "the foreign property feature switch is disabled" in new ViewTest(noIncomeSources, foreignPropertyEnabled = false) {
          document.mainContent.selectOptionalNth("h2", 3) mustBe None
          document.mainContent.selectOptionally("#add-foreign-property") mustBe None
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

  "YourIncomeSourceToSignUp" should {
    "use the correct template" in new TemplateViewTest(
      view = view(),
      title = IndividualIncomeSource.title,
      isAgent = false,
      backLink = Some(testBackUrl),
    )

    "have a heading" in new ViewTest {
      document.mainContent.selectHead("h1").text mustBe IndividualIncomeSource.heading
    }

    "have a lead paragraph" in new ViewTest {
      document.mainContent.selectHead("p.govuk-body-l").text mustBe IndividualIncomeSource.paragraph1
    }
  }

  "YourIncomeSourceToSignUp" when {
    "there are no income sources added" should {
      "have a section for sole trader income sources" which {
        "has a heading" in new ViewTest {
          document.mainContent.selectNth("h2", 1).text mustBe IndividualIncomeSource.selfEmploymentHeading
        }

        "has a description" in new ViewTest {
          document.mainContent.selectNth("p", 2).text mustBe IndividualIncomeSource.selfEmploymentDescription
        }

        "has a link to add a self employment business" in new ViewTest {
          val link: Element = document.mainContent.selectNth("a", 1)
          link.text mustBe IndividualIncomeSource.addSelfEmploymentLinkText
          link.attr("href") mustBe appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl
        }
      }

      "have a section for uk property" which {
        "has a heading" in new ViewTest {
          document.mainContent.selectNth("h2", 2).text mustBe IndividualIncomeSource.ukPropertyHeading
        }
        "has a description" in new ViewTest {
          document.mainContent.selectNth("p", 3).text mustBe IndividualIncomeSource.ukPropertyDescription
        }
        "has a link to add a uk property business" in new ViewTest {
          val link: Element = document.mainContent.selectNth("a", 2)
          link.text mustBe IndividualIncomeSource.ukPropertyLinkText
          link.attr("href") mustBe controllers.individual.tasklist.ukproperty.routes.PropertyStartDateController.show().url
        }
      }

      "have a section for foreign property" when {
        "the foreign property feature switch is enabled" which {
          "has a heading" in new ViewTest {
            document.mainContent.selectNth("h2", 3).text mustBe IndividualIncomeSource.foreignPropertyHeading
          }
          "has a description" in new ViewTest {
            document.mainContent.selectNth("p", 4).text mustBe IndividualIncomeSource.foreignPropertyDescription
          }
          "has a link to add a foreign property business" in new ViewTest {
            val link: Element = document.mainContent.selectNth("a", 3)
            link.text mustBe IndividualIncomeSource.foreignPropertyLinkText
            link.attr("href") mustBe controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show().url
          }
        }
      }

      "have no section for foreign property" when {
        "the foreign property feature switch is not enabled" in new ViewTest(foreignPropertyEnabled = false) {
          document.mainContent.selectOptionalNth("h2", 3) mustBe None
        }
      }
    }

    "there are a full set of income sources added" should {

      def completeIncomeSources: IncomeSources = IncomeSources(completeSelfEmployments, completeUKProperty, completeForeignProperty)
      "have a section for sole trader income sources" which {
        "has a heading" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe IndividualIncomeSource.selfEmploymentHeading
        }

        "has a description" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("p", 2).text mustBe IndividualIncomeSource.selfEmploymentDescription
        }

        "has a summary of incomplete self employed businesses" which {
          def selfEmploymentSummary(document: Document): Element = document.mainContent.selectNth("dl", 1)

          "has a first business" which {
            def businessSummary(document: Document): Element = selfEmploymentSummary(document)
              .selectNth("div.govuk-summary-list__row", 1)

            "has a label" in new ViewTest(completeIncomeSources) {
              businessSummary(document).selectHead("dt").text mustBe "business trade - business name"
            }
            "has a set of actions" which {
              def actions(document: Document): Element = businessSummary(document).selectHead("dd").selectHead("ul")

              "has a change action" in new ViewTest(completeIncomeSources) {
                val link: Element = actions(document).selectNth("li", 1).selectHead("a")
                link.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.change
                link.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.selfEmploymentChange("business trade - business name")
              }
              "has a remove action" in new ViewTest(completeIncomeSources) {
                val link: Element = actions(document).selectNth("li", 2).selectHead("a")
                link.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.remove
                link.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.selfEmploymentRemove("business trade - business name")
              }
            }
          }
        }
      }

      "have a section for uk property" which {
        "has a heading" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("h2", 2).text mustBe IndividualIncomeSource.ukPropertyHeading
        }
        "has a description" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("p", 3).text mustBe IndividualIncomeSource.ukPropertyDescription
        }
        "has a summary of the added uk property business" in new ViewTest(completeIncomeSources) {
          val summaryList: Element = document.mainContent.selectNth("dl", 2)
          val row: Element = summaryList.selectNth("div.govuk-summary-list__row", 1)

          row.selectHead("dt").text mustBe IndividualIncomeSource.ukPropertyLabel

          val actions: Element = row.selectHead("dd")
          val changeLink: Element = actions.selectHead("ul").selectNth("li", 1).selectHead("a")
          changeLink.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.change
          changeLink.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.ukPropertyChange

          val removeLink: Element = actions.selectHead("ul").selectNth("li", 2).selectHead("a")
          removeLink.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.remove
          removeLink.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.ukPropertyRemove
        }
        "has no link to add a uk property business" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectOptionally("#add-uk-property") mustBe None
        }
      }

      "have a section for foreign property" when {
        "the foreign property feature switch is enabled" which {
          "has a heading" in new ViewTest(completeIncomeSources) {
            document.mainContent.selectNth("h2", 3).text mustBe IndividualIncomeSource.foreignPropertyHeading
          }
          "has a description" in new ViewTest(completeIncomeSources) {
            document.mainContent.selectNth("p", 4).text mustBe IndividualIncomeSource.foreignPropertyDescription
          }
          "has a summary of the added foreign property business" in new ViewTest(completeIncomeSources) {
            val summaryList: Element = document.mainContent.selectNth("dl", 3)
            val row: Element = summaryList.selectNth("div.govuk-summary-list__row", 1)

            row.selectHead("dt").text mustBe IndividualIncomeSource.foreignPropertyLabel

            val actions: Element = row.selectHead("dd")
            val changeLink: Element = actions.selectHead("ul").selectNth("li", 1).selectHead("a")
            changeLink.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.change
            changeLink.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.foreignPropertyChange

            val removeLink: Element = actions.selectHead("ul").selectNth("li", 2).selectHead("a")
            removeLink.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.remove
            removeLink.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.foreignPropertyRemove
          }
          "has no link to add a foreign property business" in new ViewTest(completeIncomeSources) {
            document.mainContent.selectOptionally("#add-foreign-property") mustBe None
          }
        }
      }

      "have no section for foreign property" when {
        "the foreign property feature switch is not enabled" in new ViewTest(completeIncomeSources, false) {
          document.mainContent.selectOptionalNth("h2", 3) mustBe None
        }
      }

      "have a form" which {
        def form(document: Document): Element = document.mainContent.getForm

        "has the correct attributes" in new ViewTest(completeIncomeSources) {
          form(document).attr("method") mustBe testCall.method
          form(document).attr("action") mustBe testCall.url
        }
        "has a continue button" in new ViewTest(completeIncomeSources) {
          form(document).getGovukSubmitButton.text mustBe IndividualIncomeSource.continue
        }
        "has a save and come back later button" in new ViewTest(completeIncomeSources) {
          val button: Element = form(document).selectHead(".govuk-button--secondary")
          button.text mustBe IndividualIncomeSource.saveAndComeBackLater
          button.attr("href") mustBe controllers.individual.tasklist.routes.ProgressSavedController.show(Some("income-sources")).url
        }
      }
    }
  }

}}}}

