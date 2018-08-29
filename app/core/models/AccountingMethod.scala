/*
 * Copyright 2018 HM Revenue & Customs
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

package core.models

import play.api.libs.json._

sealed trait AccountingMethod

case object Cash extends AccountingMethod {
  private[models] val CASH = "Cash"

  override def toString : String = CASH
}

case object Accruals extends AccountingMethod {
  private[models] val ACCRUALS = "Accruals"

  override def toString : String = ACCRUALS

}


object AccountingMethod {

  import Accruals.ACCRUALS
  import Cash.CASH

  private val reads: Reads[AccountingMethod] = new Reads[AccountingMethod] {
    override def reads(json: JsValue): JsResult[AccountingMethod] =
      json.validate[String] map {
        case CASH => Cash
        case ACCRUALS => Accruals
      }
  }

  private val writes: Writes[AccountingMethod] = new Writes[AccountingMethod] {
    override def writes(o: AccountingMethod): JsValue = o match {
      case Cash => JsString(CASH)
      case Accruals => JsString(ACCRUALS)
    }
  }

  implicit val format: Format[AccountingMethod] = Format[AccountingMethod](reads, writes)
}


