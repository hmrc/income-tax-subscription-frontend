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

class CheckboxHelperSpec extends UnitTestTrait {

  private def checkboxHelper(field: Field, label: String)
  = views.html.helpers.checkboxHelper(field, label)(applicationMessages)

  case class TestData(checked: Boolean)

  val checkedName = "checkedAns"
  val testForm = Form(
    mapping(
      checkedName -> oText.toBoolean.verifying(DataMap.alwaysFail)
    )(TestData.apply)(TestData.unapply)
  )
  val testLabel = "my test label text"

  "checkboxHelper" should {
    "populate the relevant content in the correct positions" in {
      val testField = testForm(checkedName)

      val doc = checkboxHelper(testField, testLabel).doc
      doc.getElementsByTag("div").hasClass("multiple-choice") shouldBe true
      doc.getElementsByTag("label").text() should include(testLabel)
      val inputs = doc.getElementsByTag("input")

      inputs.size() shouldBe 1
      inputs.get(0).attr("value") shouldBe "true"
      inputs.get(0).attr("type") shouldBe "checkbox"
    }

    "if the form is populated with true, then the checkbox is marked as checked" in {
      val testField = testForm.fill(TestData(true))(checkedName)
      val doc = checkboxHelper(testField, testLabel).doc

      val inputs = doc.getElementsByTag("input")

      inputs.size() shouldBe 1
      inputs.get(0).attr("checked") shouldBe "checked"
    }

    "when there is error on the field, the errors needs to be displayed, but not otherwise" in {
      val testField = testForm(checkedName)
      val doc = checkboxHelper(testField, testLabel).doc
      doc.getElementsByTag("div").hasClass("form-field--error") shouldBe false
      doc.getElementsByClass("error-notification").isEmpty shouldBe true

      val errorField = testForm.bind(DataMap.EmptyMap)(checkedName)
      val errDoc = checkboxHelper(errorField, testLabel).doc
      errDoc.getElementsByTag("div").hasClass("form-field--error") shouldBe true
      errDoc.getElementsByClass("error-notification").isEmpty shouldBe false
    }
  }
}
