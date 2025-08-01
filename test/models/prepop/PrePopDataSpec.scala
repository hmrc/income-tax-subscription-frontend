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

import models.{Accruals, Cash}
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
        Json.fromJson[PrePopData](Json.obj()) mustBe JsSuccess(PrePopData(None, None, None))
      }
    }
  }

  lazy val prePopDataJson: JsObject = Json.obj(
    "selfEmployment" -> Json.arr(
      Json.obj(
        "accountingMethod" -> "cash"
      )
    ),
    "ukPropertyAccountingMethod" -> "accruals",
    "foreignPropertyAccountingMethod" -> "cash"
  )

  lazy val prePopDataModel: PrePopData = PrePopData(
    selfEmployment = Some(Seq(
      PrePopSelfEmployment(
        name = None,
        trade = None,
        address = None,
        startDate = None,
        accountingMethod = Some(Cash)
      )
    )),
    ukPropertyAccountingMethod = Some(Accruals),
    foreignPropertyAccountingMethod = Some(Cash)
  )

}
