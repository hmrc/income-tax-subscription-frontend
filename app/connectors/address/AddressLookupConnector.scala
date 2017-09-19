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

package connectors.address

import javax.inject.{Inject, Singleton}

import audit.Logging
import config.AppConfig
import connectors.httpparsers.AddressLookupResponseHttpParser._
import connectors.models.address.{AddressLookupFailureResponse, AddressLookupRequest}
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class AddressLookupConnector @Inject()(appConfig: AppConfig,
                                       httpPost: HttpPost,
                                       logging: Logging) {

  lazy val initUrl = appConfig.addressLookupFrontendURL + AddressLookupConnector.initUri

  def init(initAddressLookup: AddressLookupRequest)(implicit hc:HeaderCarrier): Future[InitAddressLookupResponseResponse] =
    httpPost.POST[AddressLookupRequest, InitAddressLookupResponseResponse](initUrl, initAddressLookup).map {
      case r@Right(_) =>
        logging.debug("AddressLookupConnector.init successful, returned OK")
        r
      case l@Left(AddressLookupFailureResponse(status)) =>
        logging.warn("AddressLookupConnector.init failure, status=" + status)
        l
    }

}


object AddressLookupConnector {
  lazy val initUri = "/api/init"
}
