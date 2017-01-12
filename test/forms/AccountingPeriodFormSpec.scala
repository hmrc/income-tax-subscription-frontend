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

import models.{AccountingPeriodModel, DateModel}
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import org.scalatest.Matchers._
import submapping.DateMapping

class AccountingPeriodFormSpec extends PlaySpec with OneAppPerTest {

  import AccountingPeriodForm._
  import DateMapping._

  implicit class prefixUtil(prefix: String) {
    def `*`(name: String) = s"$prefix.$name"
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
  }

}
