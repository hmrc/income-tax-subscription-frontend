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

package forms.usermatching

import forms.formatters.DateModelMapping._
import models.DateModel
import models.usermatching.UserDetailsModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import utilities.individual.TestConstants

import java.time.LocalDate

class UserDetailsFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  import forms.usermatching.UserDetailsForm._

  val testUserFirstName = "Test user first name"
  val testUserLastName = "Test user last name"
  val testUserNino: String = TestConstants.testNino
  val dob: DateModel = DateModel("01", "02", "1980")

  def setupTestData(fname: String = testUserFirstName,
                    lname: String = testUserLastName,
                    nino: String = testUserNino,
                    dob: DateModel = dob
                   ): Map[String, String] = {
    Map(
      s"$userDateOfBirth-$day" -> dob.day,
      s"$userDateOfBirth-$month" -> dob.month,
      s"$userDateOfBirth-$year" -> dob.year,
      userFirstName -> fname,
      userNino -> nino,
      userLastName -> lname
    )
  }

  val dateErrorContext: String = "error.user_details.date_of_birth"

  "The userDetailsForm" should {

    "For valid data should transform the data to the case class" in {
      val testInput = setupTestData()
      val expected = UserDetailsModel(testUserFirstName, testUserLastName, testUserNino, dob)
      val actual = userDetailsForm.bind(testInput).value
      actual mustBe Some(expected)
    }

    "when testing the validation for the data" should {

      "when testing the first name" should {

        "error if no name is supplied" in {
          val errors = "error.user_details.first_name.empty"

          val testInput = setupTestData(fname = "")
          userDetailsForm.bind(testInput).errors must contain(FormError(userFirstName, errors))
        }

        "error if an invalid name is supplied" in {
          val errors = "error.user_details.first_name.invalid"

          val testInput = setupTestData(fname = "␢")
          userDetailsForm.bind(testInput).errors must contain(FormError(userFirstName, errors))
        }

        "error if a name which is too long is supplied" in {
          val errors = "error.user_details.first_name.maxLength"

          val testInput = setupTestData(fname = "abc" * 100)
          userDetailsForm.bind(testInput).errors must contain(FormError(userFirstName, errors))
        }

      }

      "when testing the last name" should {

        "Error if no last name is supplied" in {
          val errors = "error.user_details.last_name.empty"

          val testInput = setupTestData(lname = "")
          userDetailsForm.bind(testInput).errors must contain(FormError(userLastName, errors))
        }

        "Error if an invalid last name is supplied" in {
          val errors = "error.user_details.last_name.invalid"

          val testInput = setupTestData(lname = "␢")
          userDetailsForm.bind(testInput).errors must contain(FormError(userLastName, errors))
        }

        "error if a name which is too long is supplied" in {
          val errors = "error.user_details.last_name.maxLength"

          val testInput = setupTestData(lname = "abc" * 100)
          userDetailsForm.bind(testInput).errors must contain(FormError(userLastName, errors))
        }

      }

      "when testing the NINO" should {

        "error if no NINO is supplied" in {
          val errors = "error.nino.empty"

          val testInput = setupTestData(nino = "")
          userDetailsForm.bind(testInput).errors must contain(FormError(userNino, errors))
        }

        "error if an invalid NINO is supplied" in {
          val errors = "error.nino.invalid"

          val testInput = setupTestData(nino = "3456677")
          userDetailsForm.bind(testInput).errors must contain(FormError(userNino, errors))
        }

      }

      "when testing the DoB" should {

        "error if the year provided is not the correct length" when {
          "the year is 3 digits" in {
            val error = s"$dateErrorContext.year.length"
            val testInput = setupTestData(dob = dob.copy(year = "123"))
            val errors = userDetailsForm.bind(testInput).errors
            errors must contain(FormError(s"$userDateOfBirth-dateYear", error))
          }
          "the year is 5 digits" in {
            val error = s"$dateErrorContext.year.length"
            val testInput = setupTestData(dob = dob.copy(year = "12345"))
            val errors = userDetailsForm.bind(testInput).errors
            errors must contain(FormError(s"$userDateOfBirth-dateYear", error))
          }
        }

        "error if an invalid date is supplied" which {
          "has an invalid day" in {
            val error = s"$dateErrorContext.invalid"

            val testInput = setupTestData(dob = DateModel("32", "12", "1980"))
            val errors = userDetailsForm.bind(testInput).errors
            errors must contain(FormError(s"$userDateOfBirth-dateDay", error))
          }
          "has an invalid month" in {
            val error = s"$dateErrorContext.invalid"

            val testInput = setupTestData(dob = DateModel("31", "13", "1980"))
            val errors = userDetailsForm.bind(testInput).errors
            errors must contain(FormError(s"$userDateOfBirth-dateMonth", error))
          }
          "has an invalid year" in {
            val error = s"$dateErrorContext.invalid"

            val testInput = setupTestData(dob = DateModel("31", "12", "invalid"))
            val errors = userDetailsForm.bind(testInput).errors
            errors must contain(FormError(s"$userDateOfBirth-dateYear", error))
          }
          "has multiple invalid fields" in {
            val error = s"$dateErrorContext.invalid"

            val testInput = setupTestData(dob = DateModel("32", "13", "1980"))
            val errors = userDetailsForm.bind(testInput).errors
            errors must contain(FormError(s"$userDateOfBirth-dateDay", error))
          }
        }

        "error if empty dates are supplied" which {
          "has an empty day" in {
            val error = s"$dateErrorContext.day.empty"

            val testInput = setupTestData(dob = DateModel("", "12", "1980"))
            val errors = userDetailsForm.bind(testInput).errors
            errors must contain(FormError(s"$userDateOfBirth-dateDay", error))
          }
          "has an empty month" in {
            val error = s"$dateErrorContext.month.empty"

            val testInput = setupTestData(dob = DateModel("31", "", "1980"))
            val errors = userDetailsForm.bind(testInput).errors
            errors must contain(FormError(s"$userDateOfBirth-dateMonth", error))
          }
          "has an empty year" in {
            val error = s"$dateErrorContext.year.empty"

            val testInput = setupTestData(dob = DateModel("31", "12", ""))
            val errors = userDetailsForm.bind(testInput).errors
            errors must contain(FormError(s"$userDateOfBirth-dateYear", error))
          }
          "has multiple empty fields" in {
            val error = s"$dateErrorContext.day_month.empty"

            val testInput = setupTestData(dob = DateModel("", "", "1980"))
            val errors = userDetailsForm.bind(testInput).errors
            errors must contain(FormError(s"$userDateOfBirth-dateDay", error))
          }
        }

        "error if a date not in the past is supplied" in {
          val errors = s"$dateErrorContext.not_in_past"
          val futureDate: LocalDate = LocalDate.now

          val testInput = setupTestData(dob = DateModel.dateConvert(futureDate))
          userDetailsForm.bind(testInput).errors must contain(FormError(userDateOfBirth, errors))
        }
      }
    }
  }
}
