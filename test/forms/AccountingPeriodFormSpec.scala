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

package forms

import forms.validation.models.{ErrorMessage, FieldError, SummaryError}
import forms.validation.{ErrorMessageFactory, ErrorMessageHelper}
import models.{AccountingPeriodModel, DateModel}
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import org.scalatest.Matchers._
import play.api.data.Form
import play.api.data.validation.Invalid
import submapping.DateMapping
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

import scala.util.Try

class AccountingPeriodFormSpec extends PlaySpec with OneAppPerTest {

  import AccountingPeriodForm._
  import DateMapping._

  implicit class prefixUtil(prefix: String) {
    def `*`(name: String): String = prefix match {
      case "" => name
      case _ => s"$prefix.$name"
    }
  }

  "The DateForm" should {
    "transform the request to the form case class" in {
      val testDateDay = "01"
      val testDateMonth = "02"
      val testDateYear = "2000"

      val testDateDay2 = "31"
      val testDateMonth2 = "03"
      val testDateYear2 = "2001"

      val testInput = Map(
        startDate * dateDay -> testDateDay, startDate * dateMonth -> testDateMonth, startDate * dateYear -> testDateYear,
        endDate * dateDay -> testDateDay2, endDate * dateMonth -> testDateMonth2, endDate * dateYear -> testDateYear2
      )

      val expected = AccountingPeriodModel(
        DateModel(testDateDay, testDateMonth, testDateYear),
        DateModel(testDateDay2, testDateMonth2, testDateYear2))

      val actual = accountingPeriodForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    object DataMap {
      def date(prefix: String)(day: String, month: String, year: String): Map[String, String] =
        Map(prefix * dateDay -> day, prefix * dateMonth -> month, prefix * dateYear -> year)

      val emptyDate: String => Map[String, String] = (prefix: String) => date(prefix)("", "", "")
    }

    sealed trait TestTrait {
      def hasExpectedErrors(invalid: Invalid): Unit

      def doesNotHaveSpecifiedErrors(invalid: Invalid): Unit
    }

    implicit class ErrorValidationUtil(form: Form[_]) {
      implicit def assert(fieldName: String): TestTrait = new TestTrait {

        private def errorMsgIsDefined(err: ErrorMessage): Unit = {
          //TODO uncomment when the error messages are defined
          //          withClue(s"\nthe error message for: ${err.messageKey} is not defined in the messages file\n") {
          //            err.toText should not be err.messageKey
          //          }
        }

        private def hasFieldError(invalid: Invalid): Unit = {
          withClue(s"\nThe $fieldName field did not contain the expected error:\n") {
            val oErr = ErrorMessageHelper.getFieldError(form, fieldName)
            withClue(s"No errors were found for $fieldName\nformErrors=${form.errors}\n") {
              oErr shouldBe defined
            }
            val actual = oErr.get
            val expectedErr = invalid.errors.head.args.head.asInstanceOf[FieldError]
            withClue(s"Expected: $expectedErr\nbut found: $actual\n") {
              expectedErr shouldBe actual
            }
            errorMsgIsDefined(actual)
          }
        }

        private def hasSummaryError(invalid: Invalid): Unit = {
          withClue(s"\nThe summary errors did not contain an error for $fieldName:\n") {
            withClue(s"getSummaryErrors failed, form.errors:\n${form.errors}\n") {
              Try {
                ErrorMessageHelper.getSummaryErrors(form)
              }
                .isSuccess shouldBe true
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

        override def hasExpectedErrors(invalid: Invalid): Unit = {
          hasFieldError(invalid)
          hasSummaryError(invalid)
        }

        private def doesNotHaveSpecifiedFieldError(invalid: Invalid): Unit = {
          val specifiedErr = invalid.errors.head.args.head.asInstanceOf[FieldError]
          withClue(s"\nThe $fieldName field contained the specified error $specifiedErr:\n") {
            val oErr = ErrorMessageHelper.getFieldError(form, fieldName)
            oErr match {
              case Some(actual) => actual should not be specifiedErr
              case _ =>
            }
          }
        }

        private def doesNotHavSummaryError(invalid: Invalid): Unit = {
          withClue(s"\nThe summary errors contained the specified error for $fieldName:\n") {
            withClue(s"getSummaryErrors failed, form.errors:\n${form.errors}\n") {
              Try {
                ErrorMessageHelper.getSummaryErrors(form)
              }.isSuccess shouldBe true
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

        override def doesNotHaveSpecifiedErrors(invalid: Invalid): Unit = {
          doesNotHaveSpecifiedFieldError(invalid)
          doesNotHavSummaryError(invalid)
        }
      }
    }

    "start date should be correctly validated" in {
      val empty = ErrorMessageFactory.error("error.empty_date")
      val invalid = ErrorMessageFactory.error("error.invalid_date")

      val emptyDateInput = DataMap.emptyDate(startDate)
      val emptyTest = accountingPeriodForm.bind(emptyDateInput)
      emptyTest assert startDate hasExpectedErrors empty

      val invalidDateInput = DataMap.date(startDate)("30", "2", "2017")
      val invalidTest = accountingPeriodForm.bind(invalidDateInput)
      invalidTest assert startDate hasExpectedErrors invalid
    }

    "end date should be correctly validated" in {
      val empty = ErrorMessageFactory.error("error.empty_date")
      val invalid = ErrorMessageFactory.error("error.invalid_date")
      val violation = ErrorMessageFactory.error("error.end_date_violation")

      val emptyDateInput = DataMap.emptyDate(endDate)
      val emptyTest = accountingPeriodForm.bind(emptyDateInput)
      emptyTest assert endDate hasExpectedErrors empty

      val invalidDateInput = DataMap.date(endDate)("29", "2", "2017")
      val invalidTest = accountingPeriodForm.bind(invalidDateInput)
      invalidTest assert endDate hasExpectedErrors invalid

      val endDateViolationInput = DataMap.date(startDate)("28", "2", "2017") ++ DataMap.date(endDate)("28", "2", "2017")
      val violationTest = accountingPeriodForm.bind(endDateViolationInput)
      violationTest assert endDate hasExpectedErrors violation

      val validInput = DataMap.date(startDate)("27", "2", "2017") ++ DataMap.date(endDate)("28", "2", "2017")
      val validTest = accountingPeriodForm.bind(validInput)
      validTest assert endDate doesNotHaveSpecifiedErrors violation
    }
  }

}
