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

package models.audits

import models.common.business.{Address, Country}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class AuditAddressSpec extends PlaySpec {
  private val line = "Line"
  private val town = "Town"
  private val postcode = "XYZ"
  private val country = Country("UK", "United Kingdom")
  private val uprn = Some("1234")

  private val json1Line = Json.obj(
    "addressLine1" -> line,
    "townOrCity" -> town,
    "postcode" -> postcode,
    "country" -> Json.obj(
      "code" -> country.code,
      "name" -> country.name
    ),
    "uprn" -> uprn.getOrElse("")
  )

  private val json2Lines = Json.obj(
    "addressLine1" -> line,
    "addressLine2" -> line,
    "townOrCity" -> town,
    "postcode" -> postcode,
    "country" -> Json.obj(
      "code" -> country.code,
      "name" -> country.name
    ),
    "uprn" -> uprn.getOrElse("")
  )

  private val json3Lines = Json.obj(
    "addressLine1" -> line,
    "addressLine2" -> line,
    "addressLine3" -> line,
    "townOrCity" -> town,
    "postcode" -> postcode,
    "country" -> Json.obj(
      "code" -> country.code,
      "name" -> country.name
    ),
    "uprn" -> uprn.getOrElse("")
  )

  private def completeAddress(lines: Int) = Address(
    lines = Seq.fill(lines)(line) ++ Seq(town),
    postcode = Some(postcode),
    country = Some(country),
    uprn = uprn
  )

  private val data = Map(
    1 -> AuditAddress(line, None, None, town, Some(postcode), Some(country), uprn),
    2 -> AuditAddress(line, Some(line), None, town, Some(postcode), Some(country), uprn),
    3 -> AuditAddress(line, Some(line), Some(line), town, Some(postcode), Some(country), uprn),
  )

  private val json = Map(
    1 -> json1Line,
    2 -> json2Lines,
    3 -> json3Lines
  )

  "AuditAddress" should {
    data.foreach { case (lines, expected) =>
      s"Correctly converts an completeAddress with $lines line(s)" in {
        AuditAddress(completeAddress(lines)) mustBe expected
      }
    }

    json.foreach { case (lines, expected) =>
      s"Writes address with $lines line(s) to correct Json" in {
        Json.toJson(AuditAddress(completeAddress(lines))) mustBe expected
      }
    }
  }
}
