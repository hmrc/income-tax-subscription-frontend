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
import forms.individual.incomesource.BusinessIncomeSourceForm
import forms.individual.incomesource.BusinessIncomeSourceForm.incomeSourceKey
import models.IncomeSourcesStatus
import models.common.{OverseasProperty, SelfEmployed, UkProperty}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.individual.tasklist.addbusiness.WhatIncomeSourceToSignUp

class WhatIncomeSourceToSignUpViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ForeignProperty)
  }

  object IndividualIncomeSource {
    val title = "Add an income source"
    val heading: String = title
    val paragraph1: String = "You can add your other sole trader businesses if you have more than one. If you have income from property, you can also add it."
    val paragraph1Overseas: String = "You must add all of your sole trader businesses if you have more than one. If you have income from property, you must also add it. You can add one UK property business and one overseas property business."
    val business = "Sole trader business"
    val businessHint = "A business you run as an individual. If you have other sole trader businesses, you can add them."
    val ukProperty = "UK property business"
    val ukPropertyHint = "Income from property in the UK. This includes money you earn through a lettings or rental agency.You can have incomes from multiple properties in a single UK property business."
    val foreignProperty = "Overseas property business"
    val foreignPropertyHint = "Income from renting property abroad. This includes money you earn through a lettings or rental agency. You can have incomes from multiple properties in a single overseas property business."
    val errorHeading = "There is a problem"
    val errorSummary = "Select Sole trader business, UK property rental or Overseas property rental"
  }

  val backUrl: String = ViewSpecTrait.testBackUrl

  val action: Call = ViewSpecTrait.testCall

  val incomeSource: WhatIncomeSourceToSignUp = app.injector.instanceOf[WhatIncomeSourceToSignUp]

  val testFormError: FormError = FormError(incomeSourceKey, "error.business-income-source.all-sources")

  def view(incomeSourcesStatus: IncomeSourcesStatus, hasError: Boolean = false): Html = {
    incomeSource(
      if (hasError) {
        BusinessIncomeSourceForm.businessIncomeSourceForm(incomeSourcesStatus).withError(testFormError)
      } else {
        BusinessIncomeSourceForm.businessIncomeSourceForm(incomeSourcesStatus)
      },
      postAction = testCall,
      backUrl = testBackUrl,
      incomeSourcesStatus = incomeSourcesStatus
    )
  }

  class ViewTest(
                  incomeSourcesStatus: IncomeSourcesStatus = IncomeSourcesStatus(
                    selfEmploymentAvailable = true,
                    ukPropertyAvailable = true,
                    overseasPropertyAvailable = true
                  ),
                  hasError: Boolean = false,
                  overseasEnabled: Boolean = false
                ) {

    if (overseasEnabled) enable(ForeignProperty)

    val document: Document = Jsoup.parse(view(
      incomeSourcesStatus = incomeSourcesStatus,
      hasError = hasError
    ).body)

  }

  "IncomeSource view" should {

    "display the template correctly" when {
      "there is an error" in new TemplateViewTest(
        view = view(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = true),
          hasError = true
        ),
        title = IndividualIncomeSource.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        error = Some(testFormError)
      )
      "there is no error" in new TemplateViewTest(
        view = view(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = true)
        ),
        title = IndividualIncomeSource.title,
        isAgent = false,
        backLink = Some(testBackUrl),
      )
    }

    "have the heading for the page" in new ViewTest {
      document.selectHead("h1").text mustBe IndividualIncomeSource.heading
    }

    "have paragraph 1 in an inset text block" which {
      "mentions overseas property when enabled" in new ViewTest(overseasEnabled = true) {
        document.selectHead(".govuk-inset-text").selectNth("p", 1).text mustBe IndividualIncomeSource.paragraph1Overseas
      }
      "does not mention overseas property when disabled" in new ViewTest(overseasEnabled = false) {
        document.selectHead(".govuk-inset-text").selectNth("p", 1).text mustBe IndividualIncomeSource.paragraph1
      }
    }

    "have a form to submit the checkboxes" in new ViewTest {
      val form: Element = document.selectHead("form")
      form.attr("method") mustBe testCall.method
      form.attr("action") mustBe testCall.url
    }

    "have a fieldset" which {
      "is described by an error" when {
        "there is an error" in new ViewTest(hasError = true) {
          document.selectHead("fieldset").attr("aria-describedby") mustBe s"$incomeSourceKey-error"
        }
      }
    }

    "have a legend with the page heading" in new ViewTest {
      document.selectHead("fieldset").selectHead("legend").text mustBe IndividualIncomeSource.heading
    }

    "have an error" when {
      "there is an error" in new ViewTest(hasError = true) {
        document.selectHead("fieldset").selectHead(".govuk-error-message").text mustBe s"Error: ${wrappedMessages(testFormError.message)}"
      }
    }

    "have no error" when {
      "there is no error" in new ViewTest {
        document.selectHead("fieldset").selectOptionally(".govuk-error-message") mustBe None
      }
    }

    "have a checkbox for self employments" in new ViewTest {
      testRadioButton(document, index = 1, SelfEmployed.toString, IndividualIncomeSource.business, IndividualIncomeSource.businessHint)
    }

    "have no checkbox for self employments" when {
      "self employments is not available" in new ViewTest(
        incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = false, ukPropertyAvailable = true, overseasPropertyAvailable = true)
      ) {
        document.selectHead(".govuk-radios").selectNth(".govuk-radios__item", 1).selectHead("input").attr("value") mustBe UkProperty.toString
        document.selectHead(".govuk-radios").selectNth(".govuk-radios__item", 2).selectHead("input").attr("value") mustBe OverseasProperty.toString
        document.selectHead(".govuk-radios").selectOptionally(".govuk-radios__item:nth-of-type(3)") mustBe None
      }
    }

    "have a checkbox for uk property" in new ViewTest {
      testRadioButton(document, index = 2, UkProperty.toString, IndividualIncomeSource.ukProperty, IndividualIncomeSource.ukPropertyHint)
    }

    "have no checkbox for uk property" when {
      "uk property is not available" in new ViewTest(
        incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = false, overseasPropertyAvailable = true)
      ) {
        document.selectHead(".govuk-radios").selectNth(".govuk-radios__item", 1).selectHead("input").attr("value") mustBe SelfEmployed.toString
        document.selectHead(".govuk-radios").selectNth(".govuk-radios__item", 2).selectHead("input").attr("value") mustBe OverseasProperty.toString
        document.selectHead(".govuk-radios").selectOptionally(".govuk-radios__item:nth-of-type(3)") mustBe None
      }
    }

    "have a checkbox for overseas property" when {
      "the overseas property flag is set to true" in new ViewTest {
        testRadioButton(document, index = 3, OverseasProperty.toString, IndividualIncomeSource.foreignProperty, IndividualIncomeSource.foreignPropertyHint)
      }
    }

    "have no checkbox for overseas property" when {
      "overseas property is not available" in new ViewTest(
        incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = false)
      ) {
        document.selectHead(".govuk-radios").selectNth(".govuk-radios__item", 1).selectHead("input").attr("value") mustBe SelfEmployed.toString
        document.selectHead(".govuk-radios").selectNth(".govuk-radios__item", 2).selectHead("input").attr("value") mustBe UkProperty.toString
        document.selectHead(".govuk-radios").selectOptionally(".govuk-radios__item:nth-of-type(3)") mustBe None
      }
    }

    "have a continue button" in new ViewTest {
      document.mainContent.selectHead(".govuk-button").text mustBe "Continue"
    }
  }

  private def testRadioButton(document: Document, index: Int, key: String, label: String, hint: String): Unit = {
    val radioButtonElement: Element = document.selectHead(".govuk-radios").selectNth(".govuk-radios__item", index)

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

    val radioButtonHint: Element = radioButtonElement.selectHead(".govuk-radios__hint")
    radioButtonHint.text mustBe hint
  }
}
