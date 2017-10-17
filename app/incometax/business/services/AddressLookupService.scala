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

package incometax.business.services

import javax.inject.{Inject, Singleton}

import incometax.business.connectors.AddressLookupConnector
import incometax.business.httpparsers.AddressLookupResponseHttpParser.{ConfirmAddressLookupResponseResponse, InitAddressLookupResponseResponse}
import incometax.business.models.address.AddressLookupInitRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class AddressLookupService @Inject()(addressLookupConnector: AddressLookupConnector) {

  def init(initAddressLookup: AddressLookupInitRequest)(implicit hc: HeaderCarrier): Future[InitAddressLookupResponseResponse] =
    addressLookupConnector.init(initAddressLookup)

  def retrieveAddress(journeyId: String)(implicit hc: HeaderCarrier): Future[ConfirmAddressLookupResponseResponse] =
    addressLookupConnector.retrieveAddress(journeyId)

}
