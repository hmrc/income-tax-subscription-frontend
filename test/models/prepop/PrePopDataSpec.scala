/*
 * Copyright 2024 HM Revenue & Customs
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

package models.prepop

import models.DateModel
import models.common.business.Address
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, JsSuccess, Json}

class PrePopDataSpec extends PlaySpec with Matchers {

  "PrePopData" must {
    "successfully read from json" when {
      "all data is present" in {
        Json.fromJson[PrePopData](prePopDataJson) mustBe JsSuccess(prePopDataModel)
      }
      "all optional data is missing" in {
        Json.fromJson[PrePopData](Json.obj()) mustBe JsSuccess(PrePopData(None))
      }
    }
  }

  lazy val prePopDataJson: JsObject = Json.obj(
    "selfEmployment" -> Json.arr(
      Json.obj(
        "name" -> "test-name",
        "trade" -> "test-trade",
        "address" -> Json.obj(
          "lines" -> Json.arr(
            "test-line-one",
            "test-line-two"
          ),
          "postcode" -> "test-postcode"
        ),
        "startDate" -> Json.obj(
          "day" -> "1",
          "month" -> "1",
          "year" -> "2000"
        )
      )
    )
  )

  lazy val prePopDataModel: PrePopData = PrePopData(
    selfEmployment = Some(Seq(
      PrePopSelfEmployment(
        name = Some("test-name"),
        trade = Some("test-trade"),
        address = Some(Address(
          lines = Seq("test-line-one", "test-line-two"),
          postcode = Some("test-postcode")
        )),
        startDate = Some(DateModel("1", "1", "2000"))
      )
    ))
  )

}
