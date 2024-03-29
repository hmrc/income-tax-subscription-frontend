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

package connectors.httpparser

import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess, Reads}
import uk.gov.hmrc.http.{HttpReads, HttpResponse, InternalServerException}


object GetSubscriptionDetailsHttpParser {

  implicit def getSubscriptionDetailsHttpReads[T](implicit reads: Reads[T]): HttpReads[Option[T]] =
    (_: String, _: String, response: HttpResponse) => {
      response.status match {
        case OK => response.json.validate[T] match {
          case JsSuccess(value, _) => Some(value)
          case JsError(errors) =>
            throw new InternalServerException(s"Invalid Json for getSubscriptionDetailsHttpReads: $errors")
        }
        case NO_CONTENT => None
        case status => throw new InternalServerException(s"Unexpected status: $status")
      }
    }

}
