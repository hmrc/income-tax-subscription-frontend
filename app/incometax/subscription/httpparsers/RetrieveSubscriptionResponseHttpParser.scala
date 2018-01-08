/*
 * Copyright 2018 HM Revenue & Customs
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

import incometax.subscription.models._
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object RetrieveSubscriptionResponseHttpParser {
  type RetrieveSubscriptionResponse = Either[RetrieveSubscriptionFailure, Option[StoredSubscription]]

  implicit object RetrieveSubscriptionResponseHttpReads extends HttpReads[RetrieveSubscriptionResponse] {
    override def read(method: String, url: String, response: HttpResponse): RetrieveSubscriptionResponse = {
      response.status match {
        case OK =>
          response.json.validate[StoredSubscription] match {
            case JsSuccess(storedSubscription, _) => Right(Some(storedSubscription))
            case JsError(errors) => Left(RetrieveSubscriptionFailure(errors.toString))
          }
        case NOT_FOUND => Right(None)
        case status => Left(RetrieveSubscriptionFailure(response.body))
      }
    }
  }
}
