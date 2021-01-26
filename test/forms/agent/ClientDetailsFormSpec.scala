/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate

import forms.formatters.DateModelMapping._
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.DateModel
import models.usermatching.UserDetailsModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import utilities.agent.TestConstants

class ClientDetailsFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  import forms.agent.ClientDetailsForm._

  val testClientFirstName = "Test client first name"
  val testClientLastName = "Test client last name"
  val testClientNino: String = TestConstants.testNino
  val dob = DateModel("01", "02", "1980")

  def setupTestData(fname: String = testClientFirstName,
                    lname: String = testClientLastName,
                    nino: String = testClientNino,
                    dob: DateModel = dob
                   ): Map[String, String] = {
    Map(
      clientDateOfBirth * day -> dob.day,
      clientDateOfBirth * month -> dob.month,
      clientDateOfBirth * year -> dob.year,
      clientFirstName -> fname,
      clientNino -> nino,
      clientLastName -> lname
    )
  }

  "The clientDetailsForm" should {


    "For valid data should transform the data to the case class" in {
      val testInput = setupTestData()
      val expected = UserDetailsModel(testClientFirstName, testClientLastName, testClientNino, dob)
      val actual = clientDetailsForm.bind(testInput).value
      actual mustBe Some(expected)
    }

    "when testing the validation for the data" should {

      "when testing the first name" should {

        "error if no name is supplied" in {
          val errors = "agent.error.client_details.first_name.empty"

          val testInput = setupTestData(fname = "")
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientFirstName, errors))
        }

        "error if an invalid name is supplied" in {
          val errors = "agent.error.client_details.first_name.invalid"

          val testInput = setupTestData(fname = "␢")
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientFirstName, errors))
        }

        "error if a name which is too long is supplied" in {
          val errors = "agent.error.client_details.first_name.maxLength"

          val testInput = setupTestData(fname = "abc" * 100)
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientFirstName, errors))
        }

      }

      "when testing the last name" should {

        "Error if no last name is supplied" in {
          val errors = "agent.error.client_details.last_name.empty"

          val testInput = setupTestData(lname = "")
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientLastName, errors))
        }

        "Error if an invalid last name is supplied" in {
          val errors = "agent.error.client_details.last_name.invalid"

          val testInput = setupTestData(lname = "␢")
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientLastName, errors))
        }

        "error if a name which is too long is supplied" in {
          val errors = "agent.error.client_details.last_name.maxLength"

          val testInput = setupTestData(lname = "abc" * 100)
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientLastName, errors))
        }

      }

      "when testing the NINO" should {

        "error if no NINO is supplied" in {
          val errors = "agent.error.nino.empty"

          val testInput = setupTestData(nino = "")
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientNino, errors))
        }

        "error if an invalid NINO is supplied" in {
          val errors = "agent.error.nino.invalid"

          val testInput = setupTestData(nino = "3456677")
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientNino, errors))
        }

      }

      "when testing the DoB" should {

        "error if no DoB is supplied" in {
          val errors = "agent.error.dob_date.date.empty"

          val testInput = setupTestData(dob = DateModel("", "", ""))
          clientDetailsForm.bind(testInput).errors must contain(FormError(s"$clientDateOfBirth.dateDay", errors))
        }

        "error if no day is supplied" in {
          val errors = "agent.error.dob_date.day.empty"

          val testInput = setupTestData(dob = DateModel("", "1", "1980"))
          clientDetailsForm.bind(testInput).errors must contain(FormError(s"$clientDateOfBirth.dateDay", errors))
        }

        "error if no month is supplied" in {
          val errors = "agent.error.dob_date.month.empty"

          val testInput = setupTestData(dob = DateModel("1", "", "1980"))
          clientDetailsForm.bind(testInput).errors must contain(FormError(s"$clientDateOfBirth.dateMonth", errors))
        }

        "error if no year is supplied" in {
          val errors = "agent.error.dob_date.year.empty"

          val testInput = setupTestData(dob = DateModel("1", "1", ""))
          clientDetailsForm.bind(testInput).errors must contain(FormError(s"$clientDateOfBirth.dateYear", errors))
        }

        "error if no day and month is supplied" in {
          val errors = "agent.error.dob_date.day_month.empty"

          val testInput = setupTestData(dob = DateModel("", "", "1980"))
          clientDetailsForm.bind(testInput).errors must contain(FormError(s"$clientDateOfBirth.dateDay", errors))
        }

        "error if no day and year is supplied" in {
          val errors = "agent.error.dob_date.day_year.empty"

          val testInput = setupTestData(dob = DateModel("", "1", ""))
          clientDetailsForm.bind(testInput).errors must contain(FormError(s"$clientDateOfBirth.dateDay", errors))
        }

        "error if no month and year is supplied" in {
          val errors = "agent.error.dob_date.month_year.empty"

          val testInput = setupTestData(dob = DateModel("1", "", ""))
          clientDetailsForm.bind(testInput).errors must contain(FormError(s"$clientDateOfBirth.dateMonth", errors))
        }

        "error if a none numeric day is supplied" in {
          val errors = "agent.error.dob_date.empty"

          val testInput = setupTestData(dob = DateModel("aa", "10", "1990"))
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientDateOfBirth, errors))
        }

        "error if a none numeric month is supplied" in {
          val errors = "agent.error.dob_date.empty"

          val testInput = setupTestData(dob = DateModel("01", "aa", "1990"))
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientDateOfBirth, errors))
        }

        "error if a none numeric year is supplied" in {
          val errors = "agent.error.dob_date.empty"

          val testInput = setupTestData(dob = DateModel("01", "12", "aa"))
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientDateOfBirth, errors))
        }

        "error if an invalid numeric day is supplied" in {
          val errors = "agent.error.dob_date.empty"

          val testInput = setupTestData(dob = DateModel("56", "10", "1990"))
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientDateOfBirth, errors))
        }

        "error if an invalid numeric month is supplied" in {
          val errors = "agent.error.dob_date.empty"

          val testInput = setupTestData(dob = DateModel("01", "15", "1990"))
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientDateOfBirth, errors))
        }

        "error if an invalid numeric year is supplied" in {
          val errors = "agent.error.dob_date.empty"

          val testInput = setupTestData(dob = DateModel("01", "12", "1234567899"))
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientDateOfBirth, errors))
        }

        "error if a date not in the past is supplied" in {
          val errors = "agent.error.dob_date.not_in_past"
          val futureDate: LocalDate = LocalDate.now

          val testInput = setupTestData(dob = DateModel.dateConvert(futureDate))
          clientDetailsForm.bind(testInput).errors must contain(FormError(clientDateOfBirth, errors))
        }
      }
    }
  }
}
