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

import org.scalatest.Matchers._
import play.api.data.Forms._
import play.api.data.{Field, Form}
import play.api.i18n.Messages.Implicits.applicationMessages
import util.UnitTestTrait

class InputHelperSpec extends UnitTestTrait {

  private def inputHelper(field: Field, label: Option[String], formHint: Option[String] = None, maxLength: Option[Int] = None)
  = views.html.helpers.inputHelper(field, label = label, formHint = formHint, maxLength = maxLength)(applicationMessages)

  case class TestData(input: String)

  val inputName = "input"
  val testForm = Form(
    mapping(
      inputName -> text
    )(TestData.apply)(TestData.unapply)
  )

  "InputHelper" should {
    "populate the relevant content in the correct positions" in {
      val testLabel = "my test label text"
      val testHint = "my test hint text"
      val testField = testForm(inputName)
      val maxLength = 10
      val doc = inputHelper(testField, Some(testLabel), formHint = Some(testHint), maxLength = Some(maxLength)).doc
      doc.getElementsByTag("div").hasClass("form-group") shouldBe true
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
      val testLabel = "my test label text"
      val testField = testForm.fill(TestData("My previous input"))(inputName)
      val doc = inputHelper(testField, Some(testLabel)).doc

      val inputs = doc.getElementsByTag("input")
      inputs.size() shouldBe 1
      inputs.get(0).attr("value") shouldBe "My previous input"
    }
  }


}
