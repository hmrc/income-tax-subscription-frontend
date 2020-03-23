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

import config.AppConfig
import connectors.individual.business.httpparsers.AddressLookupResponseHttpParser._
import javax.inject.{Inject, Singleton}
import models.individual.business.address.AddressLookupInitRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressLookupConnector @Inject()(appConfig: AppConfig,
                                       http: HttpClient)
                                      (implicit ec: ExecutionContext) {

  lazy val initUrl: String = appConfig.addressLookupFrontendURL + AddressLookupConnector.initUri

  def retrieveAddressUrl(id: String): String = s"${appConfig.addressLookupFrontendURL}${AddressLookupConnector.fetchAddressUri}?id=$id"

  def init(initAddressLookup: AddressLookupInitRequest)(implicit hc: HeaderCarrier): Future[InitAddressLookupResponseResponse] = {
    http.POST[AddressLookupInitRequest, InitAddressLookupResponseResponse](initUrl, initAddressLookup)
  }

  def retrieveAddress(journeyId: String)(implicit hc: HeaderCarrier): Future[ConfirmAddressLookupResponseResponse] = {
    http.GET[ConfirmAddressLookupResponseResponse](retrieveAddressUrl(journeyId))
  }

}


object AddressLookupConnector {
  lazy val initUri = "/api/init"
  lazy val fetchAddressUri = "/api/confirmed"
}
