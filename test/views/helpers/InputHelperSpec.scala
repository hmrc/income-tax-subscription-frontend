/*
 * Copyright 2021 HM Revenue & Customs
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

package views.helpers

import forms.validation.testutils.DataMap.DataMap
import forms.validation.utils.MappingUtil._
import org.scalatest.Matchers._
import play.api.data.Forms._
import play.api.data.{Field, Form}
import utilities.UnitTestTrait
import views.html.helpers.inputHelper

class InputHelperSpec extends UnitTestTrait {

  private def helper(field: Field,
                     parentForm: Form[_],
                     label: Option[String],
                     formHint: Option[String] = None,
                     maxLength: Option[Int] = None,
                     labelClass: Option[String] = None,
                     isNumeric: Boolean = false,
                     autoComplete: Option[String] = None,
                     isPageHeading: Boolean = true) = {
    inputHelper(
      field = field,
      label = label,
      formHint = formHint,
      parentForm = parentForm,
      maxLength = maxLength,
      labelClass = labelClass,
      isNumeric = isNumeric,
      autoComplete = autoComplete,
      isPageHeading = isPageHeading
    )
  }

  case class TestData(input: String)

  val inputName = "input"
  val testForm: Form[TestData] = Form(
    mapping(
      inputName -> oText.toText.verifying(DataMap.alwaysFail)
    )(TestData.apply)(TestData.unapply)
  )
  val testLabel = "my test label text"

  "InputHelper" should {
    "populate the relevant content in the correct positions" in {
      val testHint = "my test hint text"
      val testField = testForm(inputName)
      val maxLength = 10
      val doc = helper(testField, testForm, Some(testLabel), Some(testHint), maxLength = Some(maxLength)).doc
      doc.getElementsByTag("div").hasClass("form-group") shouldBe true
      doc.getElementsByTag("div").hasClass("form-field") shouldBe true
      doc.getElementsByTag("label").text() should include(testLabel)
      doc.select("div[class=form-hint]").text() shouldBe testHint

      val inputs = doc.getElementsByTag("input")
      inputs.size() shouldBe 1
      inputs.get(0).attr("value") shouldBe ""
      inputs.get(0).attr("type") shouldBe "text"
      inputs.get(0).attr("maxlength") shouldBe maxLength.toString
      inputs.get(0).attr("autocomplete") shouldBe ""
    }

    "if the form is populated, then the input should be populated correctly" in {
      val testField = testForm.fill(TestData("My previous input"))(inputName)
      val doc = helper(testField, testForm, Some(testLabel)).doc

      val inputs = doc.getElementsByTag("input")
      inputs.size() shouldBe 1
      inputs.get(0).attr("value") shouldBe "My previous input"
    }

    "have the label as the page heading" when {
      "isPageHeading is enabled" in {
        val testField = testForm(inputName)

        val doc = helper(testField, testForm, testLabel).doc

        doc.select("label").select("h1").text shouldBe testLabel
      }
    }

    "when there is error on the field, the errors needs to be displayed, but not otherwise" in {
      val testField = testForm(inputName)

      val doc = helper(testField, testForm, testLabel).doc
      doc.getElementsByTag("div").hasClass("form-field--error") shouldBe false
      doc.getElementsByClass("error-notification").isEmpty shouldBe true

      val errorForm = testForm.bind(DataMap.EmptyMap)
      val errorField = errorForm(inputName)
      val errDoc = helper(errorField, errorForm, testLabel).doc

      errDoc.getElementsByTag("div").hasClass("form-field--error") shouldBe true
      errDoc.getElementsByClass("error-notification").isEmpty shouldBe false
      errDoc.getElementsByClass("error-notification").text shouldBe "Error: always fail"
    }

    "have the correct aria-describedby attribute" when {
      "there is no hint or error" in {
        val testField = testForm(inputName)

        val doc = helper(testField, testForm, testLabel).doc

        doc.select("input").attr("aria-describedby") shouldBe ""
      }
      "there is a hint" in {
        val testHint = "my test hint text"
        val testField = testForm(inputName)

        val doc = helper(testField, testForm, testLabel, Some(testHint)).doc

        doc.select("input").attr("aria-describedby") shouldBe s"$inputName-hint"
      }
      "there is an error" in {
        val testField = testForm.bind(DataMap.EmptyMap)(inputName)

        val doc = helper(testField, testForm, testLabel).doc

        doc.select("input").attr("aria-describedby") shouldBe s"$inputName-error"
      }
      "there is a hint and an error" in {
        val testHint = "my test hint text"
        val testField = testForm.bind(DataMap.EmptyMap)(inputName)

        val doc = helper(testField, testForm, testLabel, Some(testHint)).doc

        doc.select("input").attr("aria-describedby") shouldBe s"$inputName-hint $inputName-error"
      }
    }

    "if a labelClass is supplied, render the label with an additional class tag" in {
      val testField = testForm.fill(TestData("My previous input"))(inputName)
      val doc = helper(testField, testForm, Some(testLabel), labelClass = "labelClass").doc
      doc.getElementsByTag("label").hasClass("labelClass") shouldBe true
    }

    "if the type is numeric then the input" should {
      val testField = testForm.fill(TestData("My previous input"))(inputName)
      val input = helper(testField, testForm, Some(testLabel), isNumeric = true).doc.getElementsByTag("input")

      "have an additional attribue for pattern=[0-9*]" in {
        input.attr("pattern") shouldBe "[0-9]*"
      }

      "have an additional attribute for inputmode=numeric" in {
        input.attr("inputmode") shouldBe "numeric"
      }
    }

    "if the field has an autocomplete attribute set, should be present" in {
      val testField = testForm.fill(TestData("My previous input"))(inputName)
      val input = helper(testField, testForm, Some(testLabel), isNumeric = true, autoComplete = Some("test autocomplete")).doc.getElementsByTag("input")
      input.attr("autocomplete") shouldBe "test autocomplete"
    }
  }
}
