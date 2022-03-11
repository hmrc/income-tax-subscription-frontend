/*
 * Copyright 2022 HM Revenue & Customs
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

import forms.agent.BusinessIncomeSourceForm
import forms.agent.IncomeSourceForm.incomeSourceKey
import models.IncomeSourcesStatus
import models.common.{OverseasProperty, SelfEmployed, UkProperty}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.api.mvc.Call
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.agent.WhatIncomeSourceToSignUp

class WhatIncomeSourceToSignUpViewSpec extends ViewSpec {
  object AgentIncomeSource {
    val heading = "What source of income do you want to sign up?"
    val paragraph1 = "Your client can have up to 50 sole trader businesses. " +
      "However, they can have only one UK property business and one overseas property."
    val paragraph2 = "Renting out a property includes using a letting agency."
    val soleTrader= "Sole trader business"
    val ukProperty = "UK property rental"
    val foreignProperty = "Overseas property rental"
    val errorHeading = "There is a problem"
    val errorSummary = "Select Sole trader business, UK property rental or Overseas property rental"
  }

  private val testIncomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = true)

  private val backUrl: String = ViewSpecTrait.testBackUrl

  private val action: Call = ViewSpecTrait.testCall

  private val incomeSourceView = app.injector.instanceOf[WhatIncomeSourceToSignUp]

  private val testFormError: FormError = FormError(incomeSourceKey, "test message")

  "What Income Source To Sign Up View" should {
    "display the template correctly" when {
      "there is an error" in new TemplateViewTest(
        view = page(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = true),
          hasError = true
        ),
        title = AgentIncomeSource.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true,
        error = Some(testFormError)
      )
      "there is no error" in new TemplateViewTest(
        view = page(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = true)
        ),
        title = AgentIncomeSource.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
    }

    "have the heading for the page" in {
      document().selectHead("h1").text mustBe AgentIncomeSource.heading
    }

    "have paragraph 1" in {
      document().selectHead(".govuk-inset-text").selectNth("p", 1).text mustBe AgentIncomeSource.paragraph1
    }

    "have paragraph 2" in {
      document().selectHead(".govuk-inset-text").selectNth("p", 2).text mustBe AgentIncomeSource.paragraph2
    }

    "have a form with test call attributes" in {
      val form: Element = document().selectHead("form")
      form.attr("method") mustBe testCall.method
      form.attr("action") mustBe testCall.url
    }

    "have a legend with the page heading" in {
      document().selectHead("fieldset").selectHead("legend").text mustBe AgentIncomeSource.heading
    }

    "have a radio button for sole traders" in {
      testRadioButton(document(), index = 1, SelfEmployed.toString, AgentIncomeSource.soleTrader)
    }

    "have no radio button for sole traders" when {
      "self employments is not available" in {
        val doc = document(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = false, ukPropertyAvailable = true, overseasPropertyAvailable = true)
        )
        doc.selectHead(".govuk-radios").selectNth(".govuk-radios__item", 1).selectHead("input").attr("value") mustBe UkProperty.toString
        doc.selectHead(".govuk-radios").selectNth(".govuk-radios__item", 2).selectHead("input").attr("value") mustBe OverseasProperty.toString
        doc.selectHead(".govuk-radios").selectOptionally(".govuk-radios__item:nth-of-type(3)") mustBe None
      }
    }

    "have a radio button for uk property" in {
      testRadioButton(document(), index = 2, UkProperty.toString, AgentIncomeSource.ukProperty)
    }

    "have no radio button for uk property" when {
      "uk property is not available" in {
        val doc = document(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = false, overseasPropertyAvailable = true)
        )
        doc.selectHead(".govuk-radios").selectNth(".govuk-radios__item", 1).selectHead("input").attr("value") mustBe SelfEmployed.toString
        doc.selectHead(".govuk-radios").selectNth(".govuk-radios__item", 2).selectHead("input").attr("value") mustBe OverseasProperty.toString
        doc.selectHead(".govuk-radios").selectOptionally(".govuk-radios__item:nth-of-type(3)") mustBe None
      }
    }

    "have a radio button for overseas property" when {
      "the overseas property flag is set to true" in {
        testRadioButton(document(), index = 3, OverseasProperty.toString, AgentIncomeSource.foreignProperty)
      }
    }

    "have no radio button for overseas property" when {
      "overseas property is not available" in {
        val doc = document(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = false)
        )
        doc.selectHead(".govuk-radios").selectNth(".govuk-radios__item", 1).selectHead("input").attr("value") mustBe SelfEmployed.toString
        doc.selectHead(".govuk-radios").selectNth(".govuk-radios__item", 2).selectHead("input").attr("value") mustBe UkProperty.toString
        doc.selectHead(".govuk-radios").selectOptionally(".govuk-radios__item:nth-of-type(3)") mustBe None
      }
    }

    "have a continue button" in {
      document().selectHead(".govuk-button").text mustBe "Continue"
    }
  }

  private def page(incomeSourcesStatus: IncomeSourcesStatus, hasError: Boolean = false) = {
    incomeSourceView(
      if (hasError)
        BusinessIncomeSourceForm.businessIncomeSourceForm(incomeSourcesStatus).withError(testFormError)
      else
        BusinessIncomeSourceForm.businessIncomeSourceForm(incomeSourcesStatus),
      incomeSourcesStatus,
      action,
      backUrl
    )
  }

  private def document(incomeSourcesStatus: IncomeSourcesStatus = testIncomeSourcesStatus) =
    Jsoup.parse(page(incomeSourcesStatus).body)

  private def testRadioButton(document: Document, index: Int, key: String, label: String): Unit = {
    val radioButtonElement: Element =
      document
        .selectHead(".govuk-radios")
        .selectNth(".govuk-radios__item", index)

    val radioButtonInput: Element = radioButtonElement.selectHead("input")
    radioButtonInput.attr("type") mustBe "radio"
    radioButtonInput.attr("value") mustBe key
    radioButtonInput.attr("name") mustBe "income-source"
    radioButtonInput.attr("id") mustBe s"income-source" + {
      if (index == 1) "" else s"-$index"
    }

    val radioButtonLabel: Element = radioButtonElement.selectHead("label")
    radioButtonLabel.attr("for") mustBe s"income-source" + {
      if (index == 1) "" else s"-$index"
    }
    radioButtonLabel.text mustBe label
  }
}
