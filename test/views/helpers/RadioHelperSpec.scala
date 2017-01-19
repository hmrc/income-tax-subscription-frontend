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
import play.api.i18n.Messages.Implicits._
import utils.UnitTestTrait
import views.html.helpers.RadioOption

class RadioHelperSpec extends UnitTestTrait {

  private def radioHelper(field: Field, legend: String, options: Seq[RadioOption])
  = views.html.helpers.radioHelper(field, legend, options)(applicationMessages)

  case class TestData(radio: String)

  val radioName = "radio"
  val testForm = Form(
    mapping(
      radioName -> oText.toText.verifying(DataMap.alwaysFail)
    )(TestData.apply)(TestData.unapply)
  )

  val testLegend = "my test legend text"
  val yesOption = RadioOption("yes", "Yes - you can")
  val noOption = RadioOption("no", "No - you cannot")
  val testOptions: Seq[RadioOption] = Seq(yesOption, noOption)

  "RadioOption" should {
    "have the correct implementations" in {
      val op1 = RadioOption("a", "msg")
      val op2 = op1.copy()
      val op3 = RadioOption("b", "msg")

      op1.eq(op2) shouldBe false
      op1.canEqual(op2) shouldBe true
      op1.equals(op2) shouldBe true
      op1.hashCode == op2.hashCode shouldBe true

      op1.canEqual(op3) shouldBe true
      op1.equals(op3) shouldBe false
      op1.hashCode == op3.hashCode shouldBe false

      op1 match {
        case RadioOption("a", "msg") => true shouldBe true
        case _ => fail()
      }

      op1.productArity shouldBe 2
      op1.productElement(0) shouldBe "a"
      op1.productElement(1) shouldBe "msg"
      val productElementThrown = intercept[IndexOutOfBoundsException] {
        op1.productElement(2)
      }
      productElementThrown.getMessage shouldBe "The parameter for RadioName.productElement cannot exceed 1. {2}"

      val illegalArgumentExceptionThrown = intercept[IllegalArgumentException] {
        RadioOption("a a", "b")
      }
      illegalArgumentExceptionThrown.getMessage shouldBe "RadioName: the optionName parameter must not contain any spaces {a a}"
    }
  }

  "RadioHelper" should {
    "populate the relevent content in the correct positions" in {
      val testField = testForm(radioName)
      val doc = radioHelper(testField, testLegend, testOptions).doc
      doc.getElementsByTag("div").hasClass("form-group") shouldBe true
      doc.getElementsByTag("div").hasClass("form-field") shouldBe true
      doc.getElementsByTag("legend").text() shouldBe testLegend
      val inputs = doc.getElementsByTag("input")
      inputs.size() shouldBe 2
      inputs.get(0).attr("name") shouldBe radioName
      inputs.get(0).attr("value") shouldBe yesOption.optionName
      inputs.get(0).attr("id") shouldBe s"$radioName-${yesOption.optionName}"
      inputs.get(0).attr("type") shouldBe "radio"
      inputs.get(1).attr("name") shouldBe radioName
      inputs.get(1).attr("value") shouldBe noOption.optionName
      inputs.get(1).attr("id") shouldBe s"$radioName-${noOption.optionName}"
      inputs.get(1).attr("type") shouldBe "radio"

      val lablesForInputs = doc.getElementsByTag("label")
      lablesForInputs.size() shouldBe 2
      lablesForInputs.get(0).text() shouldBe yesOption.message
      lablesForInputs.get(1).text() shouldBe noOption.message
    }

    "if the form is populated, then select the correct radio button" in {
      val testField = testForm.fill(TestData(noOption.optionName))(radioName)
      val doc = radioHelper(testField, testLegend, testOptions).doc

      val inputs = doc.getElementsByTag("input")
      inputs.size() shouldBe 2
      inputs.get(0).attr("value") shouldBe yesOption.optionName
      inputs.get(0).attr("checked") shouldBe ""
      inputs.get(1).attr("value") shouldBe noOption.optionName
      inputs.get(1).attr("checked") shouldBe "checked"
    }

    "when there is error on the field, the errors needs to be displayed, but not otherwise" in {
      val testField = testForm(radioName)
      val doc = radioHelper(testField, testLegend, testOptions).doc
      doc.getElementsByTag("div").hasClass("form-field--error") shouldBe false
      doc.getElementsByClass("error-notification").isEmpty shouldBe true

      val errorField = testForm.bind(DataMap.EmptyMap)(radioName)
      val errDoc = radioHelper(errorField, testLegend, testOptions).doc
      errDoc.getElementsByTag("div").hasClass("form-field--error") shouldBe true
      errDoc.getElementsByClass("error-notification").isEmpty shouldBe false
    }
  }

}
