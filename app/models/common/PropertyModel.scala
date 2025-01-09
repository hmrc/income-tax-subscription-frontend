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

import models.{AccountingMethod, DateModel}
import play.api.libs.json.{Json, OFormat}

case class PropertyModel(
                          startDateBeforeLimit: Option[Boolean] = None,
                          accountingMethod: Option[AccountingMethod] = None,
                          startDate: Option[DateModel] = None,
                          confirmed: Boolean = false
                        ) {

  val isComplete: Boolean = {
    startDateBeforeLimit match {
      case Some(true) => accountingMethod.isDefined
      case _ => accountingMethod.isDefined && startDate.isDefined
    }
  }

}

object PropertyModel {
  implicit val format: OFormat[PropertyModel] = Json.format[PropertyModel]
}
