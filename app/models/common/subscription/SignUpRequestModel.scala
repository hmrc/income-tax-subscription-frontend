/*
 * Copyright 2025 HM Revenue & Customs
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

import models.{AccountingYear, Current, Next}
import play.api.libs.json.{Json, OWrites}
import utilities.AccountingPeriodUtil

case class SignUpRequestModel(nino: String, utr: String, taxYear: AccountingYear)

object SignUpRequestModel {

  implicit val writes: OWrites[SignUpRequestModel] = OWrites[SignUpRequestModel] { signUpRequestModel =>

    val taxYear: String = signUpRequestModel.taxYear match {
      case Current => AccountingPeriodUtil.getCurrentTaxYear.toLongTaxYear
      case Next => AccountingPeriodUtil.getNextTaxYear.toLongTaxYear
    }

    Json.obj(
      "nino" -> signUpRequestModel.nino,
      "utr" -> signUpRequestModel.utr,
      "taxYear" -> taxYear
    )

  }
}