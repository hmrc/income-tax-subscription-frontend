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
import play.api.data.Form
import play.api.data.Forms._
import utilities.UnitTestTrait
import views.html.helpers.checkboxHelper

class CheckboxHelperSpec extends UnitTestTrait {

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

      val doc = checkboxHelper(testField, testLabel, testForm).doc
      doc.getElementsByTag("div").hasClass("multiple-choice") shouldBe true
      doc.getElementsByTag("label").text() should include(testLabel)
      val inputs = doc.getElementsByTag("input")

      inputs.size() shouldBe 1
      inputs.get(0).attr("value") shouldBe "true"
      inputs.get(0).attr("type") shouldBe "checkbox"
    }

    "if the form is populated with true, then the checkbox is marked as checked" in {
      val filledForm = testForm.fill(TestData(true))
      val testField = filledForm(checkedName)
      val doc = checkboxHelper(testField, testLabel, filledForm).doc

      val inputs = doc.getElementsByTag("input")

      inputs.size() shouldBe 1
      inputs.get(0).attr("checked") shouldBe "checked"
    }

    "when there is error on the field, the errors needs to be displayed, but not otherwise" in {
      val testField = testForm(checkedName)
      val doc = checkboxHelper(testField, testLabel, testForm).doc
      doc.getElementsByTag("div").hasClass("form-field--error") shouldBe false
      doc.getElementsByClass("error-notification").isEmpty shouldBe true

      val errorForm = testForm.bind(DataMap.EmptyMap)
      val errorField = errorForm(checkedName)
      val errDoc = checkboxHelper(errorField, testLabel, errorForm).doc
      errDoc.getElementsByTag("div").hasClass("form-field--error") shouldBe true
      errDoc.getElementsByClass("error-notification").isEmpty shouldBe false
    }
  }
}
