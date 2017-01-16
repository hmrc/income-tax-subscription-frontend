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

package forms.validation.testutils

import forms.validation.ErrorMessageHelper
import forms.validation.models.{ErrorMessage, FieldError, SummaryError}
import play.api.data.Form
import play.api.data.validation.Invalid
import org.scalatest.Matchers._
import scala.util.Try
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

trait FormValidationTrait[T] {

  val form: Form[T]
  val fieldName: String

  def errorMsgIsDefined(err: ErrorMessage)(implicit messages: Messages): Unit = {
    withClue(s"\nthe error message for: ${err.messageKey} is not defined in the messages file\n") {
      err.toText should not be err.messageKey
    }
  }

  def hasFieldError(invalid: Invalid)(implicit messages: Messages): Unit = {
    withClue(s"\nThe $fieldName field did not contain the expected error:\n") {
      val oErr = ErrorMessageHelper.getFieldError(form, fieldName)
      val expectedErr = invalid.errors.head.args.head.asInstanceOf[FieldError]
      withClue(s"No errors were found for $fieldName\nformErrors=${form.errors}\nExpected: $expectedErr") {
        oErr shouldBe defined
      }
      val actual = oErr.get
      withClue(s"Expected: $expectedErr\nbut found: $actual\n") {
        expectedErr shouldBe actual
      }
      errorMsgIsDefined(actual)
    }
  }

  def hasSummaryError(invalid: Invalid)(implicit messages: Messages): Unit = {
    withClue(s"\nThe summary errors did not contain an error for $fieldName:\n") {
      withClue(s"getSummaryErrors failed, form.errors:\n${form.errors}\n") {
        Try {
          ErrorMessageHelper.getSummaryErrors(form)
        }.isSuccess shouldBe true
      }
      val sErrs = ErrorMessageHelper.getSummaryErrors(form)
      val expectedErr = invalid.errors.head.args(1).asInstanceOf[SummaryError]
      val summaryForFieldName = sErrs.filter(_._1 == fieldName)
      withClue(s"Expected: $expectedErr\nbut it's not found in: $sErrs\n\n") {
        summaryForFieldName.size shouldBe 1
        summaryForFieldName.head._1 shouldBe fieldName
        summaryForFieldName.head._2 shouldBe expectedErr
      }
      errorMsgIsDefined(expectedErr)
    }
  }

  def hasExpectedErrors(invalid: Invalid)(implicit messages: Messages): Unit = {
    hasFieldError(invalid)
    hasSummaryError(invalid)
  }

  def doesNotHaveSpecifiedFieldError(invalid: Invalid)(implicit messages: Messages): Unit = {
    val specifiedErr = invalid.errors.head.args.head.asInstanceOf[FieldError]
    withClue(s"\nThe $fieldName field contained the specified error $specifiedErr:\n") {
      val oErr = ErrorMessageHelper.getFieldError(form, fieldName)
      oErr match {
        case Some(actual) => actual should not be specifiedErr
        case _ =>
      }
    }
  }

  def doesNotHavSummaryError(invalid: Invalid)(implicit messages: Messages): Unit = {
    withClue(s"\nThe summary errors contained the specified error for $fieldName:\n") {
      withClue(s"getSummaryErrors failed, form.errors:\n${form.errors}\n") {
        Try {
          ErrorMessageHelper.getSummaryErrors(form)
        }
          .isSuccess shouldBe true
      }
      val sErrs = ErrorMessageHelper.getSummaryErrors(form)
      val expectedErr = invalid.errors.head.args(1).asInstanceOf[SummaryError]
      val summaryForFieldName = sErrs.filter(_._1 == fieldName)
      summaryForFieldName match {
        case Nil => // valid
        case _ => summaryForFieldName.map(_._2).contains(expectedErr) shouldBe false
      }
      errorMsgIsDefined(expectedErr)
    }
  }

  def doesNotHaveSpecifiedErrors(invalid: Invalid)(implicit messages: Messages): Unit = {
    doesNotHaveSpecifiedFieldError(invalid)
    doesNotHavSummaryError(invalid)
  }

}
