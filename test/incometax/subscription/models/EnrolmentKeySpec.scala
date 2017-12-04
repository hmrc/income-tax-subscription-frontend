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

package incometax.subscription.models

import uk.gov.hmrc.play.test.UnitSpec

class EnrolmentKeySpec extends UnitSpec {
  "asString" should {
    "format the enrolment key correctly" in {
      val enrolmentId = "HMRC-MTD-IT"
      val mtdidIdentifier = "MTDITID" -> "012345678912345"

      val enrolmentKey = EnrolmentKey(enrolmentId, mtdidIdentifier)

      val expectedFormattedKey = "HMRC-MTD-IT~MTDITID~012345678912345"

      enrolmentKey.asString shouldBe expectedFormattedKey
    }
  }
}
