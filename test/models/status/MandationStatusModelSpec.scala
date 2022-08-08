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

import models.status.MandationStatus.{Mandated, Voluntary}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class MandationStatusModelSpec extends PlaySpec {

  val fullModel: MandationStatusModel = MandationStatusModel(
    currentYearStatus = Voluntary,
    nextYearStatus = Mandated
  )

  val fullJson: JsObject = Json.obj(
    "currentYearStatus" -> "mtd voluntary",
    "nextYearStatus" -> "mtd mandated"
  )

  "read" must {
    "successfully read from json" when {
      "the json has full details" in {
        Json.fromJson[MandationStatusModel](fullJson) mustBe JsSuccess(fullModel)
      }
    }
    "fail to read" when {
      "nino is missing" in {
        Json.fromJson[MandationStatusModel](fullJson - "currentYearStatus") mustBe JsError(__ \ "currentYearStatus", "error.path.missing")
      }
      "utr is missing" in {
        Json.fromJson[MandationStatusModel](fullJson - "nextYearStatus") mustBe JsError(__ \ "nextYearStatus", "error.path.missing")
      }
    }
  }

  "write" must {
    "successfully write to json" in {
      Json.toJson(fullModel) mustBe fullJson
    }
  }

}
