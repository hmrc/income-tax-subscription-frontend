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

import org.scalatest.Matchers._
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Valid, ValidationResult}

class ErrorMessageHelperSpec extends PlaySpec with OneServerPerSuite {

  case class TestModel(testField1: String, testField2: String, testField3: String)

  val testField1 = "testField1"
  val testField2 = "testField2"
  val testField3 = "testField3"

  val testInvalid = (fieldName: String) => ErrorMessageFactory.error("", fieldName, "errMsg", "arg1", "arg2")

  val checkLength: String => String => ValidationResult = (fieldName: String) => (a: String) =>
    a.length > 10 match {
      case true => Valid
      case false => testInvalid(fieldName)
    }

  def testConstraint[A](f: A => ValidationResult) = Constraint[A]("")(f)

  val testForm = Form(
    mapping(
      testField1 -> text.verifying(testConstraint(checkLength(testField1))),
      testField2 -> text.verifying(testConstraint(checkLength(testField2))),
      testField3 -> text.verifying(testConstraint(checkLength(testField3)))
    )(TestModel.apply)(TestModel.unapply)
  )

  "Error message helper" should {

    "in case of no errors, None sould be returned for the field error" in {
      val actual = ErrorMessageHelper.getFieldError(testForm, testField3)
      actual shouldBe None

      val actual2 = ErrorMessageHelper.getFieldError(testForm(testField3))
      actual2 shouldBe None

      val actual3 = ErrorMessageHelper.getFieldError(testForm(testField3), testForm)
      actual3 shouldBe None
    }

    "in case of no errors, get summary error should return an empty sequence" in {
      val actual = ErrorMessageHelper.getSummaryErrors(testForm)
      actual shouldBe Seq()
    }

    "in case of errors retrieve the error associated to the field" in {
      val testData = Map[String, String](testField1 -> "", testField2 -> "", testField3 -> "")
      val validatedForm = testForm.bind(testData)
      val expected = testInvalid(testField3).errors.head.args.head

      val actual = ErrorMessageHelper.getFieldError(validatedForm, testField3)
      actual shouldBe Some(expected)

      val actual2 = ErrorMessageHelper.getFieldError(validatedForm(testField3))
      actual2 shouldBe Some(expected)

      val actual3 = ErrorMessageHelper.getFieldError(validatedForm(testField3), validatedForm)
      actual3 shouldBe Some(expected)
    }

    "in case of errors retrieve the all the summary errors on the form" in {
      val testData = Map[String, String](testField1 -> "", testField2 -> "", testField3 -> "")
      val validatedForm = testForm.bind(testData)
      val expected = Seq(
        testInvalid(testField1).errors.map(e => e.args(1)),
        testInvalid(testField2).errors.map(e => e.args(1)),
        testInvalid(testField3).errors.map(e => e.args(1))
      ).flatten

      val actual = ErrorMessageHelper.getSummaryErrors(validatedForm)
      actual shouldBe expected
    }
  }

}
