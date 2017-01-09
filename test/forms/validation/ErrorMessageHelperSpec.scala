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

package forms.validation

import forms.validation.ErrorMessageHelper
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.Form
import play.api.data.Forms._
import org.scalatest.Matchers._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationResult}

class ErrorMessageHelperSpec extends PlaySpec with OneServerPerSuite {

  case class TestModel(testField1: String, testField2: String, testField3: String)

  val testField1 = "testField1"
  val testField2 = "testField2"
  val testField3 = "testField3"

  val checkLength: String => ValidationResult = a =>
    a.length > 10 match {
      case true => Valid
      case false => Invalid("my Error Message","arg1","arg2")
    }

  def testConstraint[A](f: A => ValidationResult) = Constraint[A]("")(f)

  val testForm = Form(
    mapping(
      testField1 -> text.verifying((a: String) => a.length > 10),
      testField2 -> text.verifying("my Error Message", (a: String) => a.length > 10),
      testField3 -> text.verifying(testConstraint(checkLength))
    )(TestModel.apply)(TestModel.unapply)
  )

  "Error message helper" should {
    "retrieve the error associated to the field" in {
      val testData = Map[String, String](testField1 -> "", testField2 -> "", testField3 -> "")
      val validatedForm = testForm.bind(testData)

      val actual = ErrorMessageHelper.getFieldError(validatedForm, testField3)
      actual shouldBe testField1
    }
  }

}
