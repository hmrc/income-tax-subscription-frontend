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

import play.api.libs.json._

sealed trait BusinessIncomeSource

case object SelfEmployed extends BusinessIncomeSource {
  val SELF_EMPLOYED = "selfEmployed"

  override def toString: String = SELF_EMPLOYED
}

case object UkProperty extends BusinessIncomeSource {
  val UK_PROPERTY = "ukProperty"

  override def toString: String = UK_PROPERTY
}

case object OverseasProperty extends BusinessIncomeSource {
  val OVERSEAS_PROPERTY = "overseasProperty"

  override def toString: String = OVERSEAS_PROPERTY
}

object BusinessIncomeSource {

  import models.common.OverseasProperty.OVERSEAS_PROPERTY
  import models.common.SelfEmployed.SELF_EMPLOYED
  import models.common.UkProperty.UK_PROPERTY

  private val reads: Reads[BusinessIncomeSource] = Reads[BusinessIncomeSource] {
    case JsString(SELF_EMPLOYED) => JsSuccess(SelfEmployed)
    case JsString(UK_PROPERTY) => JsSuccess(UkProperty)
    case JsString(OVERSEAS_PROPERTY) => JsSuccess(OverseasProperty)
    case _ => JsError("error.income-source.invalid")
  }

  private val writes: Writes[BusinessIncomeSource] = {
    case SelfEmployed => JsString(SELF_EMPLOYED)
    case UkProperty => JsString(UK_PROPERTY)
    case OverseasProperty => JsString(OVERSEAS_PROPERTY)
  }

  implicit val format: Format[BusinessIncomeSource] = Format[BusinessIncomeSource](reads, writes)
}
