/*
 * Copyright 2022 HM Revenue & Customs
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

case object ForeignProperty extends BusinessIncomeSource {
  val FOREIGN_PROPERTY = "foreignProperty"

  override def toString: String = FOREIGN_PROPERTY
}

object BusinessIncomeSource {

  import models.common.ForeignProperty.FOREIGN_PROPERTY
  import models.common.SelfEmployed.SELF_EMPLOYED
  import models.common.UkProperty.UK_PROPERTY

  private val reads: Reads[BusinessIncomeSource] = (json: JsValue) => json.validate[String] map {
    case SELF_EMPLOYED => SelfEmployed
    case UK_PROPERTY => UkProperty
    case FOREIGN_PROPERTY => ForeignProperty
  }

  private val writes: Writes[BusinessIncomeSource] = {
    case SelfEmployed => JsString(SELF_EMPLOYED)
    case UkProperty => JsString(UK_PROPERTY)
    case ForeignProperty => JsString(FOREIGN_PROPERTY)
  }

  implicit val format: Format[BusinessIncomeSource] = Format[BusinessIncomeSource](reads, writes)
}
