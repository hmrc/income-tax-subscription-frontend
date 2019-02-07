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

package incometax.incomesource.models

import core.models.{No, Yes, YesNo}
import play.api.libs.json.{Json, OFormat}

case class RentUkPropertyModel(rentUkProperty: YesNo, onlySourceOfSelfEmployedIncome: Option[YesNo]) {

  def needSecondPage: Boolean = this match {
    case RentUkPropertyModel(Yes, Some(Yes)) => false
    case RentUkPropertyModel(Yes, Some(No)) => true
    case RentUkPropertyModel(No, _) => true
  }

}

object RentUkPropertyModel {
  implicit val format: OFormat[RentUkPropertyModel] = Json.format[RentUkPropertyModel]

  implicit class RentUkPropertyModelUtil(rentUkPropertyModel: Option[RentUkPropertyModel]) {
    def needSecondPage: Boolean = rentUkPropertyModel.fold(false)(_.needSecondPage)
  }

}

