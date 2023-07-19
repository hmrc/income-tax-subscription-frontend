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
import forms.agent.BusinessIncomeSourceForm
import forms.agent.BusinessIncomeSourceForm.incomeSourceKey
import models.IncomeSourcesStatus
import models.common.{OverseasProperty, SelfEmployed, UkProperty}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.api.mvc.Call
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.agent.YourIncomeSourceToSignUp

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
    val soleTraderLink = appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl
    val ukProperty = "UK property"
    val ukPropertyLinkText = "Add UK property income source"
    val ukPropertyLink = controllers.agent.business.routes.PropertyStartDateController.show().url
    val foreignProperty = "Foreign property"
    val foreignPropertyLinkText = "Add foreign property income source"
    val foreignPropertyLink = controllers.agent.business.routes.OverseasPropertyStartDateController.show().url
    val errorHeading = "There is a problem"
    val errorSummary = "Select Sole trader business, UK property rental or Overseas property rental"
  }

  private val testIncomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = true)

  private val incomeSourceView = app.injector.instanceOf[YourIncomeSourceToSignUp]

  private val clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")

  "Your Income Source To Sign Up View" should {
    "display the template correctly" when {

      "there is no error" in new TemplateViewTest(
        view = page,
        title = AgentIncomeSource.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
    }

    "have the heading for the page" in {
      document().selectHead("h1").text mustBe AgentIncomeSource.heading
    }


    "have a section on Sole trader" in {
      document().mainContent.getElementById("heading-self-employed").text mustBe AgentIncomeSource.soleTrader
    }

    "have a  Sole trader link" in {
      val link = document().mainContent.selectNth("div.app-task-list__item", 1).selectHead("a")
      link.text mustBe AgentIncomeSource.soleTraderLinkText
      link.attr("href") mustBe AgentIncomeSource.soleTraderLink

    }

    "have a section on UK property" in {
      document().mainContent.getElementById("heading-uk-property").text mustBe AgentIncomeSource.ukProperty
    }

    "have a  UK property  link" in {
      val link = document().mainContent.selectNth("div.app-task-list__item", 2).selectHead("a")
      link.text mustBe AgentIncomeSource.ukPropertyLinkText
      link.attr("href") mustBe AgentIncomeSource.ukPropertyLink
    }

    "have a section when feature switch is enabled" which {
      "mentions overseas property when enabled" in {
        enable(ForeignProperty)
        document().mainContent.getElementById("heading-foreign-property").text mustBe AgentIncomeSource.foreignProperty
      }
      "have a  Foreign  property  link" in {
        enable(ForeignProperty)
        val link = document().mainContent.selectNth("div.app-task-list__item", 3).selectHead("a")
        link.text mustBe AgentIncomeSource.foreignPropertyLinkText
        link.attr("href") mustBe AgentIncomeSource.foreignPropertyLink
      }
    }


  }


  private def page = {
    incomeSourceView(
      testBackUrl,
      clientDetails
    )
  }


  private def document() =
    Jsoup.parse(page.body)


}
