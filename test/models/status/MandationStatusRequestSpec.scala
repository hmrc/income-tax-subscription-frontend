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

package models.status

import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class MandationStatusRequestSpec extends PlaySpec {

  val testNino: String = "AA111111A"
  val testUtr: String = "1234567890"

  val fullJsonRead: JsObject = Json.obj(
    "nino" -> testNino,
    "utr" -> testUtr
  )

  val fullJsonWrite: JsObject = Json.obj(
    "nino" -> testNino,
    "utr" -> testUtr
  )

  val fullModel: MandationStatusRequest = MandationStatusRequest(
    nino = testNino,
    utr = testUtr
  )

  "read" must {
    "successfully read from json" when {
      "the json has full details" in {
        Json.fromJson[MandationStatusRequest](fullJsonRead) mustBe JsSuccess(fullModel)
      }
    }
    "fail to read" when {
      "nino is missing" in {
        Json.fromJson[MandationStatusRequest](fullJsonRead - "nino") mustBe JsError(__ \ "nino", "error.path.missing")
      }
      "utr is missing" in {
        Json.fromJson[MandationStatusRequest](fullJsonRead - "utr") mustBe JsError(__ \ "utr", "error.path.missing")
      }
    }
  }

  "write" must {
    "successfully write to json" in {
      Json.toJson(fullModel) mustBe fullJsonWrite
    }
  }

}
