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

package incometax.subscription.models

sealed trait IncomeSourceType

case object Business extends IncomeSourceType

case object Property extends IncomeSourceType

case object Both extends IncomeSourceType

case object Other extends IncomeSourceType


object IncomeSourceType {

  import play.api.libs.json._

  val business = "Business"
  val property = "Property"
  val both = "Both"
  val other = "Other"

  private val reader: Reads[IncomeSourceType] = __.read[String].map {
    case `business` => Business
    case `property` => Property
    case `both` => Both
    case `other` => Other
  }

  private val writer: Writes[IncomeSourceType] = Writes[IncomeSourceType](incomeSourceType =>
    JsString(incomeSourceType match {
      case Business => business
      case Property => property
      case Both => both
      case Other => other
    })
  )

  implicit val format: Format[IncomeSourceType] = Format(reader, writer)

  def apply(incomeSource: String): IncomeSourceType = incomeSource match {
    case `business` => Business
    case `property` => Property
    case `both` => Both
    case `other` => Other
  }

  def unapply(incomeSourceType: IncomeSourceType): Option[String] = incomeSourceType match {
    case Business => Some(business)
    case Property => Some(property)
    case Both => Some(both)
    case Other => Some(other)
  }

}
