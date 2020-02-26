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

import play.api.libs.json.{JsValue, Json, Writes}

case class EnrolmentVerifiers(verifiers: (String, String)*)

object EnrolmentVerifiers {
  implicit val writer: Writes[EnrolmentVerifiers] = new Writes[EnrolmentVerifiers] {
    override def writes(verifiers: EnrolmentVerifiers): JsValue =
      Json.obj("verifiers" ->
        (verifiers.verifiers map {
          case (key, value) =>
            Json.obj(
              "key" -> key,
              "value" -> value
            )
        })
      )
  }
}


