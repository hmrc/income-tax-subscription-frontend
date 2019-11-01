/*
 * Copyright 2019 HM Revenue & Customs
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

import core.models.Current.CURRENT
import core.models.Next.NEXT
import play.api.libs.json._

sealed trait AccountingYear

case object Current extends AccountingYear {
  private[models] val CURRENT = "CurrentYear"

  override def toString: String = CURRENT
}

case object Next extends AccountingYear {
  private[models] val NEXT = "NextYear"

  override def toString: String = NEXT
}

object AccountingYear {
  private val reads: Reads[AccountingYear] = new Reads[AccountingYear] {
    override def reads(json: JsValue): JsResult[AccountingYear] =
      json.validate[String] map {
        case CURRENT => Current
        case NEXT => Next
      }
  }

  private val writes: Writes[AccountingYear] = new Writes[AccountingYear] {
    override def writes(o: AccountingYear): JsValue = o match {
      case Current => JsString(CURRENT)
      case Next => JsString(NEXT)
    }
  }

  implicit val format: Format[AccountingYear] = Format[AccountingYear](reads, writes)
}