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

package testonly.models

//$COVERAGE-OFF$Disabling scoverage on this model as it is only intended to be used by the test only controller
import models.DateModel
import play.api.libs.json.{Json, OFormat}

case class UserToStubModel(firstName: String, lastName: String, nino: String, sautr: Option[String], dateOfBirth: DateModel) {
  def ninoFormatted: String = nino.toUpperCase().replace(" ", "")
}

object UserToStubModel {
  implicit val format: OFormat[UserToStubModel] = Json.format[UserToStubModel]
}

// $COVERAGE-ON$
