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

import assets.MessageLookup.{IndividualIncomeSource => messages}
import forms.individual.incomesource.IncomeSourceForm
import forms.individual.incomesource.IncomeSourceForm._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.individual.incometax.incomesource.IncomeSource

class IncomeSourceViewSpec extends ViewSpec {

  val backUrl: String = ViewSpecTrait.testBackUrl

  val action: Call = ViewSpecTrait.testCall

  val incomeSource: IncomeSource = app.injector.instanceOf[IncomeSource]

  val testFormError: FormError = FormError(incomeSourceKey, "test message")

  def view(overseasPropertyEnabled: Boolean = false, editMode: Boolean = false, hasError: Boolean = false): Html = {
    incomeSource(
      if (hasError) {
        IncomeSourceForm.incomeSourceForm(overseasPropertyEnabled).withError(testFormError)
      } else {
        IncomeSourceForm.incomeSourceForm(overseasPropertyEnabled)
      },
      postAction = testCall,
      isEditMode = editMode,
      foreignProperty = overseasPropertyEnabled,
      backUrl = testBackUrl
    )
  }

  class ViewTest(overseasPropertyEnabled: Boolean = false, editMode: Boolean = false, hasError: Boolean = false) {

    val document: Document = Jsoup.parse(view(
      overseasPropertyEnabled = overseasPropertyEnabled,
      editMode = editMode,
      hasError = hasError
    ).body)

  }

  "IncomeSource view" should {

    "display the template correctly" when {
      "there is an error" in new TemplateViewTest(
        view = view(hasError = true),
        title = messages.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        error = Some(testFormError)
      )
      "there is no error" in new TemplateViewTest(
        view = view(hasError = true),
        title = messages.title,
        isAgent = false,
        backLink = Some(testBackUrl),
        error = Some(testFormError)
      )
    }

    "have the heading for the page" in new ViewTest {
      document.selectHead("h1").text mustBe messages.heading
    }

    "have some info for the user" in new ViewTest {
      document.selectHead("p[id=income-info]").text mustBe messages.line_1
    }

    "have a form to submit the checkboxes" in new ViewTest {
      val form: Element = document.selectHead("form")
      form.attr("method") mustBe testCall.method
      form.attr("action") mustBe testCall.url
    }
    "have a fieldset" which {
      "is described a hint" when {
        "there is no error" in new ViewTest {
          document.selectHead("fieldset").attr("aria-describedby") mustBe s"$incomeSourceKey-hint"
        }
      }
      "is described by a hint and error" when {
        "there is an error" in new ViewTest(hasError = true) {
          document.selectHead("fieldset").attr("aria-describedby") mustBe s"$incomeSourceKey-hint $incomeSourceKey-error"
        }
      }
    }
    "have a legend with the page heading" in new ViewTest {
      document.selectHead("fieldset").selectHead("legend").text mustBe messages.heading
    }
    "have a hint" in new ViewTest {
      document.selectHead("fieldset").selectHead(".govuk-hint").text mustBe "Select all that apply"
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
      val checkboxSet: Element = document.selectHead("fieldset").selectNth(".govuk-checkboxes__item", 1)
      val checkboxInput: Element = checkboxSet.selectHead("input")
      val checkboxLabel: Element = checkboxSet.selectHead("label")

      checkboxInput.attr("type") mustBe "checkbox"
      checkboxInput.attr("value") mustBe selfEmployedKey
      checkboxInput.attr("name") mustBe s"$incomeSourceKey[]"
      checkboxInput.attr("id") mustBe incomeSourceKey

      checkboxLabel.attr("for") mustBe incomeSourceKey
      checkboxLabel.text mustBe messages.business
    }
    "have a checkbox for uk property" in new ViewTest {
      val checkboxSet: Element = document.selectHead("fieldset").selectNth(".govuk-checkboxes__item", 2)
      val checkboxInput: Element = checkboxSet.selectHead("input")
      val checkboxLabel: Element = checkboxSet.selectHead("label")

      checkboxInput.attr("type") mustBe "checkbox"
      checkboxInput.attr("value") mustBe ukPropertyKey
      checkboxInput.attr("name") mustBe s"$incomeSourceKey[]"
      checkboxInput.attr("id") mustBe s"$incomeSourceKey-2"

      checkboxLabel.attr("for") mustBe s"$incomeSourceKey-2"
      checkboxLabel.text mustBe messages.ukProperty
    }
    "have a checkbox for overseas property" when {
      "the overseas property flag is set to true" in new ViewTest(overseasPropertyEnabled = true) {
        val checkboxSet: Element = document.selectHead("fieldset").selectNth(".govuk-checkboxes__item", 3)
        val checkboxInput: Element = checkboxSet.selectHead("input")
        val checkboxLabel: Element = checkboxSet.selectHead("label")

        checkboxInput.attr("type") mustBe "checkbox"
        checkboxInput.attr("value") mustBe overseasPropertyKey
        checkboxInput.attr("name") mustBe s"$incomeSourceKey[]"
        checkboxInput.attr("id") mustBe s"$incomeSourceKey-3"

        checkboxLabel.attr("for") mustBe s"$incomeSourceKey-3"
        checkboxLabel.text mustBe messages.foreignProperty
      }
    }
    "have no checkbox for overseas property" when {
      "the overseas property flag is set to false" in new ViewTest {
        document.selectHead("fieldset").selectOptionally(".govuk-checkboxes__item:nth-of-type(3)") mustBe None
      }
    }
    "have a continue button" when {
      "the page is not in edit mode" in new ViewTest {
        document.selectHead(".govuk-button").text mustBe "Continue"
      }
    }
    "have an update button" when {
      "the page is in edit mode" in new ViewTest(editMode = true) {
        document.selectHead(".govuk-button").text mustBe "Update"
      }
    }

  }
}
