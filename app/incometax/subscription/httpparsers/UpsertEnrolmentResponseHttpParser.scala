/*
 * Copyright 2019 HM Revenue & Customs
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

package incometax.subscription.httpparsers

import incometax.subscription.models.{KnownFactsFailure, KnownFactsSuccess}
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object UpsertEnrolmentResponseHttpParser {
  type UpsertEnrolmentResponse = Either[KnownFactsFailure, KnownFactsSuccess.type ]

  implicit object UpsertEnrolmentResponseHttpReads extends HttpReads[UpsertEnrolmentResponse] {
    override def read(method: String, url: String, response: HttpResponse): UpsertEnrolmentResponse =
      response.status match {
        case NO_CONTENT => Right(KnownFactsSuccess)
        case _ => Logger.error(s"[UpsertEnrolmentResponseHttpReads] issue upserting enrolment status: ${response.status} body: ${response.body}")
          Left(KnownFactsFailure(response.body))
      }
  }
}
