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

package models.usermatching

import models.DateModel
import models.usermatching.UserDetailsModel.StringNinoUtil
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import uk.gov.hmrc.crypto.Sensitive.SensitiveString

case class UserDetailsModel(firstName: String, lastName: String, nino: String, dateOfBirth: DateModel) {

  def ninoInBackendFormat: String = nino.toUpperCase.replace(" ", "")

  def ninoInDisplayFormat: String = nino.toNinoDisplayFormat

}

object UserDetailsModel {
  def apply(firstName: String, lastName: String, nino: String, dateOfBirth: DateModel): UserDetailsModel =
    new UserDetailsModel(
      firstName = firstName.capitaliseAll,
      lastName = lastName.capitaliseAll,
      nino = nino,
      dateOfBirth = dateOfBirth
    )

  implicit class StringCapitalise(text: String) {
    private val pattern = raw"\b\p{L}".r

    def capitaliseAll: String =
      pattern.replaceAllIn(text, _.matched.toUpperCase)
  }

  implicit class StringNinoUtil(string: String) {
    @inline def stripSpaces: String = string.toUpperCase().replace(" ", "")

    def toNinoDisplayFormat: String = string.stripSpaces.split("(?<=\\G.{2})").reduce(_ + " " + _)
  }

  def encryptedFormat(implicit sensitiveFormat: Format[SensitiveString]): OFormat[UserDetailsModel] = {
    val reads: Reads[UserDetailsModel] = (
      (__ \ "firstName").read[SensitiveString] and
        (__ \ "lastName").read[SensitiveString] and
        (__ \ "nino").read[SensitiveString] and
        (__ \ "dateOfBirth").read[DateModel](DateModel.encryptedFormat)
    )(
      (firstName, lastName, nino, dateOfBirth) =>
        UserDetailsModel.apply(firstName.decryptedValue, lastName.decryptedValue,
          nino.decryptedValue, dateOfBirth)
    )

    val writes: OWrites[UserDetailsModel] = (
      (__ \ "firstName").write[SensitiveString] and
        (__ \ "lastName").write[SensitiveString] and
        (__ \ "nino").write[SensitiveString] and
        (__ \ "dateOfBirth").write[DateModel](DateModel.encryptedFormat)
    )(
      userDetailsModel =>
        (
          SensitiveString(userDetailsModel.firstName),
          SensitiveString(userDetailsModel.lastName),
          SensitiveString(userDetailsModel.nino),
          userDetailsModel.dateOfBirth
        )
    )
  
    OFormat(reads, writes)
  }

}
