/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.formatters.DateModelMapping._
import models.DateModel
import models.usermatching.UserDetailsModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import utilities.agent.TestConstants

import java.time.LocalDate

class ClientDetailsFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  import forms.agent.ClientDetailsForm._

  private val testClientFirstName = "Test client first name"
  private val testClientLastName = "Test client last name"
  private val testClientNino: String = TestConstants.testNino
  private val dob = DateModel("1", "2", "1980")

  def setupTestData(fname: String = testClientFirstName,
                    lname: String = testClientLastName,
                    nino: String = testClientNino,
                    dob: DateModel = dob
                   ): Map[String, String] = {
    Map(
      s"$clientDateOfBirth-$day" -> dob.day,
      s"$clientDateOfBirth-$month" -> dob.month,
      s"$clientDateOfBirth-$year" -> dob.year,
      clientFirstName -> fname,
      clientNino -> nino,
      clientLastName -> lname
    )
  }

  "The clientDetailsForm" should {

    "transform valid date to the case class" in {
      val testInput = setupTestData()
      val expected = UserDetailsModel(testClientFirstName, testClientLastName, testClientNino, dob)
      val actual = clientDetailsForm.bind(testInput).value
      actual mustBe Some(expected)
    }

    "when testing the validation for the data" should {

      "when testing the first name" should {

        "error if no name is supplied" in {
          val testInput = setupTestData(fname = "")

          clientDetailsForm.bind(testInput).errors must contain(FormError(clientFirstName, ErrorKey.fNameEmpty))
        }

        "error if an invalid name is supplied" in {
          val testInput = setupTestData(fname = "␢")

          clientDetailsForm.bind(testInput).errors must contain(FormError(clientFirstName, ErrorKey.fNameInvalid))
        }

        "error if a name which is too long is supplied" in {
          val testInput = setupTestData(fname = "abc" * 100)

          clientDetailsForm.bind(testInput).errors must contain(FormError(clientFirstName, ErrorKey.fNameMaxLength))
        }

      }

      "when testing the last name" should {

        "Error if no last name is supplied" in {
          val testInput = setupTestData(lname = "")

          clientDetailsForm.bind(testInput).errors must contain(FormError(clientLastName, ErrorKey.lNameEmpty))
        }

        "Error if an invalid last name is supplied" in {
          val testInput = setupTestData(lname = "␢")

          clientDetailsForm.bind(testInput).errors must contain(FormError(clientLastName, ErrorKey.lNameInvalid))
        }

        "error if a name which is too long is supplied" in {
          val testInput = setupTestData(lname = "abc" * 100)

          clientDetailsForm.bind(testInput).errors must contain(FormError(clientLastName, ErrorKey.lNameMaxLength))
        }

      }

      "when testing the NINO" should {

        "error if no NINO is supplied" in {
          val testInput = setupTestData(nino = "")

          clientDetailsForm.bind(testInput).errors must contain(FormError(clientNino, ErrorKey.ninoEmpty))
        }

        "error if an invalid NINO is supplied" in {
          val testInput = setupTestData(nino = "3456677")

          clientDetailsForm.bind(testInput).errors must contain(FormError(clientNino, ErrorKey.ninoInvalid))
        }

        "succeed if a lowercase NINO is supplied" in {
          val lowerCaseNino = testClientNino.toLowerCase
          val testInput = setupTestData(nino = lowerCaseNino)
          val expected = UserDetailsModel(testClientFirstName, testClientLastName, testClientNino, dob)
          val actual = clientDetailsForm.bind(testInput).value
          actual mustBe Some(expected)
        }

        "succeed if there is whitespace in NINO" in {
          val testInput = setupTestData(nino = "\tAA  1\t23456D\t\n")
          val expected = UserDetailsModel(testClientFirstName, testClientLastName, "AA123456D", dob)
          val actual = clientDetailsForm.bind(testInput).value
          actual mustBe Some(expected)
        }

      }

      "when testing the DoB" should {
        "fail to bind" when {
          "empty dates are supplied" which {
            "has all fields empty" in {
              val testInput = setupTestData(dob = DateModel("", "", ""))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth", ErrorKey.dateAllEmpty))
            }
            "has an empty day" in {
              val testInput = setupTestData(dob = DateModel("", "12", "1980"))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth", ErrorKey.dateRequired, Seq("day")))
            }
            "has an empty month" in {
              val testInput = setupTestData(dob = DateModel("31", "", "1980"))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth", ErrorKey.dateRequired, Seq("month")))
            }
            "has an empty year" in {
              val testInput = setupTestData(dob = DateModel("31", "12", ""))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth", ErrorKey.dateRequired, Seq("year")))
            }
            "has an empty day and month" in {
              val testInput = setupTestData(dob = DateModel("", "", "1980"))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth", ErrorKey.dateRequiredTwo, Seq("day", "month")))
            }
            "has an empty day and year" in {
              val testInput = setupTestData(dob = DateModel("", "1", ""))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth", ErrorKey.dateRequiredTwo, Seq("day", "year")))
            }
            "has an empty month and year" in {
              val testInput = setupTestData(dob = DateModel("1", "", ""))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth", ErrorKey.dateRequiredTwo, Seq("month", "year")))
            }
          }
          "an invalid day is supplied" which {
            "is non-numeric" in {
              val testInput = setupTestData(dob = DateModel("aa", "12", "1980"))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth-dateDay", ErrorKey.dateInvalid, Seq("day")))
            }
            "is not a real day" in {
              val testInput = setupTestData(dob = DateModel("32", "12", "1980"))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth-dateDay", ErrorKey.dateInvalid, Seq("day")))
            }
          }
          "an invalid month is supplied" which {
            "is non-numeric" in {
              val testInput = setupTestData(dob = DateModel("31", "aa", "1980"))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth-dateMonth", ErrorKey.dateInvalid, Seq("month")))
            }
            "is not a real month" in {
              val testInput = setupTestData(dob = DateModel("31", "13", "1980"))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth-dateMonth", ErrorKey.dateInvalid, Seq("month")))
            }
          }
          "an invalid year is supplied" which {
            "is non-numeric" in {
              val testInput = setupTestData(dob = DateModel("31", "12", "198o"))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth-dateYear", ErrorKey.dateYearLength, Seq("year")))
            }
            "is less than 4 digits" in {
              val testInput = setupTestData(dob = dob.copy(year = "123"))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth-dateYear", ErrorKey.dateYearLength, Seq("year")))
            }
            "is more than 4 digits" in {
              val testInput = setupTestData(dob = dob.copy(year = "12345"))

              val errors = clientDetailsForm.bind(testInput).errors
              errors must contain(FormError(s"$clientDateOfBirth-dateYear", ErrorKey.dateYearLength, Seq("year")))
            }
          }
          "multiple fields are invalid" in {
            val testInput = setupTestData(dob = DateModel("32", "13", "1980"))

            val errors = clientDetailsForm.bind(testInput).errors
            errors must contain(FormError(s"$clientDateOfBirth", ErrorKey.dateInvalid))
          }
          "error if a date not in the past is supplied" in {
            val futureDate: LocalDate = LocalDate.now

            val testInput = setupTestData(dob = DateModel.dateConvert(futureDate))
            clientDetailsForm.bind(testInput).errors must contain(FormError(clientDateOfBirth, ErrorKey.dateInPast))
          }
          "not a leap year" in {
            val testInput = setupTestData(dob = DateModel("29", "2", "2025"))

            clientDetailsForm.bind(testInput).errors must contain(FormError(clientDateOfBirth, ErrorKey.dateInvalid))
          }
        }
        "bind successfully" when {
          "it is a leap year" in {
            val testInput = setupTestData(dob = DateModel("29", "2", "2024"))
            val expected = UserDetailsModel(testClientFirstName, testClientLastName, testClientNino, DateModel("29", "2", "2024"))

            val actual = clientDetailsForm.bind(testInput).value
            actual mustBe Some(expected)
          }
        }
      }
    }
  }

  object ErrorKey {
    val errorContext: String = "agent.error"
    val clientDetailsErrorContext: String = "agent.error.client-details"
    val dateErrorContext: String = s"$clientDetailsErrorContext.date-of-birth"

    val fNameEmpty = s"$clientDetailsErrorContext.first-name.empty"
    val fNameInvalid = s"$clientDetailsErrorContext.first-name.invalid"
    val fNameMaxLength = s"$clientDetailsErrorContext.first-name.max-length"

    val lNameEmpty = s"$clientDetailsErrorContext.last-name.empty"
    val lNameInvalid = s"$clientDetailsErrorContext.last-name.invalid"
    val lNameMaxLength = s"$clientDetailsErrorContext.last-name.max-length"

    val ninoEmpty = s"$errorContext.nino.empty"
    val ninoInvalid = s"$errorContext.nino.invalid"

    val dateInvalid = s"$dateErrorContext.invalid"
    val dateYearLength = s"$dateErrorContext.year.length"
    val dateRequired = s"$dateErrorContext.required"
    val dateRequiredTwo = s"$dateErrorContext.required.two"
    val dateAllEmpty = s"$dateErrorContext.all.empty"
    val dateInPast = s"$dateErrorContext.day-month-year.not-in-past"
  }

}
