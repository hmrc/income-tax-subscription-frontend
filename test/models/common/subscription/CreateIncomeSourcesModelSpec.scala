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

package models.common.subscription

import models.DateModel
import models.common.AccountingPeriodModel
import models.common.business._
import org.scalatest.matchers.must.{Matchers => MustMatchers}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import utilities.individual.TestConstants.testNino

import java.time.LocalDate
import scala.language.implicitConversions

class CreateIncomeSourcesModelSpec extends PlaySpec with MustMatchers {

  val now: LocalDate = LocalDate.now

  implicit def dateConvert(date: LocalDate): DateModel = DateModel(date.getDayOfMonth.toString, date.getMonthValue.toString, date.getYear.toString)

  val fullSoleTraderBusinesses: SoleTraderBusinesses = SoleTraderBusinesses(
    accountingPeriod = AccountingPeriodModel(now, now),
    businesses = Seq(
      SelfEmploymentData(
        id = "testBusinessId",
        businessStartDate = Some(BusinessStartDate(now)),
        businessName = Some(BusinessNameModel("testBusinessName")),
        businessTradeName = Some(BusinessTradeNameModel("testBusinessTrade")),
        businessAddress = Some(BusinessAddressModel(
          address = Address(lines = Seq("line 1", "line 2"), postcode = Some("testPostcode"))
        )),
        confirmed = true
      )
    )
  )

  val fullUkProperty: UkProperty = UkProperty(
    accountingPeriod = AccountingPeriodModel(now, now),
    tradingStartDate = LocalDate.now
  )

  val fullOverseasProperty: OverseasProperty = OverseasProperty(
    accountingPeriod = AccountingPeriodModel(now, now),
    tradingStartDate = LocalDate.now
  )

  val fullCreateIncomeSourcesModel: CreateIncomeSourcesModel = CreateIncomeSourcesModel(
    nino = testNino,
    soleTraderBusinesses = Some(fullSoleTraderBusinesses),
    ukProperty = Some(fullUkProperty),
    overseasProperty = Some(fullOverseasProperty)
  )

  val fullSoleTraderBusinessesJson: JsObject = Json.obj(
    "accountingPeriod" -> Json.obj(
      "startDate" -> Json.obj(
        "day" -> now.getDayOfMonth.toString,
        "month" -> now.getMonthValue.toString,
        "year" -> now.getYear.toString
      ),
      "endDate" -> Json.obj(
        "day" -> now.getDayOfMonth.toString,
        "month" -> now.getMonthValue.toString,
        "year" -> now.getYear.toString
      )
    ),
    "businesses" -> Json.arr(
      Json.obj(
        "id" -> "testBusinessId",
        "businessStartDate" -> Json.obj(
          "startDate" -> Json.obj(
            "day" -> now.getDayOfMonth.toString,
            "month" -> now.getMonthValue.toString,
            "year" -> now.getYear.toString
          )
        ),
        "businessName" -> Json.obj(
          "businessName" -> "testBusinessName"
        ),
        "businessTradeName" -> Json.obj(
          "businessTradeName" -> "testBusinessTrade"
        ),
        "businessAddress" -> Json.obj(
          "address" -> Json.obj(
            "lines" -> Json.arr(
              "line 1",
              "line 2"
            ),
            "postcode" -> "testPostcode"
          )
        ),
        "confirmed" -> true
      )
    )
  )

  val fullUkPropertyJson: JsObject = Json.obj(
    "accountingPeriod" -> Json.obj(
      "startDate" -> Json.obj(
        "day" -> now.getDayOfMonth.toString,
        "month" -> now.getMonthValue.toString,
        "year" -> now.getYear.toString
      ),
      "endDate" -> Json.obj(
        "day" -> now.getDayOfMonth.toString,
        "month" -> now.getMonthValue.toString,
        "year" -> now.getYear.toString
      )
    ),
    "tradingStartDate" -> Json.obj(
      "day" -> now.getDayOfMonth.toString,
      "month" -> now.getMonthValue.toString,
      "year" -> now.getYear.toString
    )
  )

  val fullOverseasPropertyJson: JsObject = Json.obj(
    "accountingPeriod" -> Json.obj(
      "startDate" -> Json.obj(
        "day" -> now.getDayOfMonth.toString,
        "month" -> now.getMonthValue.toString,
        "year" -> now.getYear.toString
      ),
      "endDate" -> Json.obj(
        "day" -> now.getDayOfMonth.toString,
        "month" -> now.getMonthValue.toString,
        "year" -> now.getYear.toString
      )
    ),
    "tradingStartDate" -> Json.obj(
      "day" -> now.getDayOfMonth.toString,
      "month" -> now.getMonthValue.toString,
      "year" -> now.getYear.toString
    )
  )

  val fullCreateIncomeSourcesModelJson: JsObject = Json.obj(
    "nino" -> testNino,
    "soleTraderBusinesses" -> fullSoleTraderBusinessesJson,
    "ukProperty" -> fullUkPropertyJson,
    "overseasProperty" -> fullOverseasPropertyJson
  )

  "CreateIncomeSourcesModel" must {
    "throw an IllegalArgumentException" when {
      "no businesses are provided to the model" in {
        intercept[IllegalArgumentException](CreateIncomeSourcesModel(testNino))
      }
    }

    "read from json successfully" when {
      "the json is complete and valid" in {
        Json.fromJson[CreateIncomeSourcesModel](fullCreateIncomeSourcesModelJson) mustBe JsSuccess(fullCreateIncomeSourcesModel)
      }
      "there is only sole trader businesses" in {
        val readModel = Json.fromJson[CreateIncomeSourcesModel](
          fullCreateIncomeSourcesModelJson - "ukProperty" - "overseasProperty"
        )
        val expectedModel = fullCreateIncomeSourcesModel.copy(ukProperty = None, overseasProperty = None)

        readModel mustBe JsSuccess(expectedModel)
      }
      "there is only a uk property business" in {
        val readModel = Json.fromJson[CreateIncomeSourcesModel](
          fullCreateIncomeSourcesModelJson - "soleTraderBusinesses" - "overseasProperty"
        )
        val expectedModel = fullCreateIncomeSourcesModel.copy(soleTraderBusinesses = None, overseasProperty = None)

        readModel mustBe JsSuccess(expectedModel)
      }
      "there is only an overseas property business" in {
        val readModel = Json.fromJson[CreateIncomeSourcesModel](
          fullCreateIncomeSourcesModelJson - "soleTraderBusinesses" - "ukProperty"
        )
        val expectedModel = fullCreateIncomeSourcesModel.copy(soleTraderBusinesses = None, ukProperty = None)

        readModel mustBe JsSuccess(expectedModel)
      }
    }
    "return a read error" when {
      "nino is missing from the json" in {
        Json.fromJson[CreateIncomeSourcesModel](fullCreateIncomeSourcesModelJson - "nino") mustBe JsError(JsPath \ "nino", "error.path.missing")
      }
    }

    "write to json successfully" when {
      "all fields are present in the model" in {
        Json.toJson(fullCreateIncomeSourcesModel) mustBe fullCreateIncomeSourcesModelJson
      }
      "only sole trader businesses are added" in {
        Json.toJson(fullCreateIncomeSourcesModel.copy(ukProperty = None, overseasProperty = None)) mustBe
          fullCreateIncomeSourcesModelJson - "ukProperty" - "overseasProperty"
      }
      "only uk property business is added" in {
        Json.toJson(fullCreateIncomeSourcesModel.copy(soleTraderBusinesses = None, overseasProperty = None)) mustBe
          fullCreateIncomeSourcesModelJson - "soleTraderBusinesses" - "overseasProperty"
      }
      "only overseas property business is added" in {
        Json.toJson(fullCreateIncomeSourcesModel.copy(soleTraderBusinesses = None, ukProperty = None)) mustBe
          fullCreateIncomeSourcesModelJson - "soleTraderBusinesses" - "ukProperty"
      }
    }

  }

  "SoleTraderBusinesses" must {
    "read from json successfully" when {
      "the json is complete and valid" in {
        Json.fromJson[SoleTraderBusinesses](fullSoleTraderBusinessesJson) mustBe JsSuccess(fullSoleTraderBusinesses)
      }
      "the json is missing businesses" in {
        val readModel = Json.fromJson[SoleTraderBusinesses](fullSoleTraderBusinessesJson ++ Json.obj("businesses" -> Json.arr()))
        val expectedModel = fullSoleTraderBusinesses.copy(businesses = Nil)

        readModel mustBe JsSuccess(expectedModel)
      }
    }
    "return a read error" when {
      "accountingPeriod is missing from the json" in {
        Json.fromJson[SoleTraderBusinesses](fullSoleTraderBusinessesJson - "accountingPeriod") mustBe
          JsError(JsPath \ "accountingPeriod", "error.path.missing")
      }
      "businesses is missing from the json" in {
        Json.fromJson[SoleTraderBusinesses](fullSoleTraderBusinessesJson - "businesses") mustBe
          JsError(JsPath \ "businesses", "error.path.missing")
      }
    }
  }

  "UkProperty" must {
    "read the json successfully" when {
      "the json is complete and valid" in {
        Json.fromJson[UkProperty](fullUkPropertyJson) mustBe JsSuccess(fullUkProperty)
      }
    }
    "return a read error" when {
      "accountingPeriod is missing from the json" in {
        Json.fromJson[UkProperty](fullUkPropertyJson - "accountingPeriod") mustBe JsError(JsPath \ "accountingPeriod", "error.path.missing")
      }
      "tradingStartDate is missing from the json" in {
        Json.fromJson[UkProperty](fullUkPropertyJson - "tradingStartDate") mustBe JsError(JsPath \ "tradingStartDate", "error.path.missing")
      }
    }
  }

  "OverseasProperty" must {
    "read the json successfully" when {
      "the json is complete and valid" in {
        Json.fromJson[OverseasProperty](fullOverseasPropertyJson) mustBe JsSuccess(fullOverseasProperty)
      }
    }
    "return a read error" when {
      "accountingPeriod is missing from the json" in {
        Json.fromJson[OverseasProperty](fullOverseasPropertyJson - "accountingPeriod") mustBe JsError(JsPath \ "accountingPeriod", "error.path.missing")
      }
      "tradingStartDate is missing from the json" in {
        Json.fromJson[OverseasProperty](fullOverseasPropertyJson - "tradingStartDate") mustBe JsError(JsPath \ "tradingStartDate", "error.path.missing")
      }
    }
  }

}
