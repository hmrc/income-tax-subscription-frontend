/*
 * Copyright 2020 HM Revenue & Customs
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

package forms.agent

import forms.validation.models.{FieldError, SummaryError}
import org.scalatest.Matchers._
import play.api.data.Form
import play.api.data.validation.Invalid
import play.api.i18n.Messages
import play.api.test.FakeRequest

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
      validated.hasErrors shouldBe false
      validated.hasGlobalErrors shouldBe false
    }

    def isValidFor(data: Map[String, String]): Unit = {
      val validated = testForm.bind(data)
      validated.hasErrors shouldBe false
      validated.hasGlobalErrors shouldBe false
    }

    def isValidFor(request: FakeRequest[_]): Unit = {
      val validated = testForm.bindFromRequest()(request)
      validated.hasErrors shouldBe false
      validated.hasGlobalErrors shouldBe false
    }
  }

  implicit class InvalidUtil(invalid: Invalid) {

    import forms.validation.ErrorMessageFactory._

    def fieldErrorIs(expectedText: String)(implicit messages: Messages): Unit =
      invalid.errors.head.args(FieldErrorLoc).asInstanceOf[FieldError].toText shouldBe expectedText

    def summaryErrorIs(expectedText: String)(implicit messages: Messages): Unit =
      invalid.errors.head.args(SummaryErrorLoc).asInstanceOf[SummaryError].toText shouldBe expectedText
  }

}
