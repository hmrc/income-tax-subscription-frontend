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

import models.DateModel
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.FormError
import play.api.data.format.Formatter

import java.time.LocalDate

class DateModelFormatterSpec extends AnyWordSpecLike with Matchers {

  def dateModelFormatter(isAgent: Boolean = false, minDate: Option[LocalDate] = None, maxDate: Option[LocalDate] = None): Formatter[DateModel] =
    DateModelMapping.DateModelFormatter(isAgent = isAgent, errorContext = "test", minDate, maxDate, None)

  val bindingKey: String = "testKey"

  def inputMap(day: Option[String], month: Option[String], year: Option[String]): Map[String, String] = List(
    s"$bindingKey-${DateModelMapping.day}" -> day,
    s"$bindingKey-${DateModelMapping.month}" -> month,
    s"$bindingKey-${DateModelMapping.year}" -> year
  ).collect {
    case (key, Some(value)) => (key, value)
  }.toMap

  def errorKey(isAgent: Boolean)(error: String): String = if (isAgent) s"agent.$error" else error

  "dateModelFormatter.bind" when {
    for (isAgent <- Seq(true, false)) {
      s"isAgent is set to $isAgent" should {
        "return a date model" when {
          "a valid day, month and year are present" in {
            val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("20"), month = Some("10"), year = Some("2020")))
            result mustBe Right(DateModel(day = "20", month = "10", year = "2020"))
          }
          "the date is 29th of feburary and it's a leap year" in {
            val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("29"), month = Some("2"), year = Some("2020")))
            result mustBe Right(DateModel(day = "29", month = "2", year = "2020"))
          }
        }
        "return an error" when {
          "the day field is empty" in {
            val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some(""), month = Some("2"), year = Some("2020")))
            result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day.empty"))))
          }
          "the day field is not present" in {
            val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = None, month = Some("2"), year = Some("2020")))
            result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day.empty"))))
          }
          "the month field is empty" in {
            val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("20"), month = Some(""), year = Some("2020")))
            result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.month}", errorKey(isAgent)("error.test.month.empty"))))
          }
          "the month field is not present" in {
            val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("20"), month = None, year = Some("2020")))
            result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.month}", errorKey(isAgent)("error.test.month.empty"))))
          }
          "the year field is empty" in {
            val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("20"), month = Some("10"), year = Some("")))
            result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.year}", errorKey(isAgent)("error.test.year.empty"))))
          }
          "the year field is not present" in {
            val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("20"), month = Some("10"), year = None))
            result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.year}", errorKey(isAgent)("error.test.year.empty"))))
          }
          "the day and month fields are empty" in {
            val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = None, month = None, year = Some("2020")))
            result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day-month.empty"))))
          }
          "the day and year fields are empty" in {
            val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = None, month = Some("2"), year = None))
            result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day-year.empty"))))
          }
          "the month and year fields are empty" in {
            val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("20"), month = None, year = None))
            result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.month}", errorKey(isAgent)("error.test.month-year.empty"))))
          }
          "all fields are empty" in {
            val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = None, month = None, year = None))
            result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day-month-year.empty"))))
          }
          "the day field is invalid" when {
            "the day is text" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("invalid"), month = Some("10"), year = Some("2020")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day.invalid"))))
            }
            "the day is 0" in {
              (1 to 12) map { month =>
                val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("0"), month = Some(month.toString), year = Some("2020")))
                result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day.invalid"))))
              }
            }
            "it's not a leap year, the month is february and day is 29th" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("29"), month = Some("2"), year = Some("2019")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day.invalid"))))
            }
            "its the 31st of month without 31 days" in {
              List(4, 6, 9, 11) map { month =>
                val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("31"), month = Some(month.toString), year = Some("2020")))
                result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day.invalid"))))
              }
            }
            "its the 32nd of the months with only 31 days" in {
              List(1, 3, 5, 7, 8, 10, 12) map { month =>
                val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("32"), month = Some(month.toString), year = Some("2020")))
                result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day.invalid"))))
              }
            }
          }
          "the month field is invalid" when {
            "the month is text" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("20"), month = Some("invalid"), year = Some("2020")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.month}", errorKey(isAgent)("error.test.month.invalid"))))
            }
            "the month is 0" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("20"), month = Some("0"), year = Some("2020")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.month}", errorKey(isAgent)("error.test.month.invalid"))))
            }
            "the month is 13" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("20"), month = Some("13"), year = Some("2020")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.month}", errorKey(isAgent)("error.test.month.invalid"))))
            }
          }
          "the year field is invalid" when {
            "the year is text" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("20"), month = Some("10"), year = Some("invalid")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.year}", errorKey(isAgent)("error.test.year.invalid"))))
            }
            "the year has 3 digits" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("20"), month = Some("10"), year = Some("999")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.year}", errorKey(isAgent)("error.test.year.length"))))
            }
            "the year has 5 digits" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("20"), month = Some("10"), year = Some("10000")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.year}", errorKey(isAgent)("error.test.year.length"))))
            }
          }
          "the date is invalid" when {
            "the date is too early" in {
              val result = dateModelFormatter(isAgent, minDate = Some(DateModel(day = "8", month = "4", year = "2022").toLocalDate))
                .bind(bindingKey, inputMap(day = Some("8"), month = Some("4"), year = Some("2020")))
              result mustBe Left(Seq(
                FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day-month-year.min-date"), List("2022-04-08"))
              ))
            }
            "the date is too late" in {
              val result = dateModelFormatter(isAgent, maxDate = Some(DateModel(day = "8", month = "4", year = "2020").toLocalDate))
                .bind(bindingKey, inputMap(day = Some("8"), month = Some("4"), year = Some("2022")))
              result mustBe Left(Seq(
                FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day-month-year.max-date"), List("2020-04-08"))
              ))
            }
          }
          "the date contains multiple issues" when {
            "the day field is empty and the year is too small" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some(""), month = Some("10"), year = Some("999")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day-year.invalid"))))
            }
            "the day field is invalid and the year is too small" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("invalid"), month = Some("10"), year = Some("10000")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day-year.invalid"))))
            }
            "the day field is invalid and the month field is empty" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("invalid"), month = Some(""), year = Some("2022")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day-month.invalid"))))
            }
            "the day field is invalid, the month field is empty and the year is too big" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("invalid"), month = Some(""), year = Some("10000")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day-month-year.invalid"))))
            }
            "the day field and the month field are invalid" in {
              val result = dateModelFormatter(isAgent).bind(bindingKey, inputMap(day = Some("56"), month = Some("13"), year = Some("1999")))
              result mustBe Left(Seq(FormError(s"$bindingKey-${DateModelMapping.day}", errorKey(isAgent)("error.test.day-month.invalid"))))
            }
          }
        }
      }
    }
  }

  "dateModelFormatter.unbind" must {
    "return a map of values in the date model relating to the original inputs" in {
      dateModelFormatter().unbind(bindingKey, DateModel("20", "10", "2020")) mustBe Map(
        s"$bindingKey-${DateModelMapping.day}" -> "20",
        s"$bindingKey-${DateModelMapping.month}" -> "10",
        s"$bindingKey-${DateModelMapping.year}" -> "2020"
      )
    }
  }

}
