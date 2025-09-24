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

import models.DateModel
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class PropertyModelSpec extends PlaySpec with BeforeAndAfterEach {

  val dateModel: DateModel = DateModel("1", "2", "1980")

  val fullModel: PropertyModel = PropertyModel(
    startDateBeforeLimit = Some(false),
    startDate = Some(dateModel),
    confirmed = true
  )

  val minimalModel: PropertyModel = PropertyModel()

  val fullJson: JsObject = Json.obj(
    "startDateBeforeLimit" -> false,
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

  "PropertyModel" should {
    "read from json successfully" when {
      "the json has all possible information" in {
        Json.fromJson[PropertyModel](fullJson) mustBe JsSuccess(fullModel)
      }
      "the json has minimal possible information" in {
        Json.fromJson[PropertyModel](minimalJson) mustBe JsSuccess(minimalModel)
      }
    }

    "fail to read from json" when {
      "confirmed is not present in the json" in {
        Json.fromJson[PropertyModel](Json.obj()) mustBe JsError(__ \ "confirmed", "error.path.missing")
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

  "PropertyModel.isComplete" when {
    "return true" when {
      "startDateBeforeLimit is defined and true" in {
        PropertyModel(startDateBeforeLimit = Some(true)).isComplete mustBe true
      }
      "startDateBeforeLimit is defined and false and start date is defined" in {
        PropertyModel(
          startDateBeforeLimit = Some(false),
          startDate = Some(dateModel)
        ).isComplete mustBe true
      }
      "start date is defined" in {
        PropertyModel(
          startDateBeforeLimit = None,
          startDate = Some(dateModel)
        ).isComplete mustBe true
      }
    }
    "return false" when {
      "startDateBeforeLimit is defined and false and start date is not defined" in {
        PropertyModel(startDateBeforeLimit = Some(false)).isComplete mustBe false
      }
      "startDateBeforeLimit is not defined and start date is not defined" in {
        PropertyModel().isComplete mustBe false
      }
    }
  }

}
