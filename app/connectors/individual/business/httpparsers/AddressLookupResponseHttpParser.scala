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

package connectors.individual.business.httpparsers

import models.individual.business.address._
import play.api.Logger
import play.api.http.HeaderNames
import play.api.http.Status.{ACCEPTED, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object AddressLookupResponseHttpParser {

  type InitAddressLookupResponseResponse = Either[AddressLookupInitFailureResponse, String]

  implicit object InitAddressLookupHttpReads extends HttpReads[InitAddressLookupResponseResponse] {
    override def read(method: String, url: String, response: HttpResponse): InitAddressLookupResponseResponse = {
      response.status match {
        case ACCEPTED =>
          Logger.debug(s"[AddressLookupResponseHttpParser][InitAddressLookupHttpReads] successful, returned status: $ACCEPTED")
          Right(response.header(HeaderNames.LOCATION).fold("")(identity))
        case status =>
          Logger.warn(s"[AddressLookupResponseHttpParser][InitAddressLookupHttpReads] failure, returned status: $status")
          Left(AddressLookupInitFailureResponse(status))
      }
    }
  }

  type ConfirmAddressLookupResponseResponse = Either[ReturnedAddressFailure, ReturnedAddress]

  implicit object ConfirmAddressLookupHttpReads extends HttpReads[ConfirmAddressLookupResponseResponse] {
    override def read(method: String, url: String, response: HttpResponse): ConfirmAddressLookupResponseResponse = {
      response.status match {
        case OK => Json.fromJson[ReturnedAddress](response.json).asOpt match {
          case Some(address) =>
            Logger.debug(s"[AddressLookupResponseHttpParser][ConfirmAddressLookupHttpReads] successful, returned $OK")
            Right(address)
          case _ =>
            Logger.warn(s"[AddressLookupResponseHttpParser][ConfirmAddressLookupHttpReads] failure, returned malformated address")
            Left(MalformatAddressReturned)
        }
        case status =>
          Logger.warn(s"[AddressLookupResponseHttpParser][ConfirmAddressLookupHttpReads] failure, returned status: $status")
          Left(UnexpectedStatusReturned(status))
      }
    }
  }

}
