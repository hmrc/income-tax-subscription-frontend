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
import play.api.libs.json.{OFormat, __}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

case class SoleTraderBusinesses(businesses: Seq[SoleTraderBusiness])

object SoleTraderBusinesses {

  def encryptedFormat(implicit crypto: Encrypter with Decrypter): OFormat[SoleTraderBusinesses] = {
    implicit val soleTraderBusinessFormat: OFormat[SoleTraderBusiness] = SoleTraderBusiness.encryptedFormat
    (__ \ "businesses").format[Seq[SoleTraderBusiness]].bimap(SoleTraderBusinesses.apply, unlift(SoleTraderBusinesses.unapply))
  }

}
