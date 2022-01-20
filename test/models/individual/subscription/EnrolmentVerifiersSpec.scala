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

import models.common.subscription.EnrolmentVerifiers
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json

class EnrolmentVerifiersSpec extends AnyWordSpecLike with Matchers with OptionValues {
  "format" should {
    "produce a correctly formatted json object" in {
      val verifierKey = "verifierKey"
      val verifierValue = "verifierValue"

      val model = EnrolmentVerifiers(verifierKey -> verifierValue)

      val expectedJson = s"""{"verifiers":[{"key":"$verifierKey","value":"$verifierValue"}]}"""

      Json.toJson(model).toString shouldBe expectedJson
    }
  }
}

