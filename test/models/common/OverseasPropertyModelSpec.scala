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

package models.common

import models.{Cash, DateModel}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class OverseasPropertyModelSpec extends PlaySpec {

  val dateModel: DateModel = DateModel("1", "2", "1980")

  val fullModel: OverseasPropertyModel = OverseasPropertyModel(
    startDateBeforeLimit = Some(false),
    accountingMethod = Some(Cash),
    startDate = Some(dateModel),
    confirmed = true
  )

  val minimalModel: OverseasPropertyModel = OverseasPropertyModel()

  val fullJson: JsObject = Json.obj(
    "startDateBeforeLimit" -> false,
    "accountingMethod" -> Cash.toString,
    "startDate" -> Json.obj(
      "day" -> "1",
      "month" -> "2",
      "year" -> "1980"
    ),
    "confirmed" -> true
  )

  val minimalJson: JsObject = Json.obj(
    "confirmed" -> false
  )

  "OverseasPropertyModel" should {
    "read from json successfully" when {
      "the json has all possible information" in {
        Json.fromJson[OverseasPropertyModel](fullJson) mustBe JsSuccess(fullModel)
      }
      "the json has minimal possible information" in {
        Json.fromJson[OverseasPropertyModel](minimalJson) mustBe JsSuccess(minimalModel)
      }
    }

    "fail to read from json" when {
      "confirmed is not present in the json" in {
        Json.fromJson[OverseasPropertyModel](Json.obj()) mustBe JsError(__ \ "confirmed", "error.path.missing")
      }
    }

    "write to json" when {
      "the model has all values present" in {
        Json.toJson(fullModel) mustBe fullJson
      }
      "the model has no values present" in {
        Json.toJson(minimalModel) mustBe minimalJson
      }
    }
  }

  "OverseasPropertyModel.isComplete" must {

    "when removeAccountingMethod is true" should {
      "return true" when {
        "startDateBeforeLimit is defined and true" in {
          OverseasPropertyModel(
            startDateBeforeLimit = Some(true)
          ).isComplete(removeAccountingMethod = true) mustBe true
        }
        "startDateBeforeLimit is defined and false and startDate defined" in {
          OverseasPropertyModel(
            startDateBeforeLimit = Some(false),
            startDate = Some(dateModel)
          ).isComplete(removeAccountingMethod = true) mustBe true
        }
      }

      "return false" when {
        "startDateBeforeLimit is defined and false and no startDate" in {
          OverseasPropertyModel(
            startDateBeforeLimit = Some(false)
          ).isComplete(removeAccountingMethod = true) mustBe false
        }
      }
    }

    "when removeAccountingMethod is false" should {
      "return true" when {
        "startDateBeforeLimit is defined and true and accounting method is defined" in {
          OverseasPropertyModel(startDateBeforeLimit = Some(true), accountingMethod = Some(Cash)).isComplete(removeAccountingMethod = false) mustBe true
        }
        "startDateBeforeLimit is defined and false, start date and accounting method are defined" in {
          OverseasPropertyModel(startDateBeforeLimit = Some(false), accountingMethod = Some(Cash), startDate = Some(dateModel)).isComplete(removeAccountingMethod = false) mustBe true
        }
        "startDateBeforeLimit is not defined, start date and accounting method are defined" in {
          OverseasPropertyModel(accountingMethod = Some(Cash), startDate = Some(dateModel)).isComplete(removeAccountingMethod = false) mustBe true
        }
      }

      "return false" when {
        "startDateBeforeLimit is defined and true and accounting method is not defined" in {
          OverseasPropertyModel(startDateBeforeLimit = Some(true)).isComplete(removeAccountingMethod = false) mustBe false
        }
        "startDateBeforeLimit is defined and false, start date is not defined" in {
          OverseasPropertyModel(startDateBeforeLimit = Some(false), accountingMethod = Some(Cash)).isComplete(removeAccountingMethod = false) mustBe false
        }
        "startDateBeforeLimit is defined and false, accounting method is not defined" in {
          OverseasPropertyModel(startDateBeforeLimit = Some(false), startDate = Some(dateModel)).isComplete(removeAccountingMethod = false) mustBe false
        }
        "startDateBeforeLimit is not defined, start date is not defined" in {
          OverseasPropertyModel(accountingMethod = Some(Cash)).isComplete(removeAccountingMethod = false) mustBe false
        }
        "startDateBeforeLimit is not defined, accounting method is not defined" in {
          OverseasPropertyModel(startDate = Some(dateModel)).isComplete(removeAccountingMethod = false) mustBe false
        }
      }
    }
  }
}
