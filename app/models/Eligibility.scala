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

package models

import play.api.libs.json._
import scala.util.Try

case class PrePopSelfEmployment(
                                 businessName: Option[String],
                                 businessTradeName: String,
                                 businessAddressFirstLine: Option[String],
                                 businessAddressPostCode: Option[String],
                                 businessStartDate: Option[DateModel],
                                 businessAccountingMethod: Option[AccountingMethod]
                               )

object PrePopSelfEmployment {
  implicit val format: OFormat[PrePopSelfEmployment] = Json.format[PrePopSelfEmployment]
}

case class PrePopUkProperty(
                             ukPropertyStartDate: Option[DateModel],
                             ukPropertyAccountingMethod: Option[AccountingMethod]
                           )

object PrePopUkProperty {
  implicit val format: OFormat[PrePopUkProperty] = Json.format[PrePopUkProperty]
}

case class PrePopOverseasProperty(
                                   overseasPropertyStartDate: Option[DateModel],
                                   overseasPropertyAccountingMethod: Option[AccountingMethod]
                                 )

object PrePopOverseasProperty {
  implicit val format: OFormat[PrePopOverseasProperty] = Json.format[PrePopOverseasProperty]
}

case class PrePopData(
                       selfEmployments: Option[List[PrePopSelfEmployment]],
                       ukProperty: Option[PrePopUkProperty],
                       overseasProperty: Option[PrePopOverseasProperty]
                     )

object PrePopData {
  implicit val format: OFormat[PrePopData] = Json.format[PrePopData]
}


case class EligibilityStatus(eligibleCurrentYear: Boolean, eligibleNextYear: Boolean, prepopData: Option[PrePopData])

object EligibilityStatus {
  implicit val format: OFormat[EligibilityStatus] = Json.format[EligibilityStatus]

  import play.api.libs.json.MapWrites.mapWrites
  import play.api.libs.json.Reads.mapReads

  implicit val eligibilityStatusYearMapReads: Reads[Map[String, Boolean]] =
    mapReads[String, Boolean](s => JsResult.fromTry(Try(s)))

  implicit val eligibilityStatusYearMapWrites: Writes[Map[String, Boolean]] =
    mapWrites[String].contramap(_.map { case (k, v) => k -> v.toString })
}
