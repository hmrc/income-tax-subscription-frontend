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

package connectors.httpparsers

import connectors.models.subscription._
import play.api.http.Status._
import uk.gov.hmrc.http.{ HttpReads, HttpResponse }

object SubscriptionResponseHttpParser {
  type SubscriptionResponse = Either[SubscriptionFailure, SubscriptionSuccess]

  implicit object SubscriptionResponseHttpReads extends HttpReads[SubscriptionResponse] {
    override def read(method: String, url: String, response: HttpResponse): SubscriptionResponse = {
      response.status match {
        case OK =>
          response.json.asOpt[SubscriptionSuccess] match {
            case Some(successResponse) => Right(successResponse)
            case _ => Left(BadlyFormattedSubscriptionResponse)
          }
        case status => Left(SubscriptionFailureResponse(status))
      }
    }
  }
}
