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

package views.individual.incometax.incomesource

import forms.individual.incomesource.BusinessIncomeSourceForm
import forms.individual.incomesource.BusinessIncomeSourceForm.{businessIncomeSourceKey, overseasPropertyKey, selfEmployedKey, ukPropertyKey}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.individual.incometax.incomesource.WhatIncomeSourceToSignUp

class WhatIncomeSourceToSignUpViewSpec extends ViewSpec {

  object IndividualIncomeSource {
    val title = "What source of income do you want to sign up?"
    val heading: String = title
    val paragraph_1: String = "Your client can have any number of sole trader businesses. " +
      "However, they can have only one UK rental property and one overseas rental property."
    val paragraph_2: String = "Renting out a property includes using a letting agency."
    val business = "Sole trader businesses"
    val ukProperty = "UK property rental"
    val foreignProperty = "Overseas property rental"
    val errorHeading = "There is a problem"
    val errorSummary = "Select Sole trader business, UK property rental or Overseas property rental"
  }

  val backUrl: String = ViewSpecTrait.testBackUrl

  val action: Call = ViewSpecTrait.testCall

  val incomeSource: WhatIncomeSourceToSignUp = app.injector.instanceOf[WhatIncomeSourceToSignUp]

  val testFormError: FormError = FormError(businessIncomeSourceKey, "test message")

  def view(overseasPropertyEnabled: Boolean = false, hasError: Boolean = false): Html = {
    incomeSource(
      if (hasError) {
        BusinessIncomeSourceForm.businessIncomeSourceForm().withError(testFormError)
      } else {
        BusinessIncomeSourceForm.businessIncomeSourceForm()
      },
      postAction = testCall,
      foreignProperty = overseasPropertyEnabled,
      backUrl = testBackUrl
    )
  }

  class ViewTest(overseasPropertyEnabled: Boolean = false, editMode: Boolean = false, hasError: Boolean = false) {

    val document: Document = Jsoup.parse(view(
      overseasPropertyEnabled = overseasPropertyEnabled,
      hasError = hasError
    ).body)

  }

  "IncomeSource view" should {

    "display the template correctly" when {
      "there is an error" in new TemplateViewTest(
        view = view(hasError = true),
        title = IndividualIncomeSource.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true,
        error = Some(testFormError)
      )
      "there is no error" in new TemplateViewTest(
        view = view(),
        title = IndividualIncomeSource.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
    }

    "have the heading for the page" in new ViewTest {
      document.selectHead("h1").text mustBe IndividualIncomeSource.heading
    }

    "have paragraph 1" in new ViewTest {
      document.selectHead("#businessIncomeSource-hint").selectNth("p", 1).text mustBe IndividualIncomeSource.paragraph_1
    }

    "have paragraph 2" in new ViewTest {
      document.selectHead("#businessIncomeSource-hint").selectNth("p", 2).text mustBe IndividualIncomeSource.paragraph_2
    }

    "have a form to submit the checkboxes" in new ViewTest {
      val form: Element = document.selectHead("form")
      form.attr("method") mustBe testCall.method
      form.attr("action") mustBe testCall.url
    }

    "have a fieldset" which {
      "is described a hint" when {
        "there is no error" in new ViewTest {
          document.selectHead("fieldset").attr("aria-describedby") mustBe s"$businessIncomeSourceKey-hint"
        }
      }
      "is described by a hint and error" when {
        "there is an error" in new ViewTest(hasError = true) {
          document.selectHead("fieldset").attr("aria-describedby") mustBe s"$businessIncomeSourceKey-hint $businessIncomeSourceKey-error"
        }
      }
    }

    "have a legend with the page heading" in new ViewTest {
      document.selectHead("fieldset").selectHead("legend").text mustBe IndividualIncomeSource.heading
    }

    "have an error" when {
      "there is an error" in new ViewTest(hasError = true) {
        document.selectHead("fieldset").selectHead(".govuk-error-message").text mustBe s"Error: ${testFormError.message}"
      }
    }

    "have no error" when {
      "there is no error" in new ViewTest {
        document.selectHead("fieldset").selectOptionally(".govuk-error-message") mustBe None
      }
    }

    "have a checkbox for self employments" in new ViewTest {
      testRadioButton(document, index = 1, selfEmployedKey, IndividualIncomeSource.business)
    }

    "have a checkbox for uk property" in new ViewTest {
      testRadioButton(document, index = 2, ukPropertyKey, IndividualIncomeSource.ukProperty)
    }

    "have a checkbox for overseas property" when {
      "the overseas property flag is set to true" in new ViewTest(overseasPropertyEnabled = true) {
        testRadioButton(document, index = 3, overseasPropertyKey, IndividualIncomeSource.foreignProperty)
      }
    }

    "have no checkbox for overseas property" when {
      "the overseas property flag is set to false" in new ViewTest {
        document.selectHead("fieldset").selectOptionally(".govuk-radios__item:nth-of-type(3)") mustBe None
      }
    }

    "have a continue button" in new ViewTest {
      document.selectHead(".govuk-button").text mustBe "Continue"
    }
  }

  private def testRadioButton(document: Document, index: Int, key: String, label: String): Unit = {
    val radioButtonElement: Element = document.selectHead(".govuk-radios").selectNth(".govuk-radios__item", index)

    val radioButtonInput: Element = radioButtonElement.selectHead("input")
    radioButtonInput.attr("type") mustBe "radio"
    radioButtonInput.attr("value") mustBe key
    radioButtonInput.attr("name") mustBe "businessIncomeSource"
    radioButtonInput.attr("id") mustBe s"incomeSource-$key"

    val radioButtonLabel: Element = radioButtonElement.selectHead("label")
    radioButtonLabel.attr("for") mustBe s"incomeSource-$key"
    radioButtonLabel.text mustBe label
  }
}
