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

package models.individual.subscription

import models.common.subscription.EnrolmentKey
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import common.Constants.GovernmentGateway._
import common.Constants._
import utilities.individual.TestConstants._

class EnrolmentKeySpec extends AnyWordSpecLike with Matchers with OptionValues {
  "asString" should {
    "format the enrolment key correctly" in {
      val enrolmentKey = EnrolmentKey(mtdItsaEnrolmentName, MTDITID -> testMTDID)

      val expectedFormattedKey = s"$mtdItsaEnrolmentName~$MTDITID~$testMTDID"

      enrolmentKey.asString shouldBe expectedFormattedKey
    }
  }
}
