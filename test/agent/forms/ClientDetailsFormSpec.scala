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

package agent.forms

import agent.assets.MessageLookup
import core.forms.submapping.DateMapping._
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.testutils._
import agent.models.DateModel
import agent.models.agent.ClientDetailsModel
import agent.utils.TestConstants
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages.Implicits._

class ClientDetailsFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  import agent.forms.ClientDetailsForm._

  val testClientFirstName = "Test client first name"
  val testClientLastName = "Test client last name"
  val testClientNino = TestConstants.testNino
  val dob = DateModel("01", "02", "1980")

  def setupTestData(fname: String = testClientFirstName,
                    lname: String = testClientLastName,
                    nino: String = testClientNino,
                    dob: DateModel = dob
                   ): Map[String, String] = {
    Map(
      clientDateOfBirth * dateDay -> dob.day,
      clientDateOfBirth * dateMonth -> dob.month,
      clientDateOfBirth * dateYear -> dob.year,
      clientFirstName -> fname,
      clientNino -> nino,
      clientLastName -> lname
    )
  }

  "The clientDetailsForm" should {

    "For valid data should transform the data to the case class" in {
      val testInput = setupTestData()
      val expected = ClientDetailsModel(testClientFirstName, testClientLastName, testClientNino, dob)
      val actual = clientDetailsForm.bind(testInput).value
      actual mustBe Some(expected)
    }

    "when testing the validation for the data" should {

      "when testing the first name" should {

        "error if no name is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.client_details.first_name.empty")
          errors fieldErrorIs MessageLookup.Error.ClientDetails.firstNameEmpty
          errors summaryErrorIs MessageLookup.Error.ClientDetails.firstNameEmpty
          val testInput = setupTestData(fname = "")
          clientDetailsForm.bind(testInput) assert clientFirstName hasExpectedErrors errors
        }

        "error if an invalid name is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.client_details.first_name.invalid")
          errors fieldErrorIs MessageLookup.Error.ClientDetails.firstNameInvalid
          errors summaryErrorIs MessageLookup.Error.ClientDetails.firstNameInvalid
          val testInput = setupTestData(fname = "␢")
          clientDetailsForm.bind(testInput) assert clientFirstName hasExpectedErrors errors
        }

        "error if a name which is too long is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.client_details.first_name.maxLength")
          errors fieldErrorIs MessageLookup.Error.ClientDetails.firstNameMaxLength
          errors summaryErrorIs MessageLookup.Error.ClientDetails.firstNameMaxLength
          val testInput = setupTestData(fname = "abc" * 100)
          clientDetailsForm.bind(testInput) assert clientFirstName hasExpectedErrors errors
        }

      }

      "when testing the last name" should {

        "Error if no last name is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.client_details.last_name.empty")
          errors fieldErrorIs MessageLookup.Error.ClientDetails.lastNameEmpty
          errors summaryErrorIs MessageLookup.Error.ClientDetails.lastNameEmpty
          val testInput = setupTestData(lname = "")
          clientDetailsForm.bind(testInput) assert clientLastName hasExpectedErrors errors
        }

        "Error if an invalid last name is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.client_details.last_name.invalid")
          errors fieldErrorIs MessageLookup.Error.ClientDetails.lastNameInvalid
          errors summaryErrorIs MessageLookup.Error.ClientDetails.lastNameInvalid
          val testInput = setupTestData(lname = "␢")
          clientDetailsForm.bind(testInput) assert clientLastName hasExpectedErrors errors
        }

        "error if a name which is too long is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.client_details.last_name.maxLength")
          errors fieldErrorIs MessageLookup.Error.ClientDetails.lastNameMaxLength
          errors summaryErrorIs MessageLookup.Error.ClientDetails.lastNameMaxLength
          val testInput = setupTestData(lname = "abc" * 100)
          clientDetailsForm.bind(testInput) assert clientLastName hasExpectedErrors errors
        }

      }

      "when testing the NINO" should {

        "error if no NINO is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.nino.empty")
          errors fieldErrorIs MessageLookup.Error.Nino.empty
          errors summaryErrorIs MessageLookup.Error.Nino.empty
          val testInput = setupTestData(nino = "")
          clientDetailsForm.bind(testInput) assert clientNino hasExpectedErrors errors
        }

        "error if an invalid NINO is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.nino.invalid")
          errors fieldErrorIs MessageLookup.Error.Nino.invalid
          errors summaryErrorIs MessageLookup.Error.Nino.invalid
          val testInput = setupTestData(nino = "3456677")
          clientDetailsForm.bind(testInput) assert clientNino hasExpectedErrors errors
        }

      }

      "when testing the DoB" should {

        "error if no DoB is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.dob_date.empty")
          errors fieldErrorIs MessageLookup.Error.DOBDate.empty
          errors summaryErrorIs MessageLookup.Error.DOBDate.empty
          val testInput = setupTestData(dob = DateModel("","",""))
          clientDetailsForm.bind(testInput) assert clientDateOfBirth hasExpectedErrors errors
        }

        "error if a none numeric day is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.dob_date.invalid_chars")
          errors fieldErrorIs MessageLookup.Error.DOBDate.invalid_chars
          errors summaryErrorIs MessageLookup.Error.DOBDate.invalid_chars
          val testInput = setupTestData(dob = DateModel("aa","10","1990"))
          clientDetailsForm.bind(testInput) assert clientDateOfBirth hasExpectedErrors errors
        }

        "error if a none numeric month is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.dob_date.invalid_chars")
          errors fieldErrorIs MessageLookup.Error.DOBDate.invalid_chars
          errors summaryErrorIs MessageLookup.Error.DOBDate.invalid_chars
          val testInput = setupTestData(dob = DateModel("01","aa","1990"))
          clientDetailsForm.bind(testInput) assert clientDateOfBirth hasExpectedErrors errors
        }

        "error if a none numeric year is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.dob_date.invalid_chars")
          errors fieldErrorIs MessageLookup.Error.DOBDate.invalid_chars
          errors summaryErrorIs MessageLookup.Error.DOBDate.invalid_chars
          val testInput = setupTestData(dob = DateModel("01","12","aa"))
          clientDetailsForm.bind(testInput) assert clientDateOfBirth hasExpectedErrors errors
        }

        "error if an invalid numeric day is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.dob_date.invalid")
          errors fieldErrorIs MessageLookup.Error.DOBDate.invalid
          errors summaryErrorIs MessageLookup.Error.DOBDate.invalid
          val testInput = setupTestData(dob = DateModel("56","10","1990"))
          clientDetailsForm.bind(testInput) assert clientDateOfBirth hasExpectedErrors errors
        }

        "error if an invalid numeric month is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.dob_date.invalid")
          errors fieldErrorIs MessageLookup.Error.DOBDate.invalid
          errors summaryErrorIs MessageLookup.Error.DOBDate.invalid
          val testInput = setupTestData(dob = DateModel("01","15","1990"))
          clientDetailsForm.bind(testInput) assert clientDateOfBirth hasExpectedErrors errors
        }

        "error if an invalid numeric year is supplied" in {
          val errors = ErrorMessageFactory.error("agent.error.dob_date.invalid")
          errors fieldErrorIs MessageLookup.Error.DOBDate.invalid
          errors summaryErrorIs MessageLookup.Error.DOBDate.invalid
          val testInput = setupTestData(dob = DateModel("01","12","1234567899"))
          clientDetailsForm.bind(testInput) assert clientDateOfBirth hasExpectedErrors errors
        }
      }
    }
  }
}
