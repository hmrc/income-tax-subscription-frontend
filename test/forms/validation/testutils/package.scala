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

package forms.validation

import org.scalatest.matchers.must.Matchers._
import play.api.data.Form
import play.api.data.FormBinding.Implicits.formBinding
import play.api.data.validation.Invalid
import play.api.test.FakeRequest
import scala.language.implicitConversions

package object testutils {

  implicit class prefixUtil(prefix: String) {
    def `*`(name: String): String = prefix match {
      case "" => name
      case _ => s"$prefix.$name"
    }
  }



  implicit class ErrorValidationUtil[T](testForm: Form[T]) {
    implicit def assert(testFieldName: String): FormValidationTrait[T] = new FormValidationTrait[T] {
      override val form: Form[T] = testForm
      override val fieldName: String = testFieldName
    }

    def isValidFor(data: T): Unit = {
      val validated = testForm.fillAndValidate(data)
      validated.hasErrors mustBe false
      validated.hasGlobalErrors mustBe false
    }

    def isValidFor(data: Map[String, String]): Unit = {
      val validated = testForm.bind(data)
      validated.hasErrors mustBe false
      validated.hasGlobalErrors mustBe false
    }

    def isValidFor(request: FakeRequest[_]): Unit = {
      val validated = testForm.bindFromRequest()(request, implicitly)
      validated.hasErrors mustBe false
      validated.hasGlobalErrors mustBe false
    }
  }

  implicit class InvalidUtil(invalid: Invalid) {

    def errorTextIs(expectedText: String): Unit =
      invalid.errors.head.message mustBe expectedText
  }

}

trait FormValidationTrait[T] {

  val form: Form[T]
  val fieldName: String
}