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

package connectors.agent.httpparsers

import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object UpsertEnrolmentResponseHttpParser {
  type UpsertEnrolmentResponse = Either[UpsertEnrolmentFailure, UpsertEnrolmentSuccess.type]

  implicit object UpsertEnrolmentResponseHttpReads extends HttpReads[UpsertEnrolmentResponse] {
    override def read(method: String, url: String, response: HttpResponse): UpsertEnrolmentResponse =
      response.status match {
        case NO_CONTENT => Right(UpsertEnrolmentSuccess)
        case status => Left(UpsertEnrolmentFailure(status, response.body))
      }
  }

  case object UpsertEnrolmentSuccess

  case class UpsertEnrolmentFailure(status: Int, message: String)

}
