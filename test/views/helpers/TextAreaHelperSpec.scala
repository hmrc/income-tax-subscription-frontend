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
import forms.validation.utils.MappingUtil._
import org.scalatest.Matchers._
import play.api.data.Forms._
import play.api.data.{Field, Form}
import play.api.i18n.Messages.Implicits.applicationMessages
import utils.UnitTestTrait

class TextAreaHelperSpec extends UnitTestTrait {

  private def textAreaHelper(
                              field: Field,
                              maxLength: Option[Int] = None,
                              cols: Option[Int] = None,
                              rows: Option[Int] = None
                            )
  = views.html.helpers.textAreaHelper(field = field, maxLength = maxLength, cols = cols, rows = rows)(applicationMessages)

  case class TestData(input: String)

  val inputName = "input"
  val testForm = Form(
    mapping(
      inputName -> oText.toText.verifying(DataMap.alwaysFail)
    )(TestData.apply)(TestData.unapply)
  )
  val testMaxLength = 120
  val testCols = 10
  val testRows = 20

  "TextAreaHelper" should {

    "use the character counter template when max length is specified" in {
      val testField = testForm(inputName)
      val doc = textAreaHelper(testField, maxLength = testMaxLength, cols = testCols, rows = testRows).doc
      val div = doc.select("div[class=char-counter]")
      div.hasClass("char-counter") mustBe true
      div.hasAttr("data-char-counter") mustBe true

      val textArea = doc.select("textarea")
      textArea.attr("maxLength") mustBe testMaxLength.toString
      textArea.hasAttr("data-char-field") mustBe true

      textArea.attr("cols") mustBe testCols.toString
      textArea.attr("rows") mustBe testRows.toString
    }

    "do not include the character counter when max length is not specified" in {
      val testField = testForm(inputName)
      val doc = textAreaHelper(testField, maxLength = None, cols = testCols, rows = testRows).doc
      val div = doc.select("div[class=char-counter]")
      div.isEmpty mustBe true

      val textArea = doc.select("textarea")
      textArea.hasAttr("maxLength") mustBe false
      textArea.hasAttr("data-char-field") mustBe false

      textArea.attr("cols") mustBe testCols.toString
      textArea.attr("rows") mustBe testRows.toString
    }

  }

}
