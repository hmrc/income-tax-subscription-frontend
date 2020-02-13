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

import models.individual.incomesource.{AreYouSelfEmployedModel, RentUkPropertyModel}
import models.{No, Yes}

sealed trait IncomeSourceType {
  val source: String
}

case object Business extends IncomeSourceType {
  override val source = IncomeSourceType.business
}

case object Property extends IncomeSourceType {
  override val source = IncomeSourceType.property
}

case object Both extends IncomeSourceType {
  override val source = IncomeSourceType.both
}

case object Incomplete


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

  def apply(incomeSource: String): IncomeSourceType = incomeSource match {
    case `business` => Business
    case `property` => Property
    case `both` => Both
  }

  def apply(rentUkPropertyModel: RentUkPropertyModel, areYouSelfEmployedModel: Option[AreYouSelfEmployedModel]): Option[IncomeSourceType] =
    from(rentUkPropertyModel, areYouSelfEmployedModel)

  def from(rentUkPropertyModel: RentUkPropertyModel, areYouSelfEmployedModel: Option[AreYouSelfEmployedModel]): Option[IncomeSourceType] =
    (rentUkPropertyModel, areYouSelfEmployedModel) match {
      case (RentUkPropertyModel(Yes, Some(Yes)), _) => Some(Property)
      case (RentUkPropertyModel(Yes, Some(No)), Some(AreYouSelfEmployedModel(Yes))) => Some(Both)
      case (RentUkPropertyModel(Yes, Some(No)), Some(AreYouSelfEmployedModel(No))) => Some(Property)
      case (RentUkPropertyModel(No, _), Some(AreYouSelfEmployedModel(Yes))) => Some(Business)
      case _ => None
    }

  def unapply(incomeSourceType: IncomeSourceType): Option[String] = incomeSourceType match {
    case Business => Some(business)
    case Property => Some(property)
    case Both => Some(both)

  }

}
