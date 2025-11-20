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

package models.common.business

import models.DateModel
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class SelfEmploymentDataSpec extends PlaySpec {

  val dateModel: DateModel = DateModel("1", "2", "1980")

  val fullModel: SelfEmploymentData = SelfEmploymentData(
    id = "id-full",
    startDateBeforeLimit = Some(false),
    businessStartDate = Some(BusinessStartDate(dateModel)),
    businessName = Some(BusinessNameModel("test name")),
    businessTradeName = Some(BusinessTradeNameModel("test trade")),
    businessAddress = Some(BusinessAddressModel(Address(
      lines = Seq("1 long road"),
      postcode = Some("ZZ1 1ZZ")
    ))),
    confirmed = true
  )

  val minimalModel: SelfEmploymentData = SelfEmploymentData(
    id = "id-minimal"
  )

  val fullJson: JsObject = Json.obj(
    "id" -> "id-full",
    "startDateBeforeLimit" -> false,
    "businessStartDate" -> Json.obj(
      "startDate" -> Json.obj(
        "day" -> "1",
        "month" -> "2",
        "year" -> "1980"
      )
    ),
    "businessName" -> Json.obj(
      "businessName" -> "test name"
    ),
    "businessTradeName" -> Json.obj(
      "businessTradeName" -> "test trade"
    ),
    "businessAddress" -> Json.obj(
      "address" -> Json.obj(
        "lines" -> Json.arr(
          "1 long road"
        ),
        "postcode" -> "ZZ1 1ZZ"
      )
    ),
    "confirmed" -> true
  )

  val minimalJson: JsObject = Json.obj(
    "id" -> "id-minimal",
    "confirmed" -> false
  )

  "SelfEmploymentData" should {
    "read from json successfully" when {
      "the json has all possible information" in {
        Json.fromJson[SelfEmploymentData](fullJson) mustBe JsSuccess(fullModel)
      }
      "the json has minimal possible information" in {
        Json.fromJson[SelfEmploymentData](minimalJson) mustBe JsSuccess(minimalModel)
      }
    }

    "fail to read from json" when {
      "id is not present in the json" in {
        Json.fromJson[SelfEmploymentData](minimalJson - "id") mustBe JsError(__ \ "id", "error.path.missing")
      }
      "confirmed is not present in the json" in {
        Json.fromJson[SelfEmploymentData](minimalJson - "confirmed") mustBe JsSuccess(minimalModel)
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

  "SelfEmploymentData.isComplete" when {
    "startDateBeforeLimit is true" should {
      "return true" when {
        "everything is defined except start date" in {
          fullModel.copy(startDateBeforeLimit = Some(true), businessStartDate = None).isComplete mustBe true
        }
      }
      "return false" when {
        "business name is missing" in {
          fullModel.copy(startDateBeforeLimit = Some(true), businessStartDate = None, businessName = None).isComplete mustBe false
        }
        "business trade is missing" in {
          fullModel.copy(startDateBeforeLimit = Some(true), businessStartDate = None, businessTradeName = None).isComplete mustBe false
        }
        "business address is missing" in {
          fullModel.copy(startDateBeforeLimit = Some(true), businessStartDate = None, businessAddress = None).isComplete mustBe false
        }
      }
    }
    "startDateBeforeLimit is false" should {
      "return true" when {
        "everything is defined" in {
          fullModel.isComplete mustBe true
        }
      }
      "return false" when {
        "start date is missing" in {
          fullModel.copy(businessStartDate = None).isComplete mustBe false
        }
        "business name is missing" in {
          fullModel.copy(businessName = None).isComplete mustBe false
        }
        "business trade is missing" in {
          fullModel.copy(businessTradeName = None).isComplete mustBe false
        }
        "business address is missing" in {
          fullModel.copy(businessAddress = None).isComplete mustBe false
        }
      }
    }
    "startDateBeforeLimit is not defined" should {
      "return true" when {
        "everything is defined" in {
          fullModel.copy(startDateBeforeLimit = None).isComplete mustBe true
        }
      }
      "return false" when {
        "start date is missing" in {
          fullModel.copy(startDateBeforeLimit = None, businessStartDate = None).isComplete mustBe false
        }
        "business name is missing" in {
          fullModel.copy(startDateBeforeLimit = None, businessName = None).isComplete mustBe false
        }
        "business trade is missing" in {
          fullModel.copy(startDateBeforeLimit = None, businessTradeName = None).isComplete mustBe false
        }
        "business address is missing" in {
          fullModel.copy(startDateBeforeLimit = None, businessAddress = None).isComplete mustBe false
        }
      }
    }
  }

}
