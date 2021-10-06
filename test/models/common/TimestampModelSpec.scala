/*
 * Copyright 2021 HM Revenue & Customs
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

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import play.api.libs.json.{JsResult, Json}
import uk.gov.hmrc.play.test.UnitSpec

class TimestampModelSpec extends UnitSpec {
  "TimestampModel" should {
    "deserialize the MongoDB response" in {
      val actual: JsResult[TimestampModel] = Json.fromJson[TimestampModel](Json.parse("""{"$date":0}"""))
      val expected = TimestampModel(
        new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC)
      )

      actual.get mustBe expected
    }
  }
}
