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

package connectors

import config.{AddressLookupConfig, AppConfig}
import connectors.httpparser.addresslookup.GetAddressLookupDetailsHttpParser._
import connectors.httpparser.addresslookup.PostAddressLookupHttpParser._
import javax.inject.Inject
import play.api.i18n.Lang
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class AddressLookupConnector @Inject()(appConfig: AppConfig,
                                       addressLookupConfig: AddressLookupConfig,
                                       http: HttpClient)(implicit ec: ExecutionContext) {

  def addressLookupInitializeUrl: String = {
    s"${appConfig.addressLookupUrl}/api/v2/init"
  }

  def getAddressDetailsUrl(id: String): String = {
    s"${appConfig.addressLookupUrl}/api/v2/confirmed?id=$id"
  }

  def initialiseAddressLookup(continueUrl: String)
                             (implicit hc: HeaderCarrier, language: Lang): Future[PostAddressLookupResponse] = {
    http.POST[JsValue, PostAddressLookupResponse](addressLookupInitializeUrl,
      Json.parse(addressLookupConfig.config(continueUrl)))
  }

  def getAddressDetails(id: String)(implicit hc: HeaderCarrier): Future[GetAddressLookupDetailsResponse] = {
    http.GET[GetAddressLookupDetailsResponse](getAddressDetailsUrl(id))
  }

}
