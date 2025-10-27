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

import models.common.subscription.SignUpRequestModel
import models.{AccountingYear, Current, Next}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}
import utilities.AccountingPeriodUtil

class SignUpModelSpec extends PlaySpec {

  def model(taxYear: AccountingYear): SignUpRequestModel = SignUpRequestModel(
    nino = "test-nino",
    utr = "test-utr",
    taxYear = taxYear
  )

  def json(taxYear: String): JsObject = Json.obj(
    "nino" -> "test-nino",
    "utr" -> "test-utr",
    "taxYear" -> taxYear
  )

  "SignUpModel" must {
    "write to json as expected" when {
      "tax year is the current year" in {
        Json.toJson(model(Current)) mustBe json(AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear)
      }
      "tax year is the next tax year" in {
        Json.toJson(model(Next)) mustBe json(AccountingPeriodUtil.getNextTaxYear.toLongTaxYear)
      }

    }
  }

}

