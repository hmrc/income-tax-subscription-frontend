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

import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec
import utilities.individual.Constants.GovernmentGateway._
import utilities.individual.TestConstants._

class EmacEnrolmentRequestSpec extends UnitSpec {
  "format" should {
    "format the enrolment request correctly as JSON" in {
      val userId = "1234567890"

      val request = EmacEnrolmentRequest(userId, testNino)

      val expectedJson = s"""{"userId":"$userId","friendlyName":"$ggFriendlyName","type":"principal","verifiers":[{"key":"$NINO","value":"$testNino"}]}"""

      Json.toJson(request).toString shouldBe expectedJson
    }
  }
}
