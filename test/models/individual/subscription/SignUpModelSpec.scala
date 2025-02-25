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

package models.individual.subscription

import models.common.subscription.SignUpModel
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}

class SignUpModelSpec extends PlaySpec {

  val model: SignUpModel = SignUpModel(
    nino = "test-nino",
    utr = "test-utr",
    taxYear = "2024-25"
  )

  val json: JsObject = Json.obj(
    "nino" -> "test-nino",
    "utr" -> "test-utr",
    "taxYear" -> "2024-25"
  )

  "SignUpModel" must {
    "write to json as expected" in {
      Json.toJson(model) mustBe json
    }
  }

}

