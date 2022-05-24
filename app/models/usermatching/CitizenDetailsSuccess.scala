/*
 * Copyright 2022 HM Revenue & Customs
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

import models.ConnectorError
import play.api.libs.json._

case class CitizenDetailsSuccess(utr: Option[String])

object CitizenDetailsSuccess {

  implicit val reader: Reads[CitizenDetailsSuccess] = (json: JsValue) => for {
    optUtr <- (json \ "ids" \ "sautr").validateOpt[String]
  } yield CitizenDetailsSuccess(optUtr)
}

case class CitizenDetailsFailureResponse(status: Int) extends ConnectorError



