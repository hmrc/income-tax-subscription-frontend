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

package views.agent

import config.featureswitch.FeatureSwitch.ForeignProperty
import forms.agent.HaveYouCompletedThisSectionForm
import models.common.business.{BusinessNameModel, BusinessTradeNameModel, SelfEmploymentData}
import models.common.{OverseasPropertyModel, PropertyModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.html.agent.tasklist.addbusiness.YourIncomeSourceToSignUp

class YourIncomeSourceToSignUpViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ForeignProperty)
  }

  object AgentIncomeSource {
    val heading = "Your client’s income sources"
    val paragraph1: String = "If your client is self-employed, you must add all of their sole trader businesses if they have more than one. " +
      "If they have income from property you must add it, but this is limited to one UK property business."
    val paragraph1Overseas: String = "Your client can have up to 50 sole trader businesses. " +
      "However, they can have only one UK property business and one overseas property."
    val paragraph2 = "Renting out a property includes using a letting agency."
    val soleTrader = "Sole trader"
    val soleTraderLinkText = "Add sole trader income source"
    val soleTraderLink: String = appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl
    val ukProperty = "UK property"
    val ukPropertyLinkText = "Add UK property income source"
    val ukPropertyLink: String = controllers.agent.tasklist.ukproperty.routes.PropertyStartDateController.show().url
    val foreignPropertyHeading = "Foreign property"
    val foreignPropertyLabel = "Foreign property income source"
    val foreignPropertyLinkText = "Add foreign property business"
    val foreignPropertyLink: String = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show().url
    val foreignPropertyChange = "Change foreign property income source"
    val foreignPropertyRemove = "Remove foreign property income source"
    val errorHeading = "There is a problem"
    val errorSummary = "Select Sole trader business, UK property rental or Overseas property rental"
    val completedThisSectionFormHeading = "Have you completed this section?"
    val completedSectionYes = "Yes, I’ve completed this section"
    val completedSectionNo = "No, I’ll come back to it later"
    val continue = "Continue"

    val change: String = "Change"
    val remove: String = "Remove"
  }

  private val incomeSourceView = app.injector.instanceOf[YourIncomeSourceToSignUp]

  private val clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")

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
    incomeSourceView(
      postAction = testCall,
      backUrl = testBackUrl,
      haveYouCompletedThisSectionForm = HaveYouCompletedThisSectionForm.form,
      clientDetails = clientDetails,
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


  "Your Income Source To Sign Up View" should {
    "display the template correctly" when {

      "there is no error" in new TemplateViewTest(
        view = view(),
        title = AgentIncomeSource.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
    }

    "have the heading for the page" in new ViewTest {
      document.mainContent.selectHead("h1").text mustBe AgentIncomeSource.heading
    }


    "have a section on Sole trader" in new ViewTest {
      document.mainContent.selectHead("h2").text mustBe AgentIncomeSource.soleTrader
    }

    "have a  Sole trader link" in new ViewTest {
      val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
      link.text mustBe AgentIncomeSource.soleTraderLinkText
      link.attr("href") mustBe AgentIncomeSource.soleTraderLink

    }

    "have a section on UK property" in new ViewTest {
      document.mainContent.selectNth("h2", 2).text mustBe AgentIncomeSource.ukProperty
    }

    "have a  UK property  link" in new ViewTest {
      val link: Element = document.mainContent.getElementById("add-uk-property").selectHead("a")
      link.text mustBe AgentIncomeSource.ukPropertyLinkText
      link.attr("href") mustBe AgentIncomeSource.ukPropertyLink
    }

    "have a section for foreign property" when {
      "the foreign property feature switch is enabled" which {
        "has a heading" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
          document.mainContent.selectNth("h2", 3).text mustBe AgentIncomeSource.foreignPropertyHeading
        }

        "has a summary of the added foreign property business" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
          val summaryList: Element = document.mainContent.selectNth("dl", 3)
          val row: Element = summaryList.selectNth("div.govuk-summary-list__row", 1)

          row.selectHead("dt").text mustBe AgentIncomeSource.foreignPropertyLabel

          val actions: Element = row.selectHead("dd")
          val changeLink: Element = actions.selectHead("ul").selectNth("li", 1).selectHead("a")
          changeLink.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.change
          changeLink.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.foreignPropertyChange

          val removeLink: Element = actions.selectHead("ul").selectNth("li", 2).selectHead("a")
          removeLink.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.remove
          removeLink.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.foreignPropertyRemove
        }
        "has no link to add a foreign property business" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
          document.mainContent.selectOptionally("#add-foreign-property") mustBe None
        }
      }
    }

    "have a Have you completed this section?" which {
      "has a heading" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
        document.mainContent.selectHead(".govuk-fieldset").selectHead("legend").text mustBe AgentIncomeSource.completedThisSectionFormHeading
      }

      "have a Yes, I have completed this section radio button" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
        document.mainContent.selectNth(".govuk-radios__label", 1).text mustBe AgentIncomeSource.completedSectionYes
      }

      "have a No, I will come back to it later radio button" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
        document.mainContent.selectNth(".govuk-radios__label", 2).text mustBe AgentIncomeSource.completedSectionNo
      }
    }

    "have a continue button and redirect to tasklist page" in new ViewTest(selfEmployments, ukProperty, foreignProperty) {
      document.mainContent.selectHead("#continue-button").text mustBe AgentIncomeSource.continue
    }
  }
}
