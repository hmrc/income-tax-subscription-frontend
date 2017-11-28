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

package core.views.helpers

import core.utils.UnitTestTrait
import play.api.data.Form
import play.api.data.Forms.{mapping, _}


class PackageSpec extends UnitTestTrait {

  case class TestModel(str: String)

  val testForm: Form[TestModel] = Form(mapping(
    "str" -> text.verifying(str => str.isEmpty)
  )(TestModel.apply)(TestModel.unapply))


  "prefixErr" should {
    val testTitle = "test title"
    import core.views.html.helpers._

    "not add prefix to the title when the dependent form has no errors" in {
      val result = prefixErr(testTitle, testForm.fillAndValidate(TestModel("")))
      result mustBe testTitle
    }

    "add prefix to the title when the dependent form has errors" in {
      val result = prefixErr(testTitle, testForm.fillAndValidate(TestModel("not empty")))
      result mustBe "Error: " + testTitle
    }
  }

}
