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

package connectors.individual.business

import core.audit.Logging
import core.config.AppConfig
import connectors.individual.business.httpparsers.AddressLookupResponseHttpParser._
import incometax.business.models.address.{AddressLookupInitFailureResponse, AddressLookupInitRequest, MalformatAddressReturned, UnexpectedStatusReturned}
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class AddressLookupConnector @Inject()(appConfig: AppConfig,
                                       http: HttpClient,
                                       logging: Logging) {

  lazy val initUrl = appConfig.addressLookupFrontendURL + AddressLookupConnector.initUri

  def retrieveAddressUrl(id: String) = s"${appConfig.addressLookupFrontendURL}${AddressLookupConnector.fetchAddressUri}?id=$id"

  def init(initAddressLookup: AddressLookupInitRequest)(implicit hc: HeaderCarrier): Future[InitAddressLookupResponseResponse] =
    http.POST[AddressLookupInitRequest, InitAddressLookupResponseResponse](initUrl, initAddressLookup).map {
      case r@Right(_) =>
        logging.debug("AddressLookupConnector.init successful, returned OK")
        r
      case l@Left(AddressLookupInitFailureResponse(status)) =>
        logging.warn("AddressLookupConnector.init failure, status=" + status)
        l
    }

  def retrieveAddress(journeyId: String)(implicit hc: HeaderCarrier): Future[ConfirmAddressLookupResponseResponse] =
    http.GET[ConfirmAddressLookupResponseResponse](retrieveAddressUrl(journeyId)).map {
      case r@Right(_) =>
        logging.debug("AddressLookupConnector.fetchAddress successful, returned OK")
        r
      case l@Left(UnexpectedStatusReturned(status)) =>
        logging.warn("AddressLookupConnector.fetchAddress failure, status=" + status)
        l
      case l@Left(MalformatAddressReturned) =>
        logging.warn("AddressLookupConnector.fetchAddress returned malformated address")
        l
    }

}


object AddressLookupConnector {
  lazy val initUri = "/api/init"
  lazy val fetchAddressUri = "/api/confirmed"
}
