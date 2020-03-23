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

package forms.usermatching

import forms.submapping.DateMapping._
import forms.validation.testutils._
import models.DateModel
import models.usermatching.UserDetailsModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import utilities.individual.TestConstants

class UserDetailsFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  import forms.usermatching.UserDetailsForm._

  val testUserFirstName = "Test user first name"
  val testUserLastName = "Test user last name"
  val testUserNino = TestConstants.testNino
  val dob = DateModel("01", "02", "1980")

  def setupTestData(fname: String = testUserFirstName,
                    lname: String = testUserLastName,
                    nino: String = testUserNino,
                    dob: DateModel = dob
                   ): Map[String, String] = {
    Map(
      userDateOfBirth * dateDay -> dob.day,
      userDateOfBirth * dateMonth -> dob.month,
      userDateOfBirth * dateYear -> dob.year,
      userFirstName -> fname,
      userNino -> nino,
      userLastName -> lname
    )
  }

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

        "error if no DoB is supplied" in {
          val errors = "error.dob_date.empty"

          val testInput = setupTestData(dob = DateModel("", "", ""))
          userDetailsForm.bind(testInput).errors must contain(FormError(userDateOfBirth, errors))
        }

        "error if a none numeric day is supplied" in {
          val errors = "error.dob_date.invalid_chars"

          val testInput = setupTestData(dob = DateModel("aa", "10", "1990"))
          userDetailsForm.bind(testInput).errors must contain(FormError(userDateOfBirth, errors))
        }

        "error if a none numeric month is supplied" in {
          val errors = "error.dob_date.invalid_chars"

          val testInput = setupTestData(dob = DateModel("01", "aa", "1990"))
          userDetailsForm.bind(testInput).errors must contain(FormError(userDateOfBirth, errors))
        }

        "error if a none numeric year is supplied" in {
          val errors = "error.dob_date.invalid_chars"

          val testInput = setupTestData(dob = DateModel("01", "12", "aa"))
          userDetailsForm.bind(testInput).errors must contain(FormError(userDateOfBirth, errors))
        }

        "error if an invalid numeric day is supplied" in {
          val errors = "error.dob_date.invalid"

          val testInput = setupTestData(dob = DateModel("56", "10", "1990"))
          userDetailsForm.bind(testInput).errors must contain(FormError(userDateOfBirth, errors))
        }

        "error if an invalid numeric month is supplied" in {
          val errors = "error.dob_date.invalid"

          val testInput = setupTestData(dob = DateModel("01", "15", "1990"))
          userDetailsForm.bind(testInput).errors must contain(FormError(userDateOfBirth, errors))
        }

        "error if an invalid numeric year is supplied" in {
          val errors = "error.dob_date.invalid"

          val testInput = setupTestData(dob = DateModel("01", "12", "1234567899"))
          userDetailsForm.bind(testInput).errors must contain(FormError(userDateOfBirth, errors))
        }
      }
    }
  }
}
