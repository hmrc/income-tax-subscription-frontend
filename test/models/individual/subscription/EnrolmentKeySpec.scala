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

package models.individual.subscription

import core.Constants.GovernmentGateway._
import core.Constants._
import core.utils.TestConstants._
import uk.gov.hmrc.play.test.UnitSpec

class EnrolmentKeySpec extends UnitSpec {
  "asString" should {
    "format the enrolment key correctly" in {
      val enrolmentKey = EnrolmentKey(mtdItsaEnrolmentName, MTDITID -> testMTDID)

      val expectedFormattedKey = s"$mtdItsaEnrolmentName~$MTDITID~$testMTDID"

      enrolmentKey.asString shouldBe expectedFormattedKey
    }
  }
}