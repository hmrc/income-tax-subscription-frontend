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

package connectors.httpparser.addresslookup

import models.individual.business.BusinessAddressModel
import play.api.http.Status._
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{HttpReads, HttpResponse}


object GetAddressLookupDetailsHttpParser {

  type GetAddressLookupDetailsResponse = Either[GetAddressLookupDetailsFailure, Option[BusinessAddressModel]]

  implicit def getAddressLookupDetailsHttpReads: HttpReads[GetAddressLookupDetailsResponse] =
    new HttpReads[GetAddressLookupDetailsResponse] {
      override def read(method: String, url: String, response: HttpResponse): GetAddressLookupDetailsResponse = {
        response.status match {
          case OK => response.json.validate[BusinessAddressModel] match {
            case JsSuccess(value, _) => Right(Some(value))
            case _ => Left(InvalidJson)
          }
          case NOT_FOUND => Right(None)
          case status => Left(UnexpectedStatusFailure(status))
        }
      }
    }

  sealed trait GetAddressLookupDetailsFailure
  case object InvalidJson extends GetAddressLookupDetailsFailure
  case class UnexpectedStatusFailure(status: Int) extends GetAddressLookupDetailsFailure
}
