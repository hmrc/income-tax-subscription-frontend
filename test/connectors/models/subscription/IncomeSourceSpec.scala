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

package connectors.models.subscription

import incometax.subscription.models.{Both, Business, IncomeSourceType, Property}
import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.play.test.UnitSpec

class IncomeSourceSpec extends UnitSpec {

  "IncomeSourceType" should {

    "Provide the correct reader and writer for Business" in {
      val business: JsValue = Json.toJson(Business)
      val expected = JsString(IncomeSourceType.business)
      business shouldBe expected
      val readBack = Json.fromJson[IncomeSourceType](business).get
      readBack shouldBe Business
    }

    "Provide the correct reader and writer for Property" in {
      val property: JsValue = Json.toJson(Property)
      val expected = JsString(IncomeSourceType.property)
      property shouldBe expected
      val readBack = Json.fromJson[IncomeSourceType](property).get
      readBack shouldBe Property
    }

    "Provide the correct reader and writer for Both" in {
      val both: JsValue = Json.toJson(Both)
      val expected = JsString(IncomeSourceType.both)
      both shouldBe expected
      val readBack = Json.fromJson[IncomeSourceType](both).get
      readBack shouldBe Both
    }

  }

}
