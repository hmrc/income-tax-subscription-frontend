/*
 * Copyright 2020 HM Revenue & Customs
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


sealed trait IncomeSourceType {
  val source: String
}

case object Business extends IncomeSourceType {
  override val source: String = IncomeSourceType.business
}

case object UkProperty extends IncomeSourceType {
  override val source: String = IncomeSourceType.ukProperty
}

case object ForeignProperty extends IncomeSourceType {
  override val source: String = IncomeSourceType.foreignProperty
}

case object Both extends IncomeSourceType {
  override val source: String = IncomeSourceType.both
}

case object Incomplete


object IncomeSourceType {

  import play.api.libs.json._

  val business = "Business"
  val ukProperty = "UkProperty"
  val foreignProperty = "ForeignProperty"
  val both = "Both"

  private val reader: Reads[IncomeSourceType] = __.read[String].map {
    case `business` => Business
    case `ukProperty` => UkProperty
    case `foreignProperty` => ForeignProperty
    case `both` => Both
  }

  private val writer: Writes[IncomeSourceType] = Writes[IncomeSourceType](incomeSourceType =>
    JsString(incomeSourceType match {
      case Business => business
      case UkProperty => ukProperty
      case ForeignProperty => foreignProperty
      case Both => both
    })
  )

  implicit val format: Format[IncomeSourceType] = Format(reader, writer)

  def apply(incomeSource: String): IncomeSourceType = incomeSource match {
    case `business` => Business
    case `ukProperty` => UkProperty
    case `foreignProperty` => ForeignProperty
    case `both` => Both
  }


  def unapply(incomeSourceType: IncomeSourceType): Option[String] = incomeSourceType match {
    case Business => Some(business)
    case UkProperty => Some(ukProperty)
    case ForeignProperty => Some(foreignProperty)
    case Both => Some(both)
  }

}
