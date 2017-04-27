/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors.models

import play.api.libs.json.Json

case class Enrolment(key: String, identifiers: Seq[Identifier], state: String) {
  def isEnrolled: Enrolment.Enrolled = state.equals("Activated")
}

object Enrolment {
  implicit val formats = Json.format[Enrolment]

  type Enrolled = Boolean
  val Enrolled: Enrolled = true
  val NotEnrolled: Enrolled = false

  implicit class OEnrolmentUtil(enrolment: Option[Enrolment]) {
    def isEnrolled: Enrolled = enrolment.fold(false)(_.isEnrolled)
  }

  implicit class OSeqEnrolmentUtil(enrolments: Option[Seq[Enrolment]]) {
    def isEnrolled(enrolment: String): Enrolled = enrolments.fold(false)(_.find(_.key == enrolment).isEnrolled)
  }

}