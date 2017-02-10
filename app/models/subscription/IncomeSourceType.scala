/*
 * Copyright 2017 HM Revenue & Customs
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

package models.subscription

sealed trait IncomeSourceType

case object Business extends IncomeSourceType

case object Property extends IncomeSourceType

case object Both extends IncomeSourceType


object IncomeSourceType {

  import play.api.libs.json._

  val business = "Business"
  val property = "Property"
  val both = "Both"

  private val reader: Reads[IncomeSourceType] = __.read[String].map {
    case `business` => Business
    case `property` => Property
    case `both` => Both
  }

  private val writer: Writes[IncomeSourceType] = Writes[IncomeSourceType](incomeSourceType =>
    JsString(incomeSourceType match {
      case Business => business
      case Property => property
      case Both => both
    })
  )

  implicit val format: Format[IncomeSourceType] = Format(reader, writer)
}
