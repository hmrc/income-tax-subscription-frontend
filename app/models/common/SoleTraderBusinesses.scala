/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{Format, OFormat, __}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString

case class SoleTraderBusinesses(businesses: Seq[SoleTraderBusiness]) {
  def getBusinessesWithDuplicateNames: Seq[SoleTraderBusiness] = businesses.filter { business =>
    business.name.exists { name =>
      businesses.exists(other => other.id != business.id && other.name.contains(name))
    }
  }

  def addDisplayNames(displayNames: Seq[String]): SoleTraderBusinesses = {
    val businessesWithDuplicateNames = getBusinessesWithDuplicateNames
    require(displayNames.size == businessesWithDuplicateNames.size, "Must have a display name for each business with a duplicate name")

    val displayNamesByBusinessId = businessesWithDuplicateNames.map(_.id).zip(displayNames).toMap
    copy(businesses = businesses.map { business =>
      displayNamesByBusinessId.get(business.id) match {
        case Some(displayName) => business.copy(displayName = Some(displayName))
        case None => business
      }
    })
  }
}

object SoleTraderBusinesses {

  def unapply(soleTraderBusinesses: SoleTraderBusinesses): Option[Seq[SoleTraderBusiness]] = Some(soleTraderBusinesses.businesses)

  def encryptedFormat(implicit sensitiveFormat: Format[SensitiveString]): OFormat[SoleTraderBusinesses] = {
    implicit val soleTraderBusinessFormat: OFormat[SoleTraderBusiness] = SoleTraderBusiness.encryptedFormat
    (__ \ "businesses").format[Seq[SoleTraderBusiness]].bimap(SoleTraderBusinesses.apply, unlift(SoleTraderBusinesses.unapply))
  }

}
