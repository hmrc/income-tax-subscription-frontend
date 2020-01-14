/*
 * Copyright 2020 HM Revenue & Customs
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

package core.views.helpers

import forms.validation.testutils.DataMap._
import forms.validation.utils.MappingUtil._
import core.utils.UnitTestTrait
import core.views.html.helpers
import org.scalatest.Matchers._
import play.api.data.Forms._
import play.api.data.{Field, Form}
import play.api.i18n.Messages.Implicits.applicationMessages

class TextAreaHelperSpec extends UnitTestTrait {

  private def textAreaHelper(field: Field,
                             label: String,
                             showLabel: Boolean,
                             maxLength: Option[Int] = None,
                             cols: Option[Int] = None,
                             rows: Option[Int] = None) =
    helpers.textAreaHelper(
      field = field,
      label = label,
      showLabel = showLabel,
      maxLength = maxLength,
      cols = cols,
      rows = rows)(applicationMessages)

  case class TestData(input: String)

  val inputName = "input"
  val testForm = Form(
    mapping(
      inputName -> oText.toText.verifying(DataMap.alwaysFail)
    )(TestData.apply)(TestData.unapply)
  )
  val testLabel = "testLabel"
  val testMaxLength = 120
  val testCols = 10
  val testRows = 20

  "TextAreaHelper" should {

    "output correctly when showLabel=true and max length specified" in {
      val testField = testForm(inputName)
      val doc = textAreaHelper(testField, testLabel, showLabel = true, maxLength = testMaxLength, cols = testCols, rows = testRows).doc

      val label = doc.select("label")
      label.hasClass("hidden") mustBe false
      label.attr("for") mustBe testField.name

      val textArea = doc.select("textarea")
      textArea.attr("maxLength") mustBe testMaxLength.toString

      textArea.attr("cols") mustBe testCols.toString
      textArea.attr("rows") mustBe testRows.toString
    }

    "output correctly when showLabel=false and max length not specified" in {
      val testField = testForm(inputName)
      val doc = textAreaHelper(testField, testLabel, showLabel = false, maxLength = None, cols = testCols, rows = testRows).doc

      val label = doc.select("label")
      label.hasClass("hidden") mustBe true
      label.attr("for") mustBe testField.name

      val textArea = doc.select("textarea")
      textArea.hasAttr("maxLength") mustBe false

      textArea.attr("cols") mustBe testCols.toString
      textArea.attr("rows") mustBe testRows.toString
    }

  }

}
