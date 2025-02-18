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

import models.common.business._
import models.{Accruals, Cash, DateModel}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import utilities.AccountingPeriodUtil

class PrePopSelfEmploymentSpec extends PlaySpec with Matchers {

  "PrePopSelfEmployment" must {
    "successfully read from json" when {
      "all data is present" in {
        Json.fromJson[PrePopSelfEmployment](prePopSelfEmploymentJsonFull) mustBe JsSuccess(prePopSelfEmploymentModelFull)
      }
      "all optional data is missing" in {
        Json.fromJson[PrePopSelfEmployment](prePopSelfEmploymentJsonMinimal) mustBe JsSuccess(prePopSelfEmploymentModelMinimal)
      }
    }
    "fail to read from json" when {
      "accountingMethod is missing from the json" in {
        Json.fromJson[PrePopSelfEmployment](prePopSelfEmploymentJsonFull - "accountingMethod") mustBe JsError(__ \ "accountingMethod", "error.path.missing")
      }
    }
  }

  "toSelfEmploymentData" should {
    "successfully translate into a self employment data model" when {
      "start date is older than the limit" in {
        prePopSelfEmploymentModelFull.copy(startDate = Some(DateModel.dateConvert(date.toLocalDate.minusDays(1))))
          .toSelfEmploymentData("test-id") mustBe SelfEmploymentData(
          id = "test-id",
          startDateBeforeLimit = Some(true),
          businessStartDate = None,
          businessName = Some(BusinessNameModel("ABC")),
          businessTradeName = Some(BusinessTradeNameModel("Plumbing")),
          businessAddress = Some(BusinessAddressModel(Address(
            lines = Seq(
              "1 long road"
            ),
            postcode = Some("ZZ1 1ZZ")
          ))))
      }
      "start date is not older than the limit" in {
        prePopSelfEmploymentModelFull
          .toSelfEmploymentData("test-id") mustBe SelfEmploymentData(
          id = "test-id",
          startDateBeforeLimit = Some(false),
          businessStartDate = Some(BusinessStartDate(date)),
          businessName = Some(BusinessNameModel("ABC")),
          businessTradeName = Some(BusinessTradeNameModel("Plumbing")),
          businessAddress = Some(BusinessAddressModel(Address(
            lines = Seq(
              "1 long road"
            ),
            postcode = Some("ZZ1 1ZZ")
          ))))
      }

    }
  }

  lazy val date: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getCurrentTaxYearStartLocalDate.minusYears(2))

  lazy val prePopSelfEmploymentJsonFull: JsObject = Json.obj(
    "name" -> "ABC",
    "trade" -> "Plumbing",
    "address" -> Json.obj(
      "lines" -> Json.arr(
        "1 long road"
      ),
      "postcode" -> "ZZ1 1ZZ"
    ),
    "startDate" -> Json.obj(
      "day" -> date.day,
      "month" -> date.month,
      "year" -> date.year
    ),
    "accountingMethod" -> "cash"
  )

  lazy val prePopSelfEmploymentModelFull: PrePopSelfEmployment = PrePopSelfEmployment(
    name = Some("ABC"),
    trade = Some("Plumbing"),
    address = Some(Address(
      lines = Seq(
        "1 long road"
      ),
      postcode = Some("ZZ1 1ZZ")
    )),
    startDate = Some(date),
    accountingMethod = Cash
  )

  lazy val prePopSelfEmploymentJsonMinimal: JsObject = Json.obj(
    "accountingMethod" -> "accruals"
  )

  lazy val prePopSelfEmploymentModelMinimal: PrePopSelfEmployment = PrePopSelfEmployment(
    name = None,
    trade = None,
    address = None,
    startDate = None,
    accountingMethod = Accruals
  )

}
