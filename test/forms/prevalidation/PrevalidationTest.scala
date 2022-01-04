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

package forms.prevalidation

import forms.prevalidation.CaseOption.CaseOption
import forms.prevalidation.TrimOption.TrimOption
import play.api.data.Form
import play.api.data.Forms._
import utilities.UnitTestTrait

class PrevalidationTest extends UnitTestTrait {

  case class DummyData(string1: String)

  object DummyForm {

    val dummyForm: Form[DummyData] = Form[DummyData](
      mapping(
        "string1" -> text
      )(DummyData.apply)(DummyData.unapply)
    )

    def preprocessedForm(trims: Map[String, TrimOption] = Map(),
                         caseRules: Map[String, CaseOption] = Map()) :PrevalidationAPI[DummyData]
    = PreprocessedForm(dummyForm, trims, caseRules)

  }

  def testData(data: String): Map[String, String] = Map[String, String]("string1" -> data)

  "Preprocessor" should {
    "trim any text strings at both ends when additional whitespace exists for option 'both'" in {
      import TrimOption._
      val defaultTrims = Map[String, TrimOption](
        "string1" -> both
      )
      val form = DummyForm.preprocessedForm(defaultTrims)

      val result = form.bind(testData(" Apples and the \t    oranges    \t")).get
      result.string1 mustBe "Apples and the \t    oranges"
    }

    "remove all whitespace if it exists for option 'all'" in {
      val defaultTrims = Map[String, TrimOption](
        "string1" -> TrimOption.all
      )
      val form = DummyForm.preprocessedForm(defaultTrims)

      val result = form.bind(testData(" Apples and the \t    oranges    \t")).get
      result.string1 mustBe "Applesandtheoranges"
    }

    "trim any text strings at both ends and compress when additional whitespace exists for option 'bothAndCompress'" in {
      import TrimOption._
      val defaultTrims = Map[String, TrimOption](
        "string1" -> bothAndCompress
      )
      val form = DummyForm.preprocessedForm(defaultTrims)

      val result = form.bind(testData(" Apples and the \t    oranges    \t")).get
      result.string1 mustBe "Apples and the oranges"
    }

    "not trim any text strings when additional whitespace exists for option 'none'" in {
      import TrimOption._
      val defaultTrims = Map[String, TrimOption](
        "string1" -> none
      )
      val form = DummyForm.preprocessedForm(defaultTrims)

      val result = form.bind(testData(" Apples and the \t    oranges    \t")).get
      result.string1 mustBe " Apples and the \t    oranges    \t"
    }

    "amend the case of any text strings to uppercase for option 'upper'" in {
      import CaseOption._
      val defaultCase = Map[String, CaseOption](
        "string1" -> upper
      )
      val form = DummyForm.preprocessedForm(caseRules = defaultCase)

      val result = form.bind(testData("Apples and the oranges")).get
      result.string1 mustBe "APPLES AND THE ORANGES"
    }

    "amend the case of any text strings to lowercase for option 'lower'" in {
      import CaseOption._
      val defaultCase = Map[String, CaseOption](
        "string1" -> lower
      )
      val form = DummyForm.preprocessedForm(caseRules = defaultCase)

      val result = form.bind(testData("Apples and the oranges")).get
      result.string1 mustBe "apples and the oranges"
    }

    "leave the case of any text strings for option 'none'" in {
      import CaseOption._
      val defaultCase = Map[String, CaseOption](
        "string1" -> none
      )
      val form = DummyForm.preprocessedForm(caseRules = defaultCase)

      val result = form.bind(testData("Apples and the oranges")).get
      result.string1 mustBe "Apples and the oranges"
    }
  }

  "Preprocessor.addNewPreprocessFunction" should {
    val func: Map[String, String] => Map[String, String] = {
      input =>
        input.map {
          case ("string1", v) => "string1" -> v.replace("%s", input("string2"))
          case x => x
        }
    }
    val rawForm = DummyForm.preprocessedForm()
    val formWithAdditonalFunction = rawForm.addNewPreprocessFunction(func)

    "apply the new preprocessor" in {
      val input = Map("string1" -> "I am %s", "string2" -> "a test")
      val unedited = rawForm.bind(input).get
      unedited.string1 mustBe "I am %s"
      val result = formWithAdditonalFunction.bind(input).get
      result.string1 mustBe "I am a test"
    }
  }

  "Preprocessor's XssFilter" should {
    val form = DummyForm.preprocessedForm()
    "ignore none threatening text" in {
      val expected = "<business><p>paragraph</p>"
      val input = testData(expected)
      val result = form.bind(input).get
      result.string1 mustBe expected
    }

    "strip out any java script" in {
      val attackStr = "my custom attack"
      val expected = "my custom attack:"
      val attack = s"$expected<script>$attackStr</script>"
      val input = testData(attack)
      val result = form.bind(input).get
      result.string1 mustBe expected
    }

    "strip out any java script scenario 2" in {
      val attackStr = "my custom attack"
      val expected = "/>mystr"
      val attack = s"$expected<script>$attackStr</script>"
      val input = testData(attack)
      val result = form.bind(input).get
      result.string1 mustBe expected
    }
  }
}
