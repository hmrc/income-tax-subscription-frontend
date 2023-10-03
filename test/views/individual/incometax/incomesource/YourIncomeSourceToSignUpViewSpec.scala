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

package views.individual.incometax.incomesource

import config.featureswitch.FeatureSwitch.ForeignProperty
import models.common.business._
import models.common.{OverseasPropertyModel, PropertyModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.individual.incometax.incomesource.YourIncomeSourceToSignUp
import forms.individual.incomesource.HaveYouCompletedThisSectionForm


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
    def selfEmploymentChange(name: String) = s"Change $name"
    def selfEmploymentRemove(name: String) = s"Remove $name"

    val addAnotherSelfEmploymentLinkText = "Add another sole trader income source"
    val ukPropertyHeading = "UK property business"
    val ukPropertyDescription = "A UK property business is when you get income from land or buildings in the UK. For instance, letting houses, flat or holiday homes either on a long or short term basis."
    val ukPropertyLabel = "UK property income source"
    val ukPropertyLinkText = "Add UK property income source"
    val ukPropertyChange = "Change UK property income source"
    val ukPropertyRemove = "Remove UK property income source"
    val foreignPropertyHeading = "Foreign property business"
    val foreignPropertyDescription = "A foreign property business is when you get income from land or buildings in another country. For instance, letting houses, flat or holiday homes either on a long or short term basis."
    val foreignPropertyLabel = "Foreign property income source"
    val foreignPropertyLinkText = "Add foreign property income source"
    val foreignPropertyChange = "Change foreign property income source"
    val foreignPropertyRemove = "Remove foreign property income source"
    val completedThisSectionFormHeading = "Have you completed this section?"
    val completedSectionYes = "Yes, I’ve completed this section"
    val completedSectionNo = "No, I’ll come back to it later"
    val continue = "Continue"

    val change: String = "Change"
    val remove: String = "Remove"
  }

  val backUrl: String = ViewSpecTrait.testBackUrl

  val action: Call = ViewSpecTrait.testCall

  val incomeSource: YourIncomeSourceToSignUp = app.injector.instanceOf[YourIncomeSourceToSignUp]

  val selfEmployments: Seq[SelfEmploymentData] = Seq(
    SelfEmploymentData("idOne", None, Some(BusinessNameModel("business name")), Some(BusinessTradeNameModel("business trade"))),
    SelfEmploymentData("idOne", None, Some(BusinessNameModel("business name"))),
    SelfEmploymentData("idOne", None, None, businessTradeName = Some(BusinessTradeNameModel("business trade"))),
    SelfEmploymentData("idOne")
  )
  val ukProperty: Option[PropertyModel] = Some(PropertyModel())
  val foreignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel())

  def view(selfEmployments: Seq[SelfEmploymentData] = Seq.empty,
           ukProperty: Option[PropertyModel] = None,
           foreignProperty: Option[OverseasPropertyModel] = None): Html = {
    incomeSource(
      postAction = testCall,
      haveYouCompletedThisSectionForm = HaveYouCompletedThisSectionForm.form,
      backUrl = testBackUrl,
      selfEmployments = selfEmployments,
      ukProperty = ukProperty,
      foreignProperty = foreignProperty
    )
  }

  class ViewTest(selfEmployments: Seq[SelfEmploymentData] = Seq.empty,
                 ukProperty: Option[PropertyModel] = None,
                 foreignProperty: Option[OverseasPropertyModel] = None,
                 foreignPropertyEnabled: Boolean = true) {

    if (foreignPropertyEnabled) enable(ForeignProperty)

    val document: Document = Jsoup.parse(view(selfEmployments, ukProperty, foreignProperty).body)

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
          link.attr("href") mustBe controllers.individual.business.routes.PropertyStartDateController.show().url
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
            link.attr("href") mustBe controllers.individual.business.routes.OverseasPropertyStartDateController.show().url
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
      "have a section for sole trader income sources" which {
        "has a heading" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
          document.mainContent.selectNth("h2", 1).text mustBe IndividualIncomeSource.selfEmploymentHeading
        }

        "has a description" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
          document.mainContent.selectNth("p", 2).text mustBe IndividualIncomeSource.selfEmploymentDescription
        }

        "has a list of added self employment businesses" which {
          "has a first row" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
            val summaryList: Element = document.mainContent.selectNth("dl", 1)
            val row: Element = summaryList.selectNth("div.govuk-summary-list__row", 1)

            row.selectHead("dt").text mustBe "business name - business trade"

            val actions: Element = row.selectHead("dd")
            val changeLink: Element = actions.selectHead("ul").selectNth("li", 1).selectHead("a")
            changeLink.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.change
            changeLink.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.selfEmploymentChange("business name - business trade")

            val removeLink: Element = actions.selectHead("ul").selectNth("li", 2).selectHead("a")
            removeLink.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.remove
            removeLink.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.selfEmploymentRemove("business name - business trade")
          }
          "has a second row" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
            val summaryList: Element = document.mainContent.selectNth("dl.govuk-summary-list", 1)
            val row: Element = summaryList.selectNth("div.govuk-summary-list__row", 2)

            row.selectHead("dt").text mustBe "business name"

            val actions: Element = row.selectHead("dd")
            val changeLink: Element = actions.selectHead("ul").selectNth("li", 1).selectHead("a")
            changeLink.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.change
            changeLink.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.selfEmploymentChange("business name")

            val removeLink: Element = actions.selectHead("ul").selectNth("li", 2).selectHead("a")
            removeLink.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.remove
            removeLink.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.selfEmploymentRemove("business name")
          }
          "has a third row" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
            val summaryList: Element = document.mainContent.selectNth("dl.govuk-summary-list", 1)
            val row: Element = summaryList.selectNth("div.govuk-summary-list__row", 3)

            row.selectHead("dt").text mustBe "business trade"

            val actions: Element = row.selectHead("dd")
            val changeLink: Element = actions.selectHead("ul").selectNth("li", 1).selectHead("a")
            changeLink.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.change
            changeLink.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.selfEmploymentChange("business trade")

            val removeLink: Element = actions.selectHead("ul").selectNth("li", 2).selectHead("a")
            removeLink.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.remove
            removeLink.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.selfEmploymentRemove("business trade")
          }
          "has a forth row" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
            val summaryList: Element = document.mainContent.selectNth("dl.govuk-summary-list", 1)
            val row: Element = summaryList.selectNth("div.govuk-summary-list__row", 4)

            row.selectHead("dt").text mustBe "Business 4"

            val actions: Element = row.selectHead("dd")
            val changeLink: Element = actions.selectHead("ul").selectNth("li", 1).selectHead("a")
            changeLink.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.change
            changeLink.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.selfEmploymentChange("Business 4")

            val removeLink: Element = actions.selectHead("ul").selectNth("li", 2).selectHead("a")
            removeLink.selectHead("span[aria-hidden=true]").text mustBe IndividualIncomeSource.remove
            removeLink.selectHead("span.govuk-visually-hidden").text mustBe IndividualIncomeSource.selfEmploymentRemove("Business 4")
          }
        }

        "has a link to add a self employment business" in new ViewTest {
          val link: Element = document.mainContent.selectHead("#add-self-employment").selectHead("a")
          link.text mustBe IndividualIncomeSource.addSelfEmploymentLinkText
          link.attr("href") mustBe appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl
        }
      }

      "have a section for uk property" which {
        "has a heading" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
          document.mainContent.selectNth("h2", 2).text mustBe IndividualIncomeSource.ukPropertyHeading
        }
        "has a description" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
          document.mainContent.selectNth("p", 3).text mustBe IndividualIncomeSource.ukPropertyDescription
        }
        "has a summary of the added uk property business" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
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
        "has no link to add a uk property business" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
          document.mainContent.selectOptionally("#add-uk-property") mustBe None
        }
      }

      "have a section for foreign property" when {
        "the foreign property feature switch is enabled" which {
          "has a heading" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
            document.mainContent.selectNth("h2", 3).text mustBe IndividualIncomeSource.foreignPropertyHeading
          }
          "has a description" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
            document.mainContent.selectNth("p", 4).text mustBe IndividualIncomeSource.foreignPropertyDescription
          }
          "has a summary of the added foreign property business" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
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
          "has no link to add a foreign property business" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
            document.mainContent.selectOptionally("#add-foreign-property") mustBe None
          }
        }
      }

      "have no section for foreign property" when {
        "the foreign property feature switch is not enabled" in new ViewTest(selfEmployments, ukProperty, foreignProperty, false) {
          document.mainContent.selectOptionalNth("h2", 3) mustBe None
        }
      }

      "has a Have you completed this section?" which {
        "has a heading" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
          document.mainContent.selectHead(".govuk-fieldset").selectHead("legend").text mustBe IndividualIncomeSource.completedThisSectionFormHeading
        }

        "have a Yes, I have completed this section radio button" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
          document.mainContent.selectNth(".govuk-radios__label", 1).text mustBe IndividualIncomeSource.completedSectionYes
        }

        "have a No, I will come back to it later radio button" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
          document.mainContent.selectNth(".govuk-radios__label", 2).text mustBe IndividualIncomeSource.completedSectionNo
        }
      }

      "have a continue button and redirect to tasklist page" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
        document.mainContent.selectHead("#continue-button").text mustBe IndividualIncomeSource.continue
      }
    }
  }

}

