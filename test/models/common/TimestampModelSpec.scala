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

package models.common

import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsResult, Json}

import java.time.LocalDateTime

class TimestampModelSpec extends AnyWordSpecLike with Matchers with OptionValues {
  "TimestampModel" should {
    "deserialize the MongoDB response" in {
      val actual: JsResult[TimestampModel] = Json.fromJson[TimestampModel](Json.parse("""{"$date":1642075200000}"""))
      val expected = TimestampModel(
        LocalDateTime.of(2022, 1, 13, 12, 0, 0, 0)
      )

      actual.isSuccess mustBe (true)
      actual.get mustBe expected
    }
    "deserialize the new MongoDB response" in {
      val actual: JsResult[TimestampModel] = Json.fromJson[TimestampModel](Json.parse("""{"$date":{"$numberLong":"1642075200000"}}"""))
      val expected = TimestampModel(
        LocalDateTime.of(2022, 1, 13, 12, 0, 0, 0)
      )

      actual.isSuccess mustBe (true)
      actual.get mustBe expected
    }
  }
}
