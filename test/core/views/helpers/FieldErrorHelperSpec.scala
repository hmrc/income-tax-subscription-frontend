/*
 * Copyright 2019 HM Revenue & Customs
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

import core.forms.validation.ErrorMessageFactory
import core.forms.validation.models.FieldError
import core.forms.validation.testutils.DataMap
import core.forms.validation.utils.MappingUtil._
import org.scalatest.Matchers._
import play.api.data.Forms.mapping
import play.api.data.validation.Invalid
import play.api.data.{Field, Form}
import play.api.i18n.Messages.Implicits.applicationMessages
import core.utils.UnitTestTrait
import core.views.html.helpers

class FieldErrorHelperSpec extends UnitTestTrait {

  import ErrorMessageFactory._

  case class TestData(field1: String)

  private def fieldErrorHelper(field: Field, form: Form[_])
  = helpers.fieldErrorHelper(field, form)(applicationMessages)

  val errorMessage: Invalid = DataMap.alwaysFailInvalid
  val fieldErrorMessage: FieldError = DataMap.alwaysFailInvalid.errors.head.args(FieldErrorLoc).asInstanceOf[FieldError]

  val fieldName = "field1"

  val testForm = Form(
    mapping(
      fieldName -> oText.toText.verifying(DataMap.alwaysFail)
    )(TestData.apply)(TestData.unapply))

  "FieldErrorHelper" should {

    "if the field does not have an error, nothing should be generated" in {
      val page = fieldErrorHelper(testForm(fieldName), testForm)
      val doc = page.doc
      val spans = doc.getElementsByTag("span")
      spans.size() shouldBe 0
      doc.getElementsByClass("error-notification").isEmpty shouldBe true
    }

    "if the field has an error, then the error should be displayed" in {
      val filledForm = testForm.bind(DataMap.EmptyMap)
      val page = fieldErrorHelper(filledForm(fieldName), filledForm)
      val doc = page.doc
      val spans = doc.getElementsByTag("span")
      spans.size() shouldBe 1
      spans.get(0).attr("class") shouldBe "error-notification"
      spans.get(0).attr("role") shouldBe "tooltip"
      spans.get(0).attr("id") shouldBe s"error-message-$fieldName"
      spans.get(0).text shouldBe fieldErrorMessage.toText
    }

  }

}
