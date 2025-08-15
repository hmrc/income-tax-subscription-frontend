/*
 * Copyright 2025 HM Revenue & Customs
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

import models.common.BusinessAccountingPeriod._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class BusinessAccountingPeriodSpec extends PlaySpec {

  "BusinessAccountingPeriod" must {
    "read from json successfully" when {
      "given the SixthAprilToFifthApril key" in {
        val json = Json.toJson(SixthAprilToFifthApril.key)
        json.as[BusinessAccountingPeriod] mustBe SixthAprilToFifthApril
      }
      "given the FirstAprilToThirtyFirstMarch key" in {
        val json = Json.toJson(FirstAprilToThirtyFirstMarch.key)
        json.as[BusinessAccountingPeriod] mustBe FirstAprilToThirtyFirstMarch
      }
      "given the OtherAccountingPeriod key" in {
        val json = Json.toJson(OtherAccountingPeriod.key)
        json.as[BusinessAccountingPeriod] mustBe OtherAccountingPeriod
      }
    }
  }
  "write to json successfully" when {
    "given a BusinessAccountingPeriod of SixthAprilToFifthApril" in {
      Json.toJson[BusinessAccountingPeriod](SixthAprilToFifthApril) mustBe JsString(SixthAprilToFifthApril.key)
    }
    "given a BusinessAccountingPeriod of FirstAprilToThirtyFirstMarch" in {
      Json.toJson[BusinessAccountingPeriod](FirstAprilToThirtyFirstMarch) mustBe JsString(FirstAprilToThirtyFirstMarch.key)
    }
    "given a BusinessAccountingPeriod of OtherAccountingPeriod" in {
      Json.toJson[BusinessAccountingPeriod](OtherAccountingPeriod) mustBe JsString(OtherAccountingPeriod.key)
    }
  }
}
