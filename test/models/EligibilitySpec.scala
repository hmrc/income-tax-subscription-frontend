/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsSuccess, Json}

class EligibilitySpec extends PlaySpec with GuiceOneServerPerSuite {

  "the Eligibility model" should {

    "handle missing prepopData" in {
      val valueWithoutPrepop =
        """{
          |  "eligibleCurrentYear": true,
          |  "eligibleNextYear": true
          |}""".stripMargin
      Json.parse(valueWithoutPrepop).validate[EligibilityStatus] mustBe JsSuccess(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason = None))
    }

    "handle old value of 'eligible'" in {
      val valueWithoutPrepop =
        """{
          |  "eligible": true,
          |  "eligibleCurrentYear": true,
          |  "eligibleNextYear": true
          |}""".stripMargin
      Json.parse(valueWithoutPrepop).validate[EligibilityStatus] mustBe JsSuccess(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason = None))
    }

    "handle empty prepopData" in {
      val valueWithEmptyPrepop =
        """{
          |  "eligibleCurrentYear": true,
          |  "eligibleNextYear": true
          |}""".stripMargin
      Json.parse(valueWithEmptyPrepop).validate[EligibilityStatus] mustBe JsSuccess(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason = None))
    }

    "handle trivial prepopData" in {
      val valueWithTrivialPrepop =
        """{
          |  "eligibleCurrentYear": true,
          |  "eligibleNextYear": true
          |}""".stripMargin
      Json.parse(valueWithTrivialPrepop).validate[EligibilityStatus] mustBe JsSuccess(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason = None))
    }

  }
}
