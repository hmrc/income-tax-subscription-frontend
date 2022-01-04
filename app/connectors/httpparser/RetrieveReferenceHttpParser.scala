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

package connectors.httpparser

import play.api.http.Status._
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{HttpReads, HttpResponse}


object RetrieveReferenceHttpParser {

  type RetrieveReferenceResponse = Either[RetrieveReferenceFailure, String]

  implicit def retrieveReferenceHttpReads: HttpReads[RetrieveReferenceResponse] =
    (_: String, _: String, response: HttpResponse) => {
      response.status match {
        case OK => (response.json \ "reference").validate[String] match {
          case JsSuccess(value, _) => Right(value)
          case _ => Left(InvalidJsonFailure)
        }
        case status => Left(UnexpectedStatusFailure(status))
      }
    }

  sealed trait RetrieveReferenceFailure

  case object InvalidJsonFailure extends RetrieveReferenceFailure

  case class UnexpectedStatusFailure(status: Int) extends RetrieveReferenceFailure

}
