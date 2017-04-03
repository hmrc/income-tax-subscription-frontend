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

package forms.agent

import forms.AccountingPeriodDateForm.startDate
import models.{ClientDetailsModel, DateModel}
import forms.validation.testutils.{DataMap, _}
import forms.submapping.DateMapping._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ClientDetailsFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  import ClientDetailsForm._

  "The clientDetailsForm" should {
    "transform the data to the case class" in {
      val testClientFirstName = "Test client first name"
      val testClientLastName = "Test client last name"
      val testClientNino = "AB123456C"

      val testDay = "01"
      val testMonth = "02"
      val testYear = "1980"

      val dob = DateModel(testDay, testMonth, testYear)


      val testInput = Map(
        clientDateOfBirth * dateDay -> testDay,
        clientDateOfBirth * dateMonth -> testMonth,
        clientDateOfBirth * dateYear -> testYear,
        clientFirstName -> testClientFirstName,
        clientNino -> testClientNino,
        clientLastName -> testClientLastName
      )

      val expected = ClientDetailsModel(testClientFirstName, testClientLastName, testClientNino, dob)
      val actual = clientDetailsForm.bind(testInput).value
      actual mustBe Some(expected)
    }
  }
}
