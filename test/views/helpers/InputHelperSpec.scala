/*
 * Copyright 2017 HM Revenue & Customs
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

import forms.validation.testutils.DataMap
import org.scalatest.Matchers._
import play.api.data.Forms._
import play.api.data.{Field, Form}
import play.api.i18n.Messages.Implicits.applicationMessages
import utils.UnitTestTrait
import forms.validation.utils.MappingUtil._

class InputHelperSpec extends UnitTestTrait {

  private def inputHelper(
                           field: Field,
                           label: Option[String],
                           formHint: Option[Seq[String]] = None,
                           maxLength: Option[Int] = None,
                           labelClass: Option[String] = None,
                           isNumeric: Boolean = false
                         )
  = views.html.helpers.inputHelper(field, label = label, formHint = formHint, maxLength = maxLength, labelClass = labelClass, isNumeric = isNumeric)(applicationMessages)

  case class TestData(input: String)

  val inputName = "input"
  val testForm = Form(
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
      val doc = inputHelper(testField, Some(testLabel), formHint = Some(Seq(testHint)), maxLength = Some(maxLength)).doc
      doc.getElementsByTag("div").hasClass("form-group") shouldBe true
      doc.getElementsByTag("div").hasClass("form-field") shouldBe true
      doc.getElementsByTag("label").text() should include(testLabel)
      doc.getElementsByTag("label").text() should include(testHint)
      doc.getElementsByTag("span").text() shouldBe testHint

      val inputs = doc.getElementsByTag("input")
      inputs.size() shouldBe 1
      inputs.get(0).attr("value") shouldBe ""
      inputs.get(0).attr("type") shouldBe "text"
      inputs.get(0).attr("maxlength") shouldBe maxLength.toString
    }

    "if the form is populated, then the input should be populated correctly" in {
      val testField = testForm.fill(TestData("My previous input"))(inputName)
      val doc = inputHelper(testField, Some(testLabel)).doc

      val inputs = doc.getElementsByTag("input")
      inputs.size() shouldBe 1
      inputs.get(0).attr("value") shouldBe "My previous input"
    }

    "when there is error on the field, the errors needs to be displayed, but not otherwise" in {
      val testField = testForm(inputName)
      val doc = inputHelper(testField, testLabel).doc
      doc.getElementsByTag("div").hasClass("form-field--error") shouldBe false
      doc.getElementsByClass("error-notification").isEmpty shouldBe true

      val errorField = testForm.bind(DataMap.EmptyMap)(inputName)
      val errDoc = inputHelper(errorField, testLabel).doc
      errDoc.getElementsByTag("div").hasClass("form-field--error") shouldBe true
      errDoc.getElementsByClass("error-notification").isEmpty shouldBe false
    }

    "if a labelClass is supplied, render the label with an additional class tag" in {
      val testField = testForm.fill(TestData("My previous input"))(inputName)
      val doc = inputHelper(testField, Some(testLabel), labelClass = "labelClass").doc
      doc.getElementsByTag("label").hasClass("labelClass") shouldBe true
    }

    "if the type is numeric then the input" should {
      val testField = testForm.fill(TestData("My previous input"))(inputName)
      val input = inputHelper(testField, Some(testLabel), isNumeric = true).doc.getElementsByTag("input")

      "have an additional attribue for pattern=[0-9*]" in {
        input.attr("pattern") shouldBe "[0-9]*"
      }

      "have an additional attribute for inputmode=numeric" in {
        input.attr("inputmode") shouldBe "numeric"
      }
    }
  }
}
