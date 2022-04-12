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

package forms.formatters

import forms.formatters.DateErrorMapping._
import forms.formatters.DateModelMapping.{DateModelValidation, HtmlIds}
import forms.formatters.DateValidation.{DayField, MonthField, YearField}
import models.DateModel
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError

class DateErrorMappingSpec extends AnyWordSpec with Matchers {
  private val testKey = "test-key"
  private val testKeys = HtmlIds(testKey)

  "highlightField" should {
    "return 'true'" when {
      "the day field key is in the empty date error message key" in {
        DateErrorMapping.highlightField(DayField, "agent.error.property.day_month_year.empty") mustBe true
      }

      "the day field key is in the empty day error message key" in {
        DateErrorMapping.highlightField(DayField, "agent.error.property.day.empty") mustBe true
      }

      "the day field key is in the empty day/month error message key" in {
        DateErrorMapping.highlightField(DayField, "agent.error.property.day_month.empty") mustBe true
      }

      "the day field key is in the invalid date error message key" in {
        DateErrorMapping.highlightField(DayField, "agent.error.property.day_month_year.invalid") mustBe true
      }

      "the day field key is in the invalid day error message key" in {
        DateErrorMapping.highlightField(DayField, "agent.error.property.day.invalid") mustBe true
      }

      "the day field key is in the invalid day/month error message key" in {
        DateErrorMapping.highlightField(DayField, "agent.error.property.day_month.invalid") mustBe true
      }

      "the day field key is in the min date error message key" in {
        DateErrorMapping.highlightField(MonthField, "agent.error.property.day_month_year.min_date") mustBe true
      }

      "the day field key is in the max date error message key" in {
        DateErrorMapping.highlightField(YearField, "agent.error.property.day_month_year.max_date") mustBe true
      }
    }

    "return 'false'" when {
      "the day field key not is in the message key" in {
        DateErrorMapping.highlightField(DayField, "agent.error.today.month_year.empty") mustBe false
      }

      "the month field key not is in the message key" in {
        DateErrorMapping.highlightField(MonthField, "agent.error.monthly_update.day.empty") mustBe false
      }

      "the year field key not is in the message key" in {
        DateErrorMapping.highlightField(YearField, "agent.error.financial_year.month.empty") mustBe false
      }
    }
  }

  "transformErrors" should {
    for (isAgent <- Seq(true, false)) {
      s"return a single${affinityGroup(isAgent)} error" when {
        "given a TooEarly error without a min date" in {
          DateErrorMapping.transformErrors(
            errors = List(TooEarly),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None
          ) mustBe error("test-key-dateDay", s"${affinityGroupKey(isAgent)}error.test.day_month_year.min_date")
        }

        "given a TooEarly error with a min date" in {
          DateErrorMapping.transformErrors(
            errors = List(TooEarly),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None,
            minDate = Some(DateModel("1", "1", "2022").toLocalDate)
          ) mustBe error("test-key-dateDay", s"${affinityGroupKey(isAgent)}error.test.day_month_year.min_date", List("2022-01-01"))
        }

        "given a TooLate error without a max date" in {
          DateErrorMapping.transformErrors(
            errors = List(TooLate),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None
          ) mustBe error("test-key-dateDay", s"${affinityGroupKey(isAgent)}error.test.day_month_year.max_date")
        }

        "given a TooLate error with a max date" in {
          DateErrorMapping.transformErrors(
            errors = List(TooLate),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None,
            maxDate = Some(DateModel("1", "1", "2022").toLocalDate)
          ) mustBe error("test-key-dateDay", s"${affinityGroupKey(isAgent)}error.test.day_month_year.max_date", List("2022-01-01"))
        }

        "given an InvalidDate error" in {
          DateErrorMapping.transformErrors(
            errors = List(InvalidDate),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None
          ) mustBe error("test-key-dateDay", s"${affinityGroupKey(isAgent)}error.test.day_month_year.empty")
        }

        "given InvalidDay and InvalidMonth errors" in {
          DateErrorMapping.transformErrors(
            errors = List(InvalidDay, InvalidMonth, InvalidDay),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None
          ) mustBe error("test-key-dateDay", s"${affinityGroupKey(isAgent)}error.test.day_month.invalid")
        }

        "given InvalidMonth and InvalidYear errors" in {
          DateErrorMapping.transformErrors(
            errors = List(InvalidMonth, InvalidYear),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None
          ) mustBe error("test-key-dateMonth", s"${affinityGroupKey(isAgent)}error.test.month_year.invalid")
        }

        "given InvalidYear errors" in {
          DateErrorMapping.transformErrors(
            errors = List(InvalidYear),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None
          ) mustBe error("test-key-dateYear", s"${affinityGroupKey(isAgent)}error.test.year.invalid")
        }

        "given EmptyDay and EmptyMonth errors" in {
          DateErrorMapping.transformErrors(
            errors = List(EmptyDay, EmptyMonth),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None
          ) mustBe error("test-key-dateDay", s"${affinityGroupKey(isAgent)}error.test.day_month.empty")
        }

        "given EmptyMonth and EmptyYear errors" in {
          DateErrorMapping.transformErrors(
            errors = List(EmptyMonth, EmptyYear),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None
          ) mustBe error("test-key-dateMonth", s"${affinityGroupKey(isAgent)}error.test.month_year.empty")
        }

        "given an EmptyYear error" in {
          DateErrorMapping.transformErrors(
            errors = List(EmptyYear),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None
          ) mustBe error("test-key-dateYear", s"${affinityGroupKey(isAgent)}error.test.year.empty")
        }

        "given an InvalidYearLength error" in {
          DateErrorMapping.transformErrors(
            errors = List(InvalidYearLength),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None
          ) mustBe error("test-key-dateYear", s"${affinityGroupKey(isAgent)}error.test.year.length")
        }

        "given EmptyDay, InvalidMonth and InvalidYearLength errors" in {
          DateErrorMapping.transformErrors(
            errors = List(EmptyDay, InvalidMonth, InvalidYearLength),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None
          ) mustBe error("test-key-dateDay", s"${affinityGroupKey(isAgent)}error.test.day_month_year.invalid")
        }

        "given InvalidDay, InvalidMonth and InvalidYearLength errors" in {
          DateErrorMapping.transformErrors(
            errors = List(InvalidDay, InvalidMonth, InvalidDay, InvalidYearLength),
            ids = testKeys,
            isAgent = isAgent,
            errorContext = "test",
            dateFormatter = None
          ) mustBe error("test-key-dateDay", s"${affinityGroupKey(isAgent)}error.test.day_month_year.invalid")
        }
      }
    }
  }

  private def affinityGroup(isAgent: Boolean) =
    if (isAgent) " agent" else ""

  private def affinityGroupKey(isAgent: Boolean) =
    if (isAgent) "agent." else ""

  private def error(formKey: String, messageKey: String, args: List[String] = List.empty[String]): DateModelValidation = {
    Left(List(
      FormError(formKey, List(messageKey), args)
    ))
  }
}
