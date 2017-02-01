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

package connectors.models

import play.api.libs.json.Json
import utils.UnitTestTrait

class EnrolmentModelSpec extends UnitTestTrait {

  "Enrolment Model" should {
    "be read correctly from json" in {
      val json = Json.parse(
        """
          {
              "key": "IR-SA",
              "identifiers": [
                {"key": "UTR", "value": "999902737"}
              ],
              "state": "Activated"
          }
        """)
      val parsed = Json.fromJson[Enrolment](json)
      parsed.isSuccess mustBe true
      parsed.get mustBe Enrolment("IR-SA", Seq(Identifier("UTR", "999902737")), "Activated")
    }

    "return the correct isEnrolled result" in {
      val enrolled = Enrolment("", Seq(), "Activated")
      val notEnrolled = Enrolment("", Seq(), "")

      enrolled.isEnrolled mustBe true
      Some(enrolled).isEnrolled mustBe true

      notEnrolled.isEnrolled mustBe false
      Some(notEnrolled).isEnrolled mustBe false
      None.asInstanceOf[Option[Enrolment]].isEnrolled mustBe false
    }
  }

}
