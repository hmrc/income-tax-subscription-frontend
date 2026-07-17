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
import play.api.libs.json.{Json, OFormat}


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
    private val space = " "
    private val hyphen = "-"

    def capitaliseAll: String = {
      text.split(space).toSeq.map { word =>
        word.split(hyphen).toSeq.map(_.capitalize).mkString(hyphen)
      }.mkString(space)
    }
  }

  implicit val format: OFormat[UserDetailsModel] = Json.format[UserDetailsModel]

  implicit class StringNinoUtil(string: String) {
    @inline def stripSpaces: String = string.toUpperCase().replace(" ", "")

    def toNinoDisplayFormat: String = string.stripSpaces.split("(?<=\\G.{2})").reduce(_ + " " + _)
  }

}
