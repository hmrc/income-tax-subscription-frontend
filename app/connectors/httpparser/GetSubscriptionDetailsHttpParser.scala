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

package connectors.httpparser

import play.api.http.Status._
import play.api.libs.json.{JsSuccess, Reads}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}


object GetSubscriptionDetailsHttpParser {

  implicit def getSubscriptionDetailsHttpReads[T](implicit reads: Reads[T]): HttpReads[Option[T]] =
    new HttpReads[Option[T]] {
      override def read(method: String, url: String, response: HttpResponse): Option[T] = {
        response.status match {
          case OK => response.json.validate[T] match {
            case JsSuccess(value, _) => Some(value)
            case _ => throw new Exception ("Invalid Json for getSubscriptionDetailsHttpReads")
          }
          case NO_CONTENT => None
          case status => throw new Exception (s"Unexpected status: $status")
        }
      }
    }

}
